package com.orcaslicer.android.gpu

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.model.SliceResult
import com.orcaslicer.android.model.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OrcaGlSurfaceView(
    context: Context,
    onObjectSelected: (String?) -> Unit
) : GLSurfaceView(context) {

    val renderer: OrcaGlRenderer = OrcaGlRenderer(onObjectSelected)
    var onTransformSelectedObject: (((SceneObject) -> SceneObject) -> Unit)? = null
    var onUpdateCutPlaneHeight: ((Float) -> Unit)? = null
    var onLayOnFaceSelected: ((Vec3, String) -> Unit)? = null
    var lastHandledPresetTriggerId: Int = 0
    var lastHandledFitTriggerId: Int = 0

    private var prevTouchX = 0f
    private var prevTouchY = 0f
    private var prevTwoFingerX = 0f
    private var prevTwoFingerY = 0f
    private var isScalingOrPanning = false
    private var draggedGizmoAxis = -1 // -1=none, 0=X, 1=Y, 2=Z, 3=XY/Uniform

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScalingOrPanning = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor.coerceIn(0.75f, 1.35f)
                renderer.cameraDistance = (renderer.cameraDistance / factor).coerceIn(30f, 1800f)
                requestRender()
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val tapX = e.x
                val tapY = e.y
                queueEvent {
                    renderer.pickObjectAtScreenPos(tapX, tapY)
                }
                requestRender()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                renderer.resetAndFitCamera()
                requestRender()
                return true
            }
        }
    )

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevTouchX = event.x
                prevTouchY = event.y
                isScalingOrPanning = false
                draggedGizmoAxis = renderer.hitTestGizmoAxis(event.x, event.y)
                renderer.activeDraggedAxis = draggedGizmoAxis
                if (draggedGizmoAxis != -1) {
                    requestRender()
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    isScalingOrPanning = true
                    draggedGizmoAxis = -1
                    renderer.activeDraggedAxis = -1
                    prevTwoFingerX = (event.getX(0) + event.getX(1)) * 0.5f
                    prevTwoFingerY = (event.getY(0) + event.getY(1)) * 0.5f
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    isScalingOrPanning = true
                    draggedGizmoAxis = -1
                    renderer.activeDraggedAxis = -1
                    val cx = (event.getX(0) + event.getX(1)) * 0.5f
                    val cy = (event.getY(0) + event.getY(1)) * 0.5f
                    val dx = cx - prevTwoFingerX
                    val dy = cy - prevTwoFingerY
                    prevTwoFingerX = cx
                    prevTwoFingerY = cy

                    val panScale = (renderer.cameraDistance * 0.0014f).coerceIn(0.05f, 2.0f)
                    val yawRad = renderer.cameraYawDeg * (PI.toFloat() / 180f)
                    // Right vector in XY plane is (-sin(yaw), cos(yaw))
                    val rightX = -sin(yawRad)
                    val rightY = cos(yawRad)

                    renderer.cameraTargetX += (-dx * rightX) * panScale
                    renderer.cameraTargetY += (-dx * rightY) * panScale
                    renderer.cameraTargetZ = (renderer.cameraTargetZ + dy * panScale).coerceIn(-20f, 400f)
                    requestRender()
                } else if (!isScalingOrPanning && event.pointerCount == 1) {
                    val oldX = prevTouchX
                    val oldY = prevTouchY
                    val dx = event.x - oldX
                    val dy = event.y - oldY
                    prevTouchX = event.x
                    prevTouchY = event.y

                    if (draggedGizmoAxis != -1 && onTransformSelectedObject != null) {
                        val halfBedX = (renderer.printerProfile?.bedWidthX ?: 256f) * 0.5f
                        val halfBedY = (renderer.printerProfile?.bedDepthY ?: 256f) * 0.5f

                        when (renderer.activeGizmoModeOrdinal) {
                            0 -> {
                                // MOVE GIZMO (Global World Orientation: +X Red, +Y Green, +Z Blue)
                                val deltaX = renderer.projectWorldAxisDelta(dx, dy, 1f, 0f, 0f)
                                val deltaY = renderer.projectWorldAxisDelta(dx, dy, 0f, 1f, 0f)
                                val deltaZ = renderer.projectWorldAxisDelta(dx, dy, 0f, 0f, 1f)

                                onTransformSelectedObject?.invoke { obj ->
                                    when (draggedGizmoAxis) {
                                        0 -> obj.copy(position = obj.position.copy(x = (obj.position.x + deltaX).coerceIn(-halfBedX, halfBedX)))
                                        1 -> obj.copy(position = obj.position.copy(y = (obj.position.y + deltaY).coerceIn(-halfBedY, halfBedY)))
                                        2 -> obj.copy(position = obj.position.copy(z = (obj.position.z + deltaZ).coerceIn(-50f, 300f)))
                                        else -> obj.copy(
                                            position = obj.position.copy(
                                                x = (obj.position.x + deltaX).coerceIn(-halfBedX, halfBedX),
                                                y = (obj.position.y + deltaY).coerceIn(-halfBedY, halfBedY)
                                            )
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // SCALE GIZMO (Global World X, Y, Z axes + Uniform scale)
                                val scaleFactorPerMm = 0.025f
                                val deltaSx = renderer.projectWorldAxisDelta(dx, dy, 1f, 0f, 0f) * scaleFactorPerMm
                                val deltaSy = renderer.projectWorldAxisDelta(dx, dy, 0f, 1f, 0f) * scaleFactorPerMm
                                val deltaSz = renderer.projectWorldAxisDelta(dx, dy, 0f, 0f, 1f) * scaleFactorPerMm
                                val uniformDelta = (dx - dy) * 0.0045f

                                onTransformSelectedObject?.invoke { obj ->
                                    when (draggedGizmoAxis) {
                                        0 -> obj.copy(scale = obj.scale.copy(x = (obj.scale.x + deltaSx).coerceIn(0.1f, 5.0f)))
                                        1 -> obj.copy(scale = obj.scale.copy(y = (obj.scale.y + deltaSy).coerceIn(0.1f, 5.0f)))
                                        2 -> obj.copy(scale = obj.scale.copy(z = (obj.scale.z + deltaSz).coerceIn(0.1f, 5.0f)))
                                        else -> {
                                            val nextS = (obj.scale.x + uniformDelta).coerceIn(0.1f, 5.0f)
                                            obj.copy(scale = Vec3(nextS, nextS, nextS))
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // ROTATE GIZMO (Global World X, Y, Z orbital rings around object 3D center)
                                val rotAxis = if (draggedGizmoAxis in 0..2) draggedGizmoAxis else 2
                                val degDelta = renderer.computeScreenRotationDeltaDeg(oldX, oldY, event.x, event.y, rotAxis)
                                onTransformSelectedObject?.invoke { obj ->
                                    when (rotAxis) {
                                        0 -> obj.copy(rotationDeg = obj.rotationDeg.copy(x = ((obj.rotationDeg.x + degDelta + 540f) % 360f) - 180f))
                                        1 -> obj.copy(rotationDeg = obj.rotationDeg.copy(y = ((obj.rotationDeg.y + degDelta + 540f) % 360f) - 180f))
                                        else -> obj.copy(rotationDeg = obj.rotationDeg.copy(z = ((obj.rotationDeg.z + degDelta + 540f) % 360f) - 180f))
                                    }
                                }
                            }
                            3 -> {
                                // CUT PLANE GIZMO: Dragging the vertical Z handle moves the horizontal cut plane up and down
                                val deltaZ = renderer.projectWorldAxisDelta(dx, dy, 0f, 0f, 1f)
                                val nextZ = renderer.cutPlaneHeightMm + deltaZ
                                renderer.cutPlaneHeightMm = nextZ
                                onUpdateCutPlaneHeight?.invoke(nextZ)
                            }
                        }
                        requestRender()
                    } else {
                        renderer.cameraYawDeg -= dx * 0.38f
                        renderer.cameraPitchDeg = (renderer.cameraPitchDeg + dy * 0.35f).coerceIn(-82f, 85f)
                        requestRender()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScalingOrPanning = false
                if (draggedGizmoAxis != -1) {
                    draggedGizmoAxis = -1
                    renderer.activeDraggedAxis = -1
                    requestRender()
                }
            }
        }
        return true
    }
}

@Composable
fun Orca3DViewport(
    modifier: Modifier = Modifier,
    objects: List<SceneObject>,
    selectedObjectId: String?,
    printer: PrinterProfile,
    filament: FilamentProfile,
    showOverhangHighlight: Boolean,
    overhangThresholdDeg: Int,
    isPreviewMode: Boolean,
    sliceResult: SliceResult?,
    minVisibleLayer: Int,
    maxVisibleLayer: Int,
    activeLayerProgress: Float,
    showTravelMoves: Boolean,
    cameraPresetTrigger: Pair<Int, ViewportCameraPreset>?,
    fitCameraTrigger: Int,
    activeGizmoModeOrdinal: Int = 0,
    showTransformGizmo: Boolean = true,
    cutPlaneHeightMm: Float = 10f,
    onObjectSelected: (String?) -> Unit,
    onTransformSelectedObject: (((SceneObject) -> SceneObject) -> Unit)? = null,
    onUpdateCutPlaneHeight: ((Float) -> Unit)? = null,
    onLayOnFaceSelected: ((Vec3, String) -> Unit)? = null
) {
    val filamentRgb = remember(filament.colorHex) {
        val r = ((filament.colorHex shr 16) and 0xFF) / 255f
        val g = ((filament.colorHex shr 8) and 0xFF) / 255f
        val b = (filament.colorHex and 0xFF) / 255f
        floatArrayOf(r, g, b)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            OrcaGlSurfaceView(ctx, onObjectSelected).apply {
                this.onTransformSelectedObject = onTransformSelectedObject
                this.onUpdateCutPlaneHeight = onUpdateCutPlaneHeight
                this.onLayOnFaceSelected = onLayOnFaceSelected
                this.renderer.onLayOnFacePicked = { normal, label ->
                    post {
                        this.onLayOnFaceSelected?.invoke(normal, label)
                    }
                }
            }
        },
        update = { view ->
            view.onTransformSelectedObject = onTransformSelectedObject
            view.onUpdateCutPlaneHeight = onUpdateCutPlaneHeight
            view.onLayOnFaceSelected = onLayOnFaceSelected
            val r = view.renderer
            r.sceneObjects = objects
            r.selectedObjectId = selectedObjectId
            r.printerProfile = printer
            r.filamentColorRgb = filamentRgb
            r.showOverhangHighlight = showOverhangHighlight
            r.overhangThresholdDeg = overhangThresholdDeg
            r.isPreviewMode = isPreviewMode
            r.sliceResult = sliceResult
            r.minVisibleLayer = minVisibleLayer
            r.maxVisibleLayer = maxVisibleLayer
            r.activeLayerProgress = activeLayerProgress
            r.showTravelMoves = showTravelMoves
            r.activeGizmoModeOrdinal = activeGizmoModeOrdinal
            r.showTransformGizmo = showTransformGizmo
            r.cutPlaneHeightMm = cutPlaneHeightMm

            if (cameraPresetTrigger != null && cameraPresetTrigger.first != view.lastHandledPresetTriggerId) {
                view.lastHandledPresetTriggerId = cameraPresetTrigger.first
                r.applyCameraPreset(cameraPresetTrigger.second)
            }
            if (fitCameraTrigger != view.lastHandledFitTriggerId) {
                view.lastHandledFitTriggerId = fitCameraTrigger
                r.resetAndFitCamera()
            }
            view.requestRender()
        }
    )
}
