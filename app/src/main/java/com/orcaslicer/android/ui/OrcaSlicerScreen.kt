package com.orcaslicer.android.ui

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.orcaslicer.android.gpu.Orca3DViewport
import com.orcaslicer.android.gpu.ViewportCameraPreset
import com.orcaslicer.android.model.BrimType
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.ScarfSeamType
import com.orcaslicer.android.model.SeamPosition
import com.orcaslicer.android.model.SupportStyle
import com.orcaslicer.android.model.SupportType
import com.orcaslicer.android.model.ToolpathType
import com.orcaslicer.android.model.Vec3
import com.orcaslicer.android.network.NetworkProtocol
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

// DolphinSlicer Dark Charcoal + Azure/Teal Theme Palette
internal val OrcaActiveTabBg = Color(0xFF1D88B8)
private val OrcaGlassBg = Color(0xEB151822)          // Deep dark slate glass
private val OrcaGlassBorder = Color(0x18FFFFFF)
private val OrcaActiveBtnBg = Color(0x3329ABE2)      // Dolphin Azure active fill
private val OrcaActiveBtnBorder = Color(0x8829ABE2)  // Dolphin Azure active border
private val EmeraldGreen = Color(0xFF10B981)
private val EmeraldDimBg = Color(0x2210B981)
private val EmeraldBorder = Color(0x4D10B981)

private val OrcaColorScheme = darkColorScheme(
    primary = OrcaTeal,
    onPrimary = Color(0xFF04141E),
    secondary = OrcaTealBright,
    background = OrcaDarkBg,
    surface = OrcaPanelBg,
    surfaceVariant = OrcaCardBg,
    onBackground = OrcaTextPrimary,
    onSurface = OrcaTextPrimary
)

@Composable
fun OrcaSlicerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrcaColorScheme,
        content = content
    )
}

