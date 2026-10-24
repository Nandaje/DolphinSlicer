package com.orcaslicer.android.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orcaslicer.android.gpu.ViewportCameraPreset
import com.orcaslicer.android.io.ModelExporter
import com.orcaslicer.android.io.ModelImporter
import com.orcaslicer.android.io.ProfileImporter
import com.orcaslicer.android.model.BuiltInPresets
import com.orcaslicer.android.model.FilamentProfile
import com.orcaslicer.android.model.PrinterProfile
import com.orcaslicer.android.model.ProceduralMeshes
import com.orcaslicer.android.model.ProcessProfile
import com.orcaslicer.android.model.SceneObject
import com.orcaslicer.android.model.SliceResult
import com.orcaslicer.android.model.Vec3
import com.orcaslicer.android.network.NetworkProtocol
import com.orcaslicer.android.network.OrcaServerProfileSync
import com.orcaslicer.android.network.PrinterNetworkClient
import com.orcaslicer.android.network.PrinterTelemetry
import com.orcaslicer.android.slicer.SlicingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * ============================================================================
 * Central Application State & Actions ViewModel (`OrcaViewModel.kt`)
 * ============================================================================
 * Single source of truth ([OrcaUiState]) for DolphinSlicer. Coordinates:
 * - Build plate scene objects (add primitive, import STL/OBJ/3MF, move, scale,
 *   rotate, lay on face, cut at Z, mirror, auto-arrange, clone, delete).
 * - Active Printer, Filament, and Process profiles + live GitHub profile sync.
 * - Background multi-threaded slicing execution ([SlicingEngine.slicePlate])
 *   with live pass/layer progress reporting and cancellation.
 * - Persistent network printer settings (`SharedPreferences("dolphin_slicer_prefs")`)
 *   and direct G-code upload over Wi-Fi.
 */
enum class WorkspaceTab(val title: String) {
    PREPARE("Prepare"),
    PREVIEW("Preview"),
    DEVICE("Device"),
    PROJECT("Project")
}

enum class ProcessCategoryTab(val title: String) {
    QUALITY("Quality"),
    STRENGTH("Strength"),
    SPEED("Speed"),
    SUPPORT("Support"),
    MULTIMATERIAL("Multimaterial"),
    OTHERS("Others")
}

enum class TransformGizmoMode(val title: String) {
    MOVE("Move"),
    SCALE("Scale"),
    ROTATE("Rotate"),
    CUT("Cut"),
    MIRROR("Mirror"),
    MEASURE("Measure"),
    LAY_ON_FACE("Lay on Face")
}

enum class BuiltInPrimitiveType(val title: String) {
    CALIBRATION_CUBE("Calibration Cube (20×20×20mm)"),
    OVERHANG_ARCH("Overhang & Bridge Test Arch"),
    CYLINDER("Cylinder (24×25mm)"),
    SPHERE("Sphere (28mm)"),
    CONE("Cone (30×28mm)"),
    TORUS("Torus Ring (32mm)")
}

enum class CalibrationTestType(val title: String, val description: String) {
    TEMP_TOWER_PLA("PLA Temperature Tower (190–230°C)", "5-tier overhang & bridge tower to find optimal layer bonding & bridging temp"),
    TEMP_TOWER_PETG("PETG Temperature Tower (225–255°C)", "5-tier PETG overhang & stringing evaluation tower"),
    FLOW_RATE_PASS1("Flow Rate Calibration (9-Tile Pass)", "9 calibration patches to dial in top surface smoothness and extrusion multiplier"),
    PRESSURE_ADVANCE("Pressure Advance (PA) Hollow Tower", "Sharp 90° thin-wall corner tower to tune Klipper PA / Marlin Linear Advance"),
    RETRACTION_TOWERS("Retraction & Stringing Twin Spires", "Two spaced cylindrical spires to test retraction length, speed, and Z-hop"),
    MAX_VOLUMETRIC_SPEED("Max Volumetric Speed Cylinder", "High-flow continuous perimeter cylinder to find hotend melting limit (mm³/s)")
}

