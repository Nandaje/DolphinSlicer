package com.orcaslicer.android.gpu

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.orcaslicer.android.model.BoundingBox
import com.orcaslicer.android.model.MeshData
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.model.SliceResult
import com.orcaslicer.android.model.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.WeakHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class ViewportCameraPreset {
    ISOMETRIC,
    TOP,
    FRONT,
    RIGHT
}

/**
 * ============================================================================
 * OpenGL ES 3.0 3D Viewport Renderer (`OrcaGlRenderer.kt`)
 * ============================================================================
 * Hardware-accelerated 3D renderer for both `Prepare` (mesh & gizmo view) and
 * `Preview` (volumetric 3D extruded G-code toolpath view).
 *
 * Coordinate System Convention:
 * - Uses standard 3D printing **right-handed Z-up** coordinates:
 *   - `X`: Left (-) to Right (+) across the build plate
 *   - `Y`: Front (-) to Back (+) across the build plate
 *   - `Z`: Build plate floor (`0 mm`) upwards (`+Z`)
 *
 * Key Rendering Passes & Ray-Picking Helpers:
 * 1. **Build Plate Grid & Volume Cage**: Draws 10 mm and 50 mm grid lines, X/Y
 *    origin axes, and printer max-Z wireframe bounds.
 * 2. **Lit 3D Mesh Shader**: Renders STL/OBJ/3MF meshes with directional Phong
 *    shading and automatic red overhang highlighting for faces steeper than
 *    [overhangThresholdDeg].
 * 3. **Interactive 3D Gizmos**:
 *    - `MOVE`, `SCALE`, `ROTATE`: Global XYZ axis arrows/handles & ray-hit testing ([pickGizmoAxis]).
 *    - `CUT`: Translucent cyan Z-cut plane quad + vertical drag handle ([isRayHittingCutHandle]).
 *    - `LAY_ON_FACE`: OrcaSlicer-style selectable flat face patch candidates rendered on the mesh ([pickLayOnFaceNormal]).
 * 4. **Volumetric 3D G-Code Preview**: Extrudes each toolpath segment into a 3D
 *    rectangular bead ribbon (`GL_TRIANGLES`) colored by [com.orcaslicer.android.model.ToolpathType].
 */