@Composable
fun OrcaSlicerScreen(viewModel: OrcaViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= 700

    var pendingExportFormat by remember { mutableStateOf(ExportFormat.GCODE) }
    var showPrinterBrowser by remember { mutableStateOf(false) }
    var showFilamentBrowser by remember { mutableStateOf(false) }
    var showCalibrationSheet by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(context.contentResolver, uri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context.contentResolver, uri, pendingExportFormat)
        }
    }

    val triggerImport = {
        importLauncher.launch(arrayOf("*/*"))
    }

    val triggerExport: (ExportFormat) -> Unit = { format ->
        pendingExportFormat = format
        val baseName = state.selectedObject?.name?.replace(" ", "_") ?: "dolphin_plate"
        exportLauncher.launch("$baseName.${format.extension}")
    }

    OrcaSlicerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OrcaDarkBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar & Hardware Hierarchy Header (referencing mobile_3d_slicer_ui.html)
                OrcaMobileReferenceHeader(
                    state = state,
                    isLandscape = isLandscape,
                    onSelectWorkspaceTab = viewModel::selectWorkspaceTab,
                    onImportClick = triggerImport,
                    onSyncServerProfiles = viewModel::syncProfilesFromOrcaServer,
                    onAddPrimitive = viewModel::addBuiltInPrimitive,
                    onOpenCalibrationModal = { showCalibrationSheet = true },
                    onClearPlate = viewModel::clearBuildPlate,
                    onSliceClick = viewModel::sliceBuildPlate,
                    onExportClick = triggerExport,
                    onOpenPrinterBrowser = { showPrinterBrowser = true },
                    onOpenFilamentBrowser = { showFilamentBrowser = true },
                    onOpenPrinterSettings = { viewModel.setPrinterSettingsDialogVisible(true) },
                    onOpenFilamentSettings = { viewModel.setFilamentSettingsDialogVisible(true) },
                    onSelectProcessPreset = viewModel::selectProcessPreset
                )

                // Main Workspace Body
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (state.activeWorkspaceTab) {
                        WorkspaceTab.PROJECT -> {
                            CalibrationWorkspacePanel(viewModel = viewModel)
                        }
                        WorkspaceTab.DEVICE -> {
                            DeviceNetworkPanel(state = state, viewModel = viewModel)
                        }
                        WorkspaceTab.PREPARE, WorkspaceTab.PREVIEW -> {
                            if (isLandscape) {
                                // LANDSCAPE: Left Sidebar + Right 3D Viewport with Reference Overlays
                                Row(modifier = Modifier.fillMaxSize()) {
                                    AnimatedVisibility(visible = state.isDockExpanded) {
                                        Row(modifier = Modifier.fillMaxHeight()) {
                                            Column(
                                                modifier = Modifier
                                                    .width(365.dp)
                                                    .fillMaxHeight()
                                                    .background(OrcaPanelBg)
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                FullPresetSelectorsCard(
                                                    state = state,
                                                    viewModel = viewModel,
                                                    onImportProfileClick = triggerImport
                                                )
                                                QuickReferenceParameterCards(
                                                    state = state,
                                                    viewModel = viewModel
                                                )
                                                FullProcessSettingsCard(state = state, viewModel = viewModel)
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    ObjectListCard(
                                                        state = state,
                                                        viewModel = viewModel,
                                                        onImportClick = triggerImport
                                                    )
                                                    if (state.sliceResult != null) {
                                                        SliceStatisticsSummaryCard(state = state, viewModel = viewModel)
                                                    } else {
                                                        FilamentQuickCalculatorCard(state = state, viewModel = viewModel)
                                                    }
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .fillMaxHeight()
                                                    .background(OrcaBorder)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        ViewportWithTouchOverlays(
                                            state = state,
                                            viewModel = viewModel,
                                            isLandscape = true,
                                            onImportClick = triggerImport
                                        )
                                    }
                                }
                            } else {
                                // PORTRAIT: 3D Viewport in back + Dynamic Adjustable Bottom Sheet in front of 3D Viewport
                                val density = LocalDensity.current
                                var workspaceHeightPx by remember { mutableFloatStateOf(0f) }
                                val maxDrawerBodyDp = if (workspaceHeightPx > 0f) {
                                    with(density) { workspaceHeightPx.toDp() - 92.dp }.coerceAtLeast(220.dp)
                                } else {
                                    (configuration.screenHeightDp.dp - 210.dp).coerceAtLeast(220.dp)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { workspaceHeightPx = it.height.toFloat() }
                                ) {
                                    // 3D Viewport stays fixed in the background; bottom padding reserves full room for the collapsed bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = 108.dp)
                                    ) {
                                        ViewportWithTouchOverlays(
                                            state = state,
                                            viewModel = viewModel,
                                            isLandscape = false,
                                            onImportClick = triggerImport
                                        )
                                    }

                                    // Dynamic Bottom Drawer overlays IN FRONT of the 3D Viewport when pulled up
                                    OrcaReferenceBottomDrawer(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        maxSheetBodyHeightDp = maxDrawerBodyDp,
                                        state = state,
                                        viewModel = viewModel,
                                        onImportClick = triggerImport,
                                        onExportClick = triggerExport
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Calibration Bottom-Sheet Modal (matching #calibrationModal in mobile_3d_slicer_ui.html)
            if (showCalibrationSheet) {
                CalibrationBottomSheetModal(
                    onSelectTest = { test ->
                        showCalibrationSheet = false
                        viewModel.launchCalibrationTest(test)
                    },
                    onDismiss = { showCalibrationSheet = false }
                )
            }

            // Printer Browser Modal
            if (showPrinterBrowser) {
                PrinterProfileBrowserModal(
                    state = state,
                    onSelectPrinter = {
                        viewModel.selectPrinter(it)
                        showPrinterBrowser = false
                    },
                    onSyncServerClick = { viewModel.syncProfilesFromOrcaServer() },
                    onImportProfileClick = {
                        showPrinterBrowser = false
                        triggerImport()
                    },
                    onDismiss = { showPrinterBrowser = false }
                )
            }

            // Filament Browser Modal
            if (showFilamentBrowser) {
                FilamentProfileBrowserModal(
                    state = state,
                    onSelectFilament = {
                        viewModel.selectFilament(it)
                        showFilamentBrowser = false
                    },
                    onSyncServerClick = { viewModel.syncProfilesFromOrcaServer() },
                    onImportProfileClick = {
                        showFilamentBrowser = false
                        triggerImport()
                    },
                    onDismiss = { showFilamentBrowser = false }
                )
            }

            // Modal Dialogs for Full Printer & Filament Settings
            if (state.showPrinterSettingsDialog) {
                PrinterSettingsModalDialog(
                    state = state,
                    viewModel = viewModel,
                    onDismiss = { viewModel.setPrinterSettingsDialogVisible(false) }
                )
            }

            if (state.showFilamentSettingsDialog) {
                FilamentSettingsModalDialog(
                    state = state,
                    viewModel = viewModel,
                    onDismiss = { viewModel.setFilamentSettingsDialogVisible(false) }
                )
            }
        }
    }
}

/**
 * Top App Bar & Hardware Hierarchy Selector referencing `mobile_3d_slicer_ui.html` (lines 117-203).
 */
@Composable
private fun OrcaMobileReferenceHeader(
    state: OrcaUiState,
    isLandscape: Boolean,
    onSelectWorkspaceTab: (WorkspaceTab) -> Unit,
    onImportClick: () -> Unit,
    onSyncServerProfiles: () -> Unit,
    onAddPrimitive: (BuiltInPrimitiveType) -> Unit,
    onOpenCalibrationModal: () -> Unit,
    onClearPlate: () -> Unit,
    onSliceClick: () -> Unit,
    onExportClick: (ExportFormat) -> Unit,
    onOpenPrinterBrowser: () -> Unit,
    onOpenFilamentBrowser: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    onOpenFilamentSettings: () -> Unit,
    onSelectProcessPreset: (com.orcaslicer.android.model.ProcessProfile) -> Unit
) {
    var showMainMenu by remember { mutableStateOf(false) }
    var showPrimitiveMenu by remember { mutableStateOf(false) }
    var showProcessDropdown by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHardwareSelectors by remember { mutableStateOf(true) }
    val uriContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrcaTopBarBg)
            .border(1.dp, Color(0x14FFFFFF))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // 1. Compact System Bar: Brand Title + Calibration Pill + Klipper Pill + Collapse Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Logo + DOLPHINSLICER + Version Badge + Main Menu Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showMainMenu = true }
                        .padding(end = 4.dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(OrcaTeal, OrcaTealBright)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        OrcaWhaleVectorIcon(modifier = Modifier.size(14.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "DOLPHIN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "SLICER",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.5.sp,
                            color = OrcaTealBright,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OrcaInputBg)
                                .border(1.dp, OrcaTealBright.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = "v1.2.1",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = OrcaTealBright,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Main Menu",
                            tint = OrcaTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                DropdownMenu(expanded = showMainMenu, onDismissRequest = { showMainMenu = false }) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Import 3MF / STL / OBJ / G-code...") },
                        onClick = {
                            showMainMenu = false
                            onImportClick()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = OrcaTealBright) },
                        text = {
                            Text(
                                if (state.isSyncingServerProfiles) "Syncing Profiles from Orca Server..."
                                else "Sync All Printer & Filament Profiles from Server"
                            )
                        },
                        onClick = {
                            showMainMenu = false
                            onSyncServerProfiles()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Import Custom Profile (.json / .ini / .orca_printer)...") },
                        onClick = {
                            showMainMenu = false
                            onImportClick()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Add Primitive (Cube, Cylinder, Arch, Sphere, Torus)...") },
                        onClick = {
                            showMainMenu = false
                            showPrimitiveMenu = true
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Calibration Suite (Flow, PA Tower, Temp Tower)...") },
                        onClick = {
                            showMainMenu = false
                            onOpenCalibrationModal()
                        }
                    )
                    if (!isLandscape) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = OrcaTealBright) },
                            text = { Text(if (showHardwareSelectors) "Hide Hardware Header Cards" else "Show Hardware Header Cards") },
                            onClick = {
                                showMainMenu = false
                                showHardwareSelectors = !showHardwareSelectors
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Save Project (.3mf)") },
                        onClick = {
                            showMainMenu = false
                            onExportClick(ExportFormat.PROJECT_3MF)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.IosShare, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Export G-code (.gcode)") },
                        onClick = {
                            showMainMenu = false
                            onExportClick(ExportFormat.GCODE)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.ViewInAr, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Export Plate Mesh (.stl)") },
                        onClick = {
                            showMainMenu = false
                            onExportClick(ExportFormat.BINARY_STL)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Printer & Extruder Settings...") },
                        onClick = {
                            showMainMenu = false
                            onOpenPrinterSettings()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Filament & Currency Settings...") },
                        onClick = {
                            showMainMenu = false
                            onOpenFilamentSettings()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF5350)) },
                        text = { Text("New Project (Clear Build Plate)") },
                        onClick = {
                            showMainMenu = false
                            onClearPlate()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Text("☕", fontSize = 15.sp) },
                        text = { Text("Support / Buy Me a Coffee (buymeacoffee.com/nandaje)") },
                        onClick = {
                            showMainMenu = false
                            runCatching {
                                uriContext.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://buymeacoffee.com/nandaje")
                                    )
                                )
                            }
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { OrcaWhaleVectorIcon(modifier = Modifier.size(18.dp)) },
                        text = { Text("About DolphinSlicer & Contributors...") },
                        onClick = {
                            showMainMenu = false
                            showAboutDialog = true
                        }
                    )
                }

                // Sub-dropdown for Add Primitive
                DropdownMenu(expanded = showPrimitiveMenu, onDismissRequest = { showPrimitiveMenu = false }) {
                    BuiltInPrimitiveType.entries.forEach { prim ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.ViewInAr, contentDescription = null, tint = OrcaTealBright) },
                            text = { Text(prim.title) },
                            onClick = {
                                showPrimitiveMenu = false
                                onAddPrimitive(prim)
                            }
                        )
                    }
                }
            }

            // Right: Calibration Menu Button + Klipper Status Indicator + Header Collapse Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Calibration Menu Button (matching #btnCalibrationMenu in mobile_3d_slicer_ui.html)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrcaCardBg)
                        .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenCalibrationModal)
                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Calibration",
                        tint = OrcaTealBright,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Calibration",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = OrcaTealBright,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = OrcaTextSecondary,
                        modifier = Modifier.size(10.dp)
                    )
                }

                // Printer Status Indicator Pill (`KLIPPER` / `OCTOPRINT`) — single line guaranteed
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(EmeraldDimBg)
                        .border(1.dp, EmeraldBorder, RoundedCornerShape(999.dp))
                        .clickable { onSelectWorkspaceTab(WorkspaceTab.DEVICE) }
                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Text(
                        text = if (state.printerProtocol == NetworkProtocol.MOONRAKER_KLIPPER) "KLIPPER" else "OCTOPRINT",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldGreen,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // 1-Tap Hardware Cards Collapse / Expand Toggle Button for extra 3D Viewport space
                if (!isLandscape) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(OrcaCardBg)
                            .border(1.dp, OrcaGlassBorder, RoundedCornerShape(5.dp))
                            .clickable { showHardwareSelectors = !showHardwareSelectors },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showHardwareSelectors) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Hardware Header",
                            tint = OrcaTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // 2. Compact Hardware Hierarchy Selector (Printer + Filament Slot + Process Row — matching HTML lines 151-188)
        if (!isLandscape && showHardwareSelectors) {
            val cleanPrinterBaseName = state.selectedPrinter.name
                .substringBefore(" (")
                .replace(Regex("\\s+0\\.\\d+\\s*nozzle", RegexOption.IGNORE_CASE), "")
                .trim()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Printer & Nozzle Selector Card (`#btnOpenPrinterSelect`)
                Row(
                    modifier = Modifier
                        .weight(0.58f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrcaCardBg)
                        .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenPrinterBrowser)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = OrcaTealBright,
                            modifier = Modifier.size(12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$cleanPrinterBaseName (${state.selectedPrinter.nozzleDiameterMm})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Textured PEI • ${state.selectedPrinter.bedWidthX.toInt()}×${state.selectedPrinter.bedDepthY.toInt()}mm",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = OrcaTextSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = OrcaTextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                }

                // Filament Slot / AMS Spool Selector (`#btnFilamentSlots`)
                Row(
                    modifier = Modifier
                        .weight(0.42f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrcaCardBg)
                        .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenFilamentBrowser)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Overlapping colored spool circles
                    Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                        val previewColors = if (state.filamentSlots.size >= 2) {
                            state.filamentSlots.take(3).map { Color(it.colorHex) }
                        } else {
                            listOf(Color(state.selectedFilament.colorHex), OrcaTealBright, Color(0xFFF1F5F9))
                        }
                        previewColors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, OrcaCardBg, CircleShape)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${state.activeFilamentSlotIndex + 1}: ${state.selectedFilament.name}",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${state.selectedFilament.nozzleTempOtherC}° / ${state.selectedFilament.bedTempOtherC}°C",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OrcaTealBright,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Compact Process Preset Row
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrcaCardBg.copy(alpha = 0.85f))
                        .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(6.dp))
                        .clickable { showProcessDropdown = true }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = OrcaTealBright,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Process:",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OrcaTextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = state.currentProcess.name,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${state.currentProcess.innerWallSpeedMmS.toInt()}mm/s",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OrcaUnitText,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                DropdownMenu(expanded = showProcessDropdown, onDismissRequest = { showProcessDropdown = false }) {
                    state.processPresets.forEach { proc ->
                        DropdownMenuItem(
                            text = { Text(proc.name, fontSize = 11.sp) },
                            onClick = {
                                showProcessDropdown = false
                                onSelectProcessPreset(proc)
                            }
                        )
                    }
                }
            }
        }

        // 3. Compact Main Segmented Tabs (PREPARE | PREVIEW | DEVICE | PROJECT — matching HTML lines 191-202)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(OrcaDarkBg)
                .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkspaceTab.entries.forEach { tab ->
                val active = state.activeWorkspaceTab == tab
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) OrcaActiveBtnBg else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (active) OrcaActiveBtnBorder else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            if (tab == WorkspaceTab.PREVIEW && state.sliceResult == null && state.sceneObjects.isNotEmpty()) {
                                onSliceClick()
                            } else {
                                onSelectWorkspaceTab(tab)
                            }
                        }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tab.title.uppercase(Locale.US),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (active) OrcaTealBright else OrcaTextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (tab == WorkspaceTab.DEVICE) {
                        Spacer(Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = OrcaPanelBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OrcaTealBright, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrcaWhaleVectorIcon(modifier = Modifier.size(26.dp))
                        Column {
                            Text(
                                text = "DolphinSlicer v1.2.2",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OrcaTealBright
                            )
                            Text(
                                text = "OrcaSlicer for Android • OpenGL ES 3.0",
                                fontSize = 11.sp,
                                color = OrcaTextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = OrcaBorder)

                    Text(
                        text = "Contributors",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrcaTextPrimary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrcaCardBg)
                            .border(1.dp, OrcaBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "@Nandaje",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrcaTealBright
                            )
                            Text(
                                text = "Creator & Lead Maintainer",
                                fontSize = 11.sp,
                                color = OrcaTextSecondary
                            )
                        }
                        HorizontalDivider(color = OrcaBorder)
                        Column {
                            Text(
                                text = "Google Gemini 4 Antigravity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrcaTealBright
                            )
                            Text(
                                text = "Google DeepMind • AI Engineering & Architecture Contributor",
                                fontSize = 11.sp,
                                color = OrcaTextSecondary
                            )
                        }
                    }

                    // Support & Donation Section (buymeacoffee.com/nandaje)
                    Text(
                        text = "Support & Donation",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrcaTextPrimary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrcaCardBg)
                            .border(1.dp, Color(0xFFFFDD00).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .clickable {
                                runCatching {
                                    uriContext.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://buymeacoffee.com/nandaje")
                                        )
                                    )
                                }
                            }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "If you enjoy using DolphinSlicer on Android, consider supporting development:",
                            fontSize = 11.sp,
                            color = OrcaTextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFDD00))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "☕ Buy Me a Coffee — buymeacoffee.com/nandaje",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D0E12)
                            )
                        }
                    }

                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrcaActiveTabBg,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 3D Slicing & Preview Viewport with Glass Overlays from `mobile_3d_slicer_ui.html` (lines 206-361):
 * - Left Vertical Tool Palette (Prepare Mode: Import, Move, Rotate, Scale, Cut, Lay Flat, Mirror, Measure, Arrange, Overhang, Delete)
 * - Model Name & Geometry Info Badge (or Empty Plate Import prompt)
 * - Top-Right Orientation Cube Gizmo, `ISO` Button, and `Fit to Bed` Button
 * - Preview Mode Top-Left Line Type Legend Overlay
 * - Preview Mode Right-Side Vertical Layer Scrubber with Auto-Play Simulator
 * - Preview Mode Bottom Horizontal Extrusion Move Scrubber
 * - Full-Viewport Slicing Processing Modal / Progress Overlay
 */
