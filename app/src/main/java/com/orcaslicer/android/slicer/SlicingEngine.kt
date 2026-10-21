package com.orcaslicer.android.slicer

import com.orcaslicer.android.model.BrimType
import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.FuzzySkinType
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.IroningType
import com.orcaslicer.android.model.PrintStatistics
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.ProcessProfile
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.model.SliceLayer
import com.orcaslicer.android.model.SliceResult
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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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

            // Detect overhang points for support generation if enabled
            val overhangPillars = if (process.enableSupport) {
                computeSupportPillars(allTriangles, process.supportThresholdAngleDeg)
            } else {
                emptyList()
            }

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
                                overhangPillars = overhangPillars,
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
        overhangPillars: List<SupportPillar>,
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

        fun appendPath(
            points: List<Vec2>,
            type: ToolpathType,
            speedMmS: Float,
            lineWidthMm: Float,
            closed: Boolean,
            flowMultiplier: Float = 1.0f
        ) {
            if (points.size < 2) return
            val extrusionPerMm = ((lineWidthMm * layerHeight) / filamentArea) * filament.flowRatio * flowMultiplier
            val first3D = Vec3(points[0].x, points[0].y, nominalZ)
            lastNozzlePos?.let { prev ->
                if (prev.distanceTo(first3D) > lineWidthMm * 1.6f) {
                    segments.add(
                        ToolpathSegment(
                            start = prev,
                            end = first3D,
                            type = ToolpathType.TRAVEL,
                            extrusionMm = 0f,
                            speedMmS = process.travelSpeedMmS,
                            layerIndex = layerIdx,
                            widthMm = 0.18f
                        )
                    )
                }
            }

            for (i in 0 until points.size - 1) {
                val p0 = Vec3(points[i].x, points[i].y, nominalZ)
                val p1 = Vec3(points[i + 1].x, points[i + 1].y, nominalZ)
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

            if (closed && points.size >= 3) {
                val pLast = Vec3(points.last().x, points.last().y, nominalZ)
                val pFirst = Vec3(points.first().x, points.first().y, nominalZ)
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

        // 2. Support structures on this layer
        if (process.enableSupport && overhangPillars.isNotEmpty()) {
            for (pillar in overhangPillars) {
                if (nominalZ < pillar.maxZ - layerHeight * 0.8f) {
                    val radius = if (process.supportType.name.contains("TREE")) {
                        val t = (nominalZ / pillar.maxZ).coerceIn(0f, 1f)
                        2.1f * (1f - 0.35f * t)
                    } else {
                        1.8f
                    }
                    val supportLoop = createOctagonLoop(pillar.x, pillar.y, radius)
                    appendPath(supportLoop, ToolpathType.SUPPORT, process.supportSpeedMmS, supportWidth, closed = true)
                }
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

    private data class SupportPillar(val x: Float, val y: Float, val maxZ: Float)

    private fun computeSupportPillars(
        triangles: List<Triangle>,
        thresholdAngleDeg: Int
    ): List<SupportPillar> {
        val cosThreshold = -cos((90 - thresholdAngleDeg.coerceIn(10, 80)) * (PI.toFloat() / 180f))
        val gridSpacing = 4.5f
        val pillarMap = HashMap<Long, Float>()

        for (tri in triangles) {
            val minZ = min(tri.v0.z, min(tri.v1.z, tri.v2.z))
            if (minZ < 1.2f) continue
            if (tri.normal.z < cosThreshold) {
                val minX = min(tri.v0.x, min(tri.v1.x, tri.v2.x))
                val maxX = max(tri.v0.x, max(tri.v1.x, tri.v2.x))
                val minY = min(tri.v0.y, min(tri.v1.y, tri.v2.y))
                val maxY = max(tri.v0.y, max(tri.v1.y, tri.v2.y))
                val avgZ = (tri.v0.z + tri.v1.z + tri.v2.z) / 3f

                val gx0 = floor(minX / gridSpacing).toInt()
                val gx1 = floor(maxX / gridSpacing).toInt()
                val gy0 = floor(minY / gridSpacing).toInt()
                val gy1 = floor(maxY / gridSpacing).toInt()

                for (gx in gx0..gx1) {
                    for (gy in gy0..gy1) {
                        val key = (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)
                        val prev = pillarMap[key] ?: 0f
                        if (avgZ > prev) pillarMap[key] = avgZ
                    }
                }
            }
        }

        return pillarMap.map { (key, topZ) ->
            val gx = (key shr 32).toInt()
            val gy = key.toInt()
            SupportPillar(gx * gridSpacing, gy * gridSpacing, topZ)
        }
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

                if (seg.type != activeType && seg.type != ToolpathType.TRAVEL) {
                    activeType = seg.type
                    sb.append(";TYPE:${seg.type.gcodeComment}\n")
                }

                val gx = seg.end.x + bedCenterX
                val gy = seg.end.y + bedCenterY
                val feedrate = (seg.speedMmS * 60f).toInt()

                if (seg.type == ToolpathType.TRAVEL || seg.extrusionMm <= 0f) {
                    sb.append(String.format(Locale.US, "G0 X%.3f Y%.3f Z%.3f F%d\n", gx, gy, seg.end.z, feedrate))
                } else {
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