class OrcaGlRenderer(
    private val onObjectPicked: (String?) -> Unit
) : GLSurfaceView.Renderer {

    // Camera orbital state (Z-up right-handed coordinate system matching 3D printers)
    @Volatile var cameraYawDeg: Float = -45f
    @Volatile var cameraPitchDeg: Float = 32f
    @Volatile var cameraDistance: Float = 340f
    @Volatile var cameraTargetX: Float = 0f
    @Volatile var cameraTargetY: Float = 0f
    @Volatile var cameraTargetZ: Float = 20f

    // Scene state updated from UI thread
    @Volatile var sceneObjects: List<SceneObject> = emptyList()
    @Volatile var selectedObjectId: String? = null
    @Volatile var printerProfile: PrinterProfile? = null
    @Volatile var filamentColorRgb: FloatArray = floatArrayOf(0.0f, 0.78f, 0.65f)
    @Volatile var showOverhangHighlight: Boolean = true
    @Volatile var overhangThresholdDeg: Int = 45

    // 3D Transform Gizmo state (0=MOVE, 1=SCALE, 2=ROTATE, 3=CUT, 4=MIRROR, 5=MEASURE, 6=LAY_ON_FACE, -1=HIDDEN)
    @Volatile var activeGizmoModeOrdinal: Int = 0
    @Volatile var showTransformGizmo: Boolean = true
    @Volatile var activeDraggedAxis: Int = -1 // -1=none, 0=X, 1=Y, 2=Z, 3=XY/Uniform
    @Volatile var cutPlaneHeightMm: Float = 10f
    @Volatile var onLayOnFacePicked: ((Vec3, String) -> Unit)? = null

    // Preview mode toolpath state
    @Volatile var isPreviewMode: Boolean = false
    @Volatile var sliceResult: SliceResult? = null
    @Volatile var minVisibleLayer: Int = 0
    @Volatile var maxVisibleLayer: Int = 0
    @Volatile var activeLayerProgress: Float = 1.0f
    @Volatile var showTravelMoves: Boolean = false

    private var viewportWidth: Int = 1
    private var viewportHeight: Int = 1

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val invVpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Shader programs
    private var meshProgram = 0
    private var lineProgram = 0

    // Cached GPU VBOs for meshes
    private class GpuMeshBuffer(val vboId: Int, val vertexCount: Int)
    private val meshVboCache = WeakHashMap<MeshData, GpuMeshBuffer>()

    // Cached GPU VBO for build plate & grid
    private var cachedGridPrinterId: String? = null
    private var gridVboId: Int = 0
    private var gridVertexCount: Int = 0
    private var plateQuadVboId: Int = 0

    // Cached GPU VBO for sliced toolpaths
    private var cachedSliceResult: SliceResult? = null
    private var toolpathVboId: Int = 0
    private var toolpathRibbonVboId: Int = 0
    private var gizmoLineVboId: Int = 0
    private var gizmoTriVboId: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Deep charcoal-slate viewport background (#0E0F12)
        GLES30.glClearColor(0.055f, 0.059f, 0.071f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        meshVboCache.clear()
        cachedGridPrinterId = null
        cachedSliceResult = null
        gridVboId = 0
        plateQuadVboId = 0
        toolpathVboId = 0
        toolpathRibbonVboId = 0
        gizmoLineVboId = 0
        gizmoTriVboId = 0

        meshProgram = createShaderProgram(MESH_VERTEX_SHADER, MESH_FRAGMENT_SHADER)
        lineProgram = createShaderProgram(LINE_VERTEX_SHADER, LINE_FRAGMENT_SHADER)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = max(1, width)
        viewportHeight = max(1, height)
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 42f, aspect, 2.0f, 3500.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        updateViewProjectionMatrices()

        val printer = printerProfile
        val bedW = printer?.bedWidthX ?: 256f
        val bedD = printer?.bedDepthY ?: 256f
        val maxH = printer?.maxPrintHeightZ ?: 256f

        // 1. Draw Build Plate & Grid
        drawBuildPlateAndGrid(bedW, bedD, maxH, printer?.id ?: "default")

        // 2. Draw either 3D Models (Prepare mode) or GPU Toolpaths (Preview mode)
        val activeSlice = sliceResult
        if (isPreviewMode && activeSlice != null && activeSlice.layers.isNotEmpty()) {
            drawToolpaths(activeSlice)
        } else {
            drawSceneMeshes(bedW, bedD, maxH)
            drawSelectedObjectGizmo()
        }
    }

    private fun updateViewProjectionMatrices() {
        val yawRad = cameraYawDeg * (PI.toFloat() / 180f)
        val pitchRad = cameraPitchDeg.coerceIn(-85f, 85f) * (PI.toFloat() / 180f)
        val dist = cameraDistance.coerceIn(30f, 1800f)

        val eyeX = cameraTargetX + dist * cos(pitchRad) * cos(yawRad)
        val eyeY = cameraTargetY + dist * cos(pitchRad) * sin(yawRad)
        val eyeZ = cameraTargetZ + dist * sin(pitchRad)

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            cameraTargetX, cameraTargetY, cameraTargetZ,
            0f, 0f, 1f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        Matrix.invertM(invVpMatrix, 0, vpMatrix, 0)
    }

    private fun drawBuildPlateAndGrid(bedW: Float, bedD: Float, maxH: Float, printerId: String) {
        if (cachedGridPrinterId != printerId || gridVboId == 0 || plateQuadVboId == 0) {
            rebuildGridVbos(bedW, bedD, maxH, printerId)
        }

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUseProgram(lineProgram)
        val uMvp = GLES30.glGetUniformLocation(lineProgram, "uMVP")
        val uAlpha = GLES30.glGetUniformLocation(lineProgram, "uAlpha")
        val uShowTravel = GLES30.glGetUniformLocation(lineProgram, "uShowTravel")

        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(uShowTravel, 1.0f)

        val stride = 7 * 4 // [x, y, z, r, g, b, typeFlag]
        // Draw dark PEI bed surface quad first
        if (plateQuadVboId != 0) {
            GLES30.glUniform1f(uAlpha, 0.92f)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, plateQuadVboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        }

        // Draw grid lines & coordinate axes
        if (gridVboId != 0 && gridVertexCount > 0) {
            GLES30.glUniform1f(uAlpha, 0.85f)
            GLES30.glLineWidth(1.5f)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, gridVertexCount)
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun rebuildGridVbos(bedW: Float, bedD: Float, maxH: Float, printerId: String) {
        val hw = bedW * 0.5f
        val hd = bedD * 0.5f

        // Dark PEI Build Plate surface (#181A20) at Z = -0.08
        val zPlate = -0.08f
        val pr = 0.094f; val pg = 0.102f; val pb = 0.125f
        val quadFloats = floatArrayOf(
            -hw, -hd, zPlate, pr, pg, pb, 0f,
             hw, -hd, zPlate, pr, pg, pb, 0f,
             hw,  hd, zPlate, pr + 0.02f, pg + 0.02f, pb + 0.025f, 0f,
            -hw, -hd, zPlate, pr, pg, pb, 0f,
             hw,  hd, zPlate, pr + 0.02f, pg + 0.02f, pb + 0.025f, 0f,
            -hw,  hd, zPlate, pr + 0.015f, pg + 0.015f, pb + 0.02f, 0f
        )
        plateQuadVboId = uploadFloatArrayToVbo(quadFloats, plateQuadVboId)

        val lines = ArrayList<Float>(2048)
        fun addLine(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, r: Float, g: Float, b: Float) {
            lines.add(x0); lines.add(y0); lines.add(z0); lines.add(r); lines.add(g); lines.add(b); lines.add(0f)
            lines.add(x1); lines.add(y1); lines.add(z1); lines.add(r); lines.add(g); lines.add(b); lines.add(0f)
        }

        val step = 10f
        var x = -hw
        var idxX = 0
        while (x <= hw + 0.01f) {
            val isCenter = kotlin.math.abs(x) < 0.01f
            val isMajor = (idxX % 5 == 0)
            if (isCenter) {
                addLine(x, -hd, 0.01f, x, hd, 0.01f, 0.18f, 0.55f, 0.28f) // Y center line (Greenish)
            } else if (isMajor) {
                addLine(x, -hd, 0f, x, hd, 0f, 0.25f, 0.28f, 0.34f)
            } else {
                addLine(x, -hd, 0f, x, hd, 0f, 0.16f, 0.18f, 0.22f)
            }
            x += step
            idxX++
        }

        var y = -hd
        var idxY = 0
        while (y <= hd + 0.01f) {
            val isCenter = kotlin.math.abs(y) < 0.01f
            val isMajor = (idxY % 5 == 0)
            if (isCenter) {
                addLine(-hw, y, 0.01f, hw, y, 0.01f, 0.62f, 0.22f, 0.24f) // X center line (Reddish)
            } else if (isMajor) {
                addLine(-hw, y, 0f, hw, y, 0f, 0.25f, 0.28f, 0.34f)
            } else {
                addLine(-hw, y, 0f, hw, y, 0f, 0.16f, 0.18f, 0.22f)
            }
            y += step
            idxY++
        }

        // Build Boundary Perimeter Line (DolphinSlicer Azure/Teal accent)
        addLine(-hw, -hd, 0.03f,  hw, -hd, 0.03f, 0.16f, 0.67f, 0.89f)
        addLine( hw, -hd, 0.03f,  hw,  hd, 0.03f, 0.16f, 0.67f, 0.89f)
        addLine( hw,  hd, 0.03f, -hw,  hd, 0.03f, 0.16f, 0.67f, 0.89f)
        addLine(-hw,  hd, 0.03f, -hw, -hd, 0.03f, 0.16f, 0.67f, 0.89f)

        // Coordinate Axes at front-left corner (-hw, -hd)
        val axisLen = 35f
        addLine(-hw, -hd, 0.05f, -hw + axisLen, -hd, 0.05f, 0.95f, 0.26f, 0.21f) // X Red
        addLine(-hw, -hd, 0.05f, -hw, -hd + axisLen, 0.05f, 0.26f, 0.85f, 0.40f) // Y Green
        addLine(-hw, -hd, 0.05f, -hw, -hd, axisLen, 0.16f, 0.67f, 0.96f)         // Z Blue

        // Build volume wireframe box
        val bc = 0.20f
        addLine(-hw, -hd, maxH, hw, -hd, maxH, bc, bc + 0.03f, bc + 0.07f)
        addLine(hw, -hd, maxH, hw, hd, maxH, bc, bc + 0.03f, bc + 0.07f)
        addLine(hw, hd, maxH, -hw, hd, maxH, bc, bc + 0.03f, bc + 0.07f)
        addLine(-hw, hd, maxH, -hw, -hd, maxH, bc, bc + 0.03f, bc + 0.07f)

        addLine(-hw, -hd, 0f, -hw, -hd, maxH, bc, bc, bc + 0.04f)
        addLine(hw, -hd, 0f, hw, -hd, maxH, bc, bc, bc + 0.04f)
        addLine(hw, hd, 0f, hw, hd, maxH, bc, bc, bc + 0.04f)
        addLine(-hw, hd, 0f, -hw, hd, maxH, bc, bc, bc + 0.04f)

        val lineArr = FloatArray(lines.size)
        for (i in lines.indices) lineArr[i] = lines[i]
        gridVertexCount = lineArr.size / 7
        gridVboId = uploadFloatArrayToVbo(lineArr, gridVboId)
        cachedGridPrinterId = printerId
    }

    private fun drawSceneMeshes(bedW: Float, bedD: Float, maxH: Float) {
        val objects = sceneObjects
        if (objects.isEmpty()) return

        GLES30.glUseProgram(meshProgram)
        val uMvp = GLES30.glGetUniformLocation(meshProgram, "uMVP")
        val uModel = GLES30.glGetUniformLocation(meshProgram, "uModel")
        val uBaseColor = GLES30.glGetUniformLocation(meshProgram, "uBaseColor")
        val uSelected = GLES30.glGetUniformLocation(meshProgram, "uSelected")
        val uShowOverhang = GLES30.glGetUniformLocation(meshProgram, "uShowOverhang")
        val uOverhangCos = GLES30.glGetUniformLocation(meshProgram, "uOverhangCos")
        val uBedHalfSize = GLES30.glGetUniformLocation(meshProgram, "uBedHalfSize")

        val cosThresh = -cos((90 - overhangThresholdDeg.coerceIn(10, 80)) * (PI.toFloat() / 180f))
        GLES30.glUniform1f(uShowOverhang, if (showOverhangHighlight) 1.0f else 0.0f)
        GLES30.glUniform1f(uOverhangCos, cosThresh)
        GLES30.glUniform3f(uBedHalfSize, bedW * 0.5f, bedD * 0.5f, maxH)

        val stride = 6 * 4 // [px, py, pz, nx, ny, nz]
        val baseRgb = filamentColorRgb

        for (obj in objects) {
            if (!obj.visible) continue
            val gpuBuf = getOrCreateMeshVbo(obj.mesh)
            if (gpuBuf.vertexCount <= 0) continue

            // Compute Model matrix matching SceneObject.transformPoint (Global Orientation + 3D center pivot + bed Z snap)
            val wb = obj.worldBoundingBox()
            val zSnap = -wb.min.z
            val lc = obj.mesh.localBounds.center

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, obj.position.x, obj.position.y, obj.position.z + zSnap + lc.z)
            Matrix.scaleM(modelMatrix, 0, obj.scale.x, obj.scale.y, obj.scale.z)
            Matrix.rotateM(modelMatrix, 0, obj.rotationDeg.z, 0f, 0f, 1f)
            Matrix.rotateM(modelMatrix, 0, obj.rotationDeg.y, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, obj.rotationDeg.x, 1f, 0f, 0f)
            Matrix.translateM(modelMatrix, 0, -lc.x, -lc.y, -lc.z)

            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

            val isSelected = (obj.id == selectedObjectId)
            GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
            GLES30.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
            GLES30.glUniform3f(uBaseColor, baseRgb[0], baseRgb[1], baseRgb[2])
            GLES30.glUniform1f(uSelected, if (isSelected) 1.0f else 0.0f)

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gpuBuf.vboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, gpuBuf.vertexCount)
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun drawToolpaths(slice: SliceResult) {
        if (cachedSliceResult !== slice || toolpathVboId == 0 || toolpathRibbonVboId == 0) {
            toolpathVboId = uploadFloatArrayToVbo(slice.gpuToolpathVertices, toolpathVboId)
            toolpathRibbonVboId = uploadFloatArrayToVbo(slice.gpuRibbonVertices, toolpathRibbonVboId)
            cachedSliceResult = slice
        }
        if (slice.layers.isEmpty()) return

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUseProgram(lineProgram)
        val uMvp = GLES30.glGetUniformLocation(lineProgram, "uMVP")
        val uAlpha = GLES30.glGetUniformLocation(lineProgram, "uAlpha")
        val uShowTravel = GLES30.glGetUniformLocation(lineProgram, "uShowTravel")

        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(uShowTravel, if (showTravelMoves) 1.0f else 0.0f)

        val stride = 7 * 4
        val layerCount = slice.layers.size
        val minL = minVisibleLayer.coerceIn(0, layerCount - 1)
        val maxL = maxVisibleLayer.coerceIn(minL, layerCount - 1)

        // 1. Draw volumetric 3D extruded bead ribbons (GL_TRIANGLES) so top/bottom surfaces & walls are solid
        if (toolpathRibbonVboId != 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, toolpathRibbonVboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)

            if (maxL > minL) {
                val startVert = slice.layerStartRibbonVertexIndex[minL]
                val endLayer = maxL - 1
                val totalVerts = (slice.layerStartRibbonVertexIndex[endLayer] + slice.layerRibbonVertexCount[endLayer]) - startVert
                if (totalVerts > 0) {
                    GLES30.glUniform1f(uAlpha, 0.90f)
                    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, startVert, totalVerts)
                }
            }

            val topStartVert = slice.layerStartRibbonVertexIndex[maxL]
            val fullTopVerts = slice.layerRibbonVertexCount[maxL]
            val scrubbedBeads = ((fullTopVerts / 6) * activeLayerProgress.coerceIn(0.01f, 1.0f)).toInt().coerceAtLeast(1)
            val scrubbedVerts = min(fullTopVerts, scrubbedBeads * 6)
            if (scrubbedVerts > 0) {
                GLES30.glUniform1f(uAlpha, 1.0f)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLES, topStartVert, scrubbedVerts)
            }
        }

        // 2. Always draw thick GL_LINES stroke overlay so walls & extrusions have bold stroke weight from all camera angles
        if (toolpathVboId != 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, toolpathVboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)

            GLES30.glLineWidth(4.5f)
            if (maxL > minL) {
                val startVert = slice.layerStartVertexIndex[minL]
                val endLayer = maxL - 1
                val totalVerts = (slice.layerStartVertexIndex[endLayer] + slice.layerVertexCount[endLayer]) - startVert
                if (totalVerts > 0) {
                    GLES30.glUniform1f(uAlpha, 0.94f)
                    GLES30.glDrawArrays(GLES30.GL_LINES, startVert, totalVerts)
                }
            }

            val topStartVert = slice.layerStartVertexIndex[maxL]
            val fullTopVerts = slice.layerVertexCount[maxL]
            val scrubbedSegments = ((fullTopVerts / 2) * activeLayerProgress.coerceIn(0.01f, 1.0f)).toInt().coerceAtLeast(1)
            val scrubbedVerts = min(fullTopVerts, scrubbedSegments * 2)
            if (scrubbedVerts > 0) {
                GLES30.glLineWidth(5.0f)
                GLES30.glUniform1f(uAlpha, 1.0f)
                GLES30.glDrawArrays(GLES30.GL_LINES, topStartVert, scrubbedVerts)
            }
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    /**
     * Renders an interactive 3D Transform Gizmo (Move arrows + cones, Rotate orbital rings,
     * Scale axis cubes, or Cut plane) directly on the selected object.
     */
    private fun drawSelectedObjectGizmo() {
        if (!showTransformGizmo || isPreviewMode) return
        val selId = selectedObjectId ?: return
        val obj = sceneObjects.firstOrNull { it.id == selId && it.visible } ?: return

        val wb = obj.worldBoundingBox()
        val zSnap = -wb.min.z
        val cx = (wb.min.x + wb.max.x) * 0.5f
        val cy = (wb.min.y + wb.max.y) * 0.5f
        val cz = ((wb.min.z + wb.max.z) * 0.5f) + zSnap
        val maxExtent = max(max(wb.size.x, wb.size.y), wb.size.z).coerceAtLeast(16f)
        val gizmoLen = (maxExtent * 0.85f).coerceIn(24f, 95f)

        val lines = ArrayList<Float>(1024)
        val tris = ArrayList<Float>(1024)

        fun addLine(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, r: Float, g: Float, b: Float) {
            lines.add(x0); lines.add(y0); lines.add(z0); lines.add(r); lines.add(g); lines.add(b); lines.add(0f)
            lines.add(x1); lines.add(y1); lines.add(z1); lines.add(r); lines.add(g); lines.add(b); lines.add(0f)
        }

        fun addTri(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            r: Float, g: Float, b: Float
        ) {
            tris.add(x0); tris.add(y0); tris.add(z0); tris.add(r); tris.add(g); tris.add(b); tris.add(0f)
            tris.add(x1); tris.add(y1); tris.add(z1); tris.add(r); tris.add(g); tris.add(b); tris.add(0f)
            tris.add(x2); tris.add(y2); tris.add(z2); tris.add(r); tris.add(g); tris.add(b); tris.add(0f)
        }

        fun addCubeHandle(hx: Float, hy: Float, hz: Float, half: Float, r: Float, g: Float, b: Float) {
            val x0 = hx - half; val x1 = hx + half
            val y0 = hy - half; val y1 = hy + half
            val z0 = hz - half; val z1 = hz + half
            // Top & Bottom
            addTri(x0, y0, z1, x1, y0, z1, x1, y1, z1, r, g, b)
            addTri(x0, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b)
            addTri(x0, y1, z0, x1, y1, z0, x1, y0, z0, r * 0.8f, g * 0.8f, b * 0.8f)
            addTri(x0, y1, z0, x1, y0, z0, x0, y0, z0, r * 0.8f, g * 0.8f, b * 0.8f)
            // Front & Back
            addTri(x0, y0, z0, x1, y0, z0, x1, y0, z1, r * 0.9f, g * 0.9f, b * 0.9f)
            addTri(x0, y0, z0, x1, y0, z1, x0, y0, z1, r * 0.9f, g * 0.9f, b * 0.9f)
            addTri(x1, y1, z0, x0, y1, z0, x0, y1, z1, r * 0.9f, g * 0.9f, b * 0.9f)
            addTri(x1, y1, z0, x0, y1, z1, x1, y1, z1, r * 0.9f, g * 0.9f, b * 0.9f)
            // Left & Right
            addTri(x0, y1, z0, x0, y0, z0, x0, y0, z1, r * 0.85f, g * 0.85f, b * 0.85f)
            addTri(x0, y1, z0, x0, y0, z1, x0, y1, z1, r * 0.85f, g * 0.85f, b * 0.85f)
            addTri(x1, y0, z0, x1, y1, z0, x1, y1, z1, r * 0.95f, g * 0.95f, b * 0.95f)
            addTri(x1, y0, z0, x1, y1, z1, x1, y0, z1, r * 0.95f, g * 0.95f, b * 0.95f)
        }

        // Global World Axis colors (X=Red, Y=Green, Z=Blue; brightened to yellow-white when actively dragged)
        val xr = if (activeDraggedAxis == 0) 1.0f else 0.96f
        val xg = if (activeDraggedAxis == 0) 0.95f else 0.24f
        val xb = if (activeDraggedAxis == 0) 0.25f else 0.24f

        val yr = if (activeDraggedAxis == 1) 1.0f else 0.26f
        val yg = if (activeDraggedAxis == 1) 0.96f else 0.88f
        val yb = if (activeDraggedAxis == 1) 0.28f else 0.40f

        val zr = if (activeDraggedAxis == 2) 1.0f else 0.16f
        val zg = if (activeDraggedAxis == 2) 0.95f else 0.67f
        val zb = if (activeDraggedAxis == 2) 0.25f else 0.96f

        // Subtle selection bounding box wireframe (Orca Azure #29ABE2)
        val bMinX = wb.min.x; val bMaxX = wb.max.x
        val bMinY = wb.min.y; val bMaxY = wb.max.y
        val bMinZ = 0f;       val bMaxZ = wb.max.z + zSnap
        addLine(bMinX, bMinY, bMinZ, bMaxX, bMinY, bMinZ, 0.16f, 0.67f, 0.89f)
        addLine(bMaxX, bMinY, bMinZ, bMaxX, bMaxY, bMinZ, 0.16f, 0.67f, 0.89f)
        addLine(bMaxX, bMaxY, bMinZ, bMinX, bMaxY, bMinZ, 0.16f, 0.67f, 0.89f)
        addLine(bMinX, bMaxY, bMinZ, bMinX, bMinY, bMinZ, 0.16f, 0.67f, 0.89f)

        when (activeGizmoModeOrdinal) {
            0 -> {
                // MOVE GIZMO (Global World Orientation: +X Red, +Y Green, +Z Blue)
                val coneLen = (gizmoLen * 0.22f).coerceIn(5f, 16f)
                val coneRad = coneLen * 0.38f

                // X shaft + pyramid tip
                addLine(cx, cy, cz, cx + gizmoLen, cy, cz, xr, xg, xb)
                val xBase = cx + gizmoLen - coneLen
                val xTip = cx + gizmoLen
                addTri(xTip, cy, cz, xBase, cy - coneRad, cz - coneRad, xBase, cy + coneRad, cz - coneRad, xr, xg, xb)
                addTri(xTip, cy, cz, xBase, cy + coneRad, cz - coneRad, xBase, cy + coneRad, cz + coneRad, xr, xg, xb)
                addTri(xTip, cy, cz, xBase, cy + coneRad, cz + coneRad, xBase, cy - coneRad, cz + coneRad, xr, xg, xb)
                addTri(xTip, cy, cz, xBase, cy - coneRad, cz + coneRad, xBase, cy - coneRad, cz - coneRad, xr, xg, xb)

                // Y shaft + pyramid tip
                addLine(cx, cy, cz, cx, cy + gizmoLen, cz, yr, yg, yb)
                val yBase = cy + gizmoLen - coneLen
                val yTip = cy + gizmoLen
                addTri(cx, yTip, cz, cx - coneRad, yBase, cz - coneRad, cx + coneRad, yBase, cz - coneRad, yr, yg, yb)
                addTri(cx, yTip, cz, cx + coneRad, yBase, cz - coneRad, cx + coneRad, yBase, cz + coneRad, yr, yg, yb)
                addTri(cx, yTip, cz, cx + coneRad, yBase, cz + coneRad, cx - coneRad, yBase, cz + coneRad, yr, yg, yb)
                addTri(cx, yTip, cz, cx - coneRad, yBase, cz + coneRad, cx - coneRad, yBase, cz - coneRad, yr, yg, yb)

                // Z shaft + pyramid tip
                addLine(cx, cy, cz, cx, cy, cz + gizmoLen, zr, zg, zb)
                val zBase = cz + gizmoLen - coneLen
                val zTip = cz + gizmoLen
                addTri(cx, cy, zTip, cx - coneRad, cy - coneRad, zBase, cx + coneRad, cy - coneRad, zBase, zr, zg, zb)
                addTri(cx, cy, zTip, cx + coneRad, cy - coneRad, zBase, cx + coneRad, cy + coneRad, zBase, zr, zg, zb)
                addTri(cx, cy, zTip, cx + coneRad, cy + coneRad, zBase, cx - coneRad, cy + coneRad, zBase, zr, zg, zb)
                addTri(cx, cy, zTip, cx - coneRad, cy + coneRad, zBase, cx - coneRad, cy - coneRad, zBase, zr, zg, zb)

                // XY planar drag quad
                val pOff = gizmoLen * 0.28f
                addLine(cx + pOff, cy, cz, cx + pOff, cy + pOff, cz, 0.16f, 0.67f, 0.89f)
                addLine(cx, cy + pOff, cz, cx + pOff, cy + pOff, cz, 0.16f, 0.67f, 0.89f)
                addCubeHandle(cx, cy, cz, coneRad * 0.55f, 0.90f, 0.95f, 1.0f)
            }

            1 -> {
                // SCALE GIZMO (Global World X, Y, Z axes + Uniform central handle)
                val cubeHalf = (gizmoLen * 0.08f).coerceIn(2.2f, 6.5f)
                addLine(cx, cy, cz, cx + gizmoLen, cy, cz, xr, xg, xb)
                addCubeHandle(cx + gizmoLen, cy, cz, cubeHalf, xr, xg, xb)

                addLine(cx, cy, cz, cx, cy + gizmoLen, cz, yr, yg, yb)
                addCubeHandle(cx, cy + gizmoLen, cz, cubeHalf, yr, yg, yb)

                addLine(cx, cy, cz, cx, cy, cz + gizmoLen, zr, zg, zb)
                addCubeHandle(cx, cy, cz + gizmoLen, cubeHalf, zr, zg, zb)

                addCubeHandle(cx, cy, cz, cubeHalf * 0.85f, 0.92f, 0.96f, 1.0f)

                // Uniform scale XY ring
                val ringSegs = 40
                val ringRad = gizmoLen * 0.45f
                for (i in 0 until ringSegs) {
                    val a0 = (i.toFloat() / ringSegs) * (2f * PI.toFloat())
                    val a1 = ((i + 1).toFloat() / ringSegs) * (2f * PI.toFloat())
                    addLine(
                        cx + cos(a0) * ringRad, cy + sin(a0) * ringRad, cz,
                        cx + cos(a1) * ringRad, cy + sin(a1) * ringRad, cz,
                        0.16f, 0.67f, 0.89f
                    )
                }
            }

            2 -> {
                // ROTATE GIZMO: 3 orthogonal Global World orbital rotation rings (X=Red YZ ring, Y=Green XZ ring, Z=Blue XY ring)
                val segs = 56
                val rad = gizmoLen * 0.82f
                for (i in 0 until segs) {
                    val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
                    val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
                    val c0 = cos(a0) * rad; val s0 = sin(a0) * rad
                    val c1 = cos(a1) * rad; val s1 = sin(a1) * rad

                    // X-axis rotation ring (in YZ plane)
                    addLine(cx, cy + c0, cz + s0, cx, cy + c1, cz + s1, xr, xg, xb)
                    // Y-axis rotation ring (in XZ plane)
                    addLine(cx + c0, cy, cz + s0, cx + c1, cy, cz + s1, yr, yg, yb)
                    // Z-axis rotation ring (in XY plane)
                    addLine(cx + c0, cy + s0, cz, cx + c1, cy + s1, cz, zr, zg, zb)
                }
                val hSize = (rad * 0.07f).coerceIn(2.0f, 5.5f)
                addCubeHandle(cx, cy + rad, cz, hSize, xr, xg, xb)
                addCubeHandle(cx + rad, cy, cz, hSize, yr, yg, yb)
                addCubeHandle(cx + rad * 0.707f, cy + rad * 0.707f, cz, hSize, zr, zg, zb)
            }

            3 -> {
                // CUT PLANE GIZMO: Interactive horizontal slice plane at Z = cutPlaneHeightMm + vertical Z drag handle
                val cutZ = cutPlaneHeightMm.coerceIn(0.5f, (bMaxZ - 0.5f).coerceAtLeast(1f))
                val pad = 10f
                val x0 = bMinX - pad; val x1 = bMaxX + pad
                val y0 = bMinY - pad; val y1 = bMaxY + pad
                val pr = if (activeDraggedAxis == 2) 1.0f else 0.16f
                val pg = if (activeDraggedAxis == 2) 0.95f else 0.67f
                val pb = if (activeDraggedAxis == 2) 0.25f else 0.96f

                // Outer cut plane frame
                addLine(x0, y0, cutZ, x1, y0, cutZ, pr, pg, pb)
                addLine(x1, y0, cutZ, x1, y1, cutZ, pr, pg, pb)
                addLine(x1, y1, cutZ, x0, y1, cutZ, pr, pg, pb)
                addLine(x0, y1, cutZ, x0, y0, cutZ, pr, pg, pb)
                // Cross-hairs on the cut plane
                addLine(x0, cy, cutZ, x1, cy, cutZ, pr * 0.8f, pg * 0.8f, pb * 0.8f)
                addLine(cx, y0, cutZ, cx, y1, cutZ, pr * 0.8f, pg * 0.8f, pb * 0.8f)

                // Translucent cut plane quad
                addTri(x0, y0, cutZ, x1, y0, cutZ, x1, y1, cutZ, 0.16f, 0.67f, 0.89f)
                addTri(x0, y0, cutZ, x1, y1, cutZ, x0, y1, cutZ, 0.16f, 0.67f, 0.89f)

                // Vertical Z-drag arrow handle centered on the cut plane (OrcaSlicer interactive Cut handle)
                val handleLen = (gizmoLen * 0.58f).coerceIn(16f, 45f)
                val coneLen = (handleLen * 0.30f).coerceIn(4.5f, 12f)
                val coneRad = coneLen * 0.42f
                val zTop = cutZ + handleLen
                val zBot = (cutZ - handleLen).coerceAtLeast(0.2f)
                addLine(cx, cy, zBot, cx, cy, zTop, zr, zg, zb)
                addCubeHandle(cx, cy, cutZ, coneRad * 0.7f, 0.95f, 0.98f, 1.0f)

                // Upward cone at zTop
                val upBase = zTop - coneLen
                addTri(cx, cy, zTop, cx - coneRad, cy - coneRad, upBase, cx + coneRad, cy - coneRad, upBase, zr, zg, zb)
                addTri(cx, cy, zTop, cx + coneRad, cy - coneRad, upBase, cx + coneRad, cy + coneRad, upBase, zr, zg, zb)
                addTri(cx, cy, zTop, cx + coneRad, cy + coneRad, upBase, cx - coneRad, cy + coneRad, upBase, zr, zg, zb)
                addTri(cx, cy, zTop, cx - coneRad, cy + coneRad, upBase, cx - coneRad, cy - coneRad, upBase, zr, zg, zb)
            }

            6 -> {
                // LAY ON FACE (Place on Face) GIZMO: Render OrcaSlicer-style translucent planar face patches on the 3D mesh
                val candidates = obj.mesh.layOnFaceCandidates
                for (cand in candidates) {
                    val poly = cand.localPolygon
                    if (poly.size < 3) continue
                    val worldN = obj.transformNormal(cand.localNormal)
                    // Offset slightly outward along world normal (+0.32mm) so patch sits cleanly on surface without Z-fighting
                    val offset = worldN * 0.32f
                    val worldCenter = obj.transformPoint(cand.localCenter).let {
                        Vec3(it.x + offset.x, it.y + offset.y, it.z + zSnap + offset.z)
                    }
                    val worldPoly = poly.map { lv ->
                        val wv = obj.transformPoint(lv)
                        Vec3(wv.x + offset.x, wv.y + offset.y, wv.z + zSnap + offset.z)
                    }

                    // Highlight face already resting on the build plate (worldN.z < -0.98f) in bright cyan-green, others in Orca white/azure
                    val isCurrentBase = worldN.z < -0.97f
                    val fillR = if (isCurrentBase) 0.16f else 0.88f
                    val fillG = if (isCurrentBase) 0.85f else 0.96f
                    val fillB = if (isCurrentBase) 0.72f else 1.00f

                    val lineR = if (isCurrentBase) 0.10f else 0.16f
                    val lineG = if (isCurrentBase) 0.95f else 0.67f
                    val lineB = if (isCurrentBase) 0.65f else 0.89f

                    for (i in worldPoly.indices) {
                        val v0 = worldPoly[i]
                        val v1 = worldPoly[(i + 1) % worldPoly.size]
                        addLine(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, lineR, lineG, lineB)
                        addTri(
                            worldCenter.x, worldCenter.y, worldCenter.z,
                            v0.x, v0.y, v0.z,
                            v1.x, v1.y, v1.z,
                            fillR, fillG, fillB
                        )
                    }

                    // Short outward normal indicator pin at face center
                    val pinTip = worldCenter + (worldN * 3.2f)
                    addLine(worldCenter.x, worldCenter.y, worldCenter.z, pinTip.x, pinTip.y, pinTip.z, lineR, lineG, lineB)
                }
            }
        }

        if (lines.isEmpty() && tris.isEmpty()) return

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES30.glUseProgram(lineProgram)
        val uMvp = GLES30.glGetUniformLocation(lineProgram, "uMVP")
        val uAlpha = GLES30.glGetUniformLocation(lineProgram, "uAlpha")
        val uShowTravel = GLES30.glGetUniformLocation(lineProgram, "uShowTravel")
        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(uShowTravel, 1.0f)

        // For Lay on Face (mode 6), keep depth test enabled so back-side patches are occluded by the 3D mesh;
        // for Move/Scale/Rotate/Cut gizmos, disable depth test so axis handles are always accessible.
        if (activeGizmoModeOrdinal == 6) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glDepthMask(false)
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        }
        val stride = 7 * 4

        if (tris.isNotEmpty()) {
            val triArr = FloatArray(tris.size)
            for (i in tris.indices) triArr[i] = tris[i]
            gizmoTriVboId = uploadFloatArrayToVbo(triArr, gizmoTriVboId)
            val triAlpha = when (activeGizmoModeOrdinal) {
                3 -> 0.30f
                6 -> 0.62f
                else -> 0.96f
            }
            GLES30.glUniform1f(uAlpha, triAlpha)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gizmoTriVboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, triArr.size / 7)
        }

        if (lines.isNotEmpty()) {
            val lineArr = FloatArray(lines.size)
            for (i in lines.indices) lineArr[i] = lines[i]
            gizmoLineVboId = uploadFloatArrayToVbo(lineArr, gizmoLineVboId)
            GLES30.glLineWidth(if (activeGizmoModeOrdinal == 6) 3.5f else 5.0f)
            GLES30.glUniform1f(uAlpha, 0.98f)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gizmoLineVboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, lineArr.size / 7)
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun projectWorldToScreen(wx: Float, wy: Float, wz: Float): Pair<Float, Float>? {
        val clip = FloatArray(4)
        val inVec = floatArrayOf(wx, wy, wz, 1.0f)
        Matrix.multiplyMV(clip, 0, vpMatrix, 0, inVec, 0)
        if (clip[3] <= 0.001f) return null
        val ndcX = clip[0] / clip[3]
        val ndcY = clip[1] / clip[3]
        val sx = (ndcX + 1.0f) * 0.5f * viewportWidth
        val sy = (1.0f - ndcY) * 0.5f * viewportHeight
        return sx to sy
    }

    private fun distToSegment2D(px: Float, py: Float, x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-4f) {
            val ex = px - x0
            val ey = py - y0
            return kotlin.math.sqrt(ex * ex + ey * ey)
        }
        val t = (((px - x0) * dx + (py - y0) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = x0 + t * dx
        val projY = y0 + t * dy
        val ex = px - projX
        val ey = py - projY
        return kotlin.math.sqrt(ex * ex + ey * ey)
    }

    /**
     * Hit-tests `(screenX, screenY)` against the active 3D Transform Gizmo handles on the selected object.
     * Returns:
     *  `0` = X axis handle
     *  `1` = Y axis handle
     *  `2` = Z axis handle
     *  `3` = Central / Free XY / Uniform handle (or selected object body when MOVE/ROTATE/SCALE is active)
     * `-1` = No gizmo hit (orbit camera as normal)
     */
    fun hitTestGizmoAxis(screenX: Float, screenY: Float): Int {
        if (!showTransformGizmo || isPreviewMode || activeGizmoModeOrdinal !in 0..3) return -1
        val selId = selectedObjectId ?: return -1
        val obj = sceneObjects.firstOrNull { it.id == selId && it.visible } ?: return -1

        val wb = obj.worldBoundingBox()
        val zSnap = -wb.min.z
        val cx = (wb.min.x + wb.max.x) * 0.5f
        val cy = (wb.min.y + wb.max.y) * 0.5f
        val cz = ((wb.min.z + wb.max.z) * 0.5f) + zSnap
        val bMaxZ = wb.max.z + zSnap
        val maxExtent = max(max(wb.size.x, wb.size.y), wb.size.z).coerceAtLeast(16f)
        val gizmoLen = (maxExtent * 0.85f).coerceIn(24f, 95f)

        val pCenter = projectWorldToScreen(cx, cy, cz) ?: return -1
        val hitThresholdPx = 56f

        if (activeGizmoModeOrdinal == 3) {
            // Cut Plane Z-axis handle hit test
            val cutZ = cutPlaneHeightMm.coerceIn(0.5f, (bMaxZ - 0.5f).coerceAtLeast(1f))
            val handleLen = (gizmoLen * 0.58f).coerceIn(16f, 45f)
            val pBot = projectWorldToScreen(cx, cy, (cutZ - handleLen).coerceAtLeast(0.2f))
            val pTop = projectWorldToScreen(cx, cy, cutZ + handleLen)
            if (pBot != null && pTop != null) {
                val dHandle = distToSegment2D(screenX, screenY, pBot.first, pBot.second, pTop.first, pTop.second)
                if (dHandle <= hitThresholdPx * 1.15f) return 2
            }
            // Also check if touch hits any edge of the horizontal cut plane frame
            val pad = 10f
            val corners = listOf(
                projectWorldToScreen(wb.min.x - pad, wb.min.y - pad, cutZ),
                projectWorldToScreen(wb.max.x + pad, wb.min.y - pad, cutZ),
                projectWorldToScreen(wb.max.x + pad, wb.max.y + pad, cutZ),
                projectWorldToScreen(wb.min.x - pad, wb.max.y + pad, cutZ)
            )
            for (i in 0..3) {
                val c0 = corners[i]
                val c1 = corners[(i + 1) % 4]
                if (c0 != null && c1 != null) {
                    if (distToSegment2D(screenX, screenY, c0.first, c0.second, c1.first, c1.second) <= hitThresholdPx * 0.85f) {
                        return 2
                    }
                }
            }
            return -1
        } else if (activeGizmoModeOrdinal == 0 || activeGizmoModeOrdinal == 1) {
            val pX = projectWorldToScreen(cx + gizmoLen, cy, cz)
            val pY = projectWorldToScreen(cx, cy + gizmoLen, cz)
            val pZ = projectWorldToScreen(cx, cy, cz + gizmoLen)

            val dCenter = distToSegment2D(screenX, screenY, pCenter.first, pCenter.second, pCenter.first, pCenter.second)
            if (dCenter < hitThresholdPx * 0.75f) return 3

            val dX = pX?.let { distToSegment2D(screenX, screenY, pCenter.first, pCenter.second, it.first, it.second) } ?: Float.POSITIVE_INFINITY
            val dY = pY?.let { distToSegment2D(screenX, screenY, pCenter.first, pCenter.second, it.first, it.second) } ?: Float.POSITIVE_INFINITY
            val dZ = pZ?.let { distToSegment2D(screenX, screenY, pCenter.first, pCenter.second, it.first, it.second) } ?: Float.POSITIVE_INFINITY

            val minAxisDist = min(dX, min(dY, dZ))
            if (minAxisDist <= hitThresholdPx) {
                return when (minAxisDist) {
                    dX -> 0
                    dY -> 1
                    else -> 2
                }
            }
        } else if (activeGizmoModeOrdinal == 2) {
            // Rotate rings: test segments along the full 3D X (YZ), Y (XZ), and Z (XY) orbital rings in screen space
            val rad = gizmoLen * 0.82f
            val ringSegs = 28
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY

            for (i in 0 until ringSegs) {
                val a0 = (i.toFloat() / ringSegs) * (2f * PI.toFloat())
                val a1 = ((i + 1).toFloat() / ringSegs) * (2f * PI.toFloat())
                val c0 = cos(a0) * rad; val s0 = sin(a0) * rad
                val c1 = cos(a1) * rad; val s1 = sin(a1) * rad

                val px0 = projectWorldToScreen(cx, cy + c0, cz + s0)
                val px1 = projectWorldToScreen(cx, cy + c1, cz + s1)
                if (px0 != null && px1 != null) {
                    minX = min(minX, distToSegment2D(screenX, screenY, px0.first, px0.second, px1.first, px1.second))
                }

                val py0 = projectWorldToScreen(cx + c0, cy, cz + s0)
                val py1 = projectWorldToScreen(cx + c1, cy, cz + s1)
                if (py0 != null && py1 != null) {
                    minY = min(minY, distToSegment2D(screenX, screenY, py0.first, py0.second, py1.first, py1.second))
                }

                val pz0 = projectWorldToScreen(cx + c0, cy + s0, cz)
                val pz1 = projectWorldToScreen(cx + c1, cy + s1, cz)
                if (pz0 != null && pz1 != null) {
                    minZ = min(minZ, distToSegment2D(screenX, screenY, pz0.first, pz0.second, pz1.first, pz1.second))
                }
            }

            val minRingDist = min(minX, min(minY, minZ))
            if (minRingDist <= hitThresholdPx * 0.95f) {
                return when (minRingDist) {
                    minX -> 0
                    minY -> 1
                    else -> 2
                }
            }
        }

        // Also check if touch ray hits the selected object's bounding box directly
        val ndcX = (2.0f * screenX) / viewportWidth - 1.0f
        val ndcY = 1.0f - (2.0f * screenY) / viewportHeight
        val nearClip = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)
        val farClip = floatArrayOf(ndcX, ndcY, 1.0f, 1.0f)
        val nearWorld = FloatArray(4)
        val farWorld = FloatArray(4)
        Matrix.multiplyMV(nearWorld, 0, invVpMatrix, 0, nearClip, 0)
        Matrix.multiplyMV(farWorld, 0, invVpMatrix, 0, farClip, 0)
        if (kotlin.math.abs(nearWorld[3]) > 1e-6f && kotlin.math.abs(farWorld[3]) > 1e-6f) {
            val rayOrigin = Vec3(nearWorld[0] / nearWorld[3], nearWorld[1] / nearWorld[3], nearWorld[2] / nearWorld[3])
            val rayEnd = Vec3(farWorld[0] / farWorld[3], farWorld[1] / farWorld[3], farWorld[2] / farWorld[3])
            val rayDir = (rayEnd - rayOrigin).normalized()
            val snappedBounds = BoundingBox(
                min = Vec3(wb.min.x, wb.min.y, 0f),
                max = Vec3(wb.max.x, wb.max.y, wb.max.z + zSnap)
            )
            if (snappedBounds.intersectRay(rayOrigin, rayDir) != null) {
                return 3
            }
        }

        return -1
    }

    /**
     * Projects a screen-space touch delta `(dxPx, dyPx)` onto the Global World Axis `(axisX, axisY, axisZ)`
     * anchored at the selected object's 3D world center, returning the exact displacement in millimeters (mm)
     * along that global axis regardless of camera yaw or pitch.
     */
    fun projectWorldAxisDelta(dxPx: Float, dyPx: Float, axisX: Float, axisY: Float, axisZ: Float): Float {
        val selId = selectedObjectId
        val obj = if (selId != null) sceneObjects.firstOrNull { it.id == selId && it.visible } else null
        val cx: Float
        val cy: Float
        val cz: Float
        if (obj != null) {
            val wb = obj.worldBoundingBox()
            val zSnap = -wb.min.z
            cx = (wb.min.x + wb.max.x) * 0.5f
            cy = (wb.min.y + wb.max.y) * 0.5f
            cz = ((wb.min.z + wb.max.z) * 0.5f) + zSnap
        } else {
            cx = cameraTargetX
            cy = cameraTargetY
            cz = cameraTargetZ
        }

        val stepMm = 10f
        val p0 = projectWorldToScreen(cx, cy, cz)
        val p1 = projectWorldToScreen(cx + axisX * stepMm, cy + axisY * stepMm, cz + axisZ * stepMm)
        if (p0 == null || p1 == null) {
            val fallbackScale = (cameraDistance * 0.0015f).coerceIn(0.05f, 1.5f)
            return (dxPx - dyPx) * fallbackScale
        }

        // Screen pixels per 1mm along Global Axis (axisX, axisY, axisZ)
        val vx = (p1.first - p0.first) / stepMm
        val vy = (p1.second - p0.second) / stepMm
        val lenSq = vx * vx + vy * vy
        if (lenSq < 0.04f) return 0f

        return ((dxPx * vx + dyPx * vy) / lenSq.coerceAtLeast(0.25f)).coerceIn(-25f, 25f)
    }

    /**
     * Computes the signed angular rotation delta (in degrees) around Global World Axis `axis` (`0=X`, `1=Y`, `2=Z`)
     * when dragging from `(prevX, prevY)` to `(curX, curY)` around the selected object's projected screen center.
     */
    fun computeScreenRotationDeltaDeg(prevX: Float, prevY: Float, curX: Float, curY: Float, axis: Int): Float {
        val selId = selectedObjectId
        val obj = if (selId != null) sceneObjects.firstOrNull { it.id == selId && it.visible } else null
        val cx: Float
        val cy: Float
        val cz: Float
        if (obj != null) {
            val wb = obj.worldBoundingBox()
            val zSnap = -wb.min.z
            cx = (wb.min.x + wb.max.x) * 0.5f
            cy = (wb.min.y + wb.max.y) * 0.5f
            cz = ((wb.min.z + wb.max.z) * 0.5f) + zSnap
        } else {
            cx = cameraTargetX
            cy = cameraTargetY
            cz = cameraTargetZ
        }

        val pCenter = projectWorldToScreen(cx, cy, cz) ?: return (curX - prevX) * 0.5f
        val r0x = prevX - pCenter.first
        val r0y = prevY - pCenter.second
        val r1x = curX - pCenter.first
        val r1y = curY - pCenter.second

        val radSq0 = r0x * r0x + r0y * r0y
        val radSq1 = r1x * r1x + r1y * r1y
        if (radSq0 < 16f || radSq1 < 16f) {
            return (curX - prevX - (curY - prevY)) * 0.45f
        }

        val a0 = kotlin.math.atan2(r0y, r0x)
        val a1 = kotlin.math.atan2(r1y, r1x)
        var dAngle = a1 - a0
        while (dAngle > PI.toFloat()) dAngle -= 2f * PI.toFloat()
        while (dAngle < -PI.toFloat()) dAngle += 2f * PI.toFloat()

        // Determine whether camera looks along +Axis or -Axis so screen clockwise/counter-clockwise matches right-hand rule
        val yawRad = cameraYawDeg * (PI.toFloat() / 180f)
        val pitchRad = cameraPitchDeg.coerceIn(-85f, 85f) * (PI.toFloat() / 180f)
        val viewDirX = -cos(pitchRad) * cos(yawRad)
        val viewDirY = -cos(pitchRad) * sin(yawRad)
        val viewDirZ = -sin(pitchRad)
        val axisDotView = when (axis) {
            0 -> viewDirX
            1 -> viewDirY
            else -> viewDirZ
        }
        val sign = if (axisDotView >= 0f) 1.0f else -1.0f
        return (dAngle * (180f / PI.toFloat()) * sign).coerceIn(-25f, 25f)
    }

    private fun getOrCreateMeshVbo(mesh: MeshData): GpuMeshBuffer {
        meshVboCache[mesh]?.let { return it }
        val floats = mesh.gpuVertexBufferData
        val vbo = uploadFloatArrayToVbo(floats, 0)
        val entry = GpuMeshBuffer(vbo, mesh.vertexCount)
        meshVboCache[mesh] = entry
        return entry
    }

    private fun uploadFloatArrayToVbo(data: FloatArray, existingVbo: Int): Int {
        if (data.isEmpty()) return 0
        val vboId = if (existingVbo != 0) {
            existingVbo
        } else {
            val ids = IntArray(1)
            GLES30.glGenBuffers(1, ids, 0)
            ids[0]
        }
        val totalBytes = data.size * 4
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, totalBytes, null, GLES30.GL_STATIC_DRAW)

        val chunkFloats = min(data.size, 16384)
        val chunkBuf: FloatBuffer = ByteBuffer
            .allocateDirect(chunkFloats * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        var offsetFloats = 0
        while (offsetFloats < data.size) {
            val count = min(chunkFloats, data.size - offsetFloats)
            chunkBuf.clear()
            chunkBuf.put(data, offsetFloats, count)
            chunkBuf.position(0)
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, offsetFloats * 4, count * 4, chunkBuf)
            offsetFloats += count
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        return vboId
    }

    fun applyCameraPreset(preset: ViewportCameraPreset) {
        when (preset) {
            ViewportCameraPreset.ISOMETRIC -> {
                cameraYawDeg = -45f
                cameraPitchDeg = 32f
            }
            ViewportCameraPreset.TOP -> {
                cameraYawDeg = -90f
                cameraPitchDeg = 84.9f
            }
            ViewportCameraPreset.FRONT -> {
                cameraYawDeg = -90f
                cameraPitchDeg = 4f
            }
            ViewportCameraPreset.RIGHT -> {
                cameraYawDeg = 0f
                cameraPitchDeg = 4f
            }
        }
    }

    fun resetAndFitCamera() {
        cameraTargetX = 0f
        cameraTargetY = 0f
        cameraTargetZ = 18f
        cameraYawDeg = -45f
        cameraPitchDeg = 32f
        val bedW = printerProfile?.bedWidthX ?: 256f
        cameraDistance = (bedW * 1.35f).coerceIn(180f, 600f)
    }

    /**
     * Performs 3D ray-casting from screen touch coordinates `(screenX, screenY)`
     * against all visible `SceneObject` world bounding boxes to select an object.
     */
    fun pickObjectAtScreenPos(screenX: Float, screenY: Float) {
        if (isPreviewMode || viewportWidth <= 0 || viewportHeight <= 0) return

        val ndcX = (2.0f * screenX) / viewportWidth - 1.0f
        val ndcY = 1.0f - (2.0f * screenY) / viewportHeight

        val nearClip = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)
        val farClip = floatArrayOf(ndcX, ndcY, 1.0f, 1.0f)
        val nearWorld = FloatArray(4)
        val farWorld = FloatArray(4)

        Matrix.multiplyMV(nearWorld, 0, invVpMatrix, 0, nearClip, 0)
        Matrix.multiplyMV(farWorld, 0, invVpMatrix, 0, farClip, 0)

        if (kotlin.math.abs(nearWorld[3]) < 1e-6f || kotlin.math.abs(farWorld[3]) < 1e-6f) return

        val rayOrigin = Vec3(
            nearWorld[0] / nearWorld[3],
            nearWorld[1] / nearWorld[3],
            nearWorld[2] / nearWorld[3]
        )
        val rayEnd = Vec3(
            farWorld[0] / farWorld[3],
            farWorld[1] / farWorld[3],
            farWorld[2] / farWorld[3]
        )
        val rayDir = (rayEnd - rayOrigin).normalized()

        // If LAY_ON_FACE mode (ordinal 6) is active, check if the tap hits a face patch or triangle on the selected object
        if (activeGizmoModeOrdinal == 6 && showTransformGizmo) {
            val selId = selectedObjectId
            val selObj = if (selId != null) sceneObjects.firstOrNull { it.id == selId && it.visible } else null
            if (selObj != null) {
                val rawBounds = selObj.worldBoundingBox()
                val zSnap = -rawBounds.min.z

                // 1. Check ray-triangle intersection across all triangles of the selected object (closest hit in front of camera)
                val snappedBounds = BoundingBox(
                    min = Vec3(rawBounds.min.x - 2f, rawBounds.min.y - 2f, -1f),
                    max = Vec3(rawBounds.max.x + 2f, rawBounds.max.y + 2f, rawBounds.max.z + zSnap + 2f)
                )
                if (snappedBounds.intersectRay(rayOrigin, rayDir) != null) {
                    var bestTriT = Float.POSITIVE_INFINITY
                    var bestLocalNormal: Vec3? = null

                    for (localTri in selObj.mesh.triangles) {
                        val w0 = selObj.transformPoint(localTri.v0).let { Vec3(it.x, it.y, it.z + zSnap) }
                        val w1 = selObj.transformPoint(localTri.v1).let { Vec3(it.x, it.y, it.z + zSnap) }
                        val w2 = selObj.transformPoint(localTri.v2).let { Vec3(it.x, it.y, it.z + zSnap) }
                        val worldTri = com.orcaslicer.android.model.Triangle(w0, w1, w2)
                        val tHit = worldTri.intersectRay(rayOrigin, rayDir)
                        if (tHit != null && tHit < bestTriT) {
                            bestTriT = tHit
                            bestLocalNormal = localTri.normal
                        }
                    }

                    // Snap to nearest candidate planar face if within 8 degrees so angled facets group cleanly
                    if (bestLocalNormal != null) {
                        val hitN = bestLocalNormal.normalized()
                        val matchedCandidate = selObj.mesh.layOnFaceCandidates.firstOrNull {
                            it.localNormal.dot(hitN) > 0.985f
                        }
                        if (matchedCandidate != null) {
                            onLayOnFacePicked?.invoke(matchedCandidate.localNormal, matchedCandidate.label)
                        } else {
                            onLayOnFacePicked?.invoke(hitN, "Selected Face")
                        }
                        return
                    }

                    // 2. Fallback: screen-space proximity to visible front-facing candidate patch centers
                    var bestPatchDistPx = 54f
                    var bestCandidate: com.orcaslicer.android.model.LayOnFaceCandidate? = null
                    for (cand in selObj.mesh.layOnFaceCandidates) {
                        val worldN = selObj.transformNormal(cand.localNormal)
                        // Only pick front-facing patches (facing toward camera)
                        if (worldN.dot(rayDir) < 0.15f) {
                            val wc = selObj.transformPoint(cand.localCenter).let { Vec3(it.x, it.y, it.z + zSnap) }
                            val pScreen = projectWorldToScreen(wc.x, wc.y, wc.z)
                            if (pScreen != null) {
                                val dx = screenX - pScreen.first
                                val dy = screenY - pScreen.second
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                if (dist < bestPatchDistPx) {
                                    bestPatchDistPx = dist
                                    bestCandidate = cand
                                }
                            }
                        }
                    }
                    if (bestCandidate != null) {
                        onLayOnFacePicked?.invoke(bestCandidate.localNormal, bestCandidate.label)
                        return
                    }
                }
            }
        }

        var closestId: String? = null
        var closestT = Float.POSITIVE_INFINITY

        for (obj in sceneObjects) {
            if (!obj.visible) continue
            val rawBounds = obj.worldBoundingBox()
            val zSnap = -rawBounds.min.z
            val snappedBounds = BoundingBox(
                min = Vec3(rawBounds.min.x, rawBounds.min.y, 0f),
                max = Vec3(rawBounds.max.x, rawBounds.max.y, rawBounds.max.z + zSnap)
            )
            val hitT = snappedBounds.intersectRay(rayOrigin, rayDir)
            if (hitT != null && hitT < closestT) {
                closestT = hitT
                closestId = obj.id
            }
        }

        onObjectPicked(closestId)
    }

    private fun createShaderProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val MESH_VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;

            uniform mat4 uMVP;
            uniform mat4 uModel;

            out vec3 vWorldPos;
            out vec3 vWorldNormal;

            void main() {
                vec4 worldPos4 = uModel * vec4(aPosition, 1.0);
                vWorldPos = worldPos4.xyz;
                vWorldNormal = normalize(mat3(uModel) * aNormal);
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """

        private const val MESH_FRAGMENT_SHADER = """#version 300 es
            precision highp float;

            in vec3 vWorldPos;
            in vec3 vWorldNormal;

            uniform vec3 uBaseColor;
            uniform float uSelected;
            uniform float uShowOverhang;
            uniform float uOverhangCos;
            uniform vec3 uBedHalfSize;

            out vec4 fragColor;

            void main() {
                vec3 N = normalize(vWorldNormal);
                vec3 L1 = normalize(vec3(-0.45, -0.65, 0.85));
                vec3 L2 = normalize(vec3(0.60, 0.50, 0.35));

                float diff1 = max(dot(N, L1), 0.0);
                float diff2 = max(dot(N, L2), 0.0) * 0.35;
                float hemi = 0.28 + 0.14 * (N.z * 0.5 + 0.5);

                vec3 base = uBaseColor;

                // Selection highlight tint (DolphinSlicer Azure/Teal)
                if (uSelected > 0.5) {
                    base = mix(base, vec3(0.16, 0.67, 0.89), 0.28);
                }

                // GPU Overhang Detection: highlight downward-facing surfaces steeper than threshold
                if (uShowOverhang > 0.5 && vWorldPos.z > 0.35 && N.z < uOverhangCos) {
                    float severity = clamp((uOverhangCos - N.z) / max(0.01, 1.0 + uOverhangCos), 0.0, 1.0);
                    vec3 overhangColor = mix(vec3(1.0, 0.55, 0.10), vec3(0.95, 0.12, 0.18), severity);
                    base = mix(base, overhangColor, 0.78);
                }

                // Out-of-build-volume warning stripes
                if (abs(vWorldPos.x) > uBedHalfSize.x ||
                    abs(vWorldPos.y) > uBedHalfSize.y ||
                    vWorldPos.z > uBedHalfSize.z) {
                    float stripe = step(0.5, fract((vWorldPos.x + vWorldPos.y + vWorldPos.z) * 0.15));
                    base = mix(base, vec3(1.0, 0.15, 0.15), 0.55 * stripe + 0.25);
                }

                // Subtle layer-line contour shading along Z
                float layerRipple = 0.97 + 0.03 * sin(vWorldPos.z * 31.4159);
                vec3 litColor = base * (hemi + diff1 * 0.65 + diff2) * layerRipple;

                // Specular highlight
                vec3 V = vec3(0.0, -0.5, 0.86);
                vec3 H = normalize(L1 + V);
                float spec = pow(max(dot(N, H), 0.0), 28.0) * 0.22;
                litColor += vec3(spec);

                fragColor = vec4(clamp(litColor, 0.0, 1.0), 1.0);
            }
        """

        private const val LINE_VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aColor;
            layout(location = 2) in float aTypeOrdinal;

            uniform mat4 uMVP;

            out vec3 vColor;
            out float vTypeOrdinal;

            void main() {
                vColor = aColor;
                vTypeOrdinal = aTypeOrdinal;
                gl_Position = uMVP * vec4(aPosition, 1.0);
            }
        """

        private const val LINE_FRAGMENT_SHADER = """#version 300 es
            precision mediump float;

            in vec3 vColor;
            in float vTypeOrdinal;

            uniform float uAlpha;
            uniform float uShowTravel;

            out vec4 fragColor;

            void main() {
                // ToolpathType.TRAVEL ordinal == 8.0
                if (vTypeOrdinal > 7.5 && uShowTravel < 0.5) {
                    discard;
                }
                fragColor = vec4(vColor, uAlpha);
            }
        """
    }
}