@Composable
private fun ViewportWithTouchOverlays(
    state: OrcaUiState,
    viewModel: OrcaViewModel,
    isLandscape: Boolean,
    onImportClick: () -> Unit
) {
    val isPreview = state.activeWorkspaceTab == WorkspaceTab.PREVIEW
    var legendCollapsed by remember { mutableStateOf(false) }
    var isPlayingLayers by remember { mutableStateOf(false) }

    // Auto-play layer animation when user taps Play on the vertical layer scrubber
    val slice = state.sliceResult
    LaunchedEffect(isPlayingLayers, isPreview, slice) {
        if (isPlayingLayers && isPreview && slice != null && slice.layers.isNotEmpty()) {
            val total = slice.layers.size
            if (state.maxVisibleLayer >= total - 1) {
                viewModel.updateLayerRange(state.minVisibleLayer, 0)
            }
            while (isPlayingLayers) {
                delay(45L)
                val current = viewModel.uiState.value.maxVisibleLayer
                if (current < total - 1) {
                    viewModel.updateLayerRange(viewModel.uiState.value.minVisibleLayer, current + 1)
                } else {
                    isPlayingLayers = false
                }
            }
        } else {
            isPlayingLayers = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Hardware-Accelerated OpenGL ES 3.0 3D Viewport
        Orca3DViewport(
            modifier = Modifier.fillMaxSize(),
            objects = state.sceneObjects,
            selectedObjectId = state.selectedObjectId,
            printer = state.selectedPrinter,
            filament = state.selectedFilament,
            showOverhangHighlight = state.showOverhangHighlight,
            overhangThresholdDeg = state.currentProcess.supportThresholdAngleDeg,
            isPreviewMode = isPreview,
            sliceResult = state.sliceResult,
            minVisibleLayer = state.minVisibleLayer,
            maxVisibleLayer = state.maxVisibleLayer,
            activeLayerProgress = state.activeLayerProgress,
            showTravelMoves = state.showTravelMoves,
            cameraPresetTrigger = state.cameraPresetTrigger,
            fitCameraTrigger = state.fitCameraTrigger,
            activeGizmoModeOrdinal = state.activeGizmoMode.ordinal,
            showTransformGizmo = !isPreview && state.selectedObjectId != null,
            cutPlaneHeightMm = state.cutPlaneHeightMm,
            onObjectSelected = viewModel::selectObject,
            onTransformSelectedObject = viewModel::updateSelectedObject,
            onUpdateCutPlaneHeight = viewModel::updateCutPlaneHeight,
            onLayOnFaceSelected = viewModel::laySelectedObjectOnLocalNormal
        )

        // 2. PREPARE MODE: Left Vertical Tool Palette (`#prepareToolsToolbar`, HTML lines 226-248)
        if (!isPreview) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 6.dp, bottom = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    GlassToolIconButton(
                        icon = Icons.Default.Code,
                        title = "Toggle Sidebar",
                        active = state.isDockExpanded,
                        onClick = viewModel::toggleDockExpanded
                    )
                }
                GlassToolIconButton(
                    icon = Icons.Default.FileOpen,
                    title = "Import 3MF / STL / OBJ",
                    active = false,
                    tint = OrcaTealBright,
                    onClick = onImportClick
                )
                GlassToolIconButton(
                    icon = Icons.Default.OpenWith,
                    title = "Move Model",
                    active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.MOVE,
                    onClick = { viewModel.selectGizmoMode(TransformGizmoMode.MOVE) }
                )
                GlassToolIconButton(
                    icon = Icons.Default.Rotate90DegreesCcw,
                    title = "Rotate",
                    active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.ROTATE,
                    onClick = { viewModel.selectGizmoMode(TransformGizmoMode.ROTATE) }
                )
                GlassToolIconButton(
                    icon = Icons.Default.AspectRatio,
                    title = "Scale",
                    active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.SCALE,
                    onClick = { viewModel.selectGizmoMode(TransformGizmoMode.SCALE) }
                )
                GlassToolIconButton(
                    icon = Icons.Default.ContentCut,
                    title = "Cut Plane",
                    active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.CUT,
                    onClick = { viewModel.selectGizmoMode(TransformGizmoMode.CUT) }
                )
                GlassToolIconButton(
                    icon = Icons.Default.VerticalAlignBottom,
                    title = "Lay on Face (Place on Face)",
                    active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.LAY_ON_FACE,
                    onClick = { viewModel.selectGizmoMode(TransformGizmoMode.LAY_ON_FACE) }
                )
                GlassToolIconButton(
                    icon = Icons.Default.Flip,
                    title = "Mirror",
                    active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.MIRROR,
                    onClick = { viewModel.selectGizmoMode(TransformGizmoMode.MIRROR) }
                )
                GlassToolIconButton(
                    icon = Icons.Default.Straighten,
                    title = "Measure",
                    active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.MEASURE,
                    onClick = { viewModel.selectGizmoMode(TransformGizmoMode.MEASURE) }
                )
                GlassToolIconButton(
                    icon = Icons.Default.GridView,
                    title = "Auto Arrange",
                    active = false,
                    onClick = viewModel::autoArrangeObjects
                )
                GlassToolIconButton(
                    icon = Icons.Default.WarningAmber,
                    title = "Highlight Overhangs",
                    active = state.showOverhangHighlight,
                    onClick = viewModel::toggleOverhangHighlight
                )
                if (state.selectedObject != null) {
                    GlassToolIconButton(
                        icon = Icons.Default.ContentCopy,
                        title = "Clone Object",
                        active = false,
                        onClick = { viewModel.duplicateSelectedObject(1) }
                    )
                    GlassToolIconButton(
                        icon = Icons.Default.DeleteOutline,
                        title = "Delete Object",
                        active = false,
                        tint = Color(0xFFEF5350),
                        onClick = viewModel::deleteSelectedObject
                    )
                }
            }

            // 3. PREPARE MODE: Model Name & Geometry Info Badge (`#modelMetaBadge`, HTML lines 250-258)
            val selObj = state.selectedObject
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 40.dp, top = 6.dp, end = 44.dp)
            ) {
                if (selObj != null) {
                    val dims = selObj.worldBoundingBox().size
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OrcaGlassBg)
                            .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                            .clickable { viewModel.setFloatingGizmoSheetVisible(!state.showFloatingGizmoSheet) }
                            .padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(OrcaTealBright)
                        )
                        Text(
                            text = selObj.name,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "|",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OrcaUnitText,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f×%.0f×%.0fmm", dims.x, dims.y, dims.z),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OrcaTextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    // Clean Empty Plate Badge (no example object preloaded)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OrcaGlassBg)
                            .border(1.dp, OrcaActiveBtnBorder, RoundedCornerShape(6.dp))
                            .clickable(onClick = onImportClick)
                            .padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = OrcaTealBright,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Empty Plate — Tap to Import 3MF / STL",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4. PREVIEW MODE: Line Type Legend Overlay (`#orcaLineLegend`, HTML lines 260-298)
        if (isPreview) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 6.dp)
                    .width(136.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrcaGlassBg)
                    .border(1.dp, OrcaGlassBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { legendCollapsed = !legendCollapsed },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LINE TYPE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Toggle Legend",
                        tint = OrcaTealBright,
                        modifier = Modifier.size(12.dp)
                    )
                }

                if (!legendCollapsed) {
                    HorizontalDivider(color = OrcaGlassBorder, modifier = Modifier.padding(vertical = 1.dp))
                    val legendItems = listOf(
                        Triple(Color(0xFFEE5D32), "Outer wall", true),
                        Triple(Color(0xFFF1C40F), "Inner wall", true),
                        Triple(Color(0xFFB46522), "Sparse infill", true),
                        Triple(Color(0xFF9B59B6), "Internal solid", true),
                        Triple(Color(0xFF3498DB), "Bridge / Overhang", true),
                        Triple(Color(0xFF2ECC71), "Support (${state.currentProcess.supportType.label.substringBefore(" ")})", true),
                        Triple(Color(0xFF00BEAC), "Skirt / Brim", true)
                    )
                    legendItems.forEach { (swatchColor, label, _) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 9.dp, height = 7.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(swatchColor)
                            )
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = OrcaTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Clickable Travel Moves toggle inside Line Type Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.toggleTravelMoves() }
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 9.dp, height = 7.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF4B5563))
                            )
                            Text(
                                text = "Travel moves",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (state.showTravelMoves) Color.White else OrcaTextSecondary
                            )
                        }
                        Text(
                            text = if (state.showTravelMoves) "ON" else "OFF",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (state.showTravelMoves) OrcaTealBright else OrcaUnitText
                        )
                    }
                }
            }
        }

        // 5. Top-Right Viewport Controls & 3D Compass Gizmo (`#cubeGizmo`, `#btnIsoView`, `#btnResetCam`, HTML lines 210-223)
        var camPresetIndex by remember { mutableIntStateOf(0) }
        val camPresets = remember {
            listOf(
                ViewportCameraPreset.TOP to "TOP",
                ViewportCameraPreset.FRONT to "FRT",
                ViewportCameraPreset.RIGHT to "RGT",
                ViewportCameraPreset.ISOMETRIC to "ISO"
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Orientation Cube Gizmo Indicator (`#cubeGizmo`)
            Column(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrcaGlassBg)
                    .border(1.dp, OrcaGlassBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        val (preset, _) = camPresets[camPresetIndex % camPresets.size]
                        viewModel.setCameraPreset(preset)
                        camPresetIndex = (camPresetIndex + 1) % camPresets.size
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = camPresets[camPresetIndex % camPresets.size].second,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = OrcaTealBright,
                    maxLines = 1,
                    softWrap = false
                )
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("X", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFEF5350))
                    Text("Y", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = EmeraldGreen)
                }
            }

            // `ISO` Button (`#btnIsoView`)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaGlassBg)
                    .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                    .clickable { viewModel.setCameraPreset(ViewportCameraPreset.ISOMETRIC) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ISO",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = OrcaTealBright,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // `Fit to Bed` Button (`#btnResetCam`)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaGlassBg)
                    .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                    .clickable { viewModel.resetCameraView() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Fit to Bed",
                    tint = OrcaTextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 6. PREVIEW MODE: Vertical Layer Scrubber (`#verticalScrubberWrap`, HTML lines 300-330)
        if (isPreview && slice != null && slice.layers.isNotEmpty()) {
            val totalLayers = slice.layers.size
            val safeMaxLayer = state.maxVisibleLayer.coerceIn(0, totalLayers - 1)
            val topLayer = slice.layers[safeMaxLayer]
            val layerFraction = if (totalLayers > 1) {
                safeMaxLayer.toFloat() / (totalLayers - 1).toFloat()
            } else {
                1f
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp, top = 104.dp, bottom = 48.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Floating Layer Number Bubble (`#layerInfoTag`)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrcaGlassBg)
                        .border(1.dp, OrcaActiveBtnBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "L: ${safeMaxLayer + 1}/$totalLayers",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = OrcaTealBright,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = String.format(Locale.US, "Z: %.1fmm", topLayer.zHeightMm),
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OrcaTextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Vertical Slider Track (`#layerSlider` + `#sliderFillBar` + `#customSliderThumb`)
                var trackHeightPx by remember { mutableFloatStateOf(1f) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .width(24.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(OrcaTopBarBg.copy(alpha = 0.85f))
                        .border(1.dp, OrcaGlassBorder, RoundedCornerShape(999.dp))
                        .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                        .pointerInput(totalLayers) {
                            detectVerticalDragGestures { change, _ ->
                                change.consume()
                                val y = change.position.y.coerceIn(0f, trackHeightPx)
                                val frac = 1f - (y / trackHeightPx)
                                val targetLayer = (frac * (totalLayers - 1)).roundToInt().coerceIn(0, totalLayers - 1)
                                viewModel.updateLayerRange(state.minVisibleLayer, targetLayer)
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Track background line
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(OrcaElevatedBg)
                    )
                    // Gradient Fill Bar from bottom
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(layerFraction.coerceIn(0.04f, 1f))
                            .width(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(OrcaTealBright, OrcaTeal, EmeraldGreen)
                                )
                            )
                    )
                    // Thumb indicator circle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxHeight(layerFraction.coerceIn(0.05f, 1f)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(OrcaTopBarBg)
                                .border(1.5.dp, OrcaTealBright, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(OrcaTealBright)
                            )
                        }
                    }
                }

                // Micro-steppers & Layer Auto-Play Button (`#btnPlayLayers`, `#layerUpBtn`, `#layerDownBtn`)
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPlayingLayers) OrcaActiveBtnBg else OrcaGlassBg)
                            .border(1.dp, if (isPlayingLayers) OrcaActiveBtnBorder else OrcaGlassBorder, RoundedCornerShape(6.dp))
                            .clickable { isPlayingLayers = !isPlayingLayers },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlayingLayers) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Layer Simulation",
                            tint = OrcaTealBright,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OrcaGlassBg)
                            .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                viewModel.updateLayerRange(state.minVisibleLayer, (safeMaxLayer + 1).coerceAtMost(totalLayers - 1))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Layer Up",
                            tint = OrcaTextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OrcaGlassBg)
                            .border(1.dp, OrcaGlassBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                viewModel.updateLayerRange(state.minVisibleLayer, (safeMaxLayer - 1).coerceAtLeast(0))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Layer Down",
                            tint = OrcaTextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // 7. PREVIEW MODE: Horizontal Extrusion Move Scrubber (`#horizontalMoveScrubber`, HTML lines 332-342)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 40.dp, bottom = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrcaGlassBg)
                    .border(1.dp, OrcaGlassBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = OrcaTealBright,
                    modifier = Modifier.size(13.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Move: ${(state.activeLayerProgress * 100).toInt()}%",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "Speed: ${state.currentProcess.outerWallSpeedMmS.toInt()} mm/s • Combing: ${state.currentProcess.combingMode.label}",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OrcaTealBright
                        )
                    }
                    Slider(
                        value = state.activeLayerProgress,
                        onValueChange = viewModel::updateActiveLayerProgress,
                        valueRange = 0.02f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = OrcaTealBright,
                            activeTrackColor = OrcaTeal,
                            inactiveTrackColor = OrcaElevatedBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )
                }
            }
        }

        // 8. Floating Interactive Gizmo Sheet (Move / Scale / Rotate / Cut / Mirror / Measure)
        // Positioned near the top-left (below the model badge) so it is never hidden behind the bottom bar
        val selObj = state.selectedObject
        if (!isPreview && state.showFloatingGizmoSheet && selObj != null) {
            FloatingGizmoControlCard(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 42.dp, top = 34.dp, end = 38.dp),
                state = state,
                selected = selObj,
                viewModel = viewModel
            )
        }

        // 9. Slicing Processing Modal / Progress Overlay (`#slicingProgressModal`, HTML lines 344-360)
        if (state.isSlicing || state.isSyncingServerProfiles) {
            val pct = ((if (state.isSlicing) state.sliceProgress else 0.65f) * 100f).toInt().coerceIn(0, 100)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OrcaDarkBg.copy(alpha = 0.90f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { if (state.isSlicing) state.sliceProgress.coerceIn(0.05f, 1f) else 0.65f },
                            modifier = Modifier.fillMaxSize(),
                            color = OrcaTealBright,
                            trackColor = OrcaInputBg,
                            strokeWidth = 4.dp
                        )
                        OrcaWhaleVectorIcon(modifier = Modifier.size(30.dp))
                    }
                    Text(
                        text = if (state.isSlicing) "DOLPHIN SLICING ENGINE" else "ORCA PROFILE SERVER SYNC",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                    Text(
                        text = state.statusBannerMessage,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OrcaTealBright
                    )
                    LinearProgressIndicator(
                        progress = { if (state.isSlicing) state.sliceProgress.coerceIn(0.02f, 1f) else 0.65f },
                        modifier = Modifier
                            .width(200.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = OrcaTealBright,
                        trackColor = OrcaInputBg
                    )
                    Text(
                        text = "$pct%",
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OrcaTextSecondary
                    )
                    if (state.isSlicing) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrcaElevatedBg)
                                .border(1.dp, OrcaBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.cancelSlicing() }
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "CANCEL SLICING",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassToolIconButton(
    icon: ImageVector,
    title: String,
    active: Boolean,
    tint: Color = OrcaTextSecondary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) OrcaActiveBtnBg else OrcaGlassBg)
            .border(
                width = 1.dp,
                color = if (active) OrcaActiveBtnBorder else OrcaGlassBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (active) OrcaTealBright else tint,
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Bottom Drawer & Tuning Panel referencing `mobile_3d_slicer_ui.html` (lines 363-568):
 * - Drag Handle
 * - Quick Estimation & Header Summary Bar (`PRINT ESTIMATION`, Time, Weight/Length, Cost, Settings/Objects/Cost switch)
 * - Expandable Multi-Tab Parameter Sheet (`QUALITY`, `STRENGTH`, `SPEED`, `SUPPORT`, `MULTIMATERIAL`, `OTHERS`)
 * - Action Footer: `SLICE PLATE` (or `IMPORT MODEL`) & `PRINT VIA KLIPPER` / `EXPORT` Buttons
 */
@Composable
private fun OrcaReferenceBottomDrawer(
    modifier: Modifier = Modifier,
    maxSheetBodyHeightDp: androidx.compose.ui.unit.Dp = 520.dp,
    state: OrcaUiState,
    viewModel: OrcaViewModel,
    onImportClick: () -> Unit,
    onExportClick: (ExportFormat) -> Unit
) {
    var drawerSubView by remember { mutableIntStateOf(0) } // 0 = Settings, 1 = Objects, 2 = Cost/Stats, 3 = Presets
    var showPrintExportMenu by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val maxBodyDpVal = maxSheetBodyHeightDp.value.coerceAtLeast(220f)
    var expandedTargetHeightDp by remember { mutableFloatStateOf(270f) }
    var isDraggingSheet by remember { mutableStateOf(false) }
    var draggedHeightDp by remember { mutableFloatStateOf(0f) }
    var handleRootY by remember { mutableFloatStateOf(0f) }
    var dragStartFingerRootY by remember { mutableFloatStateOf(0f) }
    var dragStartHeightDp by remember { mutableFloatStateOf(0f) }

    val targetBodyHeightDp = if (isDraggingSheet) {
        draggedHeightDp.coerceIn(0f, maxBodyDpVal)
    } else if (state.isDockExpanded) {
        expandedTargetHeightDp.coerceIn(110f, maxBodyDpVal)
    } else {
        0f
    }

    val animatedBodyHeightDp by animateFloatAsState(
        targetValue = targetBodyHeightDp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottomDrawerHeight"
    )
    // Use direct 1:1 finger height while actively dragging so there is zero spring lag; animate smoothly on tap/release
    val currentBodyHeightDp = if (isDraggingSheet) targetBodyHeightDp else animatedBodyHeightDp

    val slice = state.sliceResult
    val stats = slice?.statistics
    val cost = stats?.calculateFullCostBreakdown(state.selectedPrinter, state.selectedFilament)
    val cur = state.selectedFilament.currencySymbol

    // Root-coordinate vertical drag gesture: immune to the handle moving vertically as the sheet grows/shrinks
    val dragGestureModifier = Modifier
        .onGloballyPositioned { coords ->
            handleRootY = coords.positionInRoot().y
        }
        .pointerInput(maxBodyDpVal) {
            detectVerticalDragGestures(
                onDragStart = { startOffset ->
                    isDraggingSheet = true
                    dragStartHeightDp = currentBodyHeightDp
                    draggedHeightDp = currentBodyHeightDp
                    dragStartFingerRootY = handleRootY + startOffset.y
                },
                onVerticalDrag = { change, _ ->
                    change.consume()
                    val currentFingerRootY = handleRootY + change.position.y
                    val totalDeltaUpPx = dragStartFingerRootY - currentFingerRootY
                    val totalDeltaUpDp = with(density) { totalDeltaUpPx.toDp().value }
                    draggedHeightDp = (dragStartHeightDp + totalDeltaUpDp).coerceIn(0f, maxBodyDpVal)
                },
                onDragEnd = {
                    val finalDp = draggedHeightDp
                    isDraggingSheet = false
                    if (finalDp < 72f) {
                        if (state.isDockExpanded) viewModel.toggleDockExpanded()
                    } else {
                        expandedTargetHeightDp = if (finalDp > maxBodyDpVal * 0.90f) {
                            maxBodyDpVal
                        } else {
                            finalDp.coerceIn(120f, maxBodyDpVal)
                        }
                        if (!state.isDockExpanded) viewModel.toggleDockExpanded()
                    }
                },
                onDragCancel = {
                    isDraggingSheet = false
                }
            )
        }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(OrcaPanelBg.copy(alpha = 0.97f))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
    ) {
        // 1. Dynamic Drag Handle (`#drawerDragHandle`) — Pull up to expand all the way over 3D viewport, pull down to shrink/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(dragGestureModifier)
                .clickable {
                    if (!state.isDockExpanded) {
                        expandedTargetHeightDp = (maxBodyDpVal * 0.55f).coerceIn(220f, maxBodyDpVal)
                        viewModel.toggleDockExpanded()
                    } else if (expandedTargetHeightDp < maxBodyDpVal * 0.85f) {
                        // Second tap pulls full to the top in front of the 3D viewport
                        expandedTargetHeightDp = maxBodyDpVal
                    } else {
                        viewModel.toggleDockExpanded()
                    }
                }
                .padding(top = 6.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (state.isDockExpanded || isDraggingSheet) OrcaTealBright else Color(0xFF475569))
            )
        }

        // 2. Quick Estimation & Header Summary Bar (also supports smooth vertical drag resizing)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(dragGestureModifier)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                    text = "PRINT ESTIMATION",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = OrcaTextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = stats?.formattedTotalTime ?: if (state.sceneObjects.isEmpty()) "Empty Plate" else "Ready to Slice",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (stats != null) {
                            String.format(Locale.US, "%.1fg (%.2fm)", stats.filamentWeightGrams, stats.filamentLengthMeters)
                        } else {
                            "${state.sceneObjects.size} obj"
                        },
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = OrcaTealBright,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (cost != null) {
                        Text(
                            text = String.format(Locale.US, "~%s%.2f", cur, cost.totalProductionCost),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OrcaTextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Right: Sub-view pills (`Objects`, `Cost`, `Full`) + `Settings / Collapse` button (`#toggleDrawerBtn`)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Full-screen / Half-screen height toggle pill when expanded
                if (state.isDockExpanded || currentBodyHeightDp > 40f) {
                    val isFullTop = currentBodyHeightDp >= maxBodyDpVal * 0.88f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isFullTop) OrcaActiveBtnBg else OrcaGlassBg)
                            .border(
                                1.dp,
                                if (isFullTop) OrcaActiveBtnBorder else OrcaGlassBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                expandedTargetHeightDp = if (isFullTop) {
                                    (maxBodyDpVal * 0.52f).coerceIn(220f, maxBodyDpVal)
                                } else {
                                    maxBodyDpVal
                                }
                                if (!state.isDockExpanded) viewModel.toggleDockExpanded()
                            }
                            .padding(horizontal = 6.dp, vertical = 3.5.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullTop) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Toggle Full Top Sheet",
                            tint = OrcaTealBright,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Objects pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (state.isDockExpanded && drawerSubView == 1) OrcaActiveBtnBg else OrcaGlassBg)
                        .border(
                            1.dp,
                            if (state.isDockExpanded && drawerSubView == 1) OrcaActiveBtnBorder else OrcaGlassBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            drawerSubView = 1
                            if (!state.isDockExpanded) viewModel.toggleDockExpanded()
                        }
                        .padding(horizontal = 6.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = "Objects (${state.sceneObjects.size})",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.isDockExpanded && drawerSubView == 1) OrcaTealBright else OrcaTextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Cost pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (state.isDockExpanded && drawerSubView == 2) OrcaActiveBtnBg else OrcaGlassBg)
                        .border(
                            1.dp,
                            if (state.isDockExpanded && drawerSubView == 2) OrcaActiveBtnBorder else OrcaGlassBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            drawerSubView = 2
                            if (!state.isDockExpanded) viewModel.toggleDockExpanded()
                        }
                        .padding(horizontal = 6.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = "Cost",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.isDockExpanded && drawerSubView == 2) OrcaTealBright else OrcaTextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Settings / Collapse toggle button (`#toggleDrawerBtn`)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (state.isDockExpanded && drawerSubView == 0) OrcaActiveBtnBg else OrcaGlassBg)
                        .border(
                            1.dp,
                            if (state.isDockExpanded && drawerSubView == 0) OrcaActiveBtnBorder else OrcaGlassBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            if (state.isDockExpanded && drawerSubView == 0) {
                                viewModel.toggleDockExpanded()
                            } else {
                                drawerSubView = 0
                                if (!state.isDockExpanded) viewModel.toggleDockExpanded()
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 3.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (state.isDockExpanded && drawerSubView == 0) "Collapse" else "Settings",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OrcaTextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = if (state.isDockExpanded && drawerSubView == 0) Icons.Default.ExpandMore else Icons.Default.Tune,
                        contentDescription = null,
                        tint = OrcaTealBright,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        // 3. Detailed Multi-Tab Parameter Sheet (`#drawerSettingsBody`, smoothly sized from 0.dp up to full viewport height)
        if (currentBodyHeightDp > 2f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentBodyHeightDp.dp)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                when (drawerSubView) {
                    0 -> {
                        // Category Tab Pills (`QUALITY` | `STRENGTH` | `SPEED` | `SUPPORT` | `MULTIMATERIAL` | `OTHERS`)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProcessCategoryTab.entries.forEach { tab ->
                                val active = state.activeProcessTab == tab
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (active) OrcaActiveBtnBg else OrcaCardBg)
                                        .border(
                                            1.dp,
                                            if (active) OrcaActiveBtnBorder else Color.Transparent,
                                            RoundedCornerShape(5.dp)
                                        )
                                        .clickable { viewModel.selectProcessTab(tab) }
                                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                                ) {
                                    Text(
                                        text = tab.title.uppercase(Locale.US),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) OrcaTealBright else OrcaTextSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Search parameter toggle pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (state.showProcessSearchBar) OrcaActiveBtnBg else OrcaCardBg)
                                    .clickable { viewModel.toggleProcessSearchBar() }
                                    .padding(horizontal = 7.dp, vertical = 3.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Settings",
                                    tint = if (state.showProcessSearchBar) OrcaTealBright else OrcaTextSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(bottom = 4.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Reference Quick Tuning Cards from mobile_3d_slicer_ui.html
                            QuickReferenceParameterCards(state = state, viewModel = viewModel)

                            // Complete 1:1 OrcaSlicer & Cura Combing Mode Parameter Rows
                            FullProcessSettingsCard(state = state, viewModel = viewModel)
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            ObjectListCard(
                                state = state,
                                viewModel = viewModel,
                                onImportClick = onImportClick
                            )
                        }
                    }

                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (state.sliceResult != null) {
                                SliceStatisticsSummaryCard(state = state, viewModel = viewModel)
                            } else {
                                FilamentQuickCalculatorCard(state = state, viewModel = viewModel)
                            }
                        }
                    }

                    3 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            FullPresetSelectorsCard(
                                state = state,
                                viewModel = viewModel,
                                onImportProfileClick = onImportClick
                            )
                        }
                    }
                }
            }
        }

        // 4. Action Footer: Signature `SLICE PLATE` & `PRINT VIA KLIPPER` Buttons (HTML lines 551-562)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x14FFFFFF))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Primary Button: `SLICE PLATE` (or `IMPORT MODEL` when plate is empty)
            val hasObjects = state.sceneObjects.isNotEmpty()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(OrcaTeal, OrcaTealBright)
                        )
                    )
                    .clickable(enabled = !state.isSlicing) {
                        if (hasObjects) {
                            viewModel.sliceBuildPlate()
                        } else {
                            onImportClick()
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (hasObjects) Icons.Default.Layers else Icons.Default.FileOpen,
                    contentDescription = null,
                    tint = Color(0xFF06131C),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when {
                        state.isSlicing -> "SLICING..."
                        !hasObjects -> "IMPORT MODEL"
                        state.sliceResult != null && state.activeWorkspaceTab == WorkspaceTab.PREVIEW -> "RE-SLICE"
                        else -> "SLICE PLATE"
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                    color = Color(0xFF06131C),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Right Secondary Button: `PRINT VIA KLIPPER` / `EXPORT` (`#printDirectButton`)
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaActiveBtnBorder, RoundedCornerShape(10.dp))
                        .clickable { showPrintExportMenu = true }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = OrcaTealBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "PRINT / EXPORT",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = OrcaTealBright,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                DropdownMenu(
                    expanded = showPrintExportMenu,
                    onDismissRequest = { showPrintExportMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = EmeraldGreen) },
                        text = { Text("Print via Klipper / Moonraker (Upload & Start)") },
                        onClick = {
                            showPrintExportMenu = false
                            viewModel.sendGCodeToNetworkPrinter(startImmediately = true)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = OrcaTealBright) },
                        text = { Text("Upload G-Code to Klipper Queue") },
                        onClick = {
                            showPrintExportMenu = false
                            viewModel.sendGCodeToNetworkPrinter(startImmediately = false)
                        }
                    )
                    HorizontalDivider()
                    ExportFormat.entries.forEach { fmt ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = OrcaTealBright) },
                            text = { Text(fmt.label) },
                            onClick = {
                                showPrintExportMenu = false
                                onExportClick(fmt)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick-access Reference Cards from `mobile_3d_slicer_ui.html` (lines 400-549) shown at the top of each process category.
 */
@Composable
private fun QuickReferenceParameterCards(
    state: OrcaUiState,
    viewModel: OrcaViewModel
) {
    val proc = state.currentProcess
    when (state.activeProcessTab) {
        ProcessCategoryTab.QUALITY -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Layer Height Presets (`0.08`, `0.12`, `0.16`, `0.20`, `0.28`)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Layer Height Presets", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = OrcaTextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.2f mm", proc.layerHeightMm),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTealBright
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.08f, 0.12f, 0.16f, 0.20f, 0.28f).forEach { lh ->
                            val isSel = kotlin.math.abs(proc.layerHeightMm - lh) < 0.005f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) OrcaActiveBtnBg else OrcaCardBg)
                                    .border(
                                        1.dp,
                                        if (isSel) OrcaActiveBtnBorder else Color(0x0FFFFFFF),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateProcess { it.copy(layerHeightMm = lh) } }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", lh),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) OrcaTealBright else OrcaTextPrimary
                                )
                            }
                        }
                    }
                }

                // Signature OrcaSlicer Feature: Scarf Joint Seam Card (HTML lines 418-427)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrcaCardBg)
                        .border(1.dp, OrcaTealBright.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Scarf Joint Seam",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OrcaActiveBtnBg)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ORCA SIGNATURE",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OrcaTealBright
                                )
                            }
                        }
                        Text(
                            text = "Smooth ramped start/end seam hiding (${proc.scarfJointSeam.label})",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OrcaTextSecondary
                        )
                    }
                    Checkbox(
                        checked = proc.scarfJointSeam != ScarfSeamType.NONE,
                        onCheckedChange = { enabled ->
                            viewModel.updateProcess {
                                it.copy(scarfJointSeam = if (enabled) ScarfSeamType.CONTOUR else ScarfSeamType.NONE)
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = OrcaTealBright,
                            checkmarkColor = Color(0xFF06131C)
                        )
                    )
                }
            }
        }

        ProcessCategoryTab.STRENGTH -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sparse Infill Density Slider Card (HTML lines 450-456)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrcaCardBg)
                        .border(1.dp, OrcaGlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sparse Infill Density", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = OrcaTextPrimary)
                        Text(
                            text = "${proc.sparseInfillDensityPercent}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTealBright
                        )
                    }
                    Slider(
                        value = proc.sparseInfillDensityPercent.toFloat(),
                        onValueChange = { v -> viewModel.updateProcess { it.copy(sparseInfillDensityPercent = v.roundToInt()) } },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = OrcaTealBright,
                            activeTrackColor = OrcaTeal,
                            inactiveTrackColor = OrcaElevatedBg
                        ),
                        modifier = Modifier.height(22.dp)
                    )
                }

                // Infill Pattern Quick Buttons (`Gyroid`, `CrossHatch`, `Grid`, `Honeycomb`)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrcaCardBg)
                        .border(1.dp, OrcaGlassBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Quick Infill Pattern", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = OrcaTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            InfillPattern.GYROID to "Gyroid",
                            InfillPattern.CROSS_HATCH to "CrossHatch",
                            InfillPattern.GRID to "Grid",
                            InfillPattern.ADAPTIVE_CUBIC to "Adaptive"
                        ).forEach { (pat, label) ->
                            val isSel = proc.sparseInfillPattern == pat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) OrcaActiveBtnBg else OrcaInputBg)
                                    .border(
                                        1.dp,
                                        if (isSel) OrcaActiveBtnBorder else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.updateProcess { it.copy(sparseInfillPattern = pat) } }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) OrcaTealBright else OrcaTextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        ProcessCategoryTab.SPEED -> {
            // 2x2 Speed & Combing Quick Summary Grid (HTML lines 484-505)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeedSummaryTile(
                        modifier = Modifier.weight(1f),
                        label = "Outer Wall Speed",
                        value = "${proc.outerWallSpeedMmS.toInt()} mm/s"
                    )
                    SpeedSummaryTile(
                        modifier = Modifier.weight(1f),
                        label = "Inner Wall Speed",
                        value = "${proc.innerWallSpeedMmS.toInt()} mm/s"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeedSummaryTile(
                        modifier = Modifier.weight(1f),
                        label = "Travel Speed",
                        value = "${proc.travelSpeedMmS.toInt()} mm/s",
                        highlight = true
                    )
                    SpeedSummaryTile(
                        modifier = Modifier.weight(1f),
                        label = "Cura Combing Mode",
                        value = proc.combingMode.label,
                        highlight = true
                    )
                }
            }
        }

        ProcessCategoryTab.SUPPORT -> {
            // Support Quick Card (HTML lines 509-533)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrcaCardBg)
                    .border(1.dp, OrcaGlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Support (${proc.supportType.label} • ${proc.supportStyle.label})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (proc.supportOnBuildPlateOnly) "On build plate only • Threshold ${proc.supportThresholdAngleDeg}°"
                        else "Everywhere • Threshold ${proc.supportThresholdAngleDeg}°",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OrcaTextSecondary
                    )
                }
                Checkbox(
                    checked = proc.enableSupport,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(enableSupport = b) } },
                    colors = CheckboxDefaults.colors(
                        checkedColor = OrcaTealBright,
                        checkmarkColor = Color(0xFF06131C)
                    )
                )
            }
        }

        ProcessCategoryTab.OTHERS -> {
            // Bed Adhesion (Brim) Quick Card (HTML lines 536-548)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrcaCardBg)
                    .border(1.dp, OrcaGlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bed Adhesion (Brim)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Brim type: ${proc.brimType.label} (${proc.brimWidthMm}mm)",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OrcaTextSecondary
                    )
                }
                Checkbox(
                    checked = proc.brimType != BrimType.NONE,
                    onCheckedChange = { enabled ->
                        viewModel.updateProcess {
                            it.copy(brimType = if (enabled) BrimType.AUTO else BrimType.NONE)
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = OrcaTealBright,
                        checkmarkColor = Color(0xFF06131C)
                    )
                )
            }
        }

        ProcessCategoryTab.MULTIMATERIAL -> {}
    }
}

