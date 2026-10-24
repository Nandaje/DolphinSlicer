package com.orcaslicer.android.slicer

import com.orcaslicer.android.model.BoundingBox
import com.orcaslicer.android.model.BrimType
import com.orcaslicer.android.model.CombingMode
import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.FuzzySkinType
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.IroningType
import com.orcaslicer.android.model.PrintStatistics
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.ProcessProfile
import com.orcaslicer.android.model.ScarfSeamType
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
import com.orcaslicer.android.model.WallsPrintingOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
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

/**
 * ============================================================================
 * Two-Pass Multi-Threaded 3D Slicing Engine (`SlicingEngine.kt`)
 * ============================================================================
 * Converts 3D build-plate triangle meshes ([SceneObject]) into layered 2D
 * toolpaths ([SliceLayer]) and machine-ready G-code ([SliceResult]).
 *
 * Architecture & Pipeline Overview (for human maintainers):
 * ----------------------------------------------------------------------------
 * - **Pass 1 (Parallel per-layer Z-plane contour & wall extraction)**:
 *   1. `intersectTrianglesAtZ`: Intersects all world-space mesh triangles with
 *      the horizontal slice plane at `sliceZ` (in mm) to produce raw 2D line segments.
 *   2. `chainSegmentsIntoPolygons` + `cleanAndSimplifyPolygon`: Stitches raw segments
 *      into closed, counter-clockwise 2D polygon contours and simplifies collinear noise.
 *   3. `offsetPolygonInward`: Insets each outer contour repeatedly by `wallLineWidth`
 *      to generate `Outer Wall`, `Inner Wall` loops, and the `innermostBoundary`
 *      region reserved for skin/infill.
 *
 * - **Pass 2 (Parallel per-layer skin, infill, support & travel generation)**:
 *   1. **Top & Bottom Horizontal Shells (`isHorizontalSkin` / `hasLocalRoofOrFloor`)**:
 *      Compares the current layer's contours against layers above (`topShellLayers`)
 *      and below (`bottomShellLayers`) to detect both full top/bottom layers AND
 *      intermediate horizontal roofs/steps (such as Benchy decks and roofs).
 *   2. **Internal Solid & Sparse Infill (`generateClippedInfillSegments`)**:
 *      Clips infill patterns (`Gyroid`, `Grid`, `Honeycomb`, `Adaptive Cubic`,
 *      `Triangles`, `Tri-Hexagon`, `Rectilinear`, `Concentric`, `Cross Hatch`,
 *      `Lightning`) strictly inside the polygon's `innermostBoundary` using
 *      even-odd ray-casting (`computePolygonScanlineIntervals`).
 *   3. **Support Structures (`Normal Grid` & `Tree Organic`)**:
 *      Detects overhang regions exceeding `supportThresholdAngleDeg` and builds
 *      either rectilinear grid columns or organic branching tree trunks.
 *   4. **Combed Travel & Seam Alignment (`computeCuraCombedTravel`)**:
 *      Routes non-extruding travel moves inside part boundaries to prevent stringing,
 *      and applies optional Scarf Joint Seam ramping.
 *   5. **G-Code & Cost Statistics (`generateGCodeAndStats`)**:
 *      Serializes all layers into dialect-specific G-code (`Klipper`, `Bambu`,
 *      `Marlin`, `Prusa`, `RepRap`) and calculates exact filament length, weight,
 *      print time, and production cost.
 */
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

    private data class CombingRegion(
        val poly: List<Vec2>,
        val guidePoly: List<Vec2>,
        val holes: List<List<Vec2>>,
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float
    ) {
        fun containsPointFast(pt: Vec2): Boolean {
            if (pt.x < minX || pt.x > maxX || pt.y < minY || pt.y > maxY) return false
            if (!isPointInPolygon(pt, poly)) return false
            return holes.none { isPointInPolygon(pt, it) }
        }

        fun isChordInsideRegion(a: Vec2, b: Vec2): Boolean {
            if (!isChordInsidePolygon(a, b, poly)) return false
            val mid = (a + b) * 0.5f
            for (h in holes) {
                if (doesSegmentCrossPolygonBoundary(a, b, h) || isPointInPolygon(mid, h)) {
                    return false
                }
            }
            return true
        }
    }

    suspend fun sliceScene(
        objects: List<SceneObject>,
        printer: PrinterProfile,
        filament: FilamentProfile,
        process: ProcessProfile,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<SliceResult> = withContext(Dispatchers.Default) {
        try {
            val visibleObjects = objects.filter { it.visible }
            if (visibleObjects.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Add or enable at least one 3D model on the build plate before slicing.")
                )
            }

            onProgress(0.04f, "Transforming meshes to build plate...")
            val allTriangles = ArrayList<Triangle>()
            val supportTriangles = ArrayList<Triangle>()
            var maxZ = 0f
            for (obj in visibleObjects) {
                ensureActive()
                val worldTris = obj.worldTriangles()
                val objSupportEnabled = obj.supportEnabledOverride ?: process.enableSupport
                for (t in worldTris) {
                    allTriangles.add(t)
                    if (objSupportEnabled) {
                        supportTriangles.add(t)
                    }
                    maxZ = max(maxZ, max(t.v0.z, max(t.v1.z, t.v2.z)))
                }
            }

            if (allTriangles.isEmpty() || maxZ <= 0.05f) {
                return@withContext Result.failure(
                    IllegalArgumentException("Selected models have zero height or no triangles.")
                )
            }

            val effectiveProcess = process.copy(
                wallLoops = visibleObjects.mapNotNull { it.wallLoopsOverride }.firstOrNull() ?: process.wallLoops,
                sparseInfillDensityPercent = visibleObjects.mapNotNull { it.infillDensityOverride }.firstOrNull() ?: process.sparseInfillDensityPercent,
                enableSupport = supportTriangles.isNotEmpty()
            )
            val objectWallOverrides = visibleObjects.mapNotNull { obj ->
                obj.wallLoopsOverride?.let { loops -> obj.worldBoundingBox() to loops.coerceIn(1, 15) }
            }

            val layerHeight = effectiveProcess.layerHeightMm.coerceIn(0.05f, 0.60f)
            val firstLayerHeight = effectiveProcess.initialLayerHeightMm.coerceIn(0.08f, 0.60f)

            // Compute exact layer count where every layer's mid-plane sliceZ is strictly inside [0, maxZ)
            val additionalLayers = if (maxZ > firstLayerHeight) {
                floor(((maxZ - firstLayerHeight) / layerHeight) + 0.5f - 1e-4f).toInt().coerceAtLeast(0)
            } else {
                0
            }
            val totalLayers = (1 + additionalLayers).coerceIn(1, 2500)

            // Pre-bucket triangles by Z-layer range so Pass 1 only tests triangles crossing each layer's Z plane
            onProgress(0.09f, "Indexing ${allTriangles.size} triangles across $totalLayers layers...")
            val trianglesByLayer = Array(totalLayers) { ArrayList<Triangle>() }
            val safeMaxSliceZ = (maxZ - 0.02f).coerceAtLeast(0.02f)

            for (tri in allTriangles) {
                val minTriZ = min(tri.v0.z, min(tri.v1.z, tri.v2.z))
                val maxTriZ = max(tri.v0.z, max(tri.v1.z, tri.v2.z))
                if (maxTriZ - minTriZ < 1e-5f) continue

                val startLayer = if (minTriZ <= firstLayerHeight) {
                    0
                } else {
                    floor((minTriZ - firstLayerHeight) / layerHeight).toInt().coerceAtLeast(0)
                }.coerceIn(0, totalLayers - 1)

                val endLayer = if (maxTriZ <= firstLayerHeight) {
                    0
                } else {
                    ceil((maxTriZ - firstLayerHeight) / layerHeight + 1f).toInt()
                }.coerceIn(startLayer, totalLayers - 1)

                for (l in startLayer..endLayer) {
                    val rawZ = if (l == 0) firstLayerHeight * 0.5f else firstLayerHeight + (l - 0.5f) * layerHeight
                    val z = rawZ.coerceIn(0.02f, safeMaxSliceZ) + 0.000137f
                    if (z in minTriZ..maxTriZ) {
                        trianglesByLayer[l].add(tri)
                    }
                }
            }

            onProgress(0.12f, "Pass 1/2: Slicing $totalLayers layer contours & perimeters...")

            val cpuParallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            val chunkSize = max(1, (totalLayers + cpuParallelism - 1) / cpuParallelism)
            val completedPass1 = AtomicInteger(0)
            val reportStep = max(1, totalLayers / 20)

            // PASS 1: Compute 2D contours, wall loops, and infill boundary polygons for all layers in parallel
            val geometryChunks = (0 until totalLayers step chunkSize).map { startIdx ->
                val endIdx = min(totalLayers, startIdx + chunkSize)
                async(Dispatchers.Default) {
                    val list = ArrayList<LayerContourGeometry>(endIdx - startIdx)
                    for (layerIdx in startIdx until endIdx) {
                        ensureActive()
                        val rawZ = if (layerIdx == 0) {
                            firstLayerHeight * 0.5f
                        } else {
                            firstLayerHeight + (layerIdx - 0.5f) * layerHeight
                        }
                        val safeSliceZ = rawZ.coerceIn(0.02f, safeMaxSliceZ)
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
                                triangles = trianglesByLayer[layerIdx],
                                process = effectiveProcess,
                                objectWallOverrides = objectWallOverrides
                            )
                        )
                        val done = completedPass1.incrementAndGet()
                        if (done % reportStep == 0 || done == totalLayers) {
                            val p = 0.12f + 0.25f * (done.toFloat() / totalLayers)
                            onProgress(p, "Pass 1/2: Contours & walls ($done / $totalLayers layers)...")
                        }
                    }
                    list
                }
            }

            val allGeometries = geometryChunks.awaitAll().flatten().sortedBy { it.layerIdx }

            // Compute OrcaSlicer Normal or Tree (Organic) Support Plans across all layers if support is enabled
            val layerSupportPlans: List<LayerSupportPlan> = if (effectiveProcess.enableSupport && supportTriangles.isNotEmpty()) {
                ensureActive()
                onProgress(0.39f, "Generating OrcaSlicer ${effectiveProcess.supportType.label} structures...")
                if (effectiveProcess.supportType == SupportType.TREE_ORGANIC || effectiveProcess.supportType == SupportType.TREE_MANUAL) {
                    computeTreeSupportPlans(supportTriangles, allGeometries, effectiveProcess)
                } else {
                    computeNormalSupportPlans(supportTriangles, allGeometries, effectiveProcess)
                }
            } else {
                List(totalLayers) { LayerSupportPlan.EMPTY }
            }

            onProgress(0.48f, "Pass 2/2: Generating Top/Bottom surfaces, solid shells & infill...")
            val completedPass2 = AtomicInteger(0)

            // PASS 2: Generate toolpaths for every layer using multi-layer region classification
            val layerChunks = (0 until totalLayers step chunkSize).map { startIdx ->
                val endIdx = min(totalLayers, startIdx + chunkSize)
                async(Dispatchers.Default) {
                    val chunkLayers = ArrayList<SliceLayer>(endIdx - startIdx)
                    for (layerIdx in startIdx until endIdx) {
                        ensureActive()
                        chunkLayers.add(
                            buildLayerToolpaths(
                                layerIdx = layerIdx,
                                totalLayers = totalLayers,
                                allGeometries = allGeometries,
                                supportPlan = layerSupportPlans.getOrElse(layerIdx) { LayerSupportPlan.EMPTY },
                                filament = filament,
                                process = effectiveProcess
                            )
                        )
                        val done = completedPass2.incrementAndGet()
                        if (done % reportStep == 0 || done == totalLayers) {
                            val p = 0.48f + 0.34f * (done.toFloat() / totalLayers)
                            onProgress(p, "Pass 2/2: Toolpaths & infill ($done / $totalLayers layers)...")
                        }
                    }
                    chunkLayers
                }
            }

            val slicedLayers = layerChunks.awaitAll().flatten().sortedBy { it.index }
            ensureActive()
            onProgress(0.84f, "Generating G-code & calculating print statistics...")

            val (gcode, stats) = generateGCodeAndStats(
                layers = slicedLayers,
                printer = printer,
                filament = filament,
                process = effectiveProcess
            )

            ensureActive()
            onProgress(0.95f, "Building 3D volumetric toolpath preview...")
            val result = SliceResult(slicedLayers, stats, gcode)

            onProgress(1.0f, "Slicing complete (${stats.layerCount} layers, ${stats.formattedTotalTime})")
            Result.success(result)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(IllegalStateException("Slicing failed on complex mesh: ${t.message ?: t.javaClass.simpleName}", t))
        }
    }

    private fun computeLayerContourGeometry(
        layerIdx: Int,
        sliceZ: Float,
        nominalZ: Float,
        layerHeight: Float,
        triangles: List<Triangle>,
        process: ProcessProfile,
        objectWallOverrides: List<Pair<BoundingBox, Int>> = emptyList()
    ): LayerContourGeometry {
        val rawSegments = intersectTrianglesAtZ(triangles, sliceZ)
        val rawContours = chainSegmentsIntoPolygons(rawSegments)

        val isInitialLayer = layerIdx == 0
        val elephantFootInset = if (isInitialLayer && process.elephantFootCompensationMm > 0.01f) {
            process.elephantFootCompensationMm.coerceIn(0f, 0.35f)
        } else {
            0f
        }
        val xyContourComp = process.xyContourCompensationMm.coerceIn(-2.0f, 2.0f)
        val xyHoleComp = process.xyHoleCompensationMm.coerceIn(-2.0f, 2.0f)

        val outerWallWidth = if (isInitialLayer) {
            process.initialLayerLineWidthMm.coerceIn(0.25f, 1.0f)
        } else {
            process.outerWallLineWidthMm.coerceIn(0.25f, 1.0f)
        }
        val innerWallWidth = process.innerWallLineWidthMm.coerceIn(0.25f, 1.0f)

        val defaultWallCount = when {
            process.spiralVaseMode -> 1
            isInitialLayer && process.onlyOneWallFirstLayer -> 1
            else -> process.wallLoops.coerceIn(1, 15)
        }

        val outerContours = ArrayList<List<Vec2>>()
        val wallLoopsPerContour = ArrayList<List<List<Vec2>>>()
        val infillPolygons = ArrayList<List<Vec2>>()

        // Orient all raw contours CCW for consistent normal calculations, and classify Outer vs Hole by nesting depth
        val validContours = rawContours.filter { it.size >= 3 }.map { ensureCounterClockwise(it) }
        val areas = validContours.map { abs(signedPolygonArea(it)) }

        for (i in validContours.indices) {
            val rawContour = validContours[i]
            val myArea = areas[i]
            val samplePt = rawContour[0]

            val wallCount = if (!process.spiralVaseMode && !(isInitialLayer && process.onlyOneWallFirstLayer) && objectWallOverrides.isNotEmpty()) {
                objectWallOverrides.firstOrNull { (box, _) ->
                    samplePt.x in (box.min.x - 1f)..(box.max.x + 1f) &&
                        samplePt.y in (box.min.y - 1f)..(box.max.y + 1f) &&
                        sliceZ in (box.min.z - 1f)..(box.max.z + 1f)
                }?.second ?: defaultWallCount
            } else {
                defaultWallCount
            }

            // Count how many larger contours on this layer enclose `rawContour`
            var nestingDepth = 0
            for (j in validContours.indices) {
                if (i != j && areas[j] > myArea + 1e-3f) {
                    if (isPointInPolygon(samplePt, validContours[j])) {
                        nestingDepth++
                    }
                }
            }
            val isHoleContour = (nestingDepth % 2) == 1

            if (!isHoleContour) {
                // OUTER SOLID BOUNDARY: apply elephant foot inset and X-Y contour compensation
                val netOuterInset = elephantFootInset - xyContourComp
                val baseContour = when {
                    netOuterInset > 0.005f -> offsetPolygonInward(rawContour, netOuterInset).ifEmpty { rawContour }
                    netOuterInset < -0.005f -> offsetPolygonOutward(rawContour, -netOuterInset).ifEmpty { rawContour }
                    else -> rawContour
                }
                outerContours.add(baseContour)

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
                    val overlapFraction = (process.infillWallOverlapPercent.coerceIn(0, 60) / 100f)
                    val infillInset = (innerWallWidth * (0.65f - overlapFraction)).coerceAtLeast(0.05f)
                    val infillPoly = offsetPolygonInward(currentRing, infillInset)
                    if (infillPoly.size >= 3) {
                        infillPolygons.add(infillPoly)
                    } else if (currentRing.size >= 3) {
                        infillPolygons.add(currentRing)
                    }
                }
            } else if (!process.spiralVaseMode) {
                // INNER HOLE BOUNDARY: offset OUTWARD into the solid material surrounding the hole (plus X-Y hole compensation)
                val netHoleOutset = elephantFootInset + xyHoleComp
                val baseHole = when {
                    netHoleOutset > 0.005f -> offsetPolygonOutward(rawContour, netHoleOutset).ifEmpty { rawContour }
                    netHoleOutset < -0.005f -> offsetPolygonInward(rawContour, -netHoleOutset).ifEmpty { rawContour }
                    else -> rawContour
                }
                outerContours.add(baseHole)

                val firstHoleWallCenterline = offsetPolygonOutward(baseHole, outerWallWidth * 0.45f).ifEmpty { baseHole }
                val wallsForHole = ArrayList<List<Vec2>>(wallCount)
                wallsForHole.add(firstHoleWallCenterline)

                var currentRing = firstHoleWallCenterline
                for (w in 1 until wallCount) {
                    val stepOutset = if (w == 1) (outerWallWidth + innerWallWidth) * 0.48f else innerWallWidth * 0.95f
                    val outset = offsetPolygonOutward(currentRing, stepOutset)
                    if (outset.size < 3) break
                    wallsForHole.add(outset)
                    currentRing = outset
                }
                wallLoopsPerContour.add(wallsForHole)

                val overlapFraction = (process.infillWallOverlapPercent.coerceIn(0, 60) / 100f)
                val infillOutset = (innerWallWidth * (0.65f - overlapFraction)).coerceAtLeast(0.05f)
                val holeInfillCutout = offsetPolygonOutward(currentRing, infillOutset)
                if (holeInfillCutout.size >= 3) {
                    infillPolygons.add(holeInfillCutout)
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

        // Build allowed combing boundary polygons once per layer based on Cura Combing Mode
        val combingAllowedPolygons: List<List<Vec2>> = when (process.combingMode) {
            CombingMode.OFF -> emptyList()
            CombingMode.ALL -> geom.outerContours
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

        // Separate combingAllowedPolygons into Outer Islands vs Inner Holes by nesting depth
        val combValid = combingAllowedPolygons.filter { it.size >= 3 }
        val combAreas = combValid.map { abs(signedPolygonArea(it)) }
        val combingRegions = ArrayList<CombingRegion>()
        for (i in combValid.indices) {
            val poly = combValid[i]
            val myArea = combAreas[i]
            var depth = 0
            for (j in combValid.indices) {
                if (i != j && combAreas[j] > myArea + 1e-3f && isPointInPolygon(poly[0], combValid[j])) {
                    depth++
                }
            }
            if (depth % 2 == 0) {
                // Find direct holes inside this outer combing island
                val islandHoles = ArrayList<List<Vec2>>()
                for (j in combValid.indices) {
                    if (i != j && combAreas[j] < myArea - 1e-3f && isPointInPolygon(combValid[j][0], poly)) {
                        islandHoles.add(simplifyClosedPolygonRdp(combValid[j], 0.08f))
                    }
                }
                val b = computeContoursBounds(listOf(poly))
                val insetGuide = offsetPolygonInward(poly, outerWallWidth * 0.4f).ifEmpty { poly }
                val fastGuide = downsamplePolygon(simplifyClosedPolygonRdp(insetGuide, 0.08f), maxVertices = 32)
                combingRegions.add(
                    CombingRegion(
                        poly = downsamplePolygon(simplifyClosedPolygonRdp(poly, 0.06f), maxVertices = 64),
                        guidePoly = fastGuide,
                        holes = islandHoles,
                        minX = b.first.x,
                        minY = b.first.y,
                        maxX = b.second.x,
                        maxY = b.second.y
                    )
                )
            }
        }

        // Build travel obstacle polygons when "Avoid printed parts / supports when traveling" is enabled
        val travelObstaclePolygons: List<List<Vec2>> = if (process.combingMode != CombingMode.OFF &&
            (process.avoidPrintedPartsWhenTraveling || process.avoidSupportsWhenTraveling)
        ) {
            val avoidDist = process.travelAvoidDistanceMm.coerceIn(0.1f, 5.0f)
            val obs = ArrayList<List<Vec2>>()
            if (process.avoidPrintedPartsWhenTraveling) {
                for (c in geom.outerContours.take(12)) {
                    obs.add(downsamplePolygon(offsetPolygonOutward(c, avoidDist).ifEmpty { c }, maxVertices = 24))
                }
            }
            if (process.avoidSupportsWhenTraveling) {
                for (sLoop in supportPlan.closedLoops.take(12)) {
                    obs.add(downsamplePolygon(offsetPolygonOutward(sLoop, avoidDist).ifEmpty { sLoop }, maxVertices = 20))
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
            flowMultiplier: Float = 1.0f,
            wallLoopOrderOffset: Int = 0
        ) {
            if (points.size < 2) return
            val orderedPts = if (closed && points.size >= 3 && (type == ToolpathType.OUTER_WALL || type == ToolpathType.INNER_WALL)) {
                val baseAligned = when (process.seamPosition) {
                    SeamPosition.NEAREST -> {
                        val refPt = lastNozzlePos?.let { Vec2(it.x, it.y) } ?: Vec2(process.layerStartXMm, process.layerStartYMm)
                        alignClosedPolygonStart(points, refPt)
                    }
                    SeamPosition.BACK -> {
                        val b = computeContoursBounds(listOf(points))
                        val refPt = Vec2((b.first.x + b.second.x) * 0.5f, b.second.y + 100f)
                        alignClosedPolygonStart(points, refPt)
                    }
                    SeamPosition.RANDOM -> {
                        val shift = (((layerIdx + 1) * 1103515245 + 12345) ushr 1) % points.size
                        if (shift == 0) points else (points.subList(shift, points.size) + points.subList(0, shift))
                    }
                    SeamPosition.ALIGNED -> {
                        val refPt = Vec2(process.layerStartXMm, process.layerStartYMm)
                        alignClosedPolygonStart(points, refPt)
                    }
                }
                if (process.staggeredInnerSeams && type == ToolpathType.INNER_WALL && wallLoopOrderOffset > 0 && baseAligned.size >= 4) {
                    val shift = wallLoopOrderOffset.coerceAtLeast(1) % baseAligned.size
                    baseAligned.subList(shift, baseAligned.size) + baseAligned.subList(0, shift)
                } else {
                    baseAligned
                }
            } else {
                points
            }

            val useScarf = closed && orderedPts.size >= 3 && (
                (type == ToolpathType.OUTER_WALL && process.scarfJointSeam != ScarfSeamType.NONE) ||
                    (type == ToolpathType.INNER_WALL && process.scarfJointSeam == ScarfSeamType.CONTOUR_AND_HOLE)
                )
            val scarfSpeedFactor = if (useScarf) (process.wipeSpeedPercent.coerceIn(20, 150) / 100f) else 1.0f

            val extrusionPerMm = ((lineWidthMm * layerHeight) / filamentArea) * filament.flowRatio * flowMultiplier
            val first3D = Vec3(orderedPts[0].x, orderedPts[0].y, nominalZ)
            lastNozzlePos?.let { prev ->
                val gapDist = prev.distanceTo(first3D)
                if (gapDist > lineWidthMm * 1.6f) {
                    val combedTravel = computeCuraCombedTravel(
                        start = Vec2(prev.x, prev.y),
                        end = Vec2(first3D.x, first3D.y),
                        combingRegions = combingRegions,
                        obstaclePolygons = travelObstaclePolygons,
                        targetType = type,
                        lineWidthMm = lineWidthMm,
                        process = process
                    )
                    val wps = combedTravel.waypoints
                    for (wIdx in 0 until wps.size - 1) {
                        val wStart = Vec3(wps[wIdx].x, wps[wIdx].y, nominalZ)
                        val wEnd = Vec3(wps[wIdx + 1].x, wps[wIdx + 1].y, nominalZ)
                        if (wStart.distanceTo(wEnd) > 0.005f) {
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
                } else if (gapDist > 0.005f) {
                    // Short step-over travel between adjacent wall loops or scanlines (no retraction)
                    segments.add(
                        ToolpathSegment(
                            start = prev,
                            end = first3D,
                            type = ToolpathType.TRAVEL,
                            extrusionMm = 0f,
                            speedMmS = process.travelSpeedMmS,
                            layerIndex = layerIdx,
                            widthMm = 0.18f,
                            isCombed = process.combingMode != CombingMode.OFF,
                            requiresRetract = false
                        )
                    )
                }
            }

            for (i in 0 until orderedPts.size - 1) {
                val p0 = Vec3(orderedPts[i].x, orderedPts[i].y, nominalZ)
                val p1 = Vec3(orderedPts[i + 1].x, orderedPts[i + 1].y, nominalZ)
                val len = p0.distanceTo(p1)
                if (len > 0.015f) {
                    val isScarfLeadIn = useScarf && i == 0
                    val segFlow = if (isScarfLeadIn) 0.72f else 1.0f
                    val segSpeed = if (isScarfLeadIn) (speedMmS * speedScale * scarfSpeedFactor) else (speedMmS * speedScale)
                    segments.add(
                        ToolpathSegment(
                            start = p0,
                            end = p1,
                            type = type,
                            extrusionMm = len * extrusionPerMm * segFlow,
                            speedMmS = segSpeed.coerceAtLeast(8f),
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
                    val segFlow = if (useScarf) 0.72f else 1.0f
                    val segSpeed = if (useScarf) (speedMmS * speedScale * scarfSpeedFactor) else (speedMmS * speedScale)
                    segments.add(
                        ToolpathSegment(
                            start = pLast,
                            end = pFirst,
                            type = type,
                            extrusionMm = len * extrusionPerMm * segFlow,
                            speedMmS = segSpeed.coerceAtLeast(8f),
                            layerIndex = layerIdx,
                            widthMm = lineWidthMm
                        )
                    )
                    lastNozzlePos = pFirst
                }
            }
        }

        // 1. Skirt / Brim
        val shouldEmitSkirt = contours.isNotEmpty() && (
            (process.skirtLoops > 0 && layerIdx < process.skirtHeightLayers.coerceIn(1, 50)) ||
                (isInitialLayer && process.brimType == BrimType.SKIRT)
            )
        if (shouldEmitSkirt) {
            val bounds = computeContoursBounds(contours)
            val loops = process.skirtLoops.coerceIn(1, 10)
            val startGap = process.skirtDistanceMm.coerceAtLeast(1.5f)
            val skirtSpeed = if (isInitialLayer) process.initialLayerSpeedMmS else process.skirtSpeedMmS
            for (sIdx in 0 until loops) {
                val pad = startGap + sIdx * outerWallWidth * 0.95f
                val loopPts = if (contours.size == 1) {
                    offsetPolygonOutward(contours[0], pad).ifEmpty {
                        createRoundedRectLoop(bounds.first.x - pad, bounds.first.y - pad, bounds.second.x + pad, bounds.second.y + pad)
                    }
                } else {
                    createRoundedRectLoop(bounds.first.x - pad, bounds.first.y - pad, bounds.second.x + pad, bounds.second.y + pad)
                }
                appendPath(loopPts, ToolpathType.SKIRT_BRIM, skirtSpeed, outerWallWidth, closed = true)
            }
        }

        if (isInitialLayer && process.brimType != BrimType.NONE && process.brimType != BrimType.SKIRT && contours.isNotEmpty()) {
            val bounds = computeContoursBounds(contours)
            val loops = max(2, (process.brimWidthMm / outerWallWidth).toInt())
            val startGap = process.brimObjectGapMm.coerceAtLeast(outerWallWidth * 0.4f)
            val brimFlow = process.brimFlowRatio.coerceIn(0.5f, 1.5f)
            for (bIdx in 0 until loops) {
                val pad = startGap + bIdx * outerWallWidth * 0.95f
                val loopPts = if (contours.size == 1) {
                    offsetPolygonOutward(contours[0], pad).ifEmpty {
                        createRoundedRectLoop(bounds.first.x - pad, bounds.first.y - pad, bounds.second.x + pad, bounds.second.y + pad)
                    }
                } else {
                    createRoundedRectLoop(bounds.first.x - pad, bounds.first.y - pad, bounds.second.x + pad, bounds.second.y + pad)
                }
                appendPath(loopPts, ToolpathType.SKIRT_BRIM, process.initialLayerSpeedMmS, outerWallWidth, closed = true, flowMultiplier = brimFlow)
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

        // 3. Perimeters (Outer Wall + Inner Walls honoring wallsPrintingOrder)
        fun emitLayerPerimeters() {
            for (walls in geom.wallLoopsPerContour) {
                if (walls.isEmpty()) continue
                val outerWall = walls[0]
                val decoratedOuter = if (process.fuzzySkinType != FuzzySkinType.NONE && !isInitialLayer) {
                    applyFuzzySkinToPolygon(outerWall, layerIdx, process.fuzzySkinPointDistMm, process.fuzzySkinThicknessMm)
                } else {
                    outerWall
                }

                fun emitOuter() {
                    appendPath(
                        points = decoratedOuter,
                        type = ToolpathType.OUTER_WALL,
                        speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.outerWallSpeedMmS,
                        lineWidthMm = outerWallWidth,
                        closed = true
                    )
                }

                fun emitInner(wIdx: Int) {
                    appendPath(
                        points = walls[wIdx],
                        type = ToolpathType.INNER_WALL,
                        speedMmS = if (isInitialLayer) process.initialLayerSpeedMmS else process.innerWallSpeedMmS,
                        lineWidthMm = innerWallWidth,
                        closed = true,
                        wallLoopOrderOffset = wIdx
                    )
                }

                when (process.wallsPrintingOrder) {
                    WallsPrintingOrder.INNER_OUTER -> {
                        for (w in (walls.size - 1) downTo 1) {
                            emitInner(w)
                        }
                        emitOuter()
                    }
                    WallsPrintingOrder.OUTER_INNER -> {
                        emitOuter()
                        for (w in 1 until walls.size) {
                            emitInner(w)
                        }
                    }
                    WallsPrintingOrder.INNER_OUTER_INNER -> {
                        if (walls.size >= 3) {
                            for (w in (walls.size - 1) downTo 2) {
                                emitInner(w)
                            }
                            emitOuter()
                            emitInner(1)
                        } else {
                            for (w in (walls.size - 1) downTo 1) {
                                emitInner(w)
                            }
                            emitOuter()
                        }
                    }
                }
            }
        }

        // 4. Multi-Layer Region-Aware Bottom Surface, Top Surface, Internal Solid Infill, and Sparse Infill
        fun emitLayerInfillAndSurfaces() {
            val infillPolygons = geom.infillPolygons
            if (!process.spiralVaseMode && infillPolygons.isNotEmpty()) {
                val bottomShellCount = process.bottomShellLayers.coerceIn(0, 50)
                val topShellCount = process.topShellLayers.coerceIn(0, 50)

                val baseDir = if (process.solidInfillDirectionDeg != 45) {
                    process.solidInfillDirectionDeg.toFloat()
                } else {
                    process.infillDirectionDeg.toFloat()
                }
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
                    lineWidth = sparseInfillWidth,
                    infillDirectionDeg = process.infillDirectionDeg
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
        }

        if (process.printInfillFirst && !process.spiralVaseMode) {
            emitLayerInfillAndSurfaces()
            emitLayerPerimeters()
        } else {
            emitLayerPerimeters()
            emitLayerInfillAndSurfaces()
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
        lineWidth: Float,
        infillDirectionDeg: Int = 45
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

        val dirOffset = (infillDirectionDeg - 45).toFloat()
        val anglesDeg = when (pattern) {
            InfillPattern.GRID -> listOf(45f + dirOffset, -45f + dirOffset)
            InfillPattern.ADAPTIVE_CUBIC -> listOf(0f + dirOffset, 60f + dirOffset, 120f + dirOffset)
            InfillPattern.TRIANGLES, InfillPattern.TRI_HEXAGON -> listOf(0f + dirOffset, 60f + dirOffset, 120f + dirOffset)
            InfillPattern.GYROID -> listOf((if (layerIdx % 2 == 0) 0f else 90f) + dirOffset)
            InfillPattern.HONEYCOMB -> listOf((if (layerIdx % 2 == 0) 30f else -30f) + dirOffset)
            InfillPattern.CROSS_HATCH -> listOf((if (layerIdx % 2 == 0) 22.5f else -67.5f) + dirOffset)
            InfillPattern.LIGHTNING -> listOf(90f + dirOffset)
            else -> listOf((if (layerIdx % 2 == 0) 45f else -45f) + dirOffset)
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

        val threshDeg = process.supportThresholdAngleDeg.coerceIn(10, 80)
        val threshRad = threshDeg * (PI.toFloat() / 180f)
        val outerWidth = process.outerWallLineWidthMm.coerceIn(0.25f, 1.0f)
        val safeSpacing = sampleSpacingMm.coerceAtLeast(1.25f)
        val maxPtsPerLayer = 48

        // 1. Fast Layer-to-Layer 1D scanline interval overhang extraction (no O(V) signedDistance per sample point)
        for (layerIdx in 2 until totalLayers) {
            val geom = allGeometries[layerIdx]
            val prevGeom = allGeometries[layerIdx - 1]
            if (geom.outerContours.isEmpty()) continue

            val selfSupportMargin = geom.layerHeight * (1f / tan(threshRad).coerceAtLeast(0.35f)) + outerWidth * 0.55f
            val bounds = computeContoursBounds(geom.outerContours)
            var y = bounds.first.y + safeSpacing * 0.5f
            while (y <= bounds.second.y - safeSpacing * 0.25f && overhangsByLayer[layerIdx].size < maxPtsPerLayer) {
                val curIntervals = scanlineIntervals(geom.outerContours, y)
                val prevIntervals = expandIntervals(scanlineIntervals(prevGeom.outerContours, y), selfSupportMargin)
                val unsupported = subtractIntervals(curIntervals, prevIntervals)
                for (iv in unsupported) {
                    val minLen = if (process.removeSmallOverhangs) outerWidth * 1.1f else outerWidth * 0.4f
                    if (iv.length >= minLen) {
                        var x = iv.start + min(safeSpacing * 0.5f, iv.length * 0.5f)
                        while (x <= iv.end + 1e-3f && overhangsByLayer[layerIdx].size < maxPtsPerLayer) {
                            overhangsByLayer[layerIdx].add(Vec2(x.coerceIn(iv.start, iv.end), y))
                            x += safeSpacing
                        }
                    }
                }
                y += safeSpacing
            }
        }

        // 2. Downward-facing mesh triangles (strided on complex meshes so 100k+ triangle models stay instantaneous)
        val normalZCutoff = -cos((90 - threshDeg).coerceIn(15, 75) * (PI.toFloat() / 180f))
        val firstLh = allGeometries[0].layerHeight
        val stdLh = if (totalLayers > 1) allGeometries[1].layerHeight else firstLh
        val triStride = max(1, triangles.size / 6000)

        var tIdx = 0
        while (tIdx < triangles.size) {
            val tri = triangles[tIdx]
            tIdx += triStride
            val minZ = min(tri.v0.z, min(tri.v1.z, tri.v2.z))
            if (minZ < firstLh + stdLh * 1.5f) continue
            if (tri.normal.z < normalZCutoff) {
                val avgZ = (tri.v0.z + tri.v1.z + tri.v2.z) / 3f
                val targetLayer = (1 + ((avgZ - firstLh) / stdLh).roundToInt()).coerceIn(2, totalLayers - 1)
                if (overhangsByLayer[targetLayer].size >= maxPtsPerLayer) continue

                val cx = (tri.v0.x + tri.v1.x + tri.v2.x) / 3f
                val cy = (tri.v0.y + tri.v1.y + tri.v2.y) / 3f
                val prevContours = allGeometries[targetLayer - 1].outerContours
                if (!isPointInContours(Vec2(cx, cy), prevContours)) {
                    overhangsByLayer[targetLayer].add(Vec2(cx, cy))
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

        val allOuter = allGeometries.flatMap { it.outerContours }
        if (allOuter.isEmpty()) return List(totalLayers) { LayerSupportPlan.EMPTY }
        val modelBounds = computeContoursBounds(allOuter)
        val pad = max(4.0f, process.supportFirstLayerExpansionMm + 2.5f)
        val minX = modelBounds.first.x - pad
        val minY = modelBounds.first.y - pad
        val maxX = modelBounds.second.x + pad
        val maxY = modelBounds.second.y + pad

        val maxSpan = max(maxX - minX, maxY - minY).coerceAtLeast(10f)
        val cellSize = max(1.35f, maxSpan / 76f)

        val overhangsByLayer = extractOverhangPointsByLayer(triangles, allGeometries, process, sampleSpacingMm = cellSize)
        if (overhangsByLayer.all { it.isEmpty() }) {
            return List(totalLayers) { LayerSupportPlan.EMPTY }
        }

        val nx = ((maxX - minX) / cellSize).toInt().coerceIn(8, 80)
        val ny = ((maxY - minY) / cellSize).toInt().coerceIn(8, 80)
        val numCells = nx * ny

        val stdLh = if (totalLayers > 1) allGeometries[1].layerHeight else allGeometries[0].layerHeight
        val topZGapLayers = max(1, (process.supportTopZDistanceMm / stdLh).roundToInt())
        val bottomZGapLayers = max(1, (process.supportBottomZDistanceMm / stdLh).roundToInt())
        val topInterfaceCount = process.supportTopInterfaceLayers.coerceIn(0, 8)
        val bottomInterfaceCount = process.supportBottomInterfaceLayers.coerceIn(0, 8)
        val xyClearance = process.supportObjectXyDistanceMm.coerceIn(0.15f, 2.0f)

        // Precompute per-layer model blocked mask using fast 1D row scanlineIntervals instead of per-cell O(V) distance!
        val insideModelByLayer = Array(totalLayers) { BooleanArray(numCells) }
        val blockedByModelByLayer = Array(totalLayers) { BooleanArray(numCells) }

        for (layerIdx in 0 until totalLayers) {
            val contours = allGeometries[layerIdx].outerContours
            if (contours.isEmpty()) continue
            val gap = if (layerIdx == 0) max(xyClearance, process.supportObjectFirstLayerGapMm) else xyClearance
            val insideArr = insideModelByLayer[layerIdx]
            val blockedArr = blockedByModelByLayer[layerIdx]
            for (iy in 0 until ny) {
                val y = minY + (iy + 0.5f) * cellSize
                val rawIvs = scanlineIntervals(contours, y)
                if (rawIvs.isEmpty()) continue
                val rowOffset = iy * nx
                for (iv in rawIvs) {
                    val ix0 = ((iv.start - minX) / cellSize).toInt().coerceIn(0, nx - 1)
                    val ix1 = ((iv.end - minX) / cellSize).toInt().coerceIn(0, nx - 1)
                    for (ix in ix0..ix1) {
                        val cx = minX + (ix + 0.5f) * cellSize
                        if (cx in iv.start..iv.end) {
                            insideArr[rowOffset + ix] = true
                            blockedArr[rowOffset + ix] = true
                        }
                    }
                    val bx0 = ((iv.start - gap - minX) / cellSize).toInt().coerceIn(0, nx - 1)
                    val bx1 = ((iv.end + gap - minX) / cellSize).toInt().coerceIn(0, nx - 1)
                    for (ix in bx0..bx1) {
                        val cx = minX + (ix + 0.5f) * cellSize
                        if (cx in (iv.start - gap)..(iv.end + gap)) {
                            blockedArr[rowOffset + ix] = true
                        }
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

        // Cluster overhang points on each layer so tips are cleanly spaced by tipSpacing (capped at 16 per layer)
        val spawnedTipsByLayer = Array(totalLayers) { ArrayList<Vec2>() }
        for (layerIdx in 2 until totalLayers) {
            val rawPts = rawOverhangsByLayer[layerIdx]
            if (rawPts.isEmpty()) continue
            val targetTipLayer = (layerIdx - topZGapLayers).coerceAtLeast(0)
            val chosen = spawnedTipsByLayer[targetTipLayer]
            for (pt in rawPts) {
                if (chosen.size >= 16) break
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
        val cappedCircles = if (circles.size > 24) circles.take(24) else circles
        if (cappedCircles.size == 1) {
            val c = cappedCircles[0]
            return listOf(createSmoothCircleLoop(c.x, c.y, c.radius, segments = 16))
        }

        // Remove any circle completely contained inside another circle
        val filtered = cappedCircles.filter { ci ->
            cappedCircles.none { cj ->
                ci !== cj && hypot2D(ci.x - cj.x, ci.y - cj.y) + ci.radius <= cj.radius + 0.02f
            }
        }

        val arcSegments = ArrayList<Segment2D>()
        val nSegs = 14
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
        val base = cleanAndSimplifyPolygon(ensureCounterClockwise(rawPoly))
        if (base.size < 3 || expandDist <= 1e-4f) return base

        val minEdgeLen = max(0.05f, expandDist * 0.40f)
        val poly = mergeShortPolygonEdges(base, minEdgeLen)
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
            val miterScale = if (cosHalf > 0.42f) (1f / cosHalf).coerceAtMost(1.40f) else 1f
            shifted.add(curr - bisector * (expandDist * miterScale))
        }

        val pruned = collapseInvertedOffsetEdges(poly, shifted, expandDist)
        return cleanAndSimplifyPolygon(pruned)
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
     * Routes a combing travel path inside `region` from `start` to `end` by walking the shorter side
     * of the precomputed 28-vertex `guidePoly` and applying a fast 2-pass visibility shortcut.
     */
    private fun buildBoundaryCombedPathFast(
        start: Vec2,
        end: Vec2,
        region: CombingRegion
    ): List<Vec2> {
        val guidePoly = region.guidePoly
        val n = guidePoly.size
        if (n < 3) return listOf(start, end)

        val iStart = findClosestPolygonVertexIndex(start, guidePoly)
        val iEnd = findClosestPolygonVertexIndex(end, guidePoly)
        if (iStart == iEnd) {
            return listOf(start, guidePoly[iStart], end)
        }

        // Build forward chain along guidePoly
        val forward = ArrayList<Vec2>(n + 3)
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
        val backward = ArrayList<Vec2>(n + 3)
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
        return shortcutCombingWaypoints(chosen, region.poly)
    }

    /**
     * Fast bounded Visibility String-Pulling / Funnel Shortcut Pass:
     * Simplifies a boundary-following combing chain (<= 30 points) into a taut interior path.
     */
    private fun shortcutCombingWaypoints(chain: List<Vec2>, poly: List<Vec2>): List<Vec2> {
        if (chain.size <= 3) return chain
        // If the direct chord between the first interior guide vertex and last interior guide vertex is clear, jump straight across
        if (chain.size >= 4 && isChordInsidePolygon(chain[1], chain[chain.lastIndex - 1], poly)) {
            return listOf(chain.first(), chain[1], chain[chain.lastIndex - 1], chain.last())
        }

        val out = ArrayList<Vec2>(chain.size)
        var curIdx = 0
        out.add(chain[0])
        var guard = 0

        while (curIdx < chain.lastIndex && guard <= chain.size) {
            guard++
            var furthestVisible = curIdx + 1
            val maxLookahead = min(chain.lastIndex, curIdx + 8)
            for (testIdx in maxLookahead downTo curIdx + 2) {
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
            if (obs.size < 3 || currentPath.size > 16) continue
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
     * Cura Combing Mode path-routing & retraction decision algorithm (optimized for complex multi-island meshes):
     * 1. Fast O(1) short step-over path for adjacent infill/skin lines (`travelDist <= lineWidthMm * 4.5f`).
     * 2. Fast AABB + point-in-polygon check against precomputed `combingRegions`.
     * 3. If the straight chord stays inside the combing region, combs directly without crossing walls.
     * 4. If the straight chord crosses a concave wall/hole, routes along the precomputed 28-vertex `guidePoly`.
     * 5. Evaluates `maxCombDistanceWithNoRetractMm` and `retractBeforeOuterWall` to decide whether retraction is required.
     */
    private fun computeCuraCombedTravel(
        start: Vec2,
        end: Vec2,
        combingRegions: List<CombingRegion>,
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

        val directDist = start.distanceTo(end)
        // Fast O(1) path for short adjacent infill/skin step-overs
        if (combingRegions.isNotEmpty() && directDist <= lineWidthMm * 4.5f) {
            val requiresRetract = (process.retractBeforeOuterWall && targetType == ToolpathType.OUTER_WALL) ||
                (process.maxCombDistanceWithNoRetractMm > 0f && directDist > process.maxCombDistanceWithNoRetractMm)
            return CombedTravelResult(
                waypoints = listOf(start, end),
                isCombed = true,
                requiresRetract = requiresRetract
            )
        }

        val hostRegion = combingRegions.firstOrNull { region ->
            region.containsPointFast(start) && region.containsPointFast(end)
        }

        val waypoints: List<Vec2>
        val isCombed: Boolean

        if (hostRegion != null) {
            isCombed = true
            waypoints = if (hostRegion.isChordInsideRegion(start, end)) {
                listOf(start, end)
            } else {
                buildBoundaryCombedPathFast(start, end, hostRegion)
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
        val segments = ArrayList<Segment2D>(min(triangles.size, 2048))
        for (tri in triangles) {
            val v0 = tri.v0
            val v1 = tri.v1
            val v2 = tri.v2

            val minZ = min(v0.z, min(v1.z, v2.z))
            val maxZ = max(v0.z, max(v1.z, v2.z))
            if (z < minZ || z > maxZ || maxZ - minZ < 1e-5f) continue

            val p01 = intersectEdgeAtZ(v0, v1, z)
            val p12 = intersectEdgeAtZ(v1, v2, z)
            val p20 = intersectEdgeAtZ(v2, v0, z)

            val pA = p01 ?: p12
            val pB = when {
                p01 != null && p12 != null && p01.distanceTo(p12) > 1e-5f -> p12
                p01 != null && p20 != null && p01.distanceTo(p20) > 1e-5f -> p20
                p12 != null && p20 != null && p12.distanceTo(p20) > 1e-5f -> p20
                else -> null
            }
            if (pA != null && pB != null) {
                // Orient segment so solid model interior is on the LEFT of a -> b (outer contours CCW, inner holes CW)
                val n = tri.normal
                val dx = pB.x - pA.x
                val dy = pB.y - pA.y
                val orient = n.x * dy - n.y * dx
                if (orient >= 0f) {
                    segments.add(Segment2D(pA, pB))
                } else {
                    segments.add(Segment2D(pB, pA))
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

    /**
     * Exact Manifold + Spatial Grid Hash segment chainer:
     * - Respects directed half-edge orientation (`tail -> next.a` primary match) so thin walls never cross-link
     * - Never closes a loop prematurely when an unused segment endpoint is closer to `tail` than `head` is!
     */
    private fun chainSegmentsIntoPolygons(rawSegments: List<Segment2D>): List<List<Vec2>> {
        if (rawSegments.isEmpty()) return emptyList()
        val segments = rawSegments.filter { it.a.distanceTo(it.b) > 1e-5f }
        if (segments.isEmpty()) return emptyList()

        val nSegs = segments.size
        val used = BooleanArray(nSegs)
        val cellSize = 0.03f
        val invCell = 1f / cellSize

        fun cellKey(gx: Int, gy: Int): Long =
            (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)

        // Map from 2D grid cell -> list of encoded endpoints: (segIdx shl 1) | endpointBit (0 = a, 1 = b)
        val grid = HashMap<Long, IntArray>(nSegs * 2)
        val gridCounts = HashMap<Long, Int>(nSegs * 2)

        fun insertEndpoint(pt: Vec2, encoded: Int) {
            val gx = floor(pt.x * invCell).toInt()
            val gy = floor(pt.y * invCell).toInt()
            val key = cellKey(gx, gy)
            val curArr = grid[key]
            val curSize = gridCounts[key] ?: 0
            if (curArr == null) {
                val arr = IntArray(4)
                arr[0] = encoded
                grid[key] = arr
                gridCounts[key] = 1
            } else {
                val targetArr = if (curSize >= curArr.size) curArr.copyOf(curArr.size * 2) else curArr
                targetArr[curSize] = encoded
                grid[key] = targetArr
                gridCounts[key] = curSize + 1
            }
        }

        for (i in 0 until nSegs) {
            val s = segments[i]
            insertEndpoint(s.a, i shl 1)
            insertEndpoint(s.b, (i shl 1) or 1)
        }

        // Returns Triple(segIdx, matchedB, distance)
        fun findBestNextSegment(pt: Vec2, searchHeadInsteadOfTail: Boolean): Triple<Int, Boolean, Float>? {
            val gx = floor(pt.x * invCell).toInt()
            val gy = floor(pt.y * invCell).toInt()
            var bestSeg = -1
            var bestIsB = false
            var bestScore = Float.POSITIVE_INFINITY
            var bestActualDist = Float.POSITIVE_INFINITY
            val maxTol = 0.055f

            for (dy in -2..2) {
                for (dx in -2..2) {
                    val key = cellKey(gx + dx, gy + dy)
                    val arr = grid[key] ?: continue
                    val count = gridCounts[key] ?: 0
                    for (k in 0 until count) {
                        val enc = arr[k]
                        val segIdx = enc ushr 1
                        if (used[segIdx]) continue
                        val isB = (enc and 1) != 0
                        val candPt = if (isB) segments[segIdx].b else segments[segIdx].a
                        val d = pt.distanceTo(candPt)
                        if (d > maxTol) continue

                        // Prefer consistent orientation:
                        // - When extending from `tail`, we want to match the next segment's `a` (`isB == false`).
                        // - When extending backward from `head`, we want to match the prev segment's `b` (`isB == true`).
                        val orientationPenalty = if (searchHeadInsteadOfTail) {
                            if (isB) 0f else 0.008f
                        } else {
                            if (!isB) 0f else 0.008f
                        }
                        val score = d + orientationPenalty
                        if (score < bestScore) {
                            bestScore = score
                            bestSeg = segIdx
                            bestIsB = isB
                            bestActualDist = d
                        }
                    }
                }
            }
            return if (bestSeg >= 0) Triple(bestSeg, bestIsB, bestActualDist) else null
        }

        val polygons = ArrayList<List<Vec2>>()

        for (startIdx in 0 until nSegs) {
            if (used[startIdx]) continue
            used[startIdx] = true

            val deque = ArrayDeque<Vec2>(64)
            deque.addLast(segments[startIdx].a)
            deque.addLast(segments[startIdx].b)

            var steps = 0
            val maxSteps = nSegs + 2
            while (steps < maxSteps) {
                steps++
                val tail = deque.last()
                val head = deque.first()
                val dClose = tail.distanceTo(head)

                val matchTail = findBestNextSegment(tail, searchHeadInsteadOfTail = false)

                // Close the loop ONLY if we have at least 3 segments AND `head` is at least as close as any unused segment!
                if (deque.size >= 4 && dClose <= 0.002f && (matchTail == null || dClose <= matchTail.third + 1e-4f)) {
                    break
                }

                if (matchTail != null) {
                    val (segIdx, matchedB, _) = matchTail
                    used[segIdx] = true
                    val nextPt = if (matchedB) segments[segIdx].a else segments[segIdx].b
                    if (deque.size >= 3 && nextPt.distanceTo(head) <= 0.0015f) {
                        break
                    }
                    deque.addLast(nextPt)
                    continue
                }

                // If tail is already close to head and no more segments attach to tail, loop is complete
                if (deque.size >= 3 && dClose <= 0.12f) {
                    break
                }

                val matchHead = findBestNextSegment(head, searchHeadInsteadOfTail = true)
                if (matchHead != null) {
                    val (segIdx, matchedB, _) = matchHead
                    used[segIdx] = true
                    val prevPt = if (matchedB) segments[segIdx].a else segments[segIdx].b
                    if (deque.size >= 3 && prevPt.distanceTo(tail) <= 0.0015f) {
                        break
                    }
                    deque.addFirst(prevPt)
                    continue
                }

                break
            }

            if (deque.size >= 3) {
                val cleaned = cleanAndSimplifyPolygon(deque.toList())
                if (cleaned.size >= 3) {
                    val b = computeContoursBounds(listOf(cleaned))
                    val spanX = b.second.x - b.first.x
                    val spanY = b.second.y - b.first.y
                    if (spanX >= 0.08f && spanY >= 0.08f && abs(signedPolygonArea(cleaned)) >= 0.005f) {
                        polygons.add(cleaned)
                    }
                }
            }
        }

        return polygons
    }

    /**
     * Downsamples a closed polygon to at most `maxVertices` vertices while preserving sharp corners (used only for combing guides).
     */
    private fun downsamplePolygon(poly: List<Vec2>, maxVertices: Int): List<Vec2> {
        if (poly.size <= maxVertices || maxVertices < 8) return poly
        val out = ArrayList<Vec2>(maxVertices)
        val n = poly.size
        val step = n.toFloat() / maxVertices
        var cursor = 0f
        for (k in 0 until maxVertices) {
            val idx = cursor.toInt().coerceIn(0, n - 1)
            out.add(poly[idx])
            cursor += step
        }
        return out
    }

    /**
     * True Closed-Loop Ramer–Douglas–Peucker (RDP) Simplifier:
     * Preserves 100% of sharp corners and geometric features while removing redundant vertices
     * whose perpendicular deviation from the chord is less than `epsilon` mm (default 0.012mm = 12 microns).
     */
    private fun simplifyClosedPolygonRdp(pts: List<Vec2>, epsilon: Float): List<Vec2> {
        val n = pts.size
        if (n <= 6) return pts

        // 1. Find two maximally distant vertices to split the closed loop into two open chains
        var idxA = 0
        var maxDistSq = -1f
        val p0 = pts[0]
        for (i in 1 until n) {
            val dx = pts[i].x - p0.x
            val dy = pts[i].y - p0.y
            val dSq = dx * dx + dy * dy
            if (dSq > maxDistSq) {
                maxDistSq = dSq
                idxA = i
            }
        }

        var idxB = 0
        maxDistSq = -1f
        val pA = pts[idxA]
        for (i in 0 until n) {
            if (i == idxA) continue
            val dx = pts[i].x - pA.x
            val dy = pts[i].y - pA.y
            val dSq = dx * dx + dy * dy
            if (dSq > maxDistSq) {
                maxDistSq = dSq
                idxB = i
            }
        }
        if (idxA == idxB) return pts

        // Build chain 1: idxA -> idxB, and chain 2: idxB -> idxA
        val chain1 = ArrayList<Vec2>()
        var cur = idxA
        while (true) {
            chain1.add(pts[cur])
            if (cur == idxB) break
            cur = (cur + 1) % n
        }

        val chain2 = ArrayList<Vec2>()
        cur = idxB
        while (true) {
            chain2.add(pts[cur])
            if (cur == idxA) break
            cur = (cur + 1) % n
        }

        val simp1 = simplifyOpenPolylineRdp(chain1, epsilon)
        val simp2 = simplifyOpenPolylineRdp(chain2, epsilon)

        val combined = ArrayList<Vec2>(simp1.size + simp2.size)
        for (i in 0 until simp1.size - 1) combined.add(simp1[i])
        for (i in 0 until simp2.size - 1) combined.add(simp2[i])
        return if (combined.size >= 3) combined else pts
    }

    private fun simplifyOpenPolylineRdp(pts: List<Vec2>, epsilon: Float): List<Vec2> {
        val n = pts.size
        if (n <= 2) return pts
        val keep = BooleanArray(n)
        keep[0] = true
        keep[n - 1] = true

        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to (n - 1))

        while (stack.isNotEmpty()) {
            val (startIdx, endIdx) = stack.removeLast()
            if (endIdx - startIdx <= 1) continue

            val a = pts[startIdx]
            val b = pts[endIdx]
            var maxDist = -1f
            var splitIdx = -1

            for (i in (startIdx + 1) until endIdx) {
                val (d, _) = distancePointToSegment(pts[i], a, b)
                if (d > maxDist) {
                    maxDist = d
                    splitIdx = i
                }
            }

            if (maxDist > epsilon && splitIdx != -1) {
                keep[splitIdx] = true
                stack.addLast(startIdx to splitIdx)
                stack.addLast(splitIdx to endIdx)
            }
        }

        val out = ArrayList<Vec2>()
        for (i in 0 until n) {
            if (keep[i]) out.add(pts[i])
        }
        return out
    }

    /**
     * Removes duplicate points, eliminates colinear subdivision vertices along straight edges,
     * and applies 12-micron Ramer-Douglas-Peucker simplification so curves and corners match the 3D mesh 1:1.
     */
    private fun cleanAndSimplifyPolygon(pts: List<Vec2>): List<Vec2> {
        if (pts.size < 3) return emptyList()

        // 1. Remove consecutive duplicates closer than 0.01mm
        var current = ArrayList<Vec2>(pts.size)
        for (p in pts) {
            if (current.isEmpty() || current.last().distanceTo(p) > 0.01f) {
                current.add(p)
            }
        }
        if (current.size >= 2 && current.first().distanceTo(current.last()) <= 0.01f) {
            current.removeAt(current.lastIndex)
        }
        if (current.size < 3) return emptyList()

        // 2. Iteratively prune strictly colinear vertices and 180-degree hairpin spikes
        var changed = true
        var pass = 0
        while (changed && current.size > 3 && pass < 3) {
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

                if (len1 < 0.01f || len2 < 0.01f) {
                    changed = true
                    continue
                }

                val e1 = Vec2(v1.x / len1, v1.y / len1)
                val e2 = Vec2(v2.x / len2, v2.y / len2)
                val cross = abs(e1.x * e2.y - e1.y * e2.x)
                val dot = e1.x * e2.x + e1.y * e2.y

                // Drop strictly colinear vertices or degenerate 180-degree hairpin spikes
                if ((cross < 0.018f && dot > 0.995f) || dot < -0.975f) {
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

        if (current.size < 3) return emptyList()
        return if (current.size > 16) simplifyClosedPolygonRdp(current, epsilon = 0.012f) else current
    }

    private fun mergeShortPolygonEdges(poly: List<Vec2>, minEdgeLen: Float): List<Vec2> {
        if (poly.size <= 4) return poly
        val out = ArrayList<Vec2>(poly.size)
        for (p in poly) {
            if (out.isEmpty()) {
                out.add(p)
            } else {
                val prev = out.last()
                val d = prev.distanceTo(p)
                if (d >= minEdgeLen) {
                    out.add(p)
                }
            }
        }
        if (out.size >= 4 && out.first().distanceTo(out.last()) < minEdgeLen) {
            out.removeAt(out.lastIndex)
        }
        return if (out.size >= 3) out else poly
    }

    private fun collapseInvertedOffsetEdges(
        origPoly: List<Vec2>,
        initialShifted: List<Vec2>,
        offsetDist: Float
    ): List<Vec2> {
        var curOrig = origPoly
        var curShifted = initialShifted
        var pass = 0
        while (pass < 4 && curShifted.size >= 4 && curOrig.size == curShifted.size) {
            pass++
            val n = curShifted.size
            val nextOrig = ArrayList<Vec2>(n)
            val nextShifted = ArrayList<Vec2>(n)
            var collapsedAny = false
            var i = 0
            while (i < n) {
                val nextIdx = (i + 1) % n
                val origEdge = curOrig[nextIdx] - curOrig[i]
                val newEdge = curShifted[nextIdx] - curShifted[i]
                val dot = origEdge.x * newEdge.x + origEdge.y * newEdge.y
                if (dot <= 0f && origEdge.length() < offsetDist * 3.0f && i < n - 1) {
                    nextOrig.add((curOrig[i] + curOrig[nextIdx]) * 0.5f)
                    nextShifted.add((curShifted[i] + curShifted[nextIdx]) * 0.5f)
                    collapsedAny = true
                    i += 2
                } else {
                    nextOrig.add(curOrig[i])
                    nextShifted.add(curShifted[i])
                    i++
                }
            }
            if (!collapsedAny || nextShifted.size < 3) break
            curOrig = nextOrig
            curShifted = nextShifted
        }
        return curShifted
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
        val base = cleanAndSimplifyPolygon(ensureCounterClockwise(rawPoly))
        if (base.size < 3 || insetDist <= 1e-4f) return base

        val bounds = computeContoursBounds(listOf(base))
        val width = bounds.second.x - bounds.first.x
        val height = bounds.second.y - bounds.first.y
        if (width <= insetDist * 2.05f || height <= insetDist * 2.05f) return emptyList()

        val minEdgeLen = max(0.05f, insetDist * 0.40f)
        val poly = mergeShortPolygonEdges(base, minEdgeLen)
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
            val miterScale = if (cosHalf > 0.42f) (1f / cosHalf).coerceAtMost(1.40f) else 1f
            shifted.add(curr + bisector * (insetDist * miterScale))
        }

        val pruned = collapseInvertedOffsetEdges(poly, shifted, insetDist)
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

        val steps = max(2, (length / 1.8f).toInt().coerceAtMost(32))
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
            val subdivs = max(1, (len / step).toInt().coerceAtMost(24))
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

    private fun StringBuilder.appendFixed3(value: Float): StringBuilder {
        val scaled = (value * 1000f).roundToInt()
        if (scaled < 0) append('-')
        val absVal = abs(scaled)
        append(absVal / 1000)
        append('.')
        val frac = absVal % 1000
        if (frac < 100) append('0')
        if (frac < 10) append('0')
        append(frac)
        return this
    }

    private fun StringBuilder.appendFixed4(value: Float): StringBuilder {
        val scaled = (value * 10000f).roundToInt()
        if (scaled < 0) append('-')
        val absVal = abs(scaled)
        append(absVal / 10000)
        append('.')
        val frac = absVal % 10000
        if (frac < 1000) append('0')
        if (frac < 100) append('0')
        if (frac < 10) append('0')
        append(frac)
        return this
    }

    private fun StringBuilder.appendFixed5(value: Float): StringBuilder {
        val scaled = (value * 100000f).roundToInt()
        if (scaled < 0) append('-')
        val absVal = abs(scaled)
        append(absVal / 100000)
        append('.')
        val frac = absVal % 100000
        if (frac < 10000) append('0')
        if (frac < 1000) append('0')
        if (frac < 100) append('0')
        if (frac < 10) append('0')
        append(frac)
        return this
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

        // Cap in-memory G-code text buffer at 8 MB so complex multi-hundred-thousand-segment prints never OOM on mobile
        val maxGcodeChars = 8 * 1024 * 1024
        var gcodeTruncated = false

        val bedCenterX = printer.bedWidthX * 0.5f
        val bedCenterY = printer.bedDepthY * 0.5f

        val sb = StringBuilder(min(layers.size * 2048, 2 * 1024 * 1024))
        sb.append("; HEADER_BLOCK_START\n")
        sb.append("; generated by DolphinSlicer (OrcaSlicer Android) 2.2.0\n")
        sb.append("; printer_model = ${printer.name}\n")
        sb.append("; bed_center_x = $bedCenterX\n")
        sb.append("; bed_center_y = $bedCenterY\n")
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

        var nozzleGx = Float.NaN
        var nozzleGy = Float.NaN
        val defaultTravelFeed = (process.travelSpeedMmS.coerceAtLeast(30f) * 60f).toInt()

        for (layer in layers) {
            val writeGcode = sb.length < maxGcodeChars
            if (writeGcode) {
                sb.append(";LAYER_CHANGE\n;Z:")
                sb.appendFixed3(layer.zHeightMm).append("\n;HEIGHT:")
                sb.appendFixed3(layer.layerHeightMm).append("\nG1 Z")
                sb.appendFixed3(layer.zHeightMm).append(" F1800\n")

                if (layer.index == 1) {
                    val fanPwm = (filament.fanSpeedPercent * 255) / 100
                    sb.append("M106 S").append(fanPwm).append('\n')
                    sb.append("M104 S").append(filament.nozzleTempOtherC).append('\n')
                    sb.append("M140 S").append(filament.bedTempOtherC).append('\n')
                }
            } else if (!gcodeTruncated) {
                gcodeTruncated = true
                sb.append("; [Large complex print G-code preview capped in RAM; full statistics calculated below]\n")
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

                if (!writeGcode) continue

                val startGx = seg.start.x + bedCenterX
                val startGy = seg.start.y + bedCenterY
                val gx = seg.end.x + bedCenterX
                val gy = seg.end.y + bedCenterY
                val feedrate = (seg.speedMmS * 60f).toInt()

                if (seg.type == ToolpathType.TRAVEL || seg.extrusionMm <= 0f) {
                    if (seg.requiresRetract && !isRetracted && retractLen > 0.01f) {
                        sb.append("G1 E-").appendFixed4(retractLen).append(" F").append(retractFeed).append(" ; retract\n")
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
                    sb.append("G0 X").appendFixed3(gx)
                        .append(" Y").appendFixed3(gy)
                        .append(" Z").appendFixed3(zTravel)
                        .append(" F").append(feedrate).append('\n')
                    nozzleGx = gx
                    nozzleGy = gy
                } else {
                    // Ensure nozzle is positioned at seg.start before extruding to seg.end
                    val dxStart = startGx - nozzleGx
                    val dyStart = startGy - nozzleGy
                    if (!nozzleGx.isFinite() || (dxStart * dxStart + dyStart * dyStart) > 0.0001f) {
                        val needRetractForJump = !nozzleGx.isFinite() || (dxStart * dxStart + dyStart * dyStart) > 2.25f
                        if (needRetractForJump && !isRetracted && retractLen > 0.01f) {
                            sb.append("G1 E-").appendFixed4(retractLen).append(" F").append(retractFeed).append(" ; retract\n")
                            isRetracted = true
                        }
                        val zTravel = if (isRetracted && effectiveZHop > 0.01f) {
                            seg.start.z + effectiveZHop
                        } else {
                            seg.start.z
                        }
                        sb.append("G0 X").appendFixed3(startGx)
                            .append(" Y").appendFixed3(startGy)
                            .append(" Z").appendFixed3(zTravel)
                            .append(" F").append(defaultTravelFeed).append('\n')
                    }
                    if (isRetracted) {
                        if (effectiveZHop > 0.01f) {
                            sb.append("G0 Z").appendFixed3(seg.end.z).append(" F1800 ; restore Z hop\n")
                        }
                        sb.append("G1 E").appendFixed4(retractLen).append(" F").append(retractFeed).append(" ; unretract\n")
                        isRetracted = false
                    }
                    if (seg.type != activeType) {
                        activeType = seg.type
                        sb.append(";TYPE:").append(seg.type.gcodeComment).append('\n')
                    }
                    sb.append("G1 X").appendFixed3(gx)
                        .append(" Y").appendFixed3(gy)
                        .append(" E").appendFixed5(seg.extrusionMm)
                        .append(" F").append(feedrate).append('\n')
                    nozzleGx = gx
                    nozzleGy = gy
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
