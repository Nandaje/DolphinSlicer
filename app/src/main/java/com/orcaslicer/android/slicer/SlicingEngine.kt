package com.orcaslicer.android.slicer

import com.orcaslicer.android.model.BrimType
import com.orcaslicer.android.model.CombingMode
import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.FuzzySkinType
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.IroningType
import com.orcaslicer.android.model.PrintStatistics
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.ProcessProfile
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.model.SeamPosition
import com.orcaslicer.android.model.SliceLayer
import com.orcaslicer.android.model.SliceResult
import com.orcaslicer.android.model.SupportBasePattern
import com.orcaslicer.android.model.SupportInterfacePattern
import com.orcaslicer.android.model.SupportStyle
import com.orcaslicer.android.model.SupportType
import com.orcaslicer.android.model.SurfacePattern
import com.orcaslicer.android.model.ToolpathSegment
import com.orcaslicer.android.model.ToolpathType
import com.orcaslicer.android.model.Triangle
import com.orcaslicer.android.model.Vec2
import com.orcaslicer.android.model.Vec3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object SlicingEngine {

    private data class Segment2D(val a: Vec2, val b: Vec2)

    private data class Interval1D(val start: Float, val end: Float) {
        val length: Float get() = end - start
    }

    private data class LayerContourGeometry(
        val layerIdx: Int,
        val sliceZ: Float,
        val nominalZ: Float,
        val layerHeight: Float,
        val outerContours: List<List<Vec2>>,
        val wallLoopsPerContour: List<List<List<Vec2>>>,
        val infillPolygons: List<List<Vec2>>
    )

    suspend fun sliceScene(
        objects: List<SceneObject>,
        printer: PrinterProfile,
        filament: FilamentProfile,
        process: ProcessProfile,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<SliceResult> = coroutineScope {
        try {
            val visibleObjects = objects.filter { it.visible }
            if (visibleObjects.isEmpty()) {
                return@coroutineScope Result.failure(
                    IllegalArgumentException("Add or enable at least one 3D model on the build plate before slicing.")
                )
            }

            onProgress(0.05f, "Transforming meshes to build plate...")
            val allTriangles = ArrayList<Triangle>()
            var maxZ = 0f
            for (obj in visibleObjects) {
                val worldTris = obj.worldTriangles()
                for (t in worldTris) {
                    allTriangles.add(t)
                    maxZ = max(maxZ, max(t.v0.z, max(t.v1.z, t.v2.z)))
                }
            }

            if (allTriangles.isEmpty() || maxZ <= 0.05f) {
                return@coroutineScope Result.failure(
                    IllegalArgumentException("Selected models have zero height or no triangles.")
                )
            }

            val layerHeight = process.layerHeightMm.coerceIn(0.05f, 0.60f)
            val firstLayerHeight = process.initialLayerHeightMm.coerceIn(0.08f, 0.60f)

            // Compute exact layer count where every layer's mid-plane sliceZ is strictly inside [0, maxZ)
            val additionalLayers = if (maxZ > firstLayerHeight) {
                floor(((maxZ - firstLayerHeight) / layerHeight) + 0.5f - 1e-4f).toInt().coerceAtLeast(0)
            } else {
                0
            }
            val totalLayers = (1 + additionalLayers).coerceIn(1, 4000)

            onProgress(0.15f, "Pass 1/2: Slicing $totalLayers layer contours & perimeters...")

            val cpuParallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            val chunkSize = max(1, (totalLayers + cpuParallelism - 1) / cpuParallelism)

            // PASS 1: Compute 2D contours, wall loops, and infill boundary polygons for all layers in parallel
            val geometryChunks = (0 until totalLayers step chunkSize).map { startIdx ->
                val endIdx = min(totalLayers, startIdx + chunkSize)
                async(Dispatchers.Default) {
                    val list = ArrayList<LayerContourGeometry>(endIdx - startIdx)
                    for (layerIdx in startIdx until endIdx) {
                        val rawZ = if (layerIdx == 0) {
                            firstLayerHeight * 0.5f
                        } else {
                            firstLayerHeight + (layerIdx - 0.5f) * layerHeight
                        }
                        val safeSliceZ = rawZ.coerceIn(0.02f, (maxZ - 0.02f).coerceAtLeast(0.02f))
                        val nominalZ = if (layerIdx == 0) {
                            firstLayerHeight
                        } else {
                            min(maxZ, firstLayerHeight + layerIdx * layerHeight)
                        }
                        val lh = if (layerIdx == 0) firstLayerHeight else layerHeight

                        list.add(
                            computeLayerContourGeometry(
                                layerIdx = layerIdx,
                                sliceZ = safeSliceZ,
                                nominalZ = nominalZ,
                                layerHeight = lh,
                                triangles = allTriangles,
                                process = process
                            )
                        )
                    }
                    list
                }
            }

            val allGeometries = geometryChunks.awaitAll().flatten().sortedBy { it.layerIdx }

            // Compute OrcaSlicer Normal or Tree (Organic) Support Plans across all layers if support is enabled
            val layerSupportPlans: List<LayerSupportPlan> = if (process.enableSupport) {
                onProgress(0.38f, "Generating OrcaSlicer ${process.supportType.label} structures...")
                if (process.supportType == SupportType.TREE_ORGANIC || process.supportType == SupportType.TREE_MANUAL) {
                    computeTreeSupportPlans(allTriangles, allGeometries, process)
                } else {
                    computeNormalSupportPlans(allTriangles, allGeometries, process)
                }
            } else {
                List(totalLayers) { LayerSupportPlan.EMPTY }
            }

            onProgress(0.48f, "Pass 2/2: Generating Top/Bottom surfaces, solid shells & infill...")

            // PASS 2: Generate toolpaths for every layer using multi-layer region classification
            // so Top Surfaces, Bottom Surfaces, Internal Solid Shells, and Sparse Infill are 100% accurate at every Z height
            val layerChunks = (0 until totalLayers step chunkSize).map { startIdx ->
                val endIdx = min(totalLayers, startIdx + chunkSize)
                async(Dispatchers.Default) {
                    val chunkLayers = ArrayList<SliceLayer>(endIdx - startIdx)
                    for (layerIdx in startIdx until endIdx) {
                        chunkLayers.add(
                            buildLayerToolpaths(
                                layerIdx = layerIdx,
                                totalLayers = totalLayers,
                                allGeometries = allGeometries,
                                supportPlan = layerSupportPlans.getOrElse(layerIdx) { LayerSupportPlan.EMPTY },
                                filament = filament,
                                process = process
                            )
                        )
                    }
                    chunkLayers
                }
            }

            val slicedLayers = layerChunks.awaitAll().flatten().sortedBy { it.index }
            onProgress(0.82f, "Generating G-code & calculating print statistics...")

            val (gcode, stats) = generateGCodeAndStats(
                layers = slicedLayers,
                printer = printer,
                filament = filament,
                process = process
            )

            onProgress(1.0f, "Slicing complete (${stats.layerCount} layers, ${stats.formattedTotalTime})")
            Result.success(SliceResult(slicedLayers, stats, gcode))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Slicing error: ${e.message}", e))
        }
    }

    private fun computeLayerContourGeometry(
        layerIdx: Int,
        sliceZ: Float,
        nominalZ: Float,
        layerHeight: Float,
        triangles: List<Triangle>,
        process: ProcessProfile
    ): LayerContourGeometry {
        val rawSegments = intersectTrianglesAtZ(triangles, sliceZ)
        val rawContours = chainSegmentsIntoPolygons(rawSegments)

        val isInitialLayer = layerIdx == 0
        val elephantFootInset = if (isInitialLayer && process.elephantFootCompensationMm > 0.01f) {
            process.elephantFootCompensationMm.coerceIn(0f, 0.35f)
        } else {
            0f
        }

        val outerWallWidth = if (isInitialLayer) {
            process.initialLayerLineWidthMm.coerceIn(0.25f, 1.0f)
        } else {
            process.outerWallLineWidthMm.coerceIn(0.25f, 1.0f)
        }
        val innerWallWidth = process.innerWallLineWidthMm.coerceIn(0.25f, 1.0f)

        val wallCount = when {
            process.spiralVaseMode -> 1
            isInitialLayer && process.onlyOneWallFirstLayer -> 1
            else -> process.wallLoops.coerceIn(1, 12)
        }

        val outerContours = ArrayList<List<Vec2>>()
        val wallLoopsPerContour = ArrayList<List<List<Vec2>>>()
        val infillPolygons = ArrayList<List<Vec2>>()

        for (rawContour in rawContours) {
            if (rawContour.size < 3) continue
            val baseContour = if (elephantFootInset > 0.01f) {
                offsetPolygonInward(rawContour, elephantFootInset).ifEmpty { rawContour }
            } else {
                rawContour
            }
            outerContours.add(baseContour)

            // Inset outer wall centerline by half outerWallWidth so outer surface matches CAD dimensions
            val firstWallCenterline = offsetPolygonInward(baseContour, outerWallWidth * 0.45f).ifEmpty { baseContour }
            val wallsForThisContour = ArrayList<List<Vec2>>(wallCount)
            wallsForThisContour.add(firstWallCenterline)

            var currentRing = firstWallCenterline
            for (w in 1 until wallCount) {
                val stepInset = if (w == 1) (outerWallWidth + innerWallWidth) * 0.48f else innerWallWidth * 0.95f
                val inset = offsetPolygonInward(currentRing, stepInset)
                if (inset.size < 3) break
                wallsForThisContour.add(inset)
                currentRing = inset
            }
            wallLoopsPerContour.add(wallsForThisContour)

            if (!process.spiralVaseMode) {
                // Apply infill-wall overlap (e.g. 15..25%) so top/bottom skin and infill fuse solidly into the inner wall
                val overlapFraction = (process.infillWallOverlapPercent.coerceIn(0, 60) / 100f)
                val infillInset = (innerWallWidth * (0.65f - overlapFraction)).coerceAtLeast(0.05f)
                val infillPoly = offsetPolygonInward(currentRing, infillInset)
                if (infillPoly.size >= 3) {
                    infillPolygons.add(infillPoly)
                } else if (currentRing.size >= 3) {
                    infillPolygons.add(currentRing)
                }
            }
        }

        return LayerContourGeometry(
            layerIdx = layerIdx,
            sliceZ = sliceZ,
            nominalZ = nominalZ,
            layerHeight = layerHeight,
            outerContours = outerContours,
            wallLoopsPerContour = wallLoopsPerContour,
            infillPolygons = infillPolygons
        )
    }

    private fun buildLayerToolpaths(
        layerIdx: Int,
        totalLayers: Int,
        allGeometries: List<LayerContourGeometry>,
        supportPlan: LayerSupportPlan,
        filament: FilamentProfile,
        process: ProcessProfile
    ): SliceLayer {
        val geom = allGeometries[layerIdx]
        val nominalZ = geom.nominalZ
        val layerHeight = geom.layerHeight
        val contours = geom.outerContours
        val segments = ArrayList<ToolpathSegment>()

        val filamentArea = PI.toFloat() * (filament.diameterMm * 0.5f) * (filament.diameterMm * 0.5f)
        val isInitialLayer = layerIdx == 0
        val speedScale = if (isInitialLayer) 0.55f else 1.0f
        var lastNozzlePos: Vec3? = null

        val outerWallWidth = if (isInitialLayer) {
            process.initialLayerLineWidthMm.coerceIn(0.25f, 1.0f)
        } else {
            process.outerWallLineWidthMm.coerceIn(0.25f, 1.0f)
        }
        val innerWallWidth = process.innerWallLineWidthMm.coerceIn(0.25f, 1.0f)
        val topSurfaceWidth = process.topSurfaceLineWidthMm.coerceIn(0.25f, 1.0f)
        val solidInfillWidth = if (isInitialLayer) {
            process.initialLayerLineWidthMm.coerceIn(0.25f, 1.0f)
        } else {
            process.internalSolidLineWidthMm.coerceIn(0.25f, 1.0f)
        }
        val sparseInfillWidth = process.sparseInfillLineWidthMm.coerceIn(0.25f, 1.0f)
        val supportWidth = process.supportLineWidthMm.coerceIn(0.25f, 1.0f)

        val bottomShellCountForComb = process.bottomShellLayers.coerceIn(0, 50)
        val topShellCountForComb = process.topShellLayers.coerceIn(0, 50)
        val isPureSkinLayer = (layerIdx < bottomShellCountForComb) || (layerIdx >= totalLayers - topShellCountForComb)

        // Build allowed combing boundary polygons based on Cura Combing Mode
        val combingAllowedPolygons: List<List<Vec2>> = when (process.combingMode) {
            CombingMode.OFF -> emptyList()
            CombingMode.ALL -> geom.outerContours.map { offsetPolygonInward(it, outerWallWidth * 0.25f).ifEmpty { it } }
            CombingMode.NOT_ON_OUTER_SURFACE -> geom.wallLoopsPerContour.mapNotNull { loops ->
                loops.getOrNull(1) ?: loops.firstOrNull()
            }
            CombingMode.NOT_IN_SKIN -> if (isPureSkinLayer) {
                emptyList()
            } else {
                geom.infillPolygons
            }
            CombingMode.WITHIN_INFILL -> if (isPureSkinLayer) {
                emptyList()
            } else {
                geom.infillPolygons
            }
        }

        // Build travel obstacle polygons when "Avoid printed parts / supports when traveling" is enabled
        val travelObstaclePolygons: List<List<Vec2>> = if (process.combingMode != CombingMode.OFF &&
            (process.avoidPrintedPartsWhenTraveling || process.avoidSupportsWhenTraveling)
        ) {
            val avoidDist = process.travelAvoidDistanceMm.coerceIn(0.1f, 5.0f)
            val obs = ArrayList<List<Vec2>>()
            if (process.avoidPrintedPartsWhenTraveling) {
                for (c in geom.outerContours) {
                    obs.add(offsetPolygonOutward(c, avoidDist).ifEmpty { c })
                }
            }
            if (process.avoidSupportsWhenTraveling) {
                for (sLoop in supportPlan.closedLoops) {
                    obs.add(offsetPolygonOutward(sLoop, avoidDist).ifEmpty { sLoop })
                }
            }
            obs
        } else {
            emptyList()
        }

        fun appendPath(
            points: List<Vec2>,
            type: ToolpathType,
            speedMmS: Float,
            lineWidthMm: Float,
            closed: Boolean,
            flowMultiplier: Float = 1.0f
        ) {
            if (points.size < 2) return
            val orderedPts = if (closed && points.size >= 3 && (type == ToolpathType.OUTER_WALL || type == ToolpathType.INNER_WALL)) {
                val refPt = if (process.seamPosition == SeamPosition.NEAREST && lastNozzlePos != null) {
                    Vec2(lastNozzlePos!!.x, lastNozzlePos!!.y)
                } else {
                    Vec2(process.layerStartXMm, process.layerStartYMm)
                }
                alignClosedPolygonStart(points, refPt)
            } else {
                points
            }

            val extrusionPerMm = ((lineWidthMm * layerHeight) / filamentArea) * filament.flowRatio * flowMultiplier
            val first3D = Vec3(orderedPts[0].x, orderedPts[0].y, nominalZ)
            lastNozzlePos?.let { prev ->
                if (prev.distanceTo(first3D) > lineWidthMm * 1.6f) {
                    val combedTravel = computeCuraCombedTravel(
                        start = Vec2(prev.x, prev.y),
                        end = Vec2(first3D.x, first3D.y),
                        combingPolygons = combingAllowedPolygons,
                        obstaclePolygons = travelObstaclePolygons,
                        targetType = type,
                        lineWidthMm = lineWidthMm,
                        process = process
                    )
                    val wps = combedTravel.waypoints
                    for (wIdx in 0 until wps.size - 1) {
                        val wStart = Vec3(wps[wIdx].x, wps[wIdx].y, nominalZ)
                        val wEnd = Vec3(wps[wIdx + 1].x, wps[wIdx + 1].y, nominalZ)
                        if (wStart.distanceTo(wEnd) > 0.01f) {
                            segments.add(
                                ToolpathSegment(
                                    start = wStart,
                                    end = wEnd,
                                    type = ToolpathType.TRAVEL,
                                    extrusionMm = 0f,
                                    speedMmS = process.travelSpeedMmS,
                                    layerIndex = layerIdx,
                                    widthMm = 0.18f,
                                    isCombed = combedTravel.isCombed,
                                    requiresRetract = combedTravel.requiresRetract
                                )
                            )
                        }
                    }
                }
            }

            for (i in 0 until orderedPts.size - 1) {
                val p0 = Vec3(orderedPts[i].x, orderedPts[i].y, nominalZ)
                val p1 = Vec3(orderedPts[i + 1].x, orderedPts[i + 1].y, nominalZ)
                val len = p0.distanceTo(p1)
                if (len > 0.015f) {
                    segments.add(
                        ToolpathSegment(
                            start = p0,
                            end = p1,
                            type = type,
                            extrusionMm = len * extrusionPerMm,
                            speedMmS = (speedMmS * speedScale).coerceAtLeast(8f),
                            layerIndex = layerIdx,
                            widthMm = lineWidthMm
                        )
                    )
                    lastNozzlePos = p1
                }
            }

            if (closed && orderedPts.size >= 3) {
                val pLast = Vec3(orderedPts.last().x, orderedPts.last().y, nominalZ)
                val pFirst = Vec3(orderedPts.first().x, orderedPts.first().y, nominalZ)
                val len = pLast.distanceTo(pFirst)
                if (len > 0.015f) {
                    segments.add(
                        ToolpathSegment(
                            start = pLast,
                            end = pFirst,
                            type = type,
                            extrusionMm = len * extrusionPerMm,
                            speedMmS = (speedMmS * speedScale).coerceAtLeast(8f),
                            layerIndex = layerIdx,
                            widthMm = lineWidthMm
                        )
                    )
                    lastNozzlePos = pFirst
                }
            }
        }

        // 1. Skirt / Brim on Layer 0
        if (isInitialLayer && process.brimType != BrimType.NONE && contours.isNotEmpty()) {
            val bounds = computeContoursBounds(contours)
            val loops = if (process.brimType == BrimType.SKIRT) {
                process.skirtLoops.coerceIn(1, 5)
            } else {
                max(2, (process.brimWidthMm / outerWallWidth).toInt())
            }
            val startGap = if (process.brimType == BrimType.SKIRT) {
                process.skirtDistanceMm.coerceAtLeast(1.5f)
            } else {
                process.brimObjectGapMm.coerceAtLeast(outerWallWidth * 0.4f)
            }
            for (bIdx in 0 until loops) {
                val pad = startGap + bIdx * outerWallWidth * 0.95f
                val loopPts = createRoundedRectLoop(
                    minX = bounds.first.x - pad,
                    minY = bounds.first.y - pad,
                    maxX = bounds.second.x + pad,
                    maxY = bounds.second.y + pad
                )
                appendPath(loopPts, ToolpathType.SKIRT_BRIM, process.initialLayerSpeedMmS, outerWallWidth, closed = true)
            }
        }

        // 2. OrcaSlicer Support structures on this layer (Normal Grid/Snug Support OR Organic Tree Support)
        if (process.enableSupport && !supportPlan.isEmpty) {
            for (loop in supportPlan.closedLoops) {
                appendPath(
                    points = loop,
                    type = ToolpathType.SUPPORT,
                    speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.supportSpeedMmS,
                    lineWidthMm = supportWidth,
                    closed = true
                )
            }
            for (poly in supportPlan.openBasePolylines) {
                appendPath(
                    points = poly,
                    type = ToolpathType.SUPPORT,
                    speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.supportSpeedMmS,
                    lineWidthMm = supportWidth,
                    closed = false
                )
            }
            for (iface in supportPlan.interfacePolylines) {
                appendPath(
                    points = iface,
                    type = ToolpathType.SUPPORT,
                    speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.supportInterfaceSpeedMmS,
                    lineWidthMm = supportWidth,
                    closed = false
                )
            }
            for (ifaceLoop in supportPlan.interfaceClosedLoops) {
                appendPath(
                    points = ifaceLoop,
                    type = ToolpathType.SUPPORT,
                    speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.supportInterfaceSpeedMmS,
                    lineWidthMm = supportWidth,
                    closed = true
                )
            }
        }

        // 3. Perimeters (Outer Wall + Inner Walls)
        for (walls in geom.wallLoopsPerContour) {
            if (walls.isEmpty()) continue
            val outerWall = walls[0]
            val decoratedOuter = if (process.fuzzySkinType != FuzzySkinType.NONE && !isInitialLayer) {
                applyFuzzySkinToPolygon(outerWall, layerIdx, process.fuzzySkinPointDistMm, process.fuzzySkinThicknessMm)
            } else {
                outerWall
            }
            appendPath(
                points = decoratedOuter,
                type = ToolpathType.OUTER_WALL,
                speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.outerWallSpeedMmS,
                lineWidthMm = outerWallWidth,
                closed = true
            )

            for (w in 1 until walls.size) {
                appendPath(
                    points = walls[w],
                    type = ToolpathType.INNER_WALL,
                    speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.innerWallSpeedMmS,
                    lineWidthMm = innerWallWidth,
                    closed = true
                )
            }
        }

        // 4. Multi-Layer Region-Aware Bottom Surface, Top Surface, Internal Solid Infill, and Sparse Infill
        val infillPolygons = geom.infillPolygons
        if (!process.spiralVaseMode && infillPolygons.isNotEmpty()) {
            val bottomShellCount = process.bottomShellLayers.coerceIn(0, 50)
            val topShellCount = process.topShellLayers.coerceIn(0, 50)

            val baseDir = process.infillDirectionDeg.toFloat()
            val solidAngleDeg = if (layerIdx % 2 == 0) baseDir else baseDir + 90f

            // Dense spacing (0.94x line width) guarantees adjacent skin extrusions fuse with zero gaps
            val skinLineWidth = if (isInitialLayer) solidInfillWidth else topSurfaceWidth
            val solidSpacing = (skinLineWidth * 0.94f).coerceAtLeast(0.22f)

            // Special handling if user chose Concentric pattern on pure bottom or pure top layer
            val isPureBottomConcentric = isInitialLayer && bottomShellCount > 0 &&
                process.bottomSurfacePattern == SurfacePattern.CONCENTRIC
            val isPureTopConcentric = (layerIdx == totalLayers - 1) && topShellCount > 0 &&
                process.topSurfacePattern == SurfacePattern.CONCENTRIC

            if (isPureBottomConcentric) {
                val rings = generateConcentricRings(infillPolygons, solidSpacing)
                for (ring in rings) {
                    appendPath(ring, ToolpathType.BOTTOM_SURFACE, process.initialLayerInfillSpeedMmS, skinLineWidth, closed = true)
                }
            } else if (isPureTopConcentric) {
                val rings = generateConcentricRings(infillPolygons, solidSpacing)
                for (ring in rings) {
                    appendPath(ring, ToolpathType.TOP_SURFACE, process.topSurfaceSpeedMmS, topSurfaceWidth, closed = true)
                }
            } else {
                // Region-based scanline classification across adjacent layers:
                // Splits every scanline into:
                //  - BOTTOM_SURFACE (exposed floor on Layer 0 or bridge/overhang not covered by layerIdx - 1)
                //  - TOP_SURFACE (exposed roof on top layer or intermediate horizontal step not covered by layerIdx + 1)
                //  - SOLID_INFILL (internal solid bottom/top shell layers within bottomShellLayers or topShellLayers)
                val rad = solidAngleDeg * (PI.toFloat() / 180f)
                val cosA = cos(rad)
                val sinA = sin(rad)

                fun rotatePolys(polys: List<List<Vec2>>): List<List<Vec2>> =
                    polys.map { poly ->
                        poly.map { p -> Vec2(p.x * cosA + p.y * sinA, -p.x * sinA + p.y * cosA) }
                    }

                fun unrotate(rx: Float, ry: Float): Vec2 =
                    Vec2(rx * cosA - ry * sinA, rx * sinA + ry * cosA)

                val rotatedCurrentInfill = rotatePolys(infillPolygons)

                // Gather rotated outer contours of neighbor layers below & above for exact exposure testing
                val rotatedBelowImmediate = if (layerIdx > 0) {
                    rotatePolys(allGeometries[layerIdx - 1].outerContours)
                } else emptyList()

                val rotatedAboveImmediate = if (layerIdx + 1 < totalLayers) {
                    rotatePolys(allGeometries[layerIdx + 1].outerContours)
                } else emptyList()

                val belowShellRange = max(0, layerIdx - bottomShellCount + 1) until layerIdx
                val rotatedBelowShellLayers = belowShellRange.map { idx ->
                    rotatePolys(allGeometries[idx].outerContours)
                }

                val aboveShellRange = (layerIdx + 1)..min(totalLayers - 1, layerIdx + topShellCount - 1)
                val rotatedAboveShellLayers = aboveShellRange.map { idx ->
                    rotatePolys(allGeometries[idx].outerContours)
                }

                val bounds = computeContoursBounds(rotatedCurrentInfill)
                val minY = bounds.first.y + solidSpacing * 0.25f
                val maxY = bounds.second.y - solidSpacing * 0.25f

                val bottomSurfaceLines = ArrayList<List<Vec2>>()
                val topSurfaceLines = ArrayList<List<Vec2>>()
                val solidInfillLines = ArrayList<List<Vec2>>()

                if (maxY >= minY) {
                    var y = minY
                    var rowIdx = 0
                    while (y <= maxY + 1e-4f) {
                        val currentIntervals = scanlineIntervals(rotatedCurrentInfill, y)
                        if (currentIntervals.isNotEmpty()) {
                            // 1. Check Bottom Surface exposure (not covered by layerIdx - 1)
                            val bottomIntervals = if (bottomShellCount > 0) {
                                if (layerIdx == 0) {
                                    currentIntervals
                                } else {
                                    val belowCov = expandIntervals(scanlineIntervals(rotatedBelowImmediate, y), skinLineWidth * 0.6f)
                                    subtractIntervals(currentIntervals, belowCov)
                                }
                            } else emptyList()

                            val afterBottom = subtractIntervals(currentIntervals, bottomIntervals)

                            // 2. Check Top Surface exposure (not covered by layerIdx + 1)
                            val topIntervals = if (topShellCount > 0 && afterBottom.isNotEmpty()) {
                                if (layerIdx == totalLayers - 1) {
                                    afterBottom
                                } else {
                                    val aboveCov = expandIntervals(scanlineIntervals(rotatedAboveImmediate, y), skinLineWidth * 0.6f)
                                    subtractIntervals(afterBottom, aboveCov)
                                }
                            } else emptyList()

                            val afterTopAndBottom = subtractIntervals(afterBottom, topIntervals)

                            // 3. Check Internal Solid Shell (within bottomShellLayers of a floor OR within topShellLayers of a roof)
                            val solidIntervals = if (afterTopAndBottom.isNotEmpty()) {
                                var fullyCoveredByBottomShell = afterTopAndBottom
                                if (bottomShellCount > 1) {
                                    if (layerIdx < bottomShellCount) {
                                        fullyCoveredByBottomShell = emptyList()
                                    } else {
                                        for (belowPolys in rotatedBelowShellLayers) {
                                            val cov = expandIntervals(scanlineIntervals(belowPolys, y), skinLineWidth * 0.5f)
                                            fullyCoveredByBottomShell = intersectIntervals(fullyCoveredByBottomShell, cov)
                                            if (fullyCoveredByBottomShell.isEmpty()) break
                                        }
                                    }
                                }

                                var fullyCoveredByTopShell = fullyCoveredByBottomShell
                                if (topShellCount > 1 && fullyCoveredByTopShell.isNotEmpty()) {
                                    if (layerIdx >= totalLayers - topShellCount) {
                                        fullyCoveredByTopShell = emptyList()
                                    } else {
                                        for (abovePolys in rotatedAboveShellLayers) {
                                            val cov = expandIntervals(scanlineIntervals(abovePolys, y), skinLineWidth * 0.5f)
                                            fullyCoveredByTopShell = intersectIntervals(fullyCoveredByTopShell, cov)
                                            if (fullyCoveredByTopShell.isEmpty()) break
                                        }
                                    }
                                }

                                // Anything in afterTopAndBottom that is NOT in the deep interior core is Internal Solid Infill!
                                subtractIntervals(afterTopAndBottom, fullyCoveredByTopShell)
                            } else emptyList()

                            val minSegLen = skinLineWidth * 0.45f
                            for (iv in bottomIntervals) {
                                if (iv.length >= minSegLen) {
                                    val x0 = if (rowIdx % 2 == 0) iv.start else iv.end
                                    val x1 = if (rowIdx % 2 == 0) iv.end else iv.start
                                    bottomSurfaceLines.add(listOf(unrotate(x0, y), unrotate(x1, y)))
                                }
                            }
                            for (iv in topIntervals) {
                                if (iv.length >= minSegLen) {
                                    val x0 = if (rowIdx % 2 == 0) iv.start else iv.end
                                    val x1 = if (rowIdx % 2 == 0) iv.end else iv.start
                                    topSurfaceLines.add(listOf(unrotate(x0, y), unrotate(x1, y)))
                                }
                            }
                            for (iv in solidIntervals) {
                                if (iv.length >= minSegLen) {
                                    val x0 = if (rowIdx % 2 == 0) iv.start else iv.end
                                    val x1 = if (rowIdx % 2 == 0) iv.end else iv.start
                                    solidInfillLines.add(listOf(unrotate(x0, y), unrotate(x1, y)))
                                }
                            }
                        }
                        y += solidSpacing
                        rowIdx++
                    }
                }

                // Emit Bottom Surface paths
                for (line in bottomSurfaceLines) {
                    appendPath(
                        points = line,
                        type = ToolpathType.BOTTOM_SURFACE,
                        speedMmS = if (isInitialLayer) process.initialLayerInfillSpeedMmS else process.topSurfaceSpeedMmS,
                        lineWidthMm = skinLineWidth,
                        closed = false
                    )
                }

                // Emit Internal Solid Infill paths
                for (line in solidInfillLines) {
                    appendPath(
                        points = line,
                        type = ToolpathType.SOLID_INFILL,
                        speedMmS = process.internalSolidInfillSpeedMmS,
                        lineWidthMm = solidInfillWidth,
                        closed = false
                    )
                }

                // Emit Top Surface paths
                for (line in topSurfaceLines) {
                    appendPath(
                        points = line,
                        type = ToolpathType.TOP_SURFACE,
                        speedMmS = process.topSurfaceSpeedMmS,
                        lineWidthMm = topSurfaceWidth,
                        closed = false
                    )
                }

                // Optional Ironing pass over Top Surfaces
                val shouldIron = (process.ironingType == IroningType.ALL_TOP && topSurfaceLines.isNotEmpty()) ||
                    (process.ironingType == IroningType.TOPMOST_ONLY && layerIdx == totalLayers - 1 && topSurfaceLines.isNotEmpty())
                if (shouldIron) {
                    val ironRatio = (process.ironingFlowPercent.coerceIn(5, 50) / 100f)
                    for (line in topSurfaceLines) {
                        appendPath(
                            points = line,
                            type = ToolpathType.TOP_SURFACE,
                            speedMmS = process.ironingSpeedMmS,
                            lineWidthMm = topSurfaceWidth * 0.5f,
                            closed = false,
                            flowMultiplier = ironRatio
                        )
                    }
                }
            }

            // 5. Sparse Infill on deep interior regions (covered by all bottomShellLayers below AND all topShellLayers above)
            val isEntireLayerSolid = (layerIdx < bottomShellCount) || (layerIdx >= totalLayers - topShellCount)
            if (!isEntireLayerSolid && process.sparseInfillDensityPercent > 0) {
                val sparseLines = generateCoreSparseInfill(
                    infillPolygons = infillPolygons,
                    layerIdx = layerIdx,
                    totalLayers = totalLayers,
                    bottomShellCount = bottomShellCount,
                    topShellCount = topShellCount,
                    allGeometries = allGeometries,
                    nominalZ = nominalZ,
                    densityPercent = process.sparseInfillDensityPercent,
                    pattern = process.sparseInfillPattern,
                    lineWidth = sparseInfillWidth
                )
                for (line in sparseLines) {
                    appendPath(
                        points = line,
                        type = ToolpathType.SPARSE_INFILL,
                        speedMmS = process.sparseInfillSpeedMmS,
                        lineWidthMm = sparseInfillWidth,
                        closed = false
                    )
                }
            }
        }

        return SliceLayer(
            index = layerIdx,
            zHeightMm = nominalZ,
            layerHeightMm = layerHeight,
            segments = segments
        )
    }

    private fun generateConcentricRings(polygons: List<List<Vec2>>, step: Float): List<List<Vec2>> {
        val rings = ArrayList<List<Vec2>>()
        val safeStep = step.coerceAtLeast(0.25f)
        for (poly in polygons) {
            var current = poly
            var safety = 0
            while (current.size >= 3 && safety < 64) {
                rings.add(current)
                current = offsetPolygonInward(current, safeStep)
                safety++
            }
        }
        return rings
    }

    private fun generateCoreSparseInfill(
        infillPolygons: List<List<Vec2>>,
        layerIdx: Int,
        totalLayers: Int,
        bottomShellCount: Int,
        topShellCount: Int,
        allGeometries: List<LayerContourGeometry>,
        nominalZ: Float,
        densityPercent: Int,
        pattern: InfillPattern,
        lineWidth: Float
    ): List<List<Vec2>> {
        if (densityPercent <= 0 || infillPolygons.isEmpty()) return emptyList()

        if (pattern == InfillPattern.CONCENTRIC && densityPercent < 100) {
            val rings = ArrayList<List<Vec2>>()
            val step = (lineWidth * 100f / densityPercent.coerceIn(5, 100)).coerceAtLeast(lineWidth)
            for (poly in infillPolygons) {
                var current = poly
                var safety = 0
                while (current.size >= 3 && safety < 24) {
                    rings.add(current + current.first())
                    current = offsetPolygonInward(current, step)
                    safety++
                }
            }
            return rings
        }

        val spacing = if (densityPercent >= 99) {
            lineWidth * 0.96f
        } else {
            (lineWidth * 100f / densityPercent.coerceIn(5, 95)).coerceIn(lineWidth, 16f)
        }

        val anglesDeg = when (pattern) {
            InfillPattern.GRID -> listOf(45f, -45f)
            InfillPattern.ADAPTIVE_CUBIC -> listOf(0f, 60f, 120f)
            InfillPattern.TRIANGLES, InfillPattern.TRI_HEXAGON -> listOf(0f, 60f, 120f)
            InfillPattern.GYROID -> listOf(if (layerIdx % 2 == 0) 0f else 90f)
            InfillPattern.HONEYCOMB -> listOf(if (layerIdx % 2 == 0) 30f else -30f)
            InfillPattern.CROSS_HATCH -> listOf(if (layerIdx % 2 == 0) 22.5f else -67.5f)
            InfillPattern.LIGHTNING -> listOf(90f)
            else -> listOf(if (layerIdx % 2 == 0) 45f else -45f)
        }

        val effectiveSpacing = if (anglesDeg.size > 1 && densityPercent < 99) {
            spacing * anglesDeg.size * 0.65f
        } else {
            spacing
        }

        val belowShellRange = max(0, layerIdx - bottomShellCount + 1) until layerIdx
        val aboveShellRange = (layerIdx + 1)..min(totalLayers - 1, layerIdx + topShellCount - 1)

        val lines = ArrayList<List<Vec2>>()
        for (angleDeg in anglesDeg) {
            val rad = angleDeg * (PI.toFloat() / 180f)
            val cosA = cos(rad)
            val sinA = sin(rad)

            fun rotatePolys(polys: List<List<Vec2>>): List<List<Vec2>> =
                polys.map { poly ->
                    poly.map { p -> Vec2(p.x * cosA + p.y * sinA, -p.x * sinA + p.y * cosA) }
                }

            val rotatedPolys = rotatePolys(infillPolygons)
            val rotatedBelow = belowShellRange.map { idx -> rotatePolys(allGeometries[idx].outerContours) }
            val rotatedAbove = aboveShellRange.map { idx -> rotatePolys(allGeometries[idx].outerContours) }

            val bounds = computeContoursBounds(rotatedPolys)
            val minY = bounds.first.y + effectiveSpacing * 0.3f
            val maxY = bounds.second.y - effectiveSpacing * 0.3f
            if (maxY <= minY) continue

            var y = minY
            var rowIndex = 0
            while (y <= maxY) {
                var coreIntervals = scanlineIntervals(rotatedPolys, y)
                for (bPolys in rotatedBelow) {
                    if (coreIntervals.isEmpty()) break
                    val cov = expandIntervals(scanlineIntervals(bPolys, y), lineWidth * 0.5f)
                    coreIntervals = intersectIntervals(coreIntervals, cov)
                }
                for (aPolys in rotatedAbove) {
                    if (coreIntervals.isEmpty()) break
                    val cov = expandIntervals(scanlineIntervals(aPolys, y), lineWidth * 0.5f)
                    coreIntervals = intersectIntervals(coreIntervals, cov)
                }

                for (iv in coreIntervals) {
                    if (iv.length > lineWidth * 0.8f) {
                        val segmentPoints = buildPatternLine(
                            x0 = if (rowIndex % 2 == 0) iv.start else iv.end,
                            x1 = if (rowIndex % 2 == 0) iv.end else iv.start,
                            y = y,
                            z = nominalZ,
                            pattern = if (densityPercent >= 99) InfillPattern.LINES else pattern,
                            spacing = effectiveSpacing,
                            cosA = cosA,
                            sinA = sinA
                        )
                        lines.add(segmentPoints)
                    }
                }
                y += effectiveSpacing
                rowIndex++
            }
        }
        return lines
    }

    // =========================================================================
    // 1D Scanline Boolean Interval Algebra (Exact Top/Bottom Skin Clipping)
    // =========================================================================

    private fun scanlineIntervals(rotatedPolys: List<List<Vec2>>, y: Float): List<Interval1D> {
        if (rotatedPolys.isEmpty()) return emptyList()
        val xs = ArrayList<Float>()
        for (rPoly in rotatedPolys) {
            val n = rPoly.size
            if (n < 3) continue
            for (i in 0 until n) {
                val a = rPoly[i]
                val b = rPoly[(i + 1) % n]
                if ((a.y <= y && b.y > y) || (b.y <= y && a.y > y)) {
                    val t = (y - a.y) / (b.y - a.y)
                    xs.add(a.x + t * (b.x - a.x))
                }
            }
        }
        if (xs.size < 2) return emptyList()
        xs.sort()

        val raw = ArrayList<Interval1D>(xs.size / 2)
        var k = 0
        while (k + 1 < xs.size) {
            val x0 = xs[k]
            val x1 = xs[k + 1]
            if (x1 - x0 > 1e-3f) {
                raw.add(Interval1D(x0, x1))
            }
            k += 2
        }
        return mergeIntervals(raw)
    }

    private fun mergeIntervals(intervals: List<Interval1D>): List<Interval1D> {
        if (intervals.size <= 1) return intervals
        val sorted = intervals.sortedBy { it.start }
        val out = ArrayList<Interval1D>(sorted.size)
        var cur = sorted[0]
        for (i in 1 until sorted.size) {
            val nxt = sorted[i]
            if (nxt.start <= cur.end + 1e-3f) {
                cur = Interval1D(cur.start, max(cur.end, nxt.end))
            } else {
                out.add(cur)
                cur = nxt
            }
        }
        out.add(cur)
        return out
    }

    private fun expandIntervals(intervals: List<Interval1D>, margin: Float): List<Interval1D> {
        if (intervals.isEmpty() || margin <= 0f) return intervals
        return mergeIntervals(intervals.map { Interval1D(it.start - margin, it.end + margin) })
    }

    private fun subtractIntervals(a: List<Interval1D>, b: List<Interval1D>): List<Interval1D> {
        if (a.isEmpty() || b.isEmpty()) return a
        var current = a
        for (cut in b) {
            val next = ArrayList<Interval1D>()
            for (iv in current) {
                if (cut.end <= iv.start + 1e-4f || cut.start >= iv.end - 1e-4f) {
                    next.add(iv)
                } else {
                    if (cut.start > iv.start + 1e-4f) {
                        next.add(Interval1D(iv.start, cut.start))
                    }
                    if (cut.end < iv.end - 1e-4f) {
                        next.add(Interval1D(cut.end, iv.end))
                    }
                }
            }
            current = next
            if (current.isEmpty()) break
        }
        return current
    }

    private fun intersectIntervals(a: List<Interval1D>, b: List<Interval1D>): List<Interval1D> {
        if (a.isEmpty() || b.isEmpty()) return emptyList()
        val out = ArrayList<Interval1D>()
        for (ia in a) {
            for (ib in b) {
                val s = max(ia.start, ib.start)
                val e = min(ia.end, ib.end)
                if (e - s > 1e-4f) {
                    out.add(Interval1D(s, e))
                }
            }
        }
        return mergeIntervals(out)
    }

    // =========================================================================
    // 1:1 OrcaSlicer Normal Support & Organic Tree Support Engine
    // =========================================================================

    private data class LayerSupportPlan(
        val closedLoops: List<List<Vec2>> = emptyList(),
        val openBasePolylines: List<List<Vec2>> = emptyList(),
        val interfacePolylines: List<List<Vec2>> = emptyList(),
        val interfaceClosedLoops: List<List<Vec2>> = emptyList()
    ) {
        val isEmpty: Boolean
            get() = closedLoops.isEmpty() &&
                openBasePolylines.isEmpty() &&
                interfacePolylines.isEmpty() &&
                interfaceClosedLoops.isEmpty()

        companion object {
            val EMPTY = LayerSupportPlan()
        }
    }

    private fun isPointInPolygon(pt: Vec2, poly: List<Vec2>): Boolean {
        val n = poly.size
        if (n < 3) return false
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val pi = poly[i]
            val pj = poly[j]
            if (((pi.y > pt.y) != (pj.y > pt.y)) &&
                (pt.x < (pj.x - pi.x) * (pt.y - pi.y) / (pj.y - pi.y + 1e-9f) + pi.x)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun isPointInContours(pt: Vec2, contours: List<List<Vec2>>): Boolean {
        var count = 0
        for (c in contours) {
            if (isPointInPolygon(pt, c)) count++
        }
        return (count % 2) == 1
    }

    private fun distancePointToSegment(p: Vec2, a: Vec2, b: Vec2): Pair<Float, Vec2> {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val lenSq = abx * abx + aby * aby
        if (lenSq < 1e-8f) {
            return p.distanceTo(a) to a
        }
        val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lenSq).coerceIn(0f, 1f)
        val closest = Vec2(a.x + abx * t, a.y + aby * t)
        return p.distanceTo(closest) to closest
    }

    /**
     * Returns (signedDistance, closestBoundaryPoint) where:
     * - signedDistance < 0 means `pt` is INSIDE the model contours
     * - signedDistance >= 0 means `pt` is OUTSIDE the model contours
     */
    private fun signedDistanceToContours(pt: Vec2, contours: List<List<Vec2>>): Pair<Float, Vec2> {
        if (contours.isEmpty()) return Float.POSITIVE_INFINITY to pt
        var minDist = Float.POSITIVE_INFINITY
        var nearestPt = pt
        for (poly in contours) {
            val n = poly.size
            if (n < 2) continue
            for (i in 0 until n) {
                val (d, cp) = distancePointToSegment(pt, poly[i], poly[(i + 1) % n])
                if (d < minDist) {
                    minDist = d
                    nearestPt = cp
                }
            }
        }
        val inside = isPointInContours(pt, contours)
        return (if (inside) -minDist else minDist) to nearestPt
    }

    /**
     * Extracts unsupported overhang points on each layer `L` (1 .. totalLayers - 1) by combining:
     * 1. Layer-to-layer contour diff: points inside `allGeometries[L]` that lie outside `allGeometries[L - 1]`
     *    by more than the printable overhang step `layerHeight / tan(thresholdAngle)`.
     * 2. Downward-facing mesh triangles whose surface angle from horizontal is below `supportThresholdAngleDeg`.
     */
    private fun extractOverhangPointsByLayer(
        triangles: List<Triangle>,
        allGeometries: List<LayerContourGeometry>,
        process: ProcessProfile,
        sampleSpacingMm: Float
    ): Array<ArrayList<Vec2>> {
        val totalLayers = allGeometries.size
        val overhangsByLayer = Array(totalLayers) { ArrayList<Vec2>() }
        if (totalLayers <= 1) return overhangsByLayer

        // In OrcaSlicer, supportThresholdAngleDeg (default 30°) is the overhang angle from horizontal
        // (or from vertical for steeper thresholds). Any surface overhanging beyond this step needs support.
        val threshDeg = process.supportThresholdAngleDeg.coerceIn(10, 80)
        val threshRad = threshDeg * (PI.toFloat() / 180f)
        val outerWidth = process.outerWallLineWidthMm.coerceIn(0.25f, 1.0f)

        // 1. Layer-to-Layer unsupported overhang regions
        for (layerIdx in 2 until totalLayers) {
            val geom = allGeometries[layerIdx]
            val prevGeom = allGeometries[layerIdx - 1]
            if (geom.outerContours.isEmpty()) continue

            val selfSupportMargin = geom.layerHeight * (1f / tan(threshRad).coerceAtLeast(0.35f)) + outerWidth * 0.55f
            val bounds = computeContoursBounds(geom.outerContours)
            var y = bounds.first.y + sampleSpacingMm * 0.5f
            while (y <= bounds.second.y - sampleSpacingMm * 0.25f) {
                val curIntervals = scanlineIntervals(geom.outerContours, y)
                val prevIntervals = expandIntervals(scanlineIntervals(prevGeom.outerContours, y), selfSupportMargin)
                val unsupported = subtractIntervals(curIntervals, prevIntervals)
                for (iv in unsupported) {
                    val minLen = if (process.removeSmallOverhangs) outerWidth * 1.1f else outerWidth * 0.4f
                    if (iv.length >= minLen) {
                        var x = iv.start + min(sampleSpacingMm * 0.5f, iv.length * 0.5f)
                        while (x <= iv.end + 1e-3f) {
                            val pt = Vec2(x.coerceIn(iv.start, iv.end), y)
                            // Also verify point is outside prevGeom by at least outerWidth * 0.45f in 2D
                            val (sDistPrev, _) = signedDistanceToContours(pt, prevGeom.outerContours)
                            if (sDistPrev > outerWidth * 0.45f) {
                                overhangsByLayer[layerIdx].add(pt)
                            }
                            x += sampleSpacingMm
                        }
                    }
                }
                y += sampleSpacingMm
            }
        }

        // 2. Downward-facing mesh triangles (catches flat downward roofs, local minima, and steep cantilevers)
        val normalZCutoff = -cos((90 - threshDeg).coerceIn(15, 75) * (PI.toFloat() / 180f))
        val firstLh = allGeometries[0].layerHeight
        val stdLh = if (totalLayers > 1) allGeometries[1].layerHeight else firstLh

        for (tri in triangles) {
            val minZ = min(tri.v0.z, min(tri.v1.z, tri.v2.z))
            if (minZ < firstLh + stdLh * 1.5f) continue
            if (tri.normal.z < normalZCutoff) {
                val minX = min(tri.v0.x, min(tri.v1.x, tri.v2.x))
                val maxX = max(tri.v0.x, max(tri.v1.x, tri.v2.x))
                val minY = min(tri.v0.y, min(tri.v1.y, tri.v2.y))
                val maxY = max(tri.v0.y, max(tri.v1.y, tri.v2.y))
                val avgZ = (tri.v0.z + tri.v1.z + tri.v2.z) / 3f
                val targetLayer = (1 + ((avgZ - firstLh) / stdLh).roundToInt()).coerceIn(2, totalLayers - 1)

                val cx = (tri.v0.x + tri.v1.x + tri.v2.x) / 3f
                val cy = (tri.v0.y + tri.v1.y + tri.v2.y) / 3f
                val prevContours = allGeometries[targetLayer - 1].outerContours
                val (dCenter, _) = signedDistanceToContours(Vec2(cx, cy), prevContours)
                if (dCenter > outerWidth * 0.35f) {
                    overhangsByLayer[targetLayer].add(Vec2(cx, cy))
                }

                // Sample larger overhang triangles across their footprint
                if (maxX - minX > sampleSpacingMm || maxY - minY > sampleSpacingMm) {
                    var sx = minX + sampleSpacingMm * 0.5f
                    while (sx <= maxX) {
                        var sy = minY + sampleSpacingMm * 0.5f
                        while (sy <= maxY) {
                            if (pointInTriangle2D(Vec2(sx, sy), Vec2(tri.v0.x, tri.v0.y), Vec2(tri.v1.x, tri.v1.y), Vec2(tri.v2.x, tri.v2.y))) {
                                val (sd, _) = signedDistanceToContours(Vec2(sx, sy), prevContours)
                                if (sd > outerWidth * 0.35f) {
                                    overhangsByLayer[targetLayer].add(Vec2(sx, sy))
                                }
                            }
                            sy += sampleSpacingMm
                        }
                        sx += sampleSpacingMm
                    }
                }
            }
        }

        return overhangsByLayer
    }

    private fun pointInTriangle2D(p: Vec2, a: Vec2, b: Vec2, c: Vec2): Boolean {
        val d1 = (p.x - b.x) * (a.y - b.y) - (a.x - b.x) * (p.y - b.y)
        val d2 = (p.x - c.x) * (b.y - c.y) - (b.x - c.x) * (p.y - c.y)
        val d3 = (p.x - a.x) * (c.y - a.y) - (c.x - a.x) * (p.y - a.y)
        val hasNeg = (d1 < 0f) || (d2 < 0f) || (d3 < 0f)
        val hasPos = (d1 > 0f) || (d2 > 0f) || (d3 > 0f)
        return !(hasNeg && hasPos)
    }

    /**
     * OrcaSlicer Normal Support (`normal(auto)` / `normal(manual)`):
     * - Projects overhang regions vertically downward from top to bottom
     * - Subtracts model contours expanded by `supportObjectXyDistanceMm` so supports never intersect the part
     * - Filters out supports landing on the model if `supportOnBuildPlateOnly` is enabled
     * - Generates snug perimeter walls, continuous serpentine/accordion rectilinear/grid/honeycomb base lines,
     *   Layer 0 expanded bed adhesion base pad, and dense top/bottom support interface layers.
     */
    private fun computeNormalSupportPlans(
        triangles: List<Triangle>,
        allGeometries: List<LayerContourGeometry>,
        process: ProcessProfile
    ): List<LayerSupportPlan> {
        val totalLayers = allGeometries.size
        if (totalLayers <= 1) return List(totalLayers) { LayerSupportPlan.EMPTY }

        val cellSize = 1.25f
        val overhangsByLayer = extractOverhangPointsByLayer(triangles, allGeometries, process, sampleSpacingMm = cellSize)
        if (overhangsByLayer.all { it.isEmpty() }) {
            return List(totalLayers) { LayerSupportPlan.EMPTY }
        }

        val allOuter = allGeometries.flatMap { it.outerContours }
        if (allOuter.isEmpty()) return List(totalLayers) { LayerSupportPlan.EMPTY }
        val modelBounds = computeContoursBounds(allOuter)
        val pad = max(4.0f, process.supportFirstLayerExpansionMm + 2.5f)
        val minX = modelBounds.first.x - pad
        val minY = modelBounds.first.y - pad
        val maxX = modelBounds.second.x + pad
        val maxY = modelBounds.second.y + pad

        val nx = ((maxX - minX) / cellSize).toInt().coerceIn(8, 260)
        val ny = ((maxY - minY) / cellSize).toInt().coerceIn(8, 260)
        val numCells = nx * ny

        fun cellCenter(ix: Int, iy: Int): Vec2 =
            Vec2(minX + (ix + 0.5f) * cellSize, minY + (iy + 0.5f) * cellSize)

        val stdLh = if (totalLayers > 1) allGeometries[1].layerHeight else allGeometries[0].layerHeight
        val topZGapLayers = max(1, (process.supportTopZDistanceMm / stdLh).roundToInt())
        val bottomZGapLayers = max(1, (process.supportBottomZDistanceMm / stdLh).roundToInt())
        val topInterfaceCount = process.supportTopInterfaceLayers.coerceIn(0, 8)
        val bottomInterfaceCount = process.supportBottomInterfaceLayers.coerceIn(0, 8)
        val xyClearance = process.supportObjectXyDistanceMm.coerceIn(0.15f, 2.0f)

        // Precompute per-layer model blocked mask (inside model or within xyClearance) and strictly-inside-model mask
        val insideModelByLayer = Array(totalLayers) { BooleanArray(numCells) }
        val blockedByModelByLayer = Array(totalLayers) { BooleanArray(numCells) }

        for (layerIdx in 0 until totalLayers) {
            val contours = allGeometries[layerIdx].outerContours
            if (contours.isEmpty()) continue
            val gap = if (layerIdx == 0) max(xyClearance, process.supportObjectFirstLayerGapMm) else xyClearance
            val insideArr = insideModelByLayer[layerIdx]
            val blockedArr = blockedByModelByLayer[layerIdx]
            for (iy in 0 until ny) {
                for (ix in 0 until nx) {
                    val pt = cellCenter(ix, iy)
                    val (sDist, _) = signedDistanceToContours(pt, contours)
                    val idx = iy * nx + ix
                    if (sDist < 0f) {
                        insideArr[idx] = true
                        blockedArr[idx] = true
                    } else if (sDist < gap) {
                        blockedArr[idx] = true
                    }
                }
            }
        }

        // Seed overhang cells per layer
        val seededOverhang = Array(totalLayers) { BooleanArray(numCells) }
        for (layerIdx in 2 until totalLayers) {
            val pts = overhangsByLayer[layerIdx]
            if (pts.isEmpty()) continue
            val startSupportLayer = (layerIdx - topZGapLayers).coerceAtLeast(0)
            val seedArr = seededOverhang[startSupportLayer]
            for (pt in pts) {
                val ix = ((pt.x - minX) / cellSize).toInt().coerceIn(0, nx - 1)
                val iy = ((pt.y - minY) / cellSize).toInt().coerceIn(0, ny - 1)
                seedArr[iy * nx + ix] = true
            }
        }

        // Top-down propagation per column (ix, iy) from totalLayers - 1 down to 0
        // 0 = none, 1 = base support, 2 = interface support
        val supportStateByLayer = Array(totalLayers) { ByteArray(numCells) }

        for (iy in 0 until ny) {
            for (ix in 0 until nx) {
                val cellIdx = iy * nx + ix
                var activeRunLayers = 0
                var runTopLayer = -1

                fun finalizeRun(bottomLayerInclusive: Int, landedOnModel: Boolean) {
                    if (runTopLayer < bottomLayerInclusive) return
                    if (landedOnModel && process.supportOnBuildPlateOnly) {
                        // Discard column segment that landed on the model when "On build plate only" is checked
                        return
                    }
                    val actualBottom = if (landedOnModel) {
                        min(runTopLayer, bottomLayerInclusive + bottomZGapLayers)
                    } else {
                        bottomLayerInclusive
                    }
                    if (runTopLayer < actualBottom) return
                    for (l in actualBottom..runTopLayer) {
                        val distFromTop = runTopLayer - l
                        val distFromBottom = l - actualBottom
                        val isTopIface = distFromTop < topInterfaceCount
                        val isBotIface = landedOnModel && distFromBottom < bottomInterfaceCount
                        supportStateByLayer[l][cellIdx] = if (isTopIface || isBotIface) 2 else 1
                    }
                }

                for (l in (totalLayers - 1) downTo 0) {
                    if (insideModelByLayer[l][cellIdx]) {
                        if (activeRunLayers > 0) {
                            finalizeRun(l + 1, landedOnModel = true)
                            activeRunLayers = 0
                            runTopLayer = -1
                        }
                        continue
                    }

                    if (seededOverhang[l][cellIdx] && !blockedByModelByLayer[l][cellIdx]) {
                        if (activeRunLayers == 0) {
                            runTopLayer = l
                        }
                        activeRunLayers++
                    } else if (activeRunLayers > 0) {
                        if (!blockedByModelByLayer[l][cellIdx]) {
                            activeRunLayers++
                        } else {
                            // Hit XY clearance halo around model; check if model is directly below
                            if (l > 0 && insideModelByLayer[l - 1][cellIdx]) {
                                finalizeRun(l, landedOnModel = true)
                                activeRunLayers = 0
                                runTopLayer = -1
                            }
                        }
                    }
                }

                if (activeRunLayers > 0 && runTopLayer >= 0) {
                    finalizeRun(0, landedOnModel = false)
                }
            }
        }

        // Dilate Layer 0 by supportFirstLayerExpansionMm (and body layers by normalSupportExpansionMm) while respecting blockedByModel
        val firstLayerExpandCells = (process.supportFirstLayerExpansionMm.coerceIn(0f, 6f) / cellSize).roundToInt()
        val bodyExpandCells = (process.normalSupportExpansionMm.coerceIn(0f, 4f) / cellSize).roundToInt()

        for (l in 0 until totalLayers) {
            val expandRadius = if (l == 0) max(1, firstLayerExpandCells) else bodyExpandCells
            if (expandRadius > 0) {
                val src = supportStateByLayer[l].clone()
                val dst = supportStateByLayer[l]
                val blocked = blockedByModelByLayer[l]
                for (iy in 0 until ny) {
                    for (ix in 0 until nx) {
                        val idx = iy * nx + ix
                        if (src[idx] == 0.toByte() && !blocked[idx]) {
                            var found: Byte = 0
                            val y0 = max(0, iy - expandRadius)
                            val y1 = min(ny - 1, iy + expandRadius)
                            val x0 = max(0, ix - expandRadius)
                            val x1 = min(nx - 1, ix + expandRadius)
                            for (nyIdx in y0..y1) {
                                for (nxIdx in x0..x1) {
                                    val dx = nxIdx - ix
                                    val dy = nyIdx - iy
                                    if (dx * dx + dy * dy <= expandRadius * expandRadius) {
                                        val s = src[nyIdx * nx + nxIdx]
                                        if (s > found) found = s
                                    }
                                }
                            }
                            if (found > 0) dst[idx] = if (l == 0) 1 else found
                        }
                    }
                }
            }
        }

        // Convert each layer's support mask into OrcaSlicer Normal Support toolpaths
        val baseSpacing = process.supportBasePatternSpacingMm.coerceIn(0.8f, 10.0f)
        val ifaceSpacing = process.supportTopInterfaceSpacingMm.coerceIn(0.35f, 1.5f)
        val baseAngleDeg = process.supportPatternAngleDeg.toFloat()

        return (0 until totalLayers).map { layerIdx ->
            val stateArr = supportStateByLayer[layerIdx]
            if (stateArr.all { it == 0.toByte() }) {
                LayerSupportPlan.EMPTY
            } else {
                buildNormalSupportLayerPlan(
                    layerIdx = layerIdx,
                    stateArr = stateArr,
                    nx = nx,
                    ny = ny,
                    minX = minX,
                    minY = minY,
                    cellSize = cellSize,
                    baseSpacing = if (layerIdx == 0) min(baseSpacing, 1.4f) else baseSpacing,
                    ifaceSpacing = ifaceSpacing,
                    baseAngleDeg = baseAngleDeg,
                    process = process
                )
            }
        }
    }

    private fun buildNormalSupportLayerPlan(
        layerIdx: Int,
        stateArr: ByteArray,
        nx: Int,
        ny: Int,
        minX: Float,
        minY: Float,
        cellSize: Float,
        baseSpacing: Float,
        ifaceSpacing: Float,
        baseAngleDeg: Float,
        process: ProcessProfile
    ): LayerSupportPlan {
        // 1. Extract outer boundary loops around active support regions on this layer (Snug / Base Perimeter Wall)
        val closedLoops = ArrayList<List<Vec2>>()
        val wantPerimeterWall = layerIdx == 0 ||
            process.supportStyle == SupportStyle.SNUG ||
            process.supportWallLoops > 0

        val boundaryPolys = extractGridBoundaryPolygons(stateArr, nx, ny, minX, minY, cellSize)
        if (wantPerimeterWall && boundaryPolys.isNotEmpty()) {
            val wallLoopsCount = if (layerIdx == 0) 1 else process.supportWallLoops.coerceIn(1, 3)
            val supportWidth = process.supportLineWidthMm.coerceIn(0.25f, 0.8f)
            for (poly in boundaryPolys) {
                var ring = poly
                for (w in 0 until wallLoopsCount) {
                    if (ring.size < 3) break
                    closedLoops.add(ring)
                    ring = offsetPolygonInward(ring, supportWidth * 0.92f)
                }
            }
        }

        // 2. Generate continuous serpentine/accordion Base Support lines (and Grid/Honeycomb variants)
        val hasBaseCells = stateArr.any { it == 1.toByte() || (layerIdx == 0 && it > 0) }
        val hasIfaceCells = layerIdx > 0 && stateArr.any { it == 2.toByte() }

        val openBasePolylines = ArrayList<List<Vec2>>()
        if (hasBaseCells && boundaryPolys.isNotEmpty()) {
            val primaryLines = generateSerpentineSupportLines(
                polygons = boundaryPolys,
                stateArr = stateArr,
                targetState = if (layerIdx == 0) 0 else 1, // 0 means any non-zero cell on layer 0
                nx = nx,
                ny = ny,
                minX = minX,
                minY = minY,
                cellSize = cellSize,
                spacing = baseSpacing,
                angleDeg = baseAngleDeg,
                honeycombWave = process.supportBasePattern == SupportBasePattern.HONEYCOMB
            )
            openBasePolylines.addAll(primaryLines)

            if (process.supportBasePattern == SupportBasePattern.RECTILINEAR_GRID) {
                val crossLines = generateSerpentineSupportLines(
                    polygons = boundaryPolys,
                    stateArr = stateArr,
                    targetState = if (layerIdx == 0) 0 else 1,
                    nx = nx,
                    ny = ny,
                    minX = minX,
                    minY = minY,
                    cellSize = cellSize,
                    spacing = baseSpacing * 1.4f,
                    angleDeg = baseAngleDeg + 90f,
                    honeycombWave = false
                )
                openBasePolylines.addAll(crossLines)
            }
        }

        // 3. Generate dense Support Interface lines (or Concentric interface loops) for state == 2 cells
        val interfacePolylines = ArrayList<List<Vec2>>()
        val interfaceClosedLoops = ArrayList<List<Vec2>>()
        if (hasIfaceCells && boundaryPolys.isNotEmpty()) {
            if (process.supportInterfacePattern == SupportInterfacePattern.CONCENTRIC) {
                val ifacePolys = extractGridBoundaryPolygons(
                    stateArr = ByteArray(stateArr.size) { if (stateArr[it] == 2.toByte()) 1 else 0 },
                    nx = nx,
                    ny = ny,
                    minX = minX,
                    minY = minY,
                    cellSize = cellSize
                )
                val rings = generateConcentricRings(ifacePolys, ifaceSpacing.coerceAtLeast(0.38f))
                interfaceClosedLoops.addAll(rings)
            } else {
                val ifaceAngle = if (layerIdx % 2 == 0) baseAngleDeg + 90f else baseAngleDeg
                val ifaceLines = generateSerpentineSupportLines(
                    polygons = boundaryPolys,
                    stateArr = stateArr,
                    targetState = 2,
                    nx = nx,
                    ny = ny,
                    minX = minX,
                    minY = minY,
                    cellSize = cellSize,
                    spacing = ifaceSpacing.coerceAtLeast(0.40f),
                    angleDeg = ifaceAngle,
                    honeycombWave = false
                )
                interfacePolylines.addAll(ifaceLines)
            }
        }

        return LayerSupportPlan(
            closedLoops = closedLoops,
            openBasePolylines = openBasePolylines,
            interfacePolylines = interfacePolylines,
            interfaceClosedLoops = interfaceClosedLoops
        )
    }

    private fun extractGridBoundaryPolygons(
        stateArr: ByteArray,
        nx: Int,
        ny: Int,
        minX: Float,
        minY: Float,
        cellSize: Float
    ): List<List<Vec2>> {
        val edges = ArrayList<Segment2D>()
        fun isActive(ix: Int, iy: Int): Boolean {
            if (ix < 0 || ix >= nx || iy < 0 || iy >= ny) return false
            return stateArr[iy * nx + ix] > 0
        }

        for (iy in 0 until ny) {
            val y0 = minY + iy * cellSize
            val y1 = y0 + cellSize
            for (ix in 0 until nx) {
                if (!isActive(ix, iy)) continue
                val x0 = minX + ix * cellSize
                val x1 = x0 + cellSize
                if (!isActive(ix, iy - 1)) edges.add(Segment2D(Vec2(x0, y0), Vec2(x1, y0)))
                if (!isActive(ix + 1, iy)) edges.add(Segment2D(Vec2(x1, y0), Vec2(x1, y1)))
                if (!isActive(ix, iy + 1)) edges.add(Segment2D(Vec2(x1, y1), Vec2(x0, y1)))
                if (!isActive(ix - 1, iy)) edges.add(Segment2D(Vec2(x0, y1), Vec2(x0, y0)))
            }
        }
        val rawPolys = chainSegmentsIntoPolygons(edges)
        // Smooth grid staircase corners slightly for clean snug support perimeters
        return rawPolys.map { poly -> smoothGridPolygon(poly) }
    }

    private fun smoothGridPolygon(poly: List<Vec2>): List<Vec2> {
        if (poly.size < 4) return poly
        val out = ArrayList<Vec2>(poly.size)
        val n = poly.size
        for (i in 0 until n) {
            val prev = poly[(i + n - 1) % n]
            val curr = poly[i]
            val next = poly[(i + 1) % n]
            out.add(Vec2(curr.x * 0.6f + (prev.x + next.x) * 0.2f, curr.y * 0.6f + (prev.y + next.y) * 0.2f))
        }
        return cleanAndSimplifyPolygon(out).ifEmpty { poly }
    }

    /**
     * Generates OrcaSlicer's continuous serpentine (accordion / zig-zag) support lines across the support polygons,
     * linking consecutive scanline segments into continuous extruding polylines when adjacent.
     */
    private fun generateSerpentineSupportLines(
        polygons: List<List<Vec2>>,
        stateArr: ByteArray,
        targetState: Int,
        nx: Int,
        ny: Int,
        minX: Float,
        minY: Float,
        cellSize: Float,
        spacing: Float,
        angleDeg: Float,
        honeycombWave: Boolean
    ): List<List<Vec2>> {
        val rad = angleDeg * (PI.toFloat() / 180f)
        val cosA = cos(rad)
        val sinA = sin(rad)

        fun rotate(p: Vec2): Vec2 = Vec2(p.x * cosA + p.y * sinA, -p.x * sinA + p.y * cosA)
        fun unrotate(rx: Float, ry: Float): Vec2 = Vec2(rx * cosA - ry * sinA, rx * sinA + ry * cosA)

        val rotatedPolys = polygons.map { poly -> poly.map(::rotate) }
        val bounds = computeContoursBounds(rotatedPolys)
        val startY = bounds.first.y + spacing * 0.35f
        val endY = bounds.second.y - spacing * 0.35f
        if (endY < startY) return emptyList()

        data class ScanSegment(val rowIdx: Int, val y: Float, val xStart: Float, val xEnd: Float, var used: Boolean = false)

        val rows = ArrayList<List<ScanSegment>>()
        var y = startY
        var rIdx = 0
        while (y <= endY + 1e-3f) {
            val intervals = scanlineIntervals(rotatedPolys, y)
            val rowSegs = ArrayList<ScanSegment>()
            for (iv in intervals) {
                if (iv.length < 0.6f) continue
                // Filter interval by targetState in grid
                val midWorld = unrotate((iv.start + iv.end) * 0.5f, y)
                val ix = ((midWorld.x - minX) / cellSize).toInt().coerceIn(0, nx - 1)
                val iy = ((midWorld.y - minY) / cellSize).toInt().coerceIn(0, ny - 1)
                val cellVal = stateArr[iy * nx + ix].toInt()
                val matches = if (targetState == 0) cellVal > 0 else cellVal == targetState
                if (matches) {
                    rowSegs.add(ScanSegment(rIdx, y, iv.start, iv.end))
                }
            }
            if (rowSegs.isNotEmpty()) {
                rows.add(rowSegs)
                rIdx++
            }
            y += spacing
        }

        // Chain vertically adjacent scan segments into continuous serpentine accordion polylines
        val polylines = ArrayList<List<Vec2>>()
        for (i in rows.indices) {
            for (seg in rows[i]) {
                if (seg.used) continue
                seg.used = true
                val pts = ArrayList<Vec2>()
                var dirRight = (i % 2 == 0)

                fun appendSegmentPoints(s: ScanSegment, leftToRight: Boolean) {
                    val x0 = if (leftToRight) s.xStart else s.xEnd
                    val x1 = if (leftToRight) s.xEnd else s.xStart
                    if (!honeycombWave || abs(x1 - x0) < 2.5f) {
                        pts.add(unrotate(x0, s.y))
                        pts.add(unrotate(x1, s.y))
                    } else {
                        val steps = max(3, (abs(x1 - x0) / 1.8f).toInt().coerceAtMost(24))
                        for (k in 0..steps) {
                            val t = k.toFloat() / steps
                            val rx = x0 + (x1 - x0) * t
                            val wave = sin(rx * 1.4f + s.rowIdx * 1.5f) * (spacing * 0.25f)
                            pts.add(unrotate(rx, s.y + wave))
                        }
                    }
                }

                appendSegmentPoints(seg, dirRight)
                var curSeg = seg
                var curRowIdx = i

                while (curRowIdx + 1 < rows.size) {
                    val nextRow = rows[curRowIdx + 1]
                    val turnX = if (dirRight) curSeg.xEnd else curSeg.xStart
                    val candidate = nextRow.firstOrNull { nxt ->
                        !nxt.used &&
                            abs(nxt.y - curSeg.y) <= spacing * 1.6f &&
                            max(curSeg.xStart, nxt.xStart) <= min(curSeg.xEnd, nxt.xEnd) + spacing * 0.8f &&
                            abs((if (dirRight) nxt.xEnd else nxt.xStart) - turnX) <= spacing * 2.5f
                    } ?: break

                    candidate.used = true
                    dirRight = !dirRight
                    appendSegmentPoints(candidate, dirRight)
                    curSeg = candidate
                    curRowIdx++
                }

                if (pts.size >= 2) {
                    polylines.add(pts)
                }
            }
        }
        return polylines
    }

    /**
     * OrcaSlicer Tree (Organic) Support (`tree(auto)` / `tree(manual)`):
     * - Extracts overhang contact tips on every layer
     * - Simulates Top-Down Organic Tree Growth from `totalLayers - 1` down to `0`:
     *   1. Spawns branch tips with `treeSupportTipDiameterMm` right below overhangs (`supportTopZDistanceMm` air gap)
     *   2. Steers active branches outward away from `allGeometries[L].outerContours` (`supportObjectXyDistanceMm` clearance)
     *      so branches curve organically around the model instead of colliding with it
     *   3. Pulls nearby branches toward their local cluster center within the printable overhang angle (`treeSupportBranchAngleDeg`)
     *   4. Merges touching branches into thicker parent trunks (`sqrt(rA^2 + 0.65 * rB^2)` + `treeSupportBranchDiameterAngleDeg` taper)
     *   5. Computes exact circle-arc boolean union contours on each layer so merging branches form smooth figure-8 / organic tubes
     *   6. Generates `treeSupportWallLoops` concentric walls, dense tip interface pads, and Layer 0 tree root brim flanges.
     */
    private data class ActiveTreeBranch(
        var x: Float,
        var y: Float,
        var radius: Float,
        var ageLayers: Int,
        var landedOnModel: Boolean = false,
        val historyByLayer: HashMap<Int, TreeCrossSection> = HashMap()
    )

    private data class TreeCrossSection(
        val x: Float,
        val y: Float,
        val radius: Float,
        val isInterface: Boolean
    )

    private fun computeTreeSupportPlans(
        triangles: List<Triangle>,
        allGeometries: List<LayerContourGeometry>,
        process: ProcessProfile
    ): List<LayerSupportPlan> {
        val totalLayers = allGeometries.size
        if (totalLayers <= 1) return List(totalLayers) { LayerSupportPlan.EMPTY }

        val tipSpacing = max(2.4f, process.treeSupportBranchDiameterMm + process.treeSupportBranchDistanceMm)
        val rawOverhangsByLayer = extractOverhangPointsByLayer(
            triangles = triangles,
            allGeometries = allGeometries,
            process = process,
            sampleSpacingMm = tipSpacing * 0.85f
        )
        if (rawOverhangsByLayer.all { it.isEmpty() }) {
            return List(totalLayers) { LayerSupportPlan.EMPTY }
        }

        val stdLh = if (totalLayers > 1) allGeometries[1].layerHeight else allGeometries[0].layerHeight
        val topZGapLayers = max(1, (process.supportTopZDistanceMm / stdLh).roundToInt())
        val topInterfaceCount = process.supportTopInterfaceLayers.coerceIn(1, 6)
        val xyClearance = process.supportObjectXyDistanceMm.coerceIn(0.20f, 2.0f)

        val tipRadius = (process.treeSupportTipDiameterMm * 0.5f).coerceIn(0.45f, 1.6f)
        val baseBranchRadius = (process.treeSupportBranchDiameterMm * 0.5f).coerceIn(0.8f, 4.0f)
        val maxTrunkRadius = max(baseBranchRadius * 2.2f, 4.2f)

        val maxAngleRad = (process.treeSupportBranchAngleDeg.coerceIn(15, 65)) * (PI.toFloat() / 180f)
        val maxStepPerLayer = stdLh * tan(maxAngleRad)
        val taperPerLayer = stdLh * tan(process.treeSupportBranchDiameterAngleDeg.coerceIn(1, 15) * (PI.toFloat() / 180f))

        // Cluster overhang points on each layer so tips are cleanly spaced by tipSpacing
        val spawnedTipsByLayer = Array(totalLayers) { ArrayList<Vec2>() }
        for (layerIdx in 2 until totalLayers) {
            val rawPts = rawOverhangsByLayer[layerIdx]
            if (rawPts.isEmpty()) continue
            val targetTipLayer = (layerIdx - topZGapLayers).coerceAtLeast(0)
            val chosen = spawnedTipsByLayer[targetTipLayer]
            for (pt in rawPts) {
                if (chosen.none { it.distanceTo(pt) < tipSpacing * 0.85f }) {
                    chosen.add(pt)
                }
            }
        }

        val activeBranches = ArrayList<ActiveTreeBranch>()
        val completedBranches = ArrayList<ActiveTreeBranch>()
        val basePlateContours = allGeometries[0].outerContours

        // Top-Down Organic Tree Growth from totalLayers - 1 down to 0
        for (layerIdx in (totalLayers - 1) downTo 0) {
            val contours = allGeometries[layerIdx].outerContours

            // 1. Advance existing branches downward to this layer
            val iterator = activeBranches.iterator()
            while (iterator.hasNext()) {
                val b = iterator.next()
                b.ageLayers++
                val targetRadius = min(
                    maxTrunkRadius,
                    max(b.radius + taperPerLayer, min(baseBranchRadius, tipRadius + b.ageLayers * 0.08f))
                )
                b.radius = targetRadius

                // Look-ahead obstacle avoidance: if this branch is above the Layer 0 model footprint,
                // steer it smoothly outward toward the nearest edge of the model base so the trunk roots on the build plate!
                if (layerIdx > 0 && basePlateContours.isNotEmpty()) {
                    val (sDistBase, nearestBaseEdge) = signedDistanceToContours(Vec2(b.x, b.y), basePlateContours)
                    val desiredClearance = maxTrunkRadius + xyClearance + 0.5f
                    val neededOutward = desiredClearance - sDistBase
                    if (neededOutward > 0.05f && neededOutward <= layerIdx * maxStepPerLayer * 1.4f) {
                        val outDir = if (sDistBase < 0f) {
                            (nearestBaseEdge - Vec2(b.x, b.y)).normalized()
                        } else {
                            (Vec2(b.x, b.y) - nearestBaseEdge).normalized()
                        }
                        if (outDir.length() > 0.1f) {
                            val stepOut = min(maxStepPerLayer * 0.85f, neededOutward / max(1f, layerIdx * 0.45f))
                            val cand = Vec2(b.x + outDir.x * stepOut, b.y + outDir.y * stepOut)
                            val (sDistCur, _) = signedDistanceToContours(cand, contours)
                            if (sDistCur >= b.radius + xyClearance) {
                                b.x = cand.x
                                b.y = cand.y
                            }
                        }
                    }
                }

                // Check model collision / XY clearance at this layer
                if (contours.isNotEmpty()) {
                    val (sDist, nearestBoundaryPt) = signedDistanceToContours(Vec2(b.x, b.y), contours)
                    val requiredClearance = b.radius + xyClearance
                    if (sDist < requiredClearance) {
                        // Compute outward escape vector away from model contour
                        var escapeDir = if (sDist >= 0f) {
                            (Vec2(b.x, b.y) - nearestBoundaryPt).normalized()
                        } else {
                            // Branch center is inside contour: push toward nearest boundary point!
                            (nearestBoundaryPt - Vec2(b.x, b.y)).normalized()
                        }
                        if (escapeDir.length() < 0.1f) {
                            escapeDir = Vec2(1f, 0f)
                        }
                        val neededPush = requiredClearance - sDist
                        val maxEscapeStep = maxStepPerLayer * 1.45f
                        if (sDist < -b.radius * 0.8f && neededPush > maxEscapeStep * 2.2f) {
                            // Branch landed flat on top of a wide horizontal model surface
                            b.landedOnModel = true
                            completedBranches.add(b)
                            iterator.remove()
                            continue
                        } else {
                            val pushDist = min(neededPush, maxEscapeStep)
                            b.x += escapeDir.x * pushDist
                            b.y += escapeDir.y * pushDist
                        }
                    }
                }
            }

            // 2. Spawn new overhang contact tips at this layer (if not already covered by an existing branch)
            for (tipPt in spawnedTipsByLayer[layerIdx]) {
                val covered = activeBranches.any { Vec2(it.x, it.y).distanceTo(tipPt) < it.radius + tipSpacing * 0.45f }
                if (!covered) {
                    var tx = tipPt.x
                    var ty = tipPt.y
                    if (contours.isNotEmpty()) {
                        val (sDist, nearestPt) = signedDistanceToContours(tipPt, contours)
                        val req = tipRadius + xyClearance * 0.8f
                        if (sDist < req) {
                            val dir = if (sDist >= 0f) (tipPt - nearestPt).normalized() else (nearestPt - tipPt).normalized()
                            tx = nearestPt.x + dir.x * req
                            ty = nearestPt.y + dir.y * req
                        }
                    }
                    activeBranches.add(
                        ActiveTreeBranch(
                            x = tx,
                            y = ty,
                            radius = tipRadius,
                            ageLayers = 0
                        )
                    )
                }
            }

            // 3. Organic Sibling Branch Attraction (pull nearby branches toward their local cluster center within maxStepPerLayer)
            if (activeBranches.size >= 2 && layerIdx > 0) {
                val n = activeBranches.size
                val deltaX = FloatArray(n)
                val deltaY = FloatArray(n)
                val attractRadius = 22.0f

                for (i in 0 until n) {
                    val bi = activeBranches[i]
                    var sumX = 0f
                    var sumY = 0f
                    var weightSum = 0f
                    for (j in 0 until n) {
                        if (i == j) continue
                        val bj = activeBranches[j]
                        val d = hypot2D(bj.x - bi.x, bj.y - bi.y)
                        if (d in 0.05f..attractRadius) {
                            val w = (1f - d / attractRadius) * bj.radius
                            sumX += bj.x * w
                            sumY += bj.y * w
                            weightSum += w
                        }
                    }
                    if (weightSum > 0f) {
                        val targetX = sumX / weightSum
                        val targetY = sumY / weightSum
                        val dir = Vec2(targetX - bi.x, targetY - bi.y)
                        val dist = dir.length()
                        if (dist > 0.02f) {
                            val step = min(dist * 0.35f, maxStepPerLayer * 0.75f)
                            val candidate = Vec2(bi.x + (dir.x / dist) * step, bi.y + (dir.y / dist) * step)
                            // Only move toward sibling if it doesn't violate model XY clearance
                            val (sDist, _) = signedDistanceToContours(candidate, contours)
                            if (sDist >= bi.radius + xyClearance) {
                                deltaX[i] = (dir.x / dist) * step
                                deltaY[i] = (dir.y / dist) * step
                            }
                        }
                    }
                }
                for (i in 0 until n) {
                    activeBranches[i].x += deltaX[i]
                    activeBranches[i].y += deltaY[i]
                }

                // 4. Merge branches that have converged into a single parent trunk
                var mergedAny = true
                while (mergedAny && activeBranches.size >= 2) {
                    mergedAny = false
                    outer@ for (i in 0 until activeBranches.size) {
                        for (j in i + 1 until activeBranches.size) {
                            val a = activeBranches[i]
                            val b = activeBranches[j]
                            val d = hypot2D(a.x - b.x, a.y - b.y)
                            val mergeThreshold = (a.radius + b.radius) * 0.72f + process.treeSupportBranchDistanceMm * 0.2f
                            if (d <= mergeThreshold) {
                                val wA = a.radius * a.radius
                                val wB = b.radius * b.radius
                                val wTotal = (wA + wB).coerceAtLeast(1e-4f)
                                a.x = (a.x * wA + b.x * wB) / wTotal
                                a.y = (a.y * wA + b.y * wB) / wTotal
                                a.radius = min(maxTrunkRadius, sqrt(wA + wB * 0.65f))
                                a.ageLayers = max(a.ageLayers, b.ageLayers)
                                // Merge layer histories so both sub-branches above this layer are preserved!
                                for ((histLayer, sec) in b.historyByLayer) {
                                    if (!a.historyByLayer.containsKey(histLayer)) {
                                        a.historyByLayer[histLayer] = sec
                                    }
                                }
                                // Keep a secondary cross-section list by moving `b` into completedBranches
                                completedBranches.add(b)
                                activeBranches.removeAt(j)
                                mergedAny = true
                                break@outer
                            }
                        }
                    }
                }
            }

            // 5. Record cross-section of all active branches on this layer
            for (b in activeBranches) {
                b.historyByLayer[layerIdx] = TreeCrossSection(
                    x = b.x,
                    y = b.y,
                    radius = b.radius,
                    isInterface = b.ageLayers < topInterfaceCount
                )
            }
        }

        completedBranches.addAll(activeBranches)

        // Filter out branches that landed on the model if `supportOnBuildPlateOnly` is enabled
        val validBranches = if (process.supportOnBuildPlateOnly) {
            completedBranches.filter { !it.landedOnModel }
        } else {
            completedBranches
        }

        // Gather cross-sections per layer and build organic union contours + tip interfaces + root brims
        val sectionsByLayer = Array(totalLayers) { ArrayList<TreeCrossSection>() }
        for (branch in validBranches) {
            for ((layerIdx, sec) in branch.historyByLayer) {
                if (layerIdx in 0 until totalLayers) {
                    // Avoid duplicate identical circles from merged histories
                    if (sectionsByLayer[layerIdx].none { hypot2D(it.x - sec.x, it.y - sec.y) < 0.08f && abs(it.radius - sec.radius) < 0.08f }) {
                        sectionsByLayer[layerIdx].add(sec)
                    }
                }
            }
        }

        val supportWidth = process.supportLineWidthMm.coerceIn(0.25f, 0.8f)
        val treeWallCount = process.treeSupportWallLoops.coerceIn(1, 4)
        val rootBrimExpand = max(process.supportFirstLayerExpansionMm, process.treeSupportBrimWidthMm).coerceIn(0f, 8f)
        val ifaceSpacing = process.supportTopInterfaceSpacingMm.coerceIn(0.35f, 1.0f)

        return (0 until totalLayers).map { layerIdx ->
            val sections = sectionsByLayer[layerIdx]
            if (sections.isEmpty()) {
                LayerSupportPlan.EMPTY
            } else {
                val closedLoops = ArrayList<List<Vec2>>()
                val interfaceClosedLoops = ArrayList<List<Vec2>>()

                val bodySections = sections.filter { !it.isInterface }
                val ifaceSections = sections.filter { it.isInterface }

                // 1. Organic boolean union of overlapping tree branch circles on this layer
                if (bodySections.isNotEmpty()) {
                    val unionPolys = computeCircleUnionPolygons(bodySections)
                    for (poly in unionPolys) {
                        // On Layer 0, add outward Tree Root Brim flange loops around each trunk root!
                        if (layerIdx == 0 && rootBrimExpand >= supportWidth) {
                            val brimRings = (rootBrimExpand / (supportWidth * 0.92f)).toInt().coerceIn(1, 8)
                            var outerBrim = poly
                            for (b in 1..brimRings) {
                                outerBrim = offsetPolygonOutward(outerBrim, supportWidth * 0.92f)
                                if (outerBrim.size >= 3) {
                                    closedLoops.add(outerBrim)
                                }
                            }
                        }

                        // Hollow organic tree branch perimeter wall loops (1..treeWallCount)
                        var currentWall = poly
                        val wallsToDraw = if (layerIdx == 0) max(treeWallCount, 3) else treeWallCount
                        for (w in 0 until wallsToDraw) {
                            if (currentWall.size < 3) break
                            closedLoops.add(currentWall)
                            currentWall = offsetPolygonInward(currentWall, supportWidth * 0.92f)
                        }
                    }
                }

                // 2. Dense Support Interface pads at Tree Branch Contact Tips right under overhangs
                if (ifaceSections.isNotEmpty()) {
                    val ifacePolys = computeCircleUnionPolygons(ifaceSections)
                    for (poly in ifacePolys) {
                        var ring = poly
                        var safety = 0
                        while (ring.size >= 3 && safety < 12) {
                            interfaceClosedLoops.add(ring)
                            ring = offsetPolygonInward(ring, ifaceSpacing)
                            safety++
                        }
                    }
                }

                LayerSupportPlan(
                    closedLoops = closedLoops,
                    interfaceClosedLoops = interfaceClosedLoops
                )
            }
        }
    }

    /**
     * Computes the exact 2D boolean union contours of a set of (possibly overlapping) circles `(x, y, radius)`
     * so merging tree branches form smooth figure-8 / peanut organic cross-sections without internal crossing walls.
     */
    private fun computeCircleUnionPolygons(circles: List<TreeCrossSection>): List<List<Vec2>> {
        if (circles.isEmpty()) return emptyList()
        if (circles.size == 1) {
            val c = circles[0]
            return listOf(createSmoothCircleLoop(c.x, c.y, c.radius, segments = 24))
        }

        // Remove any circle completely contained inside another circle
        val filtered = circles.filter { ci ->
            circles.none { cj ->
                ci !== cj && hypot2D(ci.x - cj.x, ci.y - cj.y) + ci.radius <= cj.radius + 0.02f
            }
        }

        val arcSegments = ArrayList<Segment2D>()
        val nSegs = 24
        for (i in filtered.indices) {
            val ci = filtered[i]
            val pts = createSmoothCircleLoop(ci.x, ci.y, ci.radius, segments = nSegs)
            for (k in 0 until nSegs) {
                val a = pts[k]
                val b = pts[(k + 1) % nSegs]
                val mid = (a + b) * 0.5f
                val insideOther = filtered.indices.any { j ->
                    if (i == j) false
                    else {
                        val cj = filtered[j]
                        hypot2D(mid.x - cj.x, mid.y - cj.y) < cj.radius - 0.015f
                    }
                }
                if (!insideOther) {
                    arcSegments.add(Segment2D(a, b))
                }
            }
        }

        return chainSegmentsIntoPolygons(arcSegments)
    }

    private fun createSmoothCircleLoop(cx: Float, cy: Float, r: Float, segments: Int = 24): List<Vec2> {
        val pts = ArrayList<Vec2>(segments)
        val step = (2.0 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val a = i * step
            pts.add(Vec2(cx + cos(a) * r, cy + sin(a) * r))
        }
        return pts
    }

    private fun offsetPolygonOutward(rawPoly: List<Vec2>, expandDist: Float): List<Vec2> {
        val poly = cleanAndSimplifyPolygon(ensureCounterClockwise(rawPoly))
        if (poly.size < 3) return emptyList()
        val n = poly.size
        val shifted = ArrayList<Vec2>(n)
        for (i in 0 until n) {
            val prev = poly[(i + n - 1) % n]
            val curr = poly[i]
            val next = poly[(i + 1) % n]
            val e1 = (curr - prev).normalized()
            val e2 = (next - curr).normalized()
            val n1 = e1.perp()
            val n2 = e2.perp()
            val bisector = (n1 + n2).normalized()
            val cosHalf = bisector.x * n1.x + bisector.y * n1.y
            val miterScale = if (cosHalf > 0.35f) (1f / cosHalf).coerceAtMost(1.65f) else 1f
            shifted.add(curr - bisector * (expandDist * miterScale))
        }
        return shifted
    }

    private fun hypot2D(dx: Float, dy: Float): Float = sqrt(dx * dx + dy * dy)

    // =========================================================================
    // Cura Combing Mode & Travel Obstacle Avoidance Engine
    // =========================================================================

    private data class CombedTravelResult(
        val waypoints: List<Vec2>,
        val isCombed: Boolean,
        val requiresRetract: Boolean
    )

    private fun alignClosedPolygonStart(poly: List<Vec2>, referencePt: Vec2): List<Vec2> {
        if (poly.size < 3) return poly
        var bestIdx = 0
        var bestDist = Float.POSITIVE_INFINITY
        for (i in poly.indices) {
            val d = poly[i].distanceTo(referencePt)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        if (bestIdx == 0) return poly
        val rotated = ArrayList<Vec2>(poly.size)
        for (k in poly.indices) {
            rotated.add(poly[(bestIdx + k) % poly.size])
        }
        return rotated
    }

    private fun segmentsProperlyIntersect(a: Vec2, b: Vec2, c: Vec2, d: Vec2): Boolean {
        fun ccw(p1: Vec2, p2: Vec2, p3: Vec2): Float =
            (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x)

        val d1 = ccw(c, d, a)
        val d2 = ccw(c, d, b)
        val d3 = ccw(a, b, c)
        val d4 = ccw(a, b, d)
        return ((d1 > 1e-4f && d2 < -1e-4f) || (d1 < -1e-4f && d2 > 1e-4f)) &&
            ((d3 > 1e-4f && d4 < -1e-4f) || (d3 < -1e-4f && d4 > 1e-4f))
    }

    private fun doesSegmentCrossPolygonBoundary(a: Vec2, b: Vec2, poly: List<Vec2>): Boolean {
        val n = poly.size
        if (n < 3) return false
        for (i in 0 until n) {
            val p0 = poly[i]
            val p1 = poly[(i + 1) % n]
            if (segmentsProperlyIntersect(a, b, p0, p1)) return true
        }
        return false
    }

    private fun isChordInsidePolygon(a: Vec2, b: Vec2, poly: List<Vec2>): Boolean {
        if (doesSegmentCrossPolygonBoundary(a, b, poly)) return false
        val mid = (a + b) * 0.5f
        return isPointInPolygon(mid, poly)
    }

    private fun findClosestPolygonVertexIndex(pt: Vec2, poly: List<Vec2>): Int {
        var bestIdx = 0
        var bestDist = Float.POSITIVE_INFINITY
        for (i in poly.indices) {
            val d = poly[i].distanceTo(pt)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun polylineLength(pts: List<Vec2>): Float {
        var len = 0f
        for (i in 0 until pts.size - 1) {
            len += pts[i].distanceTo(pts[i + 1])
        }
        return len
    }

    /**
     * Routes a combing travel path inside `poly` from `start` to `end` by walking the shorter side
     * of an inward-offset guide polygon and applying visibility string-pulling shortcuts.
     */
    private fun buildBoundaryCombedPath(
        start: Vec2,
        end: Vec2,
        poly: List<Vec2>,
        lineWidthMm: Float
    ): List<Vec2> {
        val guidePoly = offsetPolygonInward(poly, lineWidthMm * 0.45f).ifEmpty { poly }
        val n = guidePoly.size
        if (n < 3) return listOf(start, end)

        val iStart = findClosestPolygonVertexIndex(start, guidePoly)
        val iEnd = findClosestPolygonVertexIndex(end, guidePoly)
        if (iStart == iEnd) {
            return listOf(start, guidePoly[iStart], end)
        }

        // Build forward chain along guidePoly
        val forward = ArrayList<Vec2>()
        forward.add(start)
        var idx = iStart
        var steps = 0
        while (idx != iEnd && steps <= n) {
            forward.add(guidePoly[idx])
            idx = (idx + 1) % n
            steps++
        }
        forward.add(guidePoly[iEnd])
        forward.add(end)

        // Build backward chain along guidePoly
        val backward = ArrayList<Vec2>()
        backward.add(start)
        idx = iStart
        steps = 0
        while (idx != iEnd && steps <= n) {
            backward.add(guidePoly[idx])
            idx = (idx - 1 + n) % n
            steps++
        }
        backward.add(guidePoly[iEnd])
        backward.add(end)

        val chosen = if (polylineLength(forward) <= polylineLength(backward)) forward else backward
        return shortcutCombingWaypoints(chosen, poly)
    }

    /**
     * Visibility String-Pulling / Funnel Shortcut Pass:
     * Simplifies a boundary-following combing chain into the shortest taut path that stays strictly inside `poly`.
     */
    private fun shortcutCombingWaypoints(chain: List<Vec2>, poly: List<Vec2>): List<Vec2> {
        if (chain.size <= 2) return chain
        val out = ArrayList<Vec2>()
        var curIdx = 0
        out.add(chain[0])

        while (curIdx < chain.lastIndex) {
            var furthestVisible = curIdx + 1
            for (testIdx in chain.lastIndex downTo curIdx + 2) {
                if (isChordInsidePolygon(chain[curIdx], chain[testIdx], poly)) {
                    furthestVisible = testIdx
                    break
                }
            }
            out.add(chain[furthestVisible])
            curIdx = furthestVisible
        }
        return out
    }

    /**
     * Routes an open-air travel move around any obstacle polygons (`avoidPrintedPartsWhenTraveling` / `avoidSupportsWhenTraveling`).
     */
    private fun detourAroundObstacles(
        start: Vec2,
        end: Vec2,
        obstacles: List<List<Vec2>>
    ): List<Vec2> {
        if (obstacles.isEmpty()) return listOf(start, end)
        var currentPath = listOf(start, end)

        for (obs in obstacles) {
            if (obs.size < 3) continue
            // Only detour around obstacles where neither start nor end is strictly inside the obstacle
            if (isPointInPolygon(start, obs) || isPointInPolygon(end, obs)) continue

            val nextPath = ArrayList<Vec2>()
            nextPath.add(currentPath[0])
            for (segIdx in 0 until currentPath.size - 1) {
                val pA = currentPath[segIdx]
                val pB = currentPath[segIdx + 1]
                if (doesSegmentCrossPolygonBoundary(pA, pB, obs)) {
                    val n = obs.size
                    val iStart = findClosestPolygonVertexIndex(pA, obs)
                    val iEnd = findClosestPolygonVertexIndex(pB, obs)

                    val fwd = ArrayList<Vec2>()
                    var k = iStart
                    var steps = 0
                    while (k != iEnd && steps <= n) {
                        fwd.add(obs[k])
                        k = (k + 1) % n
                        steps++
                    }
                    fwd.add(obs[iEnd])

                    val bwd = ArrayList<Vec2>()
                    k = iStart
                    steps = 0
                    while (k != iEnd && steps <= n) {
                        bwd.add(obs[k])
                        k = (k - 1 + n) % n
                        steps++
                    }
                    bwd.add(obs[iEnd])

                    val detour = if (polylineLength(listOf(pA) + fwd + listOf(pB)) <=
                        polylineLength(listOf(pA) + bwd + listOf(pB))
                    ) fwd else bwd

                    nextPath.addAll(detour)
                }
                nextPath.add(pB)
            }
            currentPath = nextPath
        }
        return currentPath
    }

    /**
     * Cura Combing Mode path-routing & retraction decision algorithm:
     * 1. Checks if `start` and `end` lie within the same allowed combing region (`combingPolygons`).
     * 2. If the straight chord stays inside the combing region, combs directly without crossing walls.
     * 3. If the straight chord crosses a concave wall/hole, routes along the interior contour boundary with string-pulling shortcuts.
     * 4. If travelling between separate islands and `avoidPrintedPartsWhenTraveling` / `avoidSupportsWhenTraveling` is enabled,
     *    detours around obstacle boundaries with `travelAvoidDistanceMm` clearance.
     * 5. Evaluates `maxCombDistanceWithNoRetractMm` and `retractBeforeOuterWall` to decide whether retraction is required.
     */
    private fun computeCuraCombedTravel(
        start: Vec2,
        end: Vec2,
        combingPolygons: List<List<Vec2>>,
        obstaclePolygons: List<List<Vec2>>,
        targetType: ToolpathType,
        lineWidthMm: Float,
        process: ProcessProfile
    ): CombedTravelResult {
        if (process.combingMode == CombingMode.OFF) {
            return CombedTravelResult(
                waypoints = listOf(start, end),
                isCombed = false,
                requiresRetract = true
            )
        }

        val snapMargin = lineWidthMm * 1.6f
        val hostPoly = combingPolygons.firstOrNull { poly ->
            val (dStart, _) = signedDistanceToContours(start, listOf(poly))
            val (dEnd, _) = signedDistanceToContours(end, listOf(poly))
            dStart <= snapMargin && dEnd <= snapMargin
        }

        val waypoints: List<Vec2>
        val isCombed: Boolean

        if (hostPoly != null) {
            isCombed = true
            waypoints = if (isChordInsidePolygon(start, end, hostPoly)) {
                listOf(start, end)
            } else {
                buildBoundaryCombedPath(start, end, hostPoly, lineWidthMm)
            }
        } else {
            isCombed = false
            waypoints = if (obstaclePolygons.isNotEmpty()) {
                detourAroundObstacles(start, end, obstaclePolygons)
            } else {
                listOf(start, end)
            }
        }

        val totalDist = polylineLength(waypoints)
        val requiresRetract = when {
            !isCombed -> true
            process.retractBeforeOuterWall && targetType == ToolpathType.OUTER_WALL -> true
            process.maxCombDistanceWithNoRetractMm > 0f && totalDist > process.maxCombDistanceWithNoRetractMm -> true
            else -> false
        }

        return CombedTravelResult(
            waypoints = waypoints,
            isCombed = isCombed,
            requiresRetract = requiresRetract
        )
    }

    private fun intersectTrianglesAtZ(triangles: List<Triangle>, rawZ: Float): List<Segment2D> {
        // Perturb Z by a tiny irrational-like epsilon so the slicing plane never lands dead-on a vertex or horizontal face
        val z = rawZ + 0.000137f
        val segments = ArrayList<Segment2D>()
        for (tri in triangles) {
            val v0 = tri.v0
            val v1 = tri.v1
            val v2 = tri.v2

            val minZ = min(v0.z, min(v1.z, v2.z))
            val maxZ = max(v0.z, max(v1.z, v2.z))
            if (z < minZ || z > maxZ || maxZ - minZ < 1e-5f) continue

            val pts = ArrayList<Vec2>(3)
            intersectEdgeAtZ(v0, v1, z)?.let { pts.add(it) }
            intersectEdgeAtZ(v1, v2, z)?.let { pts.add(it) }
            intersectEdgeAtZ(v2, v0, z)?.let { pts.add(it) }

            if (pts.size >= 2) {
                val pA = pts[0]
                val pB = if (pts[0].distanceTo(pts[1]) > 1e-4f) pts[1] else pts.getOrNull(2)
                if (pB != null && pA.distanceTo(pB) > 1e-4f) {
                    segments.add(Segment2D(pA, pB))
                }
            }
        }
        return segments
    }

    private fun intersectEdgeAtZ(a: Vec3, b: Vec3, z: Float): Vec2? {
        if ((a.z <= z && b.z >= z) || (b.z <= z && a.z >= z)) {
            val dz = b.z - a.z
            if (abs(dz) < 1e-6f) return null
            val t = ((z - a.z) / dz).coerceIn(0f, 1f)
            return Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
        return null
    }

    private fun chainSegmentsIntoPolygons(segments: List<Segment2D>): List<List<Vec2>> {
        if (segments.isEmpty()) return emptyList()
        val used = BooleanArray(segments.size)
        val polygons = ArrayList<List<Vec2>>()
        val snapTol = 0.12f

        for (startIdx in segments.indices) {
            if (used[startIdx]) continue
            used[startIdx] = true

            val poly = ArrayList<Vec2>()
            poly.add(segments[startIdx].a)
            poly.add(segments[startIdx].b)

            var searching = true
            while (searching) {
                searching = false
                val tail = poly.last()
                val head = poly.first()
                var bestIdx = -1
                var bestDist = snapTol
                var reverseMatch = false
                var prependToHead = false

                for (j in segments.indices) {
                    if (used[j]) continue
                    val dTailA = tail.distanceTo(segments[j].a)
                    if (dTailA < bestDist) {
                        bestDist = dTailA; bestIdx = j; reverseMatch = false; prependToHead = false
                    }
                    val dTailB = tail.distanceTo(segments[j].b)
                    if (dTailB < bestDist) {
                        bestDist = dTailB; bestIdx = j; reverseMatch = true; prependToHead = false
                    }
                    val dHeadA = head.distanceTo(segments[j].a)
                    if (dHeadA < bestDist) {
                        bestDist = dHeadA; bestIdx = j; reverseMatch = false; prependToHead = true
                    }
                    val dHeadB = head.distanceTo(segments[j].b)
                    if (dHeadB < bestDist) {
                        bestDist = dHeadB; bestIdx = j; reverseMatch = true; prependToHead = true
                    }
                }

                if (bestIdx >= 0) {
                    used[bestIdx] = true
                    if (!prependToHead) {
                        val nextPt = if (reverseMatch) segments[bestIdx].a else segments[bestIdx].b
                        if (nextPt.distanceTo(poly.first()) <= snapTol && poly.size >= 3) {
                            break
                        }
                        poly.add(nextPt)
                    } else {
                        val prevPt = if (reverseMatch) segments[bestIdx].a else segments[bestIdx].b
                        if (prevPt.distanceTo(poly.last()) <= snapTol && poly.size >= 3) {
                            break
                        }
                        poly.add(0, prevPt)
                    }
                    searching = true
                }
            }

            if (poly.size >= 3) {
                val cleaned = cleanAndSimplifyPolygon(ensureCounterClockwise(poly))
                if (cleaned.size >= 3) {
                    polygons.add(cleaned)
                }
            }
        }
        return polygons
    }

    /**
     * Removes duplicate points AND eliminates colinear subdivision vertices along straight edges.
     * This prevents corners from overtaking triangle-diagonal split vertices during inward offsetting!
     */
    private fun cleanAndSimplifyPolygon(pts: List<Vec2>): List<Vec2> {
        if (pts.size < 3) return emptyList()

        // 1. Remove consecutive duplicates closer than 0.02mm
        var current = ArrayList<Vec2>(pts.size)
        for (p in pts) {
            if (current.isEmpty() || current.last().distanceTo(p) > 0.02f) {
                current.add(p)
            }
        }
        if (current.size >= 2 && current.first().distanceTo(current.last()) <= 0.02f) {
            current.removeAt(current.lastIndex)
        }
        if (current.size < 3) return emptyList()

        // 2. Iteratively prune colinear vertices and 180-degree spikes
        var changed = true
        var pass = 0
        while (changed && current.size > 3 && pass < 4) {
            changed = false
            pass++
            val nextList = ArrayList<Vec2>(current.size)
            val n = current.size
            for (i in 0 until n) {
                val prev = if (nextList.isNotEmpty()) nextList.last() else current[(i + n - 1) % n]
                val curr = current[i]
                val next = current[(i + 1) % n]

                val v1 = curr - prev
                val v2 = next - curr
                val len1 = v1.length()
                val len2 = v2.length()

                if (len1 < 0.02f || len2 < 0.02f) {
                    changed = true
                    continue
                }

                val e1 = Vec2(v1.x / len1, v1.y / len1)
                val e2 = Vec2(v2.x / len2, v2.y / len2)
                val cross = abs(e1.x * e2.y - e1.y * e2.x)
                val dot = e1.x * e2.x + e1.y * e2.y

                // If prev -> curr -> next is straight (colinear) or a degenerate hairpin spike, drop curr
                if ((cross < 0.025f && dot > 0.99f) || dot < -0.98f) {
                    changed = true
                } else {
                    nextList.add(curr)
                }
            }
            if (nextList.size >= 3) {
                current = nextList
            } else {
                break
            }
        }

        return if (current.size >= 3) current else emptyList()
    }

    private fun signedPolygonArea(pts: List<Vec2>): Float {
        var area = 0f
        for (i in pts.indices) {
            val p0 = pts[i]
            val p1 = pts[(i + 1) % pts.size]
            area += (p0.x * p1.y - p1.x * p0.y)
        }
        return area * 0.5f
    }

    private fun ensureCounterClockwise(pts: List<Vec2>): List<Vec2> {
        return if (signedPolygonArea(pts) < 0f) pts.reversed() else pts
    }

    private fun offsetPolygonInward(rawPoly: List<Vec2>, insetDist: Float): List<Vec2> {
        val poly = cleanAndSimplifyPolygon(ensureCounterClockwise(rawPoly))
        if (poly.size < 3) return emptyList()

        val bounds = computeContoursBounds(listOf(poly))
        val width = bounds.second.x - bounds.first.x
        val height = bounds.second.y - bounds.first.y
        if (width <= insetDist * 2.05f || height <= insetDist * 2.05f) return emptyList()

        val n = poly.size
        val shifted = ArrayList<Vec2>(n)
        for (i in 0 until n) {
            val prev = poly[(i + n - 1) % n]
            val curr = poly[i]
            val next = poly[(i + 1) % n]

            val e1 = (curr - prev).normalized()
            val e2 = (next - curr).normalized()
            val n1 = e1.perp()
            val n2 = e2.perp()
            val bisector = (n1 + n2).normalized()
            val cosHalf = bisector.x * n1.x + bisector.y * n1.y
            val miterScale = if (cosHalf > 0.35f) (1f / cosHalf).coerceAtMost(1.85f) else 1f
            shifted.add(curr + bisector * (insetDist * miterScale))
        }

        // Prune any short edges that inverted direction after miter offset (prevents corner bowties on curved meshes)
        val pruned = ArrayList<Vec2>(n)
        var i = 0
        while (i < n) {
            val nextIdx = (i + 1) % n
            val origEdge = poly[nextIdx] - poly[i]
            val newEdge = shifted[nextIdx] - shifted[i]
            val dot = origEdge.x * newEdge.x + origEdge.y * newEdge.y
            if (dot <= 0f && origEdge.length() < insetDist * 2.5f) {
                // Collapse pinched edge to its midpoint
                pruned.add((shifted[i] + shifted[nextIdx]) * 0.5f)
                i += 2
            } else {
                pruned.add(shifted[i])
                i++
            }
        }

        val cleaned = cleanAndSimplifyPolygon(pruned)
        if (cleaned.size < 3) return emptyList()
        if (signedPolygonArea(cleaned) < insetDist * insetDist * 0.25f) {
            return emptyList()
        }
        return cleaned
    }

    private fun buildPatternLine(
        x0: Float,
        x1: Float,
        y: Float,
        z: Float,
        pattern: InfillPattern,
        spacing: Float,
        cosA: Float,
        sinA: Float
    ): List<Vec2> {
        fun unrotate(rx: Float, ry: Float): Vec2 =
            Vec2(rx * cosA - ry * sinA, rx * sinA + ry * cosA)

        val length = abs(x1 - x0)
        if ((pattern != InfillPattern.GYROID && pattern != InfillPattern.HONEYCOMB) || length < 2.0f) {
            return listOf(unrotate(x0, y), unrotate(x1, y))
        }

        val steps = max(2, (length / 1.5f).toInt().coerceAtMost(48))
        val pts = ArrayList<Vec2>(steps + 1)
        val freq = (2f * PI.toFloat()) / (spacing * 2.2f).coerceAtLeast(3.5f)
        val amp = (spacing * 0.28f).coerceAtMost(2.5f)

        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val rx = x0 + (x1 - x0) * t
            val waveOffset = if (pattern == InfillPattern.GYROID) {
                sin(rx * freq + z * 0.65f) * cos(y * freq - z * 0.45f) * amp
            } else {
                val sVal = sin(rx * freq)
                (sVal * 1.4f).coerceIn(-1f, 1f) * amp
            }
            pts.add(unrotate(rx, y + waveOffset))
        }
        return pts
    }

    private fun applyFuzzySkinToPolygon(
        poly: List<Vec2>,
        layerIdx: Int,
        pointDistMm: Float,
        thicknessMm: Float
    ): List<Vec2> {
        val out = ArrayList<Vec2>(poly.size * 2)
        val step = pointDistMm.coerceIn(0.3f, 3.0f)
        val amp = thicknessMm.coerceIn(0.05f, 1.2f)
        var seed = (layerIdx + 1) * 1103515245 + 12345

        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val edge = b - a
            val len = edge.length()
            val dir = edge.normalized()
            val norm = dir.perp()
            val subdivs = max(1, (len / step).toInt().coerceAtMost(32))
            for (s in 0 until subdivs) {
                val t = s.toFloat() / subdivs
                val basePt = a + edge * t
                seed = seed * 1664525 + 1013904223
                val rand01 = ((seed ushr 8) and 0xFFFF) / 65535f
                val jitter = (rand01 - 0.5f) * 2f * amp
                out.add(basePt + norm * jitter)
            }
        }
        return if (out.size >= 3) out else poly
    }

    private fun computeContoursBounds(contours: List<List<Vec2>>): Pair<Vec2, Vec2> {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (c in contours) {
            for (p in c) {
                minX = min(minX, p.x); minY = min(minY, p.y)
                maxX = max(maxX, p.x); maxY = max(maxY, p.y)
            }
        }
        if (!minX.isFinite()) return Vec2(0f, 0f) to Vec2(0f, 0f)
        return Vec2(minX, minY) to Vec2(maxX, maxY)
    }

    private fun createRoundedRectLoop(minX: Float, minY: Float, maxX: Float, maxY: Float): List<Vec2> =
        listOf(
            Vec2(minX, minY),
            Vec2(maxX, minY),
            Vec2(maxX, maxY),
            Vec2(minX, maxY)
        )

    private fun createOctagonLoop(cx: Float, cy: Float, r: Float): List<Vec2> {
        val pts = ArrayList<Vec2>(8)
        for (i in 0 until 8) {
            val a = i * (PI.toFloat() / 4f)
            pts.add(Vec2(cx + cos(a) * r, cy + sin(a) * r))
        }
        return pts
    }

    private fun generateGCodeAndStats(
        layers: List<SliceLayer>,
        printer: PrinterProfile,
        filament: FilamentProfile,
        process: ProcessProfile
    ): Pair<String, PrintStatistics> {
        var totalExtrusionMm = 0f
        var supportAndBrimExtrusionMm = 0f
        var totalTimeSeconds = 0f
        var totalSegments = 0
        val featureTimes = HashMap<ToolpathType, Float>()

        val sb = StringBuilder(min(layers.size * 4096, 4 * 1024 * 1024))
        sb.append("; HEADER_BLOCK_START\n")
        sb.append("; generated by DolphinSlicer (OrcaSlicer Android) 2.2.0\n")
        sb.append("; printer_model = ${printer.name}\n")
        sb.append("; filament_type = ${filament.materialType}\n")
        sb.append("; layer_height = ${process.layerHeightMm}\n")
        sb.append("; wall_loops = ${process.wallLoops}\n")
        sb.append("; top_shell_layers = ${process.topShellLayers}\n")
        sb.append("; bottom_shell_layers = ${process.bottomShellLayers}\n")
        sb.append("; sparse_infill_density = ${process.sparseInfillDensityPercent}%\n")
        sb.append("; sparse_infill_pattern = ${process.sparseInfillPattern.label}\n")
        sb.append("; combing_mode = ${process.combingMode.label}\n")
        sb.append("; max_comb_distance_with_no_retract = ${process.maxCombDistanceWithNoRetractMm}\n")
        sb.append("; total_layers = ${layers.size}\n")
        sb.append("; HEADER_BLOCK_END\n\n")

        val startResolved = printer.startGcode
            .replace("[bed_temperature_initial_layer]", filament.bedTempInitialC.toString())
            .replace("[nozzle_temperature_initial_layer]", filament.nozzleTempInitialC.toString())
            .replace("[bed_depth]", printer.bedDepthY.toInt().toString())
        sb.append(startResolved).append("\n")

        if (filament.enablePressureAdvance) {
            sb.append(String.format(Locale.US, "SET_PRESSURE_ADVANCE ADVANCE=%.4f ; Klipper PA\n", filament.pressureAdvance))
            sb.append(String.format(Locale.US, "M900 K%.4f ; Marlin Linear Advance\n", filament.pressureAdvance))
        }
        if (process.excludeObjectGcode) {
            sb.append("EXCLUDE_OBJECT_DEFINE NAME=Plate_Object_1 CENTER=${(printer.bedWidthX * 0.5f).toInt()},${(printer.bedDepthY * 0.5f).toInt()}\n")
        }
        sb.append("\n")

        val bedCenterX = printer.bedWidthX * 0.5f
        val bedCenterY = printer.bedDepthY * 0.5f

        for (layer in layers) {
            sb.append(";LAYER_CHANGE\n")
            sb.append(String.format(Locale.US, ";Z:%.3f\n", layer.zHeightMm))
            sb.append(String.format(Locale.US, ";HEIGHT:%.3f\n", layer.layerHeightMm))
            sb.append(String.format(Locale.US, "G1 Z%.3f F1800\n", layer.zHeightMm))

            if (layer.index == 1) {
                val fanPwm = (filament.fanSpeedPercent * 255) / 100
                sb.append("M106 S$fanPwm\n")
                sb.append("M104 S${filament.nozzleTempOtherC}\n")
                sb.append("M140 S${filament.bedTempOtherC}\n")
            }

            var activeType: ToolpathType? = null
            var isRetracted = false
            val retractLen = printer.retractionLengthMm.coerceIn(0f, 10f)
            val retractFeed = (printer.retractionSpeedMmS.coerceAtLeast(10f) * 60f).toInt()
            val effectiveZHop = if (process.zHopWhenRetracted) {
                process.zHopHeightMm.coerceIn(0f, 3.0f)
            } else {
                0f
            }

            for (seg in layer.segments) {
                totalSegments++
                val dist = seg.start.distanceTo(seg.end)
                val dt = (dist / seg.speedMmS.coerceAtLeast(5f)) * 1.12f
                totalTimeSeconds += dt
                featureTimes[seg.type] = (featureTimes[seg.type] ?: 0f) + dt
                totalExtrusionMm += seg.extrusionMm
                if (seg.type == ToolpathType.SUPPORT || seg.type == ToolpathType.SKIRT_BRIM) {
                    supportAndBrimExtrusionMm += seg.extrusionMm
                }

                val gx = seg.end.x + bedCenterX
                val gy = seg.end.y + bedCenterY
                val feedrate = (seg.speedMmS * 60f).toInt()

                if (seg.type == ToolpathType.TRAVEL || seg.extrusionMm <= 0f) {
                    if (seg.requiresRetract && !isRetracted && retractLen > 0.01f) {
                        sb.append(String.format(Locale.US, "G1 E-%.4f F%d ; retract\n", retractLen, retractFeed))
                        isRetracted = true
                    }
                    val zTravel = if (isRetracted && effectiveZHop > 0.01f && (!process.zHopOnlyOverPrintedParts || !seg.isCombed)) {
                        seg.end.z + effectiveZHop
                    } else {
                        seg.end.z
                    }
                    if (process.verboseGcode && seg.isCombed) {
                        sb.append(";COMBED_TRAVEL\n")
                    }
                    sb.append(String.format(Locale.US, "G0 X%.3f Y%.3f Z%.3f F%d\n", gx, gy, zTravel, feedrate))
                } else {
                    if (isRetracted) {
                        if (effectiveZHop > 0.01f) {
                            sb.append(String.format(Locale.US, "G0 Z%.3f F1800 ; restore Z hop\n", seg.end.z))
                        }
                        sb.append(String.format(Locale.US, "G1 E%.4f F%d ; unretract\n", retractLen, retractFeed))
                        isRetracted = false
                    }
                    if (seg.type != activeType) {
                        activeType = seg.type
                        sb.append(";TYPE:${seg.type.gcodeComment}\n")
                    }
                    sb.append(
                        String.format(
                            Locale.US,
                            "G1 X%.3f Y%.3f E%.5f F%d\n",
                            gx,
                            gy,
                            seg.extrusionMm,
                            feedrate
                        )
                    )
                }
            }
        }

        val endResolved = printer.endGcode
            .replace("[bed_depth]", printer.bedDepthY.toInt().toString())
        sb.append("\n").append(endResolved).append("\n")

        val filamentLengthMeters = totalExtrusionMm / 1000f
        val radiusMm = filament.diameterMm * 0.5f
        val crossSectionMm2 = PI.toFloat() * radiusMm * radiusMm
        val volumeCm3 = (totalExtrusionMm * crossSectionMm2) / 1000f
        val weightGrams = volumeCm3 * filament.densityGPerCm3
        val supportVolumeCm3 = (supportAndBrimExtrusionMm * crossSectionMm2) / 1000f
        val supportWeightGrams = supportVolumeCm3 * filament.densityGPerCm3
        val modelOnlyWeightGrams = (weightGrams - supportWeightGrams).coerceAtLeast(0f)
        val estimatedCostUsd = (weightGrams / 1000f) * filament.costPerKgUsd

        val stats = PrintStatistics(
            layerCount = layers.size,
            totalSegmentCount = totalSegments,
            filamentLengthMeters = filamentLengthMeters,
            filamentWeightGrams = weightGrams,
            estimatedCostUsd = estimatedCostUsd,
            totalTimeSeconds = totalTimeSeconds,
            featureTimeSeconds = featureTimes,
            filamentVolumeCm3 = volumeCm3,
            modelWeightGrams = modelOnlyWeightGrams,
            supportWasteWeightGrams = supportWeightGrams
        )

        return sb.toString() to stats
    }
}