@Composable
private fun SpeedSummaryTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(OrcaCardBg)
            .border(1.dp, OrcaGlassBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = OrcaTextSecondary)
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (highlight) OrcaTealBright else Color.White
        )
    }
}

/**
 * Calibration Selection Modal matching `#calibrationModal` in `mobile_3d_slicer_ui.html` (lines 570-617).
 */
@Composable
private fun CalibrationBottomSheetModal(
    onSelectTest: (CalibrationTestType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(OrcaPanelBg)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = OrcaTealBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "DOLPHINSLICER CALIBRATION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OrcaTextSecondary)
                    }
                }

                HorizontalDivider(color = OrcaGlassBorder)

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalibrationTestType.entries.forEach { test ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(OrcaCardBg)
                                .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                                .clickable { onSelectTest(test) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = test.title,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = test.description,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OrcaTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Launch",
                                tint = OrcaTealBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaGlassBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DISMISS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = OrcaTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewportMiniBtn(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) OrcaTeal else OrcaCardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color(0xFF041F18) else OrcaTextPrimary
        )
    }
}

@Composable
private fun FloatingGizmoControlCard(
    modifier: Modifier = Modifier,
    state: OrcaUiState,
    selected: com.orcaslicer.android.model.SceneObject,
    viewModel: OrcaViewModel
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 235.dp)
            .border(1.dp, OrcaTealBright.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OrcaGlassBg)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${state.activeGizmoMode.title.uppercase()} — ${selected.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrcaTealBright
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OrcaActiveBtnBg)
                            .border(1.dp, OrcaActiveBtnBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "GLOBAL XYZ",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTealBright
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.setFloatingGizmoSheetVisible(false) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Gizmo", tint = OrcaTextSecondary)
                }
            }

            when (state.activeGizmoMode) {
                TransformGizmoMode.MOVE -> {
                    val halfX = state.selectedPrinter.bedWidthX * 0.5f
                    val halfY = state.selectedPrinter.bedDepthY * 0.5f
                    TouchNumberSetting(
                        label = "Position X",
                        valueText = String.format(Locale.US, "%.1f mm", selected.position.x),
                        value = selected.position.x,
                        range = -halfX..halfX,
                        step = 2.0f,
                        onValueChange = { x ->
                            viewModel.updateSelectedObject { it.copy(position = it.position.copy(x = x)) }
                        }
                    )
                    TouchNumberSetting(
                        label = "Position Y",
                        valueText = String.format(Locale.US, "%.1f mm", selected.position.y),
                        value = selected.position.y,
                        range = -halfY..halfY,
                        step = 2.0f,
                        onValueChange = { y ->
                            viewModel.updateSelectedObject { it.copy(position = it.position.copy(y = y)) }
                        }
                    )
                }

                TransformGizmoMode.SCALE -> {
                    val uniformPct = selected.scale.x * 100f
                    TouchNumberSetting(
                        label = "Uniform Scale",
                        valueText = String.format(Locale.US, "%.0f%%", uniformPct),
                        value = uniformPct,
                        range = 10f..400f,
                        step = 5f,
                        onValueChange = { pct ->
                            val s = (pct / 100f).coerceIn(0.1f, 5.0f)
                            viewModel.updateSelectedObject { it.copy(scale = Vec3(s, s, s)) }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f to "50%", 0.75f to "75%", 1.0f to "100%", 1.25f to "125%", 1.5f to "150%", 2.0f to "200%").forEach { (s, label) ->
                            ViewportMiniBtn(label) {
                                viewModel.updateSelectedObject { it.copy(scale = Vec3(s, s, s)) }
                            }
                        }
                    }
                }

                TransformGizmoMode.ROTATE -> {
                    TouchNumberSetting(
                        label = "Rotate X (Pitch)",
                        valueText = "${selected.rotationDeg.x.toInt()}°",
                        value = selected.rotationDeg.x,
                        range = -180f..180f,
                        step = 15f,
                        onValueChange = { rx ->
                            viewModel.updateSelectedObject { it.copy(rotationDeg = it.rotationDeg.copy(x = rx)) }
                        }
                    )
                    TouchNumberSetting(
                        label = "Rotate Y (Roll)",
                        valueText = "${selected.rotationDeg.y.toInt()}°",
                        value = selected.rotationDeg.y,
                        range = -180f..180f,
                        step = 15f,
                        onValueChange = { ry ->
                            viewModel.updateSelectedObject { it.copy(rotationDeg = it.rotationDeg.copy(y = ry)) }
                        }
                    )
                    TouchNumberSetting(
                        label = "Rotate Z (Yaw)",
                        valueText = "${selected.rotationDeg.z.toInt()}°",
                        value = selected.rotationDeg.z,
                        range = -180f..180f,
                        step = 15f,
                        onValueChange = { rz ->
                            viewModel.updateSelectedObject { it.copy(rotationDeg = it.rotationDeg.copy(z = rz)) }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ViewportMiniBtn("Reset 0°") {
                            viewModel.updateSelectedObject { it.copy(rotationDeg = Vec3(0f, 0f, 0f)) }
                        }
                        ViewportMiniBtn("X +90°") {
                            viewModel.updateSelectedObject { it.copy(rotationDeg = it.rotationDeg.copy(x = (it.rotationDeg.x + 90f) % 360f)) }
                        }
                        ViewportMiniBtn("Y +90°") {
                            viewModel.updateSelectedObject { it.copy(rotationDeg = it.rotationDeg.copy(y = (it.rotationDeg.y + 90f) % 360f)) }
                        }
                        ViewportMiniBtn("Z +90°") {
                            viewModel.updateSelectedObject { it.copy(rotationDeg = it.rotationDeg.copy(z = (it.rotationDeg.z + 90f) % 360f)) }
                        }
                    }
                }

                TransformGizmoMode.CUT -> {
                    val objHeight = selected.worldBoundingBox().size.z.coerceAtLeast(2f)
                    val safeMaxCut = (objHeight - 0.5f).coerceAtLeast(1.0f)
                    val clampedCutZ = state.cutPlaneHeightMm.coerceIn(0.5f, safeMaxCut)
                    Text(
                        text = "Drag the blue vertical arrow in the 3D viewport or adjust the cut plane height below:",
                        fontSize = 10.sp,
                        color = OrcaTextSecondary
                    )
                    TouchNumberSetting(
                        label = "Cut Plane Height Z (World)",
                        valueText = String.format(Locale.US, "%.2f / %.1f mm", clampedCutZ, objHeight),
                        value = clampedCutZ,
                        range = 0.5f..safeMaxCut,
                        step = 0.5f,
                        onValueChange = viewModel::updateCutPlaneHeight
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ViewportMiniBtn("25%") { viewModel.updateCutPlaneHeight(objHeight * 0.25f) }
                        ViewportMiniBtn("50% (Center)") { viewModel.updateCutPlaneHeight(objHeight * 0.50f) }
                        ViewportMiniBtn("75%") { viewModel.updateCutPlaneHeight(objHeight * 0.75f) }
                    }
                    // OrcaSlicer Cut Options: Keep Upper, Keep Lower, Place on Cut Face
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ViewportMiniBtn(
                            label = if (state.cutKeepUpper) "✓ Keep Upper" else "Keep Upper",
                            active = state.cutKeepUpper,
                            onClick = { viewModel.updateCutOptions(keepUpper = !state.cutKeepUpper) }
                        )
                        ViewportMiniBtn(
                            label = if (state.cutKeepLower) "✓ Keep Lower" else "Keep Lower",
                            active = state.cutKeepLower,
                            onClick = { viewModel.updateCutOptions(keepLower = !state.cutKeepLower) }
                        )
                        ViewportMiniBtn(
                            label = if (state.cutPlaceOnCutFace) "✓ Place on Cut" else "Place on Cut",
                            active = state.cutPlaceOnCutFace,
                            onClick = { viewModel.updateCutOptions(placeOnCutFace = !state.cutPlaceOnCutFace) }
                        )
                    }
                    Button(
                        onClick = { viewModel.cutSelectedObjectAtZ(clampedCutZ) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrcaTealBright, contentColor = Color(0xFF041F18)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Perform Cut", fontWeight = FontWeight.Bold)
                    }
                }

                TransformGizmoMode.LAY_ON_FACE -> {
                    val candidates = selected.mesh.layOnFaceCandidates
                    Text(
                        text = "Tap any highlighted white/cyan face patch directly on the 3D model in the viewport to place that face flat on the build plate (Z = 0):",
                        fontSize = 10.sp,
                        color = OrcaTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = viewModel::autoOrientSelectedObjectLargestFace,
                            colors = ButtonDefaults.buttonColors(containerColor = OrcaTealBright, contentColor = Color(0xFF041F18)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auto-Orient (Largest Face)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = viewModel::laySelectedObjectFlat
                        ) {
                            Text("Snap 90°", fontSize = 11.sp)
                        }
                    }
                    if (candidates.isNotEmpty()) {
                        Text(
                            text = "Detected Flat Exterior Faces (${candidates.size}):",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTextPrimary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            candidates.take(10).forEachIndexed { idx, cand ->
                                val worldN = selected.transformNormal(cand.localNormal)
                                val isCurrentBase = worldN.z < -0.97f
                                ViewportMiniBtn(
                                    label = "#${idx + 1} ${cand.label} (${cand.areaMm2.toInt()} mm²)",
                                    active = isCurrentBase,
                                    onClick = {
                                        viewModel.laySelectedObjectOnLocalNormal(
                                            cand.localNormal,
                                            "${cand.label} • ${cand.areaMm2.toInt()} mm²"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                TransformGizmoMode.MIRROR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.mirrorSelectedObject(mirrorX = true, mirrorY = false, mirrorZ = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrcaCardBg, contentColor = OrcaTextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mirror X")
                        }
                        Button(
                            onClick = { viewModel.mirrorSelectedObject(mirrorX = false, mirrorY = true, mirrorZ = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrcaCardBg, contentColor = OrcaTextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mirror Y")
                        }
                        Button(
                            onClick = { viewModel.mirrorSelectedObject(mirrorX = false, mirrorY = false, mirrorZ = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrcaCardBg, contentColor = OrcaTextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mirror Z")
                        }
                    }
                }

                TransformGizmoMode.MEASURE -> {
                    val wb = selected.worldBoundingBox().size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMetricItem("Size X", String.format(Locale.US, "%.2f mm", wb.x))
                        StatMetricItem("Size Y", String.format(Locale.US, "%.2f mm", wb.y))
                        StatMetricItem("Size Z", String.format(Locale.US, "%.2f mm", wb.z))
                        StatMetricItem("Triangles", "${selected.mesh.triangleCount}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjectListCard(
    state: OrcaUiState,
    viewModel: OrcaViewModel,
    onImportClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeaderRow(Icons.Default.ViewInAr, "Objects on Plate (${state.sceneObjects.size})")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ViewportMiniBtn("Import", onClick = onImportClick)
                    if (state.sceneObjects.isNotEmpty()) {
                        ViewportMiniBtn("Clone ×3") { viewModel.duplicateSelectedObject(3) }
                        ViewportMiniBtn("Arrange", onClick = viewModel::autoArrangeObjects)
                    }
                }
            }

            if (state.sceneObjects.isEmpty()) {
                Text(
                    text = "Build plate is empty. Tap 'Import' to load a 3MF, STL, OBJ, or G-code file.",
                    fontSize = 11.sp,
                    color = OrcaTextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            state.sceneObjects.forEach { obj ->
                val isSel = obj.id == state.selectedObjectId
                val dims = obj.worldBoundingBox().size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) OrcaActiveBtnBg else OrcaPanelBg)
                        .border(1.dp, if (isSel) OrcaTealBright else OrcaBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.selectObject(obj.id)
                            viewModel.setFloatingGizmoSheetVisible(true)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = obj.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1f × %.1f × %.1f mm • %d tris",
                                dims.x, dims.y, dims.z, obj.mesh.triangleCount
                            ),
                            fontSize = 10.sp,
                            color = OrcaTextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                viewModel.selectObject(obj.id)
                                viewModel.updateSelectedObject { it.copy(visible = !it.visible) }
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = if (obj.visible) OrcaTealBright else OrcaTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = obj.sourceFormat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTealBright
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrcaWhaleVectorIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.orcaslicer.android.R.drawable.ic_dolphin_logo),
        contentDescription = "DolphinSlicer Icon",
        modifier = modifier
    )
}

@Composable
private fun FilamentQuickCalculatorCard(
    state: OrcaUiState,
    viewModel: OrcaViewModel
) {
    val f = state.selectedFilament
    Card(
        colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeaderRow(Icons.Default.Analytics, "Filament Cost & Spool Calculator")
            Text(
                text = "Configure currency, spool price, and weight below, then tap 'Slice' to compute exact filament meters, weight (g), electricity cost, and quote price.",
                fontSize = 11.sp,
                color = OrcaTextSecondary
            )
            CustomCurrencySelectorSection(
                currentSymbol = f.currencySymbol,
                onCurrencyChange = { sym -> viewModel.setGlobalCurrencySymbol(sym) }
            )
            TouchNumberSetting(
                label = "Filament Price (${f.currencySymbol} / kg)",
                subtitle = String.format(
                    Locale.US,
                    "Cost/g: %s%.4f • %dg Spool: %s%.2f",
                    f.currencySymbol,
                    f.costPerGram,
                    f.spoolWeightGrams.toInt(),
                    f.currencySymbol,
                    f.spoolTotalCost
                ),
                valueText = String.format(Locale.US, "%s%.2f/kg", f.currencySymbol, f.costPerKgUsd),
                value = f.costPerKgUsd,
                range = 0f..1000000f,
                step = 1f,
                onValueChange = { v -> viewModel.updateFilament { it.copy(costPerKgUsd = v) } }
            )
        }
    }
}

@Composable
private fun SliceStatisticsSummaryCard(
    state: OrcaUiState,
    viewModel: OrcaViewModel
) {
    val slice = state.sliceResult ?: return
    val stats = slice.statistics
    val f = state.selectedFilament
    val cost = stats.calculateFullCostBreakdown(state.selectedPrinter, f)
    val cur = cost.currencySymbol

    Card(
        colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeaderRow(Icons.Default.Analytics, "Print Summary & Filament Cost Calculator")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetricItem("Print Time", stats.formattedTotalTime)
                StatMetricItem(
                    "Total Filament",
                    String.format(Locale.US, "%.2f m (%.1f g)", stats.filamentLengthMeters, stats.filamentWeightGrams)
                )
                StatMetricItem(
                    "Material Cost",
                    String.format(Locale.US, "%s%.2f", cur, cost.materialCost)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrcaPanelBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CostLineRow("Model Filament Weight", String.format(Locale.US, "%.2f g (%.2f cm³)", stats.modelWeightGrams, stats.filamentVolumeCm3))
                CostLineRow("Support & Brim Weight", String.format(Locale.US, "%.2f g", stats.supportWasteWeightGrams))
                CostLineRow(
                    "Spool Usage (${f.spoolWeightGrams.toInt()}g spool)",
                    String.format(Locale.US, "%.1f%% (~%d prints/spool)", cost.spoolUsagePercent, cost.printsPerSpool)
                )
                HorizontalDivider(color = OrcaBorder)
                CostLineRow(
                    "Filament Material (${cur}${String.format(Locale.US, "%.2f", f.costPerKgUsd)}/kg)",
                    String.format(Locale.US, "%s%.2f", cur, cost.materialCost)
                )
                CostLineRow(
                    "Electricity (${state.selectedPrinter.powerConsumptionWatts.toInt()}W • ${String.format(Locale.US, "%.3f", cost.kwhUsed)} kWh)",
                    String.format(Locale.US, "%s%.2f", cur, cost.electricityCost)
                )
                CostLineRow(
                    "Machine Wear / Depreciation",
                    String.format(Locale.US, "%s%.2f", cur, cost.machineWearCost)
                )
                HorizontalDivider(color = OrcaBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Production Cost", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrcaTextPrimary)
                    Text(
                        String.format(Locale.US, "%s%.2f", cur, cost.totalProductionCost),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrcaTealBright
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        String.format(Locale.US, "Suggested Quote Price (%.1f×)", f.markupMultiplier),
                        fontSize = 11.sp,
                        color = OrcaTextSecondary
                    )
                    Text(
                        String.format(Locale.US, "%s%.2f", cur, cost.suggestedQuotePrice),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            CustomCurrencySelectorSection(
                currentSymbol = f.currencySymbol,
                onCurrencyChange = { sym -> viewModel.setGlobalCurrencySymbol(sym) }
            )
            TouchNumberSetting(
                label = "Filament Price (${cur} / kg)",
                valueText = String.format(Locale.US, "%s%.2f/kg", cur, f.costPerKgUsd),
                value = f.costPerKgUsd,
                range = 0.0f..1000000.0f,
                step = 1.0f,
                onValueChange = { v -> viewModel.updateFilament { it.copy(costPerKgUsd = v) } }
            )

            HorizontalDivider(color = OrcaBorder)

            ToolpathType.entries.forEach { t ->
                val sec = stats.featureTimeSeconds[t] ?: 0f
                val pct = if (stats.totalTimeSeconds > 0f) (sec / stats.totalTimeSeconds) * 100f else 0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(t.r, t.g, t.b))
                        )
                        Text(t.displayName, fontSize = 11.sp, color = OrcaTextPrimary)
                    }
                    Text(
                        text = String.format(Locale.US, "%.0fs (%.0f%%)", sec, pct),
                        fontSize = 11.sp,
                        color = OrcaTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CostLineRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = OrcaTextSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OrcaTextPrimary)
    }
}

@Composable
private fun StatMetricItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = OrcaTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrcaTextPrimary)
    }
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DeviceNetworkPanel(
    state: OrcaUiState,
    viewModel: OrcaViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showApiDrawer by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasNavigatedUrl by remember { mutableStateOf(state.printerHost.trim() != "192.168.1.100" && state.printerHost.isNotBlank()) }
    var pageLoadError by remember { mutableStateOf<String?>(null) }

    var addressBarText by remember {
        val initHost = state.printerHost.trim().ifEmpty { "192.168.1.100" }
        mutableStateOf(
            if (initHost.startsWith("http://", ignoreCase = true) || initHost.startsWith("https://", ignoreCase = true)) {
                initHost
            } else {
                "http://$initHost"
            }
        )
    }

    fun navigateToAddress(rawInput: String) {
        focusManager.clearFocus()
        val trimmed = rawInput.trim().ifEmpty { "192.168.1.100" }
        val normalizedUrl = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "http://$trimmed"
        }
        addressBarText = normalizedUrl
        val hostOnly = normalizedUrl.substringAfter("://").substringBefore("/").substringBefore(":")
        if (hostOnly.isNotBlank()) {
            viewModel.updatePrinterNetworkConfig(host = hostOnly)
        }
        pageLoadError = null
        hasNavigatedUrl = true
        isLoadingPage = true
        loadProgress = 12
        webViewRef?.stopLoading()
        webViewRef?.loadUrl(normalizedUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrcaDarkBg)
    ) {
        // 1. Web Browser Navigation & Printer IP / URL Address Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrcaTopBarBg)
                .border(1.dp, OrcaBorder)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        if (webViewRef?.canGoBack() == true) webViewRef?.goBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OrcaTextPrimary)
            }

            // Forward button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        if (webViewRef?.canGoForward() == true) webViewRef?.goForward()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("→", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OrcaTextSecondary)
            }

            // Reload button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                    .clickable { webViewRef?.reload() },
                contentAlignment = Alignment.Center
            ) {
                Text("↻", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OrcaTealBright)
            }

            // Browser IP / URL Input Box
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaTealBright.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = OrcaTealBright,
                    modifier = Modifier.size(13.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = addressBarText,
                    onValueChange = { addressBarText = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Go
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onGo = { navigateToAddress(addressBarText) }
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(OrcaTealBright),
                    modifier = Modifier.weight(1f)
                )
                if (addressBarText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(OrcaElevatedBg)
                            .clickable { addressBarText = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear URL",
                            tint = OrcaTextSecondary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // GO button
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaTealBright)
                    .clickable { navigateToAddress(addressBarText) }
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("GO", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF04141E))
            }

            // Quick Upload G-Code Pill
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaTealBright.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .clickable(enabled = !state.isNetworkBusy) {
                        viewModel.sendGCodeToNetworkPrinter(startImmediately = false)
                    }
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = OrcaTealBright, modifier = Modifier.size(11.dp))
                    Text("Send G-Code", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = OrcaTealBright, maxLines = 1, softWrap = false)
                }
            }

            // Toggle Moonraker API Config Drawer
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showApiDrawer) OrcaActiveBtnBg else OrcaInputBg)
                    .border(1.dp, if (showApiDrawer) OrcaTealBright else OrcaBorder, RoundedCornerShape(6.dp))
                    .clickable { showApiDrawer = !showApiDrawer },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tune, contentDescription = "API Settings", tint = OrcaTealBright, modifier = Modifier.size(14.dp))
            }
        }

        if (isLoadingPage) {
            LinearProgressIndicator(
                progress = { (loadProgress / 100f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = OrcaTealBright,
                trackColor = OrcaDarkBg
            )
        }

        // 2. Optional Collapsible Moonraker / OctoPrint API & Direct Upload Drawer
        AnimatedVisibility(visible = showApiDrawer) {
            Card(
                colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OrcaBorder)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SectionHeaderRow(Icons.Default.Router, "Moonraker / Klipper Direct API & Telemetry")
                    TouchEnumSelector(
                        label = "Printer API Protocol",
                        options = NetworkProtocol.entries,
                        selected = state.printerProtocol,
                        optionLabel = { it.label },
                        onSelect = { proto ->
                            viewModel.updatePrinterNetworkConfig(
                                protocol = proto,
                                port = proto.defaultPort.toString()
                            )
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = state.printerHost,
                            onValueChange = {
                                viewModel.updatePrinterNetworkConfig(host = it)
                                addressBarText = "http://${it.trim()}"
                            },
                            label = { Text("Klipper IP / Hostname", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.68f)
                        )
                        OutlinedTextField(
                            value = state.printerPort,
                            onValueChange = { viewModel.updatePrinterNetworkConfig(port = it) },
                            label = { Text("API Port", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.32f)
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = viewModel::testPrinterConnection,
                            enabled = !state.isNetworkBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrcaTealBright,
                                contentColor = Color(0xFF04141E)
                            )
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Query Status", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.sendGCodeToNetworkPrinter(startImmediately = false) },
                            enabled = !state.isNetworkBusy
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload G-Code", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.sendGCodeToNetworkPrinter(startImmediately = true) },
                            enabled = !state.isNetworkBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrcaTeal,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload & Start Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    state.printerTelemetry?.let { tel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetricItem("State", tel.stateText)
                            StatMetricItem("Nozzle", String.format(Locale.US, "%.1f°C / %.0f°C", tel.nozzleTempC, tel.nozzleTargetC))
                            StatMetricItem("Bed", String.format(Locale.US, "%.1f°C / %.0f°C", tel.bedTempC, tel.bedTargetC))
                            StatMetricItem("Progress", "${tel.progressPercent}%")
                        }
                    }
                }
            }
        }

        // 3. Full-Screen Web Browser View (Fluidd / Mainsail / Printer IP) with Start & Error Hub so screen is never blank
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(OrcaDarkBg)
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = true
                        isFocusableInTouchMode = true
                        setBackgroundColor(android.graphics.Color.parseColor("#16181D"))
                        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            allowFileAccess = true
                            allowContentAccess = true
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportZoom(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                return false
                            }

                            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoadingPage = true
                                if (!url.isNullOrBlank() && url != "about:blank") {
                                    addressBarText = url
                                    hasNavigatedUrl = true
                                }
                            }

                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                isLoadingPage = false
                            }

                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoadingPage = false
                                    val desc = error?.description?.toString() ?: "Unable to connect to printer host"
                                    val failedUrl = request.url?.toString() ?: addressBarText
                                    pageLoadError = "$desc ($failedUrl)"
                                }
                            }

                            @android.annotation.SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(
                                view: android.webkit.WebView?,
                                handler: android.webkit.SslErrorHandler?,
                                error: android.net.http.SslError?
                            ) {
                                // Proceed on self-signed HTTPS certificates commonly used on local Klipper / Mainsail / OctoPrint hosts
                                handler?.proceed()
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                if (newProgress >= 100) isLoadingPage = false
                            }
                        }
                        webViewRef = this
                        if (hasNavigatedUrl) {
                            loadUrl(addressBarText)
                        }
                    }
                },
                update = { view ->
                    webViewRef = view
                }
            )

            // Interactive Browser Start & Diagnostic Overlay (shown before first navigation or when host is unreachable)
            if (!hasNavigatedUrl || pageLoadError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OrcaDarkBg.copy(alpha = 0.96f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .width(460.dp)
                            .border(1.dp, OrcaBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = OrcaTealBright,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Klipper / Fluidd / Mainsail Web Browser",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Enter your 3D printer's local IP address or hostname above and tap GO.",
                                        fontSize = 11.sp,
                                        color = OrcaTextSecondary
                                    )
                                }
                            }

                            if (pageLoadError != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x26EF5350))
                                        .border(1.dp, Color(0x88EF5350), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Could not reach printer web UI",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8A80)
                                    )
                                    Text(
                                        text = pageLoadError ?: "",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = OrcaTextSecondary
                                    )
                                    Text(
                                        text = "Make sure your phone/tablet is on the same Wi-Fi LAN as your Klipper printer.",
                                        fontSize = 10.sp,
                                        color = OrcaTextSecondary
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = addressBarText,
                                onValueChange = { addressBarText = it },
                                label = { Text("Printer IP / URL (e.g. http://192.168.1.50 or http://fluidd.local)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { navigateToAddress(addressBarText) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OrcaTealBright,
                                        contentColor = Color(0xFF04141E)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (pageLoadError != null) "Retry Connection" else "Open Printer Web UI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            val target = if (addressBarText.startsWith("http")) addressBarText else "http://$addressBarText"
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(target)
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Text("External Browser", fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "Quick Bookmarks & Endpoints:",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrcaTextPrimary
                            )

                            val cleanHost = state.printerHost.trim().ifEmpty { "192.168.1.100" }
                                .substringAfter("://")
                                .substringBefore("/")
                                .substringBefore(":")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Fluidd / Mainsail (:80)" to "http://$cleanHost",
                                    "Moonraker (:7125)" to "http://$cleanHost:7125",
                                    "fluidd.local" to "http://fluidd.local",
                                    "mainsailos.local" to "http://mainsailos.local",
                                    "OctoPrint (:5000)" to "http://$cleanHost:5000",
                                    "Test Web (Google)" to "https://www.google.com"
                                ).forEach { (title, url) ->
                                    ViewportMiniBtn(
                                        label = title,
                                        active = addressBarText.equals(url, ignoreCase = true),
                                        onClick = { navigateToAddress(url) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
