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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OrcaGlSurfaceView(
    context: Context,
    onObjectSelected: (String?) -> Unit
) : GLSurfaceView(context) {

    val renderer: OrcaGlRenderer = OrcaGlRenderer(onObjectSelected)
    var lastHandledPresetTriggerId: Int = 0
    var lastHandledFitTriggerId: Int = 0

    private var prevTouchX = 0f
    private var prevTouchY = 0f
    private var prevTwoFingerX = 0f
    private var prevTwoFingerY = 0f
    private var isScalingOrPanning = false

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
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    isScalingOrPanning = true
                    prevTwoFingerX = (event.getX(0) + event.getX(1)) * 0.5f
                    prevTwoFingerY = (event.getY(0) + event.getY(1)) * 0.5f
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    isScalingOrPanning = true
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
                    val dx = event.x - prevTouchX
                    val dy = event.y - prevTouchY
                    prevTouchX = event.x
                    prevTouchY = event.y

                    renderer.cameraYawDeg -= dx * 0.38f
                    renderer.cameraPitchDeg = (renderer.cameraPitchDeg + dy * 0.35f).coerceIn(-82f, 85f)
                    requestRender()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScalingOrPanning = false
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
    onObjectSelected: (String?) -> Unit
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
            OrcaGlSurfaceView(ctx, onObjectSelected)
        },
        update = { view ->
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