data class OrcaUiState(
    val activeWorkspaceTab: WorkspaceTab = WorkspaceTab.PREPARE,
    val activeProcessTab: ProcessCategoryTab = ProcessCategoryTab.QUALITY,
    val activeGizmoMode: TransformGizmoMode = TransformGizmoMode.MOVE,

    // Maximized 3D Viewport control: collapsible dock and floating gizmo drawer
    val isDockExpanded: Boolean = false,
    val showFloatingGizmoSheet: Boolean = false,
    val showPrinterSettingsDialog: Boolean = false,
    val showFilamentSettingsDialog: Boolean = false,
    val showAdvancedSettings: Boolean = true,
    val isGlobalProcessMode: Boolean = true,
    val showProcessSearchBar: Boolean = false,
    val settingsSearchQuery: String = "",
    val isSyncingServerProfiles: Boolean = false,

    val printers: List<PrinterProfile> = BuiltInPresets.printers,
    val selectedPrinter: PrinterProfile = BuiltInPresets.printers.first(),

    val filaments: List<FilamentProfile> = BuiltInPresets.filaments,
    val selectedFilament: FilamentProfile = BuiltInPresets.filaments.first(),
    val filamentSlots: List<FilamentProfile> = listOf(BuiltInPresets.filaments.first()),
    val activeFilamentSlotIndex: Int = 0,

    val processPresets: List<ProcessProfile> = BuiltInPresets.processes,
    val currentProcess: ProcessProfile = BuiltInPresets.processes.first(),

    val sceneObjects: List<SceneObject> = emptyList(),
    val selectedObjectId: String? = null,
    val cutPlaneHeightMm: Float = 10.0f,
    val cutKeepUpper: Boolean = true,
    val cutKeepLower: Boolean = true,
    val cutPlaceOnCutFace: Boolean = true,

    val showOverhangHighlight: Boolean = true,
    val showTravelMoves: Boolean = false,

    val isSlicing: Boolean = false,
    val sliceProgress: Float = 0f,
    val sliceResult: SliceResult? = null,
    val minVisibleLayer: Int = 0,
    val maxVisibleLayer: Int = 0,
    val activeLayerProgress: Float = 1.0f,

    val cameraPresetTrigger: Pair<Int, ViewportCameraPreset>? = null,
    val fitCameraTrigger: Int = 0,

    // Network printer (Device tab)
    val printerHost: String = "192.168.1.100",
    val printerPort: String = "7125",
    val printerApiKey: String = "",
    val printerProtocol: NetworkProtocol = NetworkProtocol.MOONRAKER_KLIPPER,
    val printerWebUrl: String = "",
    val printerTelemetry: PrinterTelemetry? = null,
    val isNetworkBusy: Boolean = false,

    val statusBannerMessage: String = "Ready — OpenGL ES 3.0 GPU Viewport"
) {
    val selectedObject: SceneObject?
        get() = sceneObjects.firstOrNull { it.id == selectedObjectId }
}

class OrcaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrcaUiState())
    val uiState: StateFlow<OrcaUiState> = _uiState.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null
    private var triggerCounter = 1

    fun initPreferences(context: android.content.Context) {
        if (prefs != null) return
        val sp = context.getSharedPreferences("dolphin_slicer_prefs", android.content.Context.MODE_PRIVATE)
        prefs = sp
        val savedHost = sp.getString("printer_host", null)?.takeIf { it.isNotBlank() } ?: "192.168.1.100"
        val savedPort = sp.getString("printer_port", null)?.takeIf { it.isNotBlank() } ?: "7125"
        val savedApiKey = sp.getString("printer_api_key", null) ?: ""
        val savedProtoName = sp.getString("printer_protocol", null)
        val savedProto = NetworkProtocol.entries.firstOrNull { it.name == savedProtoName }
            ?: NetworkProtocol.MOONRAKER_KLIPPER
        val savedWebUrl = sp.getString("printer_web_url", null) ?: ""

        _uiState.update {
            it.copy(
                printerHost = savedHost,
                printerPort = savedPort,
                printerApiKey = savedApiKey,
                printerProtocol = savedProto,
                printerWebUrl = savedWebUrl
            )
        }
    }

    fun selectWorkspaceTab(tab: WorkspaceTab) {
        _uiState.update { it.copy(activeWorkspaceTab = tab) }
    }

    fun selectProcessTab(tab: ProcessCategoryTab) {
        _uiState.update { it.copy(activeProcessTab = tab) }
    }

    fun selectGizmoMode(mode: TransformGizmoMode) {
        _uiState.update { state ->
            val autoSelectedId = state.selectedObjectId ?: state.sceneObjects.firstOrNull()?.id
            val selObj = state.sceneObjects.firstOrNull { it.id == autoSelectedId }
            val toggleVisible = if (state.activeGizmoMode == mode && state.showFloatingGizmoSheet) {
                false
            } else {
                true
            }
            val nextCutZ = if (mode == TransformGizmoMode.CUT && selObj != null) {
                val worldH = selObj.worldBoundingBox().size.z.coerceAtLeast(2f)
                if (state.cutPlaneHeightMm <= 0.5f || state.cutPlaneHeightMm >= worldH - 0.5f) {
                    (worldH * 0.5f).coerceIn(0.5f, worldH - 0.5f)
                } else {
                    state.cutPlaneHeightMm
                }
            } else {
                state.cutPlaneHeightMm
            }
            state.copy(
                selectedObjectId = autoSelectedId,
                activeGizmoMode = mode,
                showFloatingGizmoSheet = toggleVisible,
                cutPlaneHeightMm = nextCutZ
            )
        }
    }

    fun toggleDockExpanded() {
        _uiState.update { it.copy(isDockExpanded = !it.isDockExpanded) }
    }

    fun setDockExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isDockExpanded = expanded) }
    }

    fun setFloatingGizmoSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showFloatingGizmoSheet = visible) }
    }

    fun setPrinterSettingsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showPrinterSettingsDialog = visible) }
    }

    fun setFilamentSettingsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showFilamentSettingsDialog = visible) }
    }

    fun toggleAdvancedSettings() {
        _uiState.update { it.copy(showAdvancedSettings = !it.showAdvancedSettings) }
    }

    fun setGlobalProcessMode(isGlobal: Boolean) {
        _uiState.update { it.copy(isGlobalProcessMode = isGlobal) }
    }

    fun toggleProcessSearchBar() {
        _uiState.update {
            val next = !it.showProcessSearchBar
            it.copy(
                showProcessSearchBar = next,
                settingsSearchQuery = if (!next) "" else it.settingsSearchQuery
            )
        }
    }

    fun addFilamentSlot() {
        _uiState.update { state ->
            if (state.filamentSlots.size >= 8) return@update state
            val nextFil = state.filaments.getOrElse(state.filamentSlots.size % state.filaments.size) { state.selectedFilament }
            val updated = state.filamentSlots + nextFil
            state.copy(
                filamentSlots = updated,
                activeFilamentSlotIndex = updated.lastIndex,
                selectedFilament = nextFil,
                statusBannerMessage = "Added Filament Slot #${updated.size} (${nextFil.name})"
            )
        }
    }

    fun selectFilamentSlot(index: Int) {
        _uiState.update { state ->
            val safeIdx = index.coerceIn(0, (state.filamentSlots.size - 1).coerceAtLeast(0))
            val fil = state.filamentSlots.getOrElse(safeIdx) { state.selectedFilament }
            state.copy(
                activeFilamentSlotIndex = safeIdx,
                selectedFilament = fil
            )
        }
    }

    fun syncProfilesFromOrcaServer() {
        if (_uiState.value.isSyncingServerProfiles) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncingServerProfiles = true,
                    statusBannerMessage = "Syncing all printer & filament profiles from OrcaSlicer GitHub server..."
                )
            }
            val res = OrcaServerProfileSync.syncAllVendorsFromServer { progressMsg ->
                _uiState.update { it.copy(statusBannerMessage = progressMsg) }
            }
            res.fold(
                onSuccess = { summary ->
                    _uiState.update { state ->
                        val existingPrinterIds = state.printers.map { it.name.lowercase() }.toHashSet()
                        val newPrinters = summary.printers.filter { existingPrinterIds.add(it.name.lowercase()) }
                        val mergedPrinters = state.printers + newPrinters

                        val existingFilamentIds = state.filaments.map { it.name.lowercase() }.toHashSet()
                        val newFilaments = summary.filaments.filter { existingFilamentIds.add(it.name.lowercase()) }
                        val mergedFilaments = state.filaments + newFilaments

                        state.copy(
                            isSyncingServerProfiles = false,
                            printers = mergedPrinters,
                            filaments = mergedFilaments,
                            statusBannerMessage = "Server Sync Complete: ${mergedPrinters.size} Printers & ${mergedFilaments.size} Filaments available (${summary.vendorsSynced} vendors)"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSyncingServerProfiles = false,
                            statusBannerMessage = err.message ?: "Server profile sync failed"
                        )
                    }
                }
            )
        }
    }

    fun updateSettingsSearchQuery(query: String) {
        _uiState.update { it.copy(settingsSearchQuery = query) }
    }

    fun updateCutPlaneHeight(heightMm: Float) {
        _uiState.update { state ->
            val maxH = state.selectedObject?.worldBoundingBox()?.size?.z?.coerceAtLeast(2f) ?: 250f
            state.copy(cutPlaneHeightMm = heightMm.coerceIn(0.5f, (maxH - 0.5f).coerceAtLeast(1f)))
        }
    }

    fun updateCutOptions(
        keepUpper: Boolean = _uiState.value.cutKeepUpper,
        keepLower: Boolean = _uiState.value.cutKeepLower,
        placeOnCutFace: Boolean = _uiState.value.cutPlaceOnCutFace
    ) {
        _uiState.update {
            // Ensure at least one part is kept
            val validUpper = if (!keepUpper && !keepLower) true else keepUpper
            it.copy(
                cutKeepUpper = validUpper,
                cutKeepLower = keepLower,
                cutPlaceOnCutFace = placeOnCutFace
            )
        }
    }

    fun selectPrinter(printer: PrinterProfile) {
        _uiState.update {
            it.copy(
                selectedPrinter = printer,
                statusBannerMessage = "Printer: ${printer.name} (${printer.bedWidthX.toInt()}×${printer.bedDepthY.toInt()}×${printer.maxPrintHeightZ.toInt()} mm)"
            )
        }
    }

    fun updatePrinter(transform: (PrinterProfile) -> PrinterProfile) {
        _uiState.update { it.copy(selectedPrinter = transform(it.selectedPrinter)) }
    }

    fun selectFilament(filament: FilamentProfile) {
        _uiState.update { state ->
            val updatedSlots = state.filamentSlots.toMutableList().apply {
                if (state.activeFilamentSlotIndex in indices) {
                    this[state.activeFilamentSlotIndex] = filament
                }
            }
            state.copy(
                selectedFilament = filament,
                filamentSlots = updatedSlots,
                statusBannerMessage = "Filament: ${filament.name} (${filament.nozzleTempOtherC}°C / ${filament.bedTempOtherC}°C)"
            )
        }
    }

    fun selectProcessPreset(preset: ProcessProfile) {
        _uiState.update {
            it.copy(
                currentProcess = preset,
                statusBannerMessage = "Process preset: ${preset.name}"
            )
        }
    }

    fun updateProcess(transform: (ProcessProfile) -> ProcessProfile) {
        _uiState.update { it.copy(currentProcess = transform(it.currentProcess)) }
    }

    fun updateFilament(transform: (FilamentProfile) -> FilamentProfile) {
        _uiState.update { state ->
            val updated = transform(state.selectedFilament)
            val updatedSlots = state.filamentSlots.toMutableList().apply {
                if (state.activeFilamentSlotIndex in indices) {
                    this[state.activeFilamentSlotIndex] = updated
                }
            }
            val updatedCatalog = state.filaments.map { if (it.id == updated.id) updated else it }
            state.copy(
                selectedFilament = updated,
                filamentSlots = updatedSlots,
                filaments = updatedCatalog
            )
        }
    }

    fun setGlobalCurrencySymbol(symbol: String) {
        val clean = symbol.trim().ifEmpty { "$" }
        _uiState.update { state ->
            val updatedSelected = state.selectedFilament.copy(currencySymbol = clean)
            val updatedSlots = state.filamentSlots.map { it.copy(currencySymbol = clean) }
            val updatedCatalog = state.filaments.map { it.copy(currencySymbol = clean) }
            state.copy(
                selectedFilament = updatedSelected,
                filamentSlots = updatedSlots,
                filaments = updatedCatalog,
                statusBannerMessage = "Currency updated to '$clean' across all filament profiles"
            )
        }
    }

    fun selectObject(objectId: String?) {
        _uiState.update { state ->
            val resolved = objectId ?: state.selectedObjectId
            state.copy(selectedObjectId = resolved)
        }
    }

    fun toggleOverhangHighlight() {
        _uiState.update { it.copy(showOverhangHighlight = !it.showOverhangHighlight) }
    }

    fun toggleTravelMoves() {
        _uiState.update { it.copy(showTravelMoves = !it.showTravelMoves) }
    }

    fun setCameraPreset(preset: ViewportCameraPreset) {
        triggerCounter++
        _uiState.update {
            it.copy(
                cameraPresetTrigger = triggerCounter to preset,
                statusBannerMessage = "Camera: ${preset.name.lowercase().replaceFirstChar { c -> c.uppercase() }} view"
            )
        }
    }

    fun resetCameraView() {
        triggerCounter++
        _uiState.update {
            it.copy(
                fitCameraTrigger = triggerCounter,
                statusBannerMessage = "Camera fit to build plate"
            )
        }
    }

    fun addBuiltInPrimitive(type: BuiltInPrimitiveType) {
        val mesh = when (type) {
            BuiltInPrimitiveType.CALIBRATION_CUBE -> ProceduralMeshes.createCalibrationCube(20f)
            BuiltInPrimitiveType.OVERHANG_ARCH -> ProceduralMeshes.createOverhangTestArch()
            BuiltInPrimitiveType.CYLINDER -> ProceduralMeshes.createCylinder(12f, 25f)
            BuiltInPrimitiveType.SPHERE -> ProceduralMeshes.createSphere(14f)
            BuiltInPrimitiveType.CONE -> ProceduralMeshes.createCone(15f, 28f)
            BuiltInPrimitiveType.TORUS -> ProceduralMeshes.createTorus(16f, 5f)
        }
        val count = _uiState.value.sceneObjects.size
        val offset = if (count == 0) Vec3(0f, 0f, 0f) else Vec3((count % 3) * 34f - 17f, (count / 3) * 34f, 0f)
        val newObj = SceneObject(
            id = UUID.randomUUID().toString(),
            name = type.title.substringBefore(" ("),
            sourceFormat = "PRIMITIVE",
            mesh = mesh,
            position = offset
        )
        _uiState.update { state ->
            state.copy(
                sceneObjects = state.sceneObjects + newObj,
                selectedObjectId = newObj.id,
                activeWorkspaceTab = WorkspaceTab.PREPARE,
                statusBannerMessage = "Added '${newObj.name}' (${mesh.triangleCount} triangles)"
            )
        }
    }

    fun launchCalibrationTest(test: CalibrationTestType) {
        val mesh = when (test) {
            CalibrationTestType.TEMP_TOWER_PLA,
            CalibrationTestType.TEMP_TOWER_PETG -> ProceduralMeshes.createTemperatureTower(5)
            CalibrationTestType.FLOW_RATE_PASS1 -> ProceduralMeshes.createFlowRateCalibrationSet()
            CalibrationTestType.PRESSURE_ADVANCE -> ProceduralMeshes.createPressureAdvanceTower()
            CalibrationTestType.RETRACTION_TOWERS -> ProceduralMeshes.createRetractionTowers()
            CalibrationTestType.MAX_VOLUMETRIC_SPEED -> ProceduralMeshes.createMaxVolumetricSpeedTest()
        }
        val newObj = SceneObject(
            id = UUID.randomUUID().toString(),
            name = test.title.substringBefore(" ("),
            sourceFormat = "CALIBRATION",
            mesh = mesh,
            position = Vec3(0f, 0f, 0f)
        )
        _uiState.update { state ->
            val updatedProcess = when (test) {
                CalibrationTestType.PRESSURE_ADVANCE -> state.currentProcess.copy(
                    wallLoops = 1,
                    sparseInfillDensityPercent = 0,
                    topShellLayers = 0,
                    bottomShellLayers = 2,
                    outerWallSpeedMmS = 120f
                )
                CalibrationTestType.MAX_VOLUMETRIC_SPEED -> state.currentProcess.copy(
                    spiralVaseMode = true,
                    wallLoops = 1,
                    sparseInfillDensityPercent = 0,
                    outerWallSpeedMmS = 260f
                )
                else -> state.currentProcess
            }
            state.copy(
                sceneObjects = listOf(newObj),
                selectedObjectId = newObj.id,
                currentProcess = updatedProcess,
                activeWorkspaceTab = WorkspaceTab.PREPARE,
                statusBannerMessage = "Generated Orca Calibration: ${test.title}"
            )
        }
    }

    fun updateSelectedObject(transform: (SceneObject) -> SceneObject) {
        _uiState.update { state ->
            val selId = state.selectedObjectId ?: return@update state
            val updatedList = state.sceneObjects.map { obj ->
                if (obj.id == selId) transform(obj).snappedToBed() else obj
            }
            state.copy(sceneObjects = updatedList)
        }
    }

    fun centerSelectedObjectOnBed() {
        updateSelectedObject { obj ->
            obj.copy(position = Vec3(0f, 0f, 0f))
        }
        _uiState.update { it.copy(statusBannerMessage = "Centered object on build plate (0, 0)") }
    }

    fun laySelectedObjectFlat() {
        updateSelectedObject { obj ->
            fun snap90(deg: Float): Float = (Math.round(deg / 90f) * 90f) % 360f
            obj.copy(
                rotationDeg = Vec3(
                    snap90(obj.rotationDeg.x),
                    snap90(obj.rotationDeg.y),
                    snap90(obj.rotationDeg.z)
                ),
                position = obj.position.copy(z = 0f)
            )
        }
        _uiState.update { it.copy(statusBannerMessage = "Snapped object rotation to nearest 90° flat on build plate (Z = 0)") }
    }

    /**
     * OrcaSlicer "Lay on Face" (Place on Face): rotates the selected object so the given local outward normal
     * `localNormal` points straight down at the build plate `(0, 0, -1)` and snaps the face flush to `Z = 0`.
     */
    fun laySelectedObjectOnLocalNormal(localNormal: Vec3, faceLabel: String = "") {
        val sel = _uiState.value.selectedObject ?: return
        val newRot = sel.rotationToLayNormalFlat(localNormal)
        updateSelectedObject { obj ->
            obj.copy(
                rotationDeg = newRot,
                position = obj.position.copy(z = 0f)
            )
        }
        val labelSuffix = if (faceLabel.isNotBlank()) " ($faceLabel)" else ""
        _uiState.update {
            it.copy(
                statusBannerMessage = "Placed '${sel.name}' on selected face$labelSuffix → Rx=${newRot.x.toInt()}°, Ry=${newRot.y.toInt()}°, Rz=${newRot.z.toInt()}°"
            )
        }
    }

    /**
     * Auto-orients the selected object onto its largest flat exterior face (OrcaSlicer Auto-Orient).
     */
    fun autoOrientSelectedObjectLargestFace() {
        val sel = _uiState.value.selectedObject ?: return
        val largest = sel.mesh.layOnFaceCandidates.firstOrNull()
        if (largest != null) {
            laySelectedObjectOnLocalNormal(largest.localNormal, "${largest.label} • ${largest.areaMm2.toInt()} mm²")
        } else {
            laySelectedObjectFlat()
        }
    }

    fun mirrorSelectedObject(mirrorX: Boolean, mirrorY: Boolean, mirrorZ: Boolean) {
        val sel = _uiState.value.selectedObject ?: return
        val mirroredMesh = ProceduralMeshes.mirrorMesh(sel.mesh, mirrorX, mirrorY, mirrorZ)
        updateSelectedObject { it.copy(mesh = mirroredMesh) }
        val axisName = when {
            mirrorX -> "X"
            mirrorY -> "Y"
            else -> "Z"
        }
        _uiState.update { it.copy(statusBannerMessage = "Mirrored '${sel.name}' along $axisName-axis") }
    }

    fun cutSelectedObjectAtZ(cutHeightZ: Float) {
        val state = _uiState.value
        val sel = state.selectedObject ?: return
        val worldTris = sel.worldTriangles()
        val wb = sel.worldBoundingBox()
        val maxH = wb.size.z.coerceAtLeast(1.5f)
        val clampedCut = cutHeightZ.coerceIn(0.5f, (maxH - 0.5f).coerceAtLeast(0.6f))
        val (lowerMesh, upperMesh) = ProceduralMeshes.cutTrianglesAtWorldZ(worldTris, clampedCut)
        if (lowerMesh.triangleCount == 0 && upperMesh.triangleCount == 0) {
            _uiState.update { it.copy(statusBannerMessage = "Cut plane ($clampedCut mm) is outside object height.") }
            return
        }

        val createdParts = ArrayList<SceneObject>(2)
        val keepLower = state.cutKeepLower && lowerMesh.triangleCount > 0
        val keepUpper = state.cutKeepUpper && upperMesh.triangleCount > 0
        val bothKept = keepLower && keepUpper
        val spreadX = if (bothKept) (wb.size.x * 0.6f).coerceIn(14f, 65f) else 0f

        if (keepLower) {
            // If Place on Cut Face is enabled, flip the lower part 180° around X so its newly cut top face rests flat on the bed
            val lowerRot = if (state.cutPlaceOnCutFace) Vec3(180f, 0f, 0f) else Vec3(0f, 0f, 0f)
            val lowerObj = sel.copy(
                id = UUID.randomUUID().toString(),
                name = if (bothKept) "${sel.name}_lower" else "${sel.name}_cut",
                mesh = lowerMesh,
                rotationDeg = lowerRot,
                scale = Vec3(1f, 1f, 1f),
                position = Vec3(sel.position.x - spreadX, sel.position.y, 0f)
            ).snappedToBed()
            createdParts.add(lowerObj)
        }

        if (keepUpper) {
            // Upper part's cut face is already at its bottom (-Z), so rotation (0,0,0) places its cut face directly on the bed
            val upperObj = sel.copy(
                id = UUID.randomUUID().toString(),
                name = if (bothKept) "${sel.name}_upper" else "${sel.name}_cut",
                mesh = upperMesh,
                rotationDeg = Vec3(0f, 0f, 0f),
                scale = Vec3(1f, 1f, 1f),
                position = Vec3(sel.position.x + spreadX, sel.position.y, 0f)
            ).snappedToBed()
            createdParts.add(upperObj)
        }

        if (createdParts.isEmpty()) {
            _uiState.update { it.copy(statusBannerMessage = "Cut produced no geometry at Z = ${String.format("%.1f", clampedCut)} mm.") }
            return
        }

        val newSelected = createdParts.last()
        val nextCutZ = (newSelected.worldBoundingBox().size.z * 0.5f).coerceAtLeast(1.0f)

        _uiState.update { s ->
            val remaining = s.sceneObjects.filterNot { it.id == sel.id } + createdParts
            s.copy(
                sceneObjects = remaining,
                selectedObjectId = newSelected.id,
                cutPlaneHeightMm = nextCutZ,
                statusBannerMessage = "Cut '${sel.name}' at Z=${String.format("%.1f", clampedCut)}mm (${createdParts.size} part${if (createdParts.size > 1) "s" else ""} on plate)"
            )
        }
    }

    fun duplicateSelectedObject(copies: Int = 1) {
        val state = _uiState.value
        val sel = state.selectedObject ?: return
        val newCopies = (1..copies.coerceIn(1, 15)).map { idx ->
            sel.copy(
                id = UUID.randomUUID().toString(),
                name = "${sel.name}_${idx + 1}",
                position = Vec3(sel.position.x + idx * 28f, sel.position.y + (idx % 2) * 16f, 0f)
            )
        }
        _uiState.update {
            it.copy(
                sceneObjects = it.sceneObjects + newCopies,
                selectedObjectId = newCopies.last().id,
                statusBannerMessage = "Cloned $copies copy/copies of '${sel.name}'"
            )
        }
        if (copies > 1) {
            autoArrangeObjects()
        }
    }

    fun deleteSelectedObject() {
        _uiState.update { state ->
            val selId = state.selectedObjectId ?: return@update state
            val remaining = state.sceneObjects.filterNot { it.id == selId }
            state.copy(
                sceneObjects = remaining,
                selectedObjectId = remaining.lastOrNull()?.id,
                statusBannerMessage = "Deleted selected object"
            )
        }
    }

    fun clearBuildPlate() {
        _uiState.update {
            it.copy(
                sceneObjects = emptyList(),
                selectedObjectId = null,
                sliceResult = null,
                activeWorkspaceTab = WorkspaceTab.PREPARE,
                statusBannerMessage = "Cleared build plate"
            )
        }
    }

    fun autoArrangeObjects() {
        _uiState.update { state ->
            val objs = state.sceneObjects
            if (objs.isEmpty()) return@update state
            val cols = ceil(sqrt(objs.size.toDouble())).toInt().coerceAtLeast(1)
            val rows = ceil(objs.size.toDouble() / cols).toInt().coerceAtLeast(1)
            val cellStep = 48f

            val arranged = objs.mapIndexed { idx, obj ->
                val c = idx % cols
                val r = idx / cols
                val x = (c - (cols - 1) * 0.5f) * cellStep
                val y = (r - (rows - 1) * 0.5f) * cellStep
                obj.copy(position = Vec3(x, y, 0f)).snappedToBed()
            }
            state.copy(
                sceneObjects = arranged,
                statusBannerMessage = "Auto-arranged ${arranged.size} object(s) on build plate"
            )
        }
    }

    fun updateLayerRange(minLayer: Int, maxLayer: Int) {
        _uiState.update { state ->
            val maxIdx = (state.sliceResult?.layers?.size ?: 1) - 1
            val safeMin = minLayer.coerceIn(0, maxIdx)
            val safeMax = maxLayer.coerceIn(safeMin, maxIdx)
            state.copy(minVisibleLayer = safeMin, maxVisibleLayer = safeMax)
        }
    }

    fun updateActiveLayerProgress(progress: Float) {
        _uiState.update { it.copy(activeLayerProgress = progress.coerceIn(0.01f, 1.0f)) }
    }

    private var slicingJob: kotlinx.coroutines.Job? = null

    fun cancelSlicing() {
        slicingJob?.cancel()
        slicingJob = null
        _uiState.update {
            it.copy(
                isSlicing = false,
                sliceProgress = 0f,
                statusBannerMessage = "Slicing cancelled"
            )
        }
    }

    fun sliceBuildPlate() {
        val snapshot = _uiState.value
        if (snapshot.isSlicing) return

        slicingJob?.cancel()
        slicingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSlicing = true,
                    sliceProgress = 0.02f,
                    statusBannerMessage = "Slicing plate..."
                )
            }

            try {
                val result = SlicingEngine.sliceScene(
                    objects = snapshot.sceneObjects,
                    printer = snapshot.selectedPrinter,
                    filament = snapshot.selectedFilament,
                    process = snapshot.currentProcess,
                    onProgress = { progress, msg ->
                        _uiState.update {
                            it.copy(sliceProgress = progress, statusBannerMessage = msg)
                        }
                    }
                )

                result.fold(
                    onSuccess = { sliceRes ->
                        val topIdx = (sliceRes.layers.size - 1).coerceAtLeast(0)
                        _uiState.update {
                            it.copy(
                                isSlicing = false,
                                sliceProgress = 1.0f,
                                sliceResult = sliceRes,
                                minVisibleLayer = 0,
                                maxVisibleLayer = topIdx,
                                activeLayerProgress = 1.0f,
                                activeWorkspaceTab = WorkspaceTab.PREVIEW,
                                statusBannerMessage = "Sliced ${sliceRes.statistics.layerCount} layers • Est. ${sliceRes.statistics.formattedTotalTime} • ${String.format("%.1f", sliceRes.statistics.filamentWeightGrams)}g"
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isSlicing = false,
                                sliceProgress = 0f,
                                statusBannerMessage = err.message ?: "Failed to slice plate"
                            )
                        }
                    }
                )
            } catch (ce: kotlinx.coroutines.CancellationException) {
                _uiState.update {
                    it.copy(
                        isSlicing = false,
                        sliceProgress = 0f,
                        statusBannerMessage = "Slicing cancelled"
                    )
                }
                throw ce
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isSlicing = false,
                        sliceProgress = 0f,
                        statusBannerMessage = "Slicing error: ${t.message ?: t.javaClass.simpleName}"
                    )
                }
            } finally {
                _uiState.update { state ->
                    if (state.isSlicing) state.copy(isSlicing = false) else state
                }
            }
        }
    }

    fun importFromUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val fileName = resolveDisplayName(contentResolver, uri) ?: (uri.lastPathSegment ?: "model.stl")
            val lowerName = fileName.lowercase()
            if (lowerName.endsWith(".json") ||
                lowerName.endsWith(".orca_printer") ||
                lowerName.endsWith(".orca_filament") ||
                lowerName.endsWith(".ini")
            ) {
                importProfileFromUri(contentResolver, uri, fileName)
                return@launch
            }

            _uiState.update { it.copy(statusBannerMessage = "Importing '$fileName'...") }

            val importRes = withContext(Dispatchers.IO) {
                val stream = contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IllegalArgumentException("Cannot open input stream for '$fileName'."))
                stream.use { ModelImporter.importFromStream(it, fileName) }
            }

            importRes.fold(
                onSuccess = { content ->
                    when (content) {
                        is ModelImporter.ImportedContent.Models -> {
                            val totalTris = content.objects.sumOf { it.mesh.triangleCount }
                            _uiState.update { state ->
                                val combined = state.sceneObjects + content.objects
                                state.copy(
                                    sceneObjects = combined,
                                    selectedObjectId = content.objects.lastOrNull()?.id ?: state.selectedObjectId,
                                    activeWorkspaceTab = WorkspaceTab.PREPARE,
                                    statusBannerMessage = "Imported ${content.objects.size} model(s) from '$fileName' ($totalTris triangles)"
                                )
                            }
                        }
                        is ModelImporter.ImportedContent.GCodePreview -> {
                            val slice = content.sliceResult
                            val topIdx = (slice.layers.size - 1).coerceAtLeast(0)
                            _uiState.update { state ->
                                state.copy(
                                    sliceResult = slice,
                                    minVisibleLayer = 0,
                                    maxVisibleLayer = topIdx,
                                    activeLayerProgress = 1.0f,
                                    activeWorkspaceTab = WorkspaceTab.PREVIEW,
                                    statusBannerMessage = "Loaded G-code '$fileName' (${slice.statistics.layerCount} layers)"
                                )
                            }
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(statusBannerMessage = err.message ?: "Import failed")
                    }
                }
            )
        }
    }

    private suspend fun importProfileFromUri(contentResolver: ContentResolver, uri: Uri, fileName: String) {
        val res = withContext(Dispatchers.IO) {
            val stream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot open profile '$fileName'."))
            stream.use { ProfileImporter.importProfileFromStream(it, fileName) }
        }
        res.fold(
            onSuccess = { imported ->
                when (imported) {
                    is ProfileImporter.ImportedProfile.Printer -> {
                        _uiState.update { state ->
                            state.copy(
                                printers = listOf(imported.profile) + state.printers,
                                selectedPrinter = imported.profile,
                                statusBannerMessage = "Imported Printer Profile: '${imported.profile.name}'"
                            )
                        }
                    }
                    is ProfileImporter.ImportedProfile.Filament -> {
                        _uiState.update { state ->
                            state.copy(
                                filaments = listOf(imported.profile) + state.filaments,
                                selectedFilament = imported.profile,
                                statusBannerMessage = "Imported Filament Profile: '${imported.profile.name}'"
                            )
                        }
                    }
                    is ProfileImporter.ImportedProfile.Process -> {
                        _uiState.update { state ->
                            state.copy(
                                processPresets = listOf(imported.profile) + state.processPresets,
                                currentProcess = imported.profile,
                                statusBannerMessage = "Imported Process Profile: '${imported.profile.name}'"
                            )
                        }
                    }
                }
            },
            onFailure = { err ->
                _uiState.update { it.copy(statusBannerMessage = err.message ?: "Profile import failed") }
            }
        )
    }

    fun exportToUri(contentResolver: ContentResolver, uri: Uri, format: ExportFormat) {
        val snapshot = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(statusBannerMessage = "Exporting ${format.extension.uppercase()}...") }

            var activeSlice = snapshot.sliceResult
            if (format == ExportFormat.GCODE && activeSlice == null) {
                val autoSlice = SlicingEngine.sliceScene(
                    objects = snapshot.sceneObjects,
                    printer = snapshot.selectedPrinter,
                    filament = snapshot.selectedFilament,
                    process = snapshot.currentProcess
                )
                if (autoSlice.isFailure) {
                    _uiState.update {
                        it.copy(statusBannerMessage = autoSlice.exceptionOrNull()?.message ?: "Cannot slice for export")
                    }
                    return@launch
                }
                activeSlice = autoSlice.getOrNull()
                _uiState.update { it.copy(sliceResult = activeSlice) }
            }

            val exportRes = withContext(Dispatchers.IO) {
                val outStream = contentResolver.openOutputStream(uri)
                    ?: return@withContext Result.failure(IllegalStateException("Unable to open output stream for export."))
                outStream.use { stream ->
                    when (format) {
                        ExportFormat.GCODE -> {
                            val gcode = activeSlice?.gcodeText ?: ""
                            ModelExporter.exportGCode(stream, gcode)
                        }
                        ExportFormat.PROJECT_3MF -> {
                            ModelExporter.export3mfProject(
                                outputStream = stream,
                                objects = snapshot.sceneObjects,
                                printer = snapshot.selectedPrinter,
                                filament = snapshot.selectedFilament,
                                process = snapshot.currentProcess
                            )
                        }
                        ExportFormat.BINARY_STL -> {
                            ModelExporter.exportBinaryStl(
                                outputStream = stream,
                                objects = snapshot.sceneObjects
                            )
                        }
                    }
                }
            }

            exportRes.fold(
                onSuccess = {
                    val fileName = resolveDisplayName(contentResolver, uri) ?: "exported.${format.extension}"
                    _uiState.update {
                        it.copy(statusBannerMessage = "Exported '$fileName' successfully")
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(statusBannerMessage = err.message ?: "Export failed")
                    }
                }
            )
        }
    }

    fun updatePrinterNetworkConfig(
        host: String = _uiState.value.printerHost,
        port: String = _uiState.value.printerPort,
        apiKey: String = _uiState.value.printerApiKey,
        protocol: NetworkProtocol = _uiState.value.printerProtocol,
        webUrl: String = _uiState.value.printerWebUrl
    ) {
        _uiState.update {
            it.copy(
                printerHost = host,
                printerPort = port,
                printerApiKey = apiKey,
                printerProtocol = protocol,
                printerWebUrl = webUrl
            )
        }
        prefs?.edit()?.apply {
            putString("printer_host", host)
            putString("printer_port", port)
            putString("printer_api_key", apiKey)
            putString("printer_protocol", protocol.name)
            putString("printer_web_url", webUrl)
            apply()
        }
    }

    fun testPrinterConnection() {
        val state = _uiState.value
        val portNum = state.printerPort.toIntOrNull() ?: state.printerProtocol.defaultPort
        viewModelScope.launch {
            _uiState.update { it.copy(isNetworkBusy = true, statusBannerMessage = "Connecting to ${state.printerHost}:$portNum...") }
            val res = PrinterNetworkClient.queryPrinterStatus(
                hostOrIp = state.printerHost,
                port = portNum,
                apiKey = state.printerApiKey,
                protocol = state.printerProtocol
            )
            res.fold(
                onSuccess = { telemetry ->
                    _uiState.update {
                        it.copy(
                            isNetworkBusy = false,
                            printerTelemetry = telemetry,
                            statusBannerMessage = telemetry.statusMessage
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isNetworkBusy = false,
                            statusBannerMessage = err.message ?: "Printer unreachable"
                        )
                    }
                }
            )
        }
    }

    fun sendGCodeToNetworkPrinter(startImmediately: Boolean) {
        val state = _uiState.value
        val portNum = state.printerPort.toIntOrNull() ?: state.printerProtocol.defaultPort
        viewModelScope.launch {
            var activeSlice = state.sliceResult
            if (activeSlice == null) {
                _uiState.update { it.copy(isNetworkBusy = true, statusBannerMessage = "Slicing model before upload...") }
                val sliceRes = SlicingEngine.sliceScene(
                    objects = state.sceneObjects,
                    printer = state.selectedPrinter,
                    filament = state.selectedFilament,
                    process = state.currentProcess
                )
                if (sliceRes.isFailure) {
                    _uiState.update {
                        it.copy(
                            isNetworkBusy = false,
                            statusBannerMessage = sliceRes.exceptionOrNull()?.message ?: "Slicing failed"
                        )
                    }
                    return@launch
                }
                activeSlice = sliceRes.getOrNull()
                _uiState.update { it.copy(sliceResult = activeSlice) }
            }

            val gcode = activeSlice?.gcodeText ?: return@launch
            val jobName = (state.selectedObject?.name ?: "orca_plate") + ".gcode"
            _uiState.update { it.copy(isNetworkBusy = true, statusBannerMessage = "Uploading '$jobName' to ${state.printerHost}...") }

            val uploadRes = PrinterNetworkClient.uploadGCodeToPrinter(
                hostOrIp = state.printerHost,
                port = portNum,
                apiKey = state.printerApiKey,
                protocol = state.printerProtocol,
                fileName = jobName,
                gcodeText = gcode,
                startPrintImmediately = startImmediately
            )
            uploadRes.fold(
                onSuccess = { msg ->
                    _uiState.update { it.copy(isNetworkBusy = false, statusBannerMessage = msg) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isNetworkBusy = false, statusBannerMessage = err.message ?: "Upload failed") }
                }
            )
        }
    }

    private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    GCODE("Export G-code (.gcode)", "gcode", "application/octet-stream"),
    PROJECT_3MF("Save Orca Project (.3mf)", "3mf", "application/vnd.ms-package.3dmanufacturing-3dmodel+xml"),
    BINARY_STL("Export Plate Mesh (.stl)", "stl", "application/sla")
}
