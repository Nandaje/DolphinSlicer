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
    @Volatile var filamentColorRgb: FloatArray = floatArrayOf(0.0f, 0.78f, 0.59f)
    @Volatile var showOverhangHighlight: Boolean = true
    @Volatile var overhangThresholdDeg: Int = 45

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

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Dark studio viewport background (#0E0F12) matching mobile_3d_slicer_ui.html
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
            // Draw ghosted silhouette of models at low alpha or draw toolpaths directly
            drawToolpaths(activeSlice)
        } else {
            drawSceneMeshes(bedW, bedD, maxH)
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

        // Textured PEI Build Plate dark charcoal surface (#181A20) at Z = -0.08
        val zPlate = -0.08f
        val pr = 0.094f; val pg = 0.102f; val pb = 0.125f
        val quadFloats = floatArrayOf(
            -hw, -hd, zPlate, pr, pg, pb, 0f,
             hw, -hd, zPlate, pr, pg, pb, 0f,
             hw,  hd, zPlate, pr + 0.018f, pg + 0.020f, pb + 0.024f, 0f,
            -hw, -hd, zPlate, pr, pg, pb, 0f,
             hw,  hd, zPlate, pr + 0.018f, pg + 0.020f, pb + 0.024f, 0f,
            -hw,  hd, zPlate, pr + 0.012f, pg + 0.014f, pb + 0.018f, 0f
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
            val isMajor = (idxX % 5 == 0)
            if (isMajor) {
                addLine(x, -hd, 0f, x, hd, 0f, 0.14f, 0.34f, 0.46f)
            } else {
                addLine(x, -hd, 0f, x, hd, 0f, 0.145f, 0.157f, 0.196f)
            }
            x += step
            idxX++
        }

        var y = -hd
        var idxY = 0
        while (y <= hd + 0.01f) {
            val isMajor = (idxY % 5 == 0)
            if (isMajor) {
                addLine(-hw, y, 0f, hw, y, 0f, 0.14f, 0.34f, 0.46f)
            } else {
                addLine(-hw, y, 0f, hw, y, 0f, 0.145f, 0.157f, 0.196f)
            }
            y += step
            idxY++
        }

        // Build Boundary Perimeter Line (Dolphin Blue/Teal accent)
        addLine(-hw, -hd, 0.03f,  hw, -hd, 0.03f, 0.16f, 0.67f, 0.89f)
        addLine( hw, -hd, 0.03f,  hw,  hd, 0.03f, 0.16f, 0.67f, 0.89f)
        addLine( hw,  hd, 0.03f, -hw,  hd, 0.03f, 0.16f, 0.67f, 0.89f)
        addLine(-hw,  hd, 0.03f, -hw, -hd, 0.03f, 0.16f, 0.67f, 0.89f)

        // Coordinate Axes at front-left corner (-hw, -hd)
        val axisLen = 35f
        addLine(-hw, -hd, 0.05f, -hw + axisLen, -hd, 0.05f, 0.95f, 0.26f, 0.26f) // X Red
        addLine(-hw, -hd, 0.05f, -hw, -hd + axisLen, 0.05f, 0.26f, 0.88f, 0.40f) // Y Green
        addLine(-hw, -hd, 0.05f, -hw, -hd, axisLen, 0.28f, 0.62f, 0.98f)         // Z Blue

        // Build volume wireframe box
        val bc = 0.19f
        addLine(-hw, -hd, maxH, hw, -hd, maxH, bc, bc, bc + 0.03f)
        addLine(hw, -hd, maxH, hw, hd, maxH, bc, bc, bc + 0.03f)
        addLine(hw, hd, maxH, -hw, hd, maxH, bc, bc, bc + 0.03f)
        addLine(-hw, hd, maxH, -hw, -hd, maxH, bc, bc, bc + 0.03f)

        addLine(-hw, -hd, 0f, -hw, -hd, maxH, bc, bc, bc)
        addLine(hw, -hd, 0f, hw, -hd, maxH, bc, bc, bc)
        addLine(hw, hd, 0f, hw, hd, maxH, bc, bc, bc)
        addLine(-hw, hd, 0f, -hw, hd, maxH, bc, bc, bc)

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

            // Compute Model matrix matching SceneObject.transformPoint + bed Z snap
            val wb = obj.worldBoundingBox()
            val zSnap = -wb.min.z

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, obj.position.x, obj.position.y, obj.position.z + zSnap)
            Matrix.rotateM(modelMatrix, 0, obj.rotationDeg.z, 0f, 0f, 1f)
            Matrix.rotateM(modelMatrix, 0, obj.rotationDeg.y, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, obj.rotationDeg.x, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, obj.scale.x, obj.scale.y, obj.scale.z)

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

        // 2. Draw travel lines (or centerline overlay when travel is enabled)
        if (showTravelMoves && toolpathVboId != 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, toolpathVboId)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, stride, 6 * 4)

            val topStartVert = slice.layerStartVertexIndex[maxL]
            val fullTopVerts = slice.layerVertexCount[maxL]
            val scrubbedSegments = ((fullTopVerts / 2) * activeLayerProgress.coerceIn(0.01f, 1.0f)).toInt().coerceAtLeast(1)
            val scrubbedVerts = min(fullTopVerts, scrubbedSegments * 2)
            if (scrubbedVerts > 0) {
                GLES30.glLineWidth(2.0f)
                GLES30.glUniform1f(uAlpha, 0.85f)
                GLES30.glDrawArrays(GLES30.GL_LINES, topStartVert, scrubbedVerts)
            }
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
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

                // Selection highlight tint
                if (uSelected > 0.5) {
                    base = mix(base, vec3(0.25, 0.98, 0.78), 0.25);
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
