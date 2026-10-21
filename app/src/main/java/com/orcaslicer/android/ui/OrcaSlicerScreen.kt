package com.orcaslicer.android.ui

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orcaslicer.android.gpu.Orca3DViewport
import com.orcaslicer.android.gpu.ViewportCameraPreset
import com.orcaslicer.android.model.ToolpathType
import com.orcaslicer.android.model.Vec3
import com.orcaslicer.android.network.NetworkProtocol
import java.util.Locale

internal val OrcaActiveTabBg = Color(0xFF00796B)

private val OrcaColorScheme = darkColorScheme(
    primary = OrcaTeal,
    onPrimary = Color(0xFF041F18),
    secondary = Color(0xFF38BDF8),
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
        val baseName = state.selectedObject?.name?.replace(" ", "_") ?: "orca_plate"
        exportLauncher.launch("$baseName.${format.extension}")
    }

    OrcaSlicerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = OrcaDarkBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1:1 OrcaSlicer Top Navigation & Action Bar (#1F2022)
                OrcaTopMenuAndActionBar(
                    state = state,
                    onSelectWorkspaceTab = viewModel::selectWorkspaceTab,
                    onImportClick = triggerImport,
                    onSyncServerProfiles = viewModel::syncProfilesFromOrcaServer,
                    onAddPrimitive = viewModel::addBuiltInPrimitive,
                    onLaunchCalibration = viewModel::launchCalibrationTest,
                    onClearPlate = viewModel::clearBuildPlate,
                    onSliceClick = viewModel::sliceBuildPlate,
                    onExportClick = triggerExport,
                    onOpenPrinterSettings = { viewModel.setPrinterSettingsDialogVisible(true) },
                    onOpenFilamentSettings = { viewModel.setFilamentSettingsDialogVisible(true) }
                )

                if (state.isSlicing || state.isSyncingServerProfiles) {
                    LinearProgressIndicator(
                        progress = { if (state.isSlicing) state.sliceProgress else 0.65f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = OrcaTeal,
                        trackColor = OrcaCardBg
                    )
                }

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
                                // LANDSCAPE: 1:1 OrcaSlicer Left Sidebar (#262729, 365dp) + Right 3D GPU Viewport (#38393B)
                                Row(modifier = Modifier.fillMaxSize()) {
                                    AnimatedVisibility(visible = state.isDockExpanded) {
                                        Row(modifier = Modifier.fillMaxHeight()) {
                                            Column(
                                                modifier = Modifier
                                                    .width(365.dp)
                                                    .fillMaxHeight()
                                                    .background(OrcaPanelBg)
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                FullPresetSelectorsCard(
                                                    state = state,
                                                    viewModel = viewModel,
                                                    onImportProfileClick = triggerImport
                                                )
                                                FullProcessSettingsCard(state = state, viewModel = viewModel)
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    ObjectListCard(state = state, viewModel = viewModel)
                                                    if (state.sliceResult != null) {
                                                        SliceStatisticsSummaryCard(state = state, viewModel = viewModel)
                                                    } else {
                                                        FilamentQuickCalculatorCard(state = state, viewModel = viewModel)
                                                    }
                                                }
                                            }
                                            // 1dp vertical separator between left sidebar and 3D viewport
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF18191B))
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
                                            isLandscape = true
                                        )
                                    }
                                }
                            } else {
                                // PORTRAIT: Upper 3D Viewport + 1:1 OrcaSlicer Bottom Panel
                                var portraitTab by remember { mutableIntStateOf(0) }
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(if (state.isDockExpanded) 0.52f else 1.0f)
                                            .fillMaxWidth()
                                    ) {
                                        ViewportWithTouchOverlays(
                                            state = state,
                                            viewModel = viewModel,
                                            isLandscape = false
                                        )
                                    }

                                    // Touch-friendly OrcaSlicer Bottom Panel
                                    Column(
                                        modifier = Modifier
                                            .then(
                                                if (state.isDockExpanded) Modifier.weight(0.48f)
                                                else Modifier.height(42.dp)
                                            )
                                            .fillMaxWidth()
                                            .background(OrcaPanelBg)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(OrcaDarkBg)
                                                .border(1.dp, OrcaBorder)
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val tabSpecs = listOf(
                                                Triple(0, "Sidebar (1:1)", Icons.Default.Tune),
                                                Triple(1, "Presets", Icons.Default.Print),
                                                Triple(2, "Objects (${state.sceneObjects.size})", Icons.Default.ViewInAr),
                                                Triple(3, "Cost/Stats", Icons.Default.Analytics)
                                            )
                                            tabSpecs.forEach { (idx, label, icon) ->
                                                val active = state.isDockExpanded && portraitTab == idx
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (active) OrcaActiveTabBg else OrcaPanelBg)
                                                        .clickable {
                                                            portraitTab = idx
                                                            if (!state.isDockExpanded) {
                                                                viewModel.toggleDockExpanded()
                                                            }
                                                        }
                                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = label,
                                                        tint = if (active) Color.White else OrcaTeal,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (active) Color.White else OrcaTextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            // Collapse / Expand Bottom Sheet button
                                            IconButton(
                                                onClick = viewModel::toggleDockExpanded,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(OrcaElevatedBg)
                                            ) {
                                                Icon(
                                                    imageVector = if (state.isDockExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                    contentDescription = "Toggle Bottom Drawer",
                                                    tint = Color.White
                                                )
                                            }
                                        }

                                        if (state.isDockExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                when (portraitTab) {
                                                    0 -> {
                                                        FullPresetSelectorsCard(
                                                            state = state,
                                                            viewModel = viewModel,
                                                            onImportProfileClick = triggerImport
                                                        )
                                                        FullProcessSettingsCard(state = state, viewModel = viewModel)
                                                    }
                                                    1 -> FullPresetSelectorsCard(
                                                        state = state,
                                                        viewModel = viewModel,
                                                        onImportProfileClick = triggerImport
                                                    )
                                                    2 -> Box(Modifier.padding(10.dp)) {
                                                        ObjectListCard(state = state, viewModel = viewModel)
                                                    }
                                                    3 -> Box(Modifier.padding(10.dp)) {
                                                        if (state.sliceResult != null) {
                                                            SliceStatisticsSummaryCard(state = state, viewModel = viewModel)
                                                        } else {
                                                            FilamentQuickCalculatorCard(state = state, viewModel = viewModel)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Compact Bottom Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF18191B))
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.statusBannerMessage,
                        fontSize = 11.sp,
                        color = OrcaTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DolphinSlicer • ${state.printers.size} Printers • ${state.filaments.size} Filaments • ${state.selectedPrinter.bedWidthX.toInt()}×${state.selectedPrinter.bedDepthY.toInt()}×${state.selectedPrinter.maxPrintHeightZ.toInt()}mm",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrcaTeal
                    )
                }
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

@Composable
private fun OrcaTopMenuAndActionBar(
    state: OrcaUiState,
    onSelectWorkspaceTab: (WorkspaceTab) -> Unit,
    onImportClick: () -> Unit,
    onSyncServerProfiles: () -> Unit,
    onAddPrimitive: (BuiltInPrimitiveType) -> Unit,
    onLaunchCalibration: (CalibrationTestType) -> Unit,
    onClearPlate: () -> Unit,
    onSliceClick: () -> Unit,
    onExportClick: (ExportFormat) -> Unit,
    onOpenPrinterSettings: () -> Unit,
    onOpenFilamentSettings: () -> Unit
) {
    var showMainMenu by remember { mutableStateOf(false) }
    var showPrimitiveMenu by remember { mutableStateOf(false) }
    var showCalibrationMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    // 1:1 OrcaSlicer Top Navigation Bar (#1F2022 background, 38dp height)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(OrcaDarkBg)
            .border(1.dp, Color(0xFF151618))
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LEFT: Home Icon + Menu Chevron + Divider + Prepare | Preview | Device | Project Tabs
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Button (🏠)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onSelectWorkspaceTab(WorkspaceTab.PREPARE) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = OrcaTextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Down-Chevron Main Menu Button (⌄)
            Box(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { showMainMenu = true }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Main Menu",
                        tint = OrcaTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(expanded = showMainMenu, onDismissRequest = { showMainMenu = false }) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Import 3MF / STL / OBJ / G-code...") },
                        onClick = {
                            showMainMenu = false
                            onImportClick()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Import All Printer & Filament Profiles from Orca Server") },
                        onClick = {
                            showMainMenu = false
                            onSyncServerProfiles()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Import Custom Profile (.json / .ini / .orca_printer)...") },
                        onClick = {
                            showMainMenu = false
                            onImportClick()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Add Primitive (Cube, Cylinder, Benchy, Calibration)...") },
                        onClick = {
                            showMainMenu = false
                            showPrimitiveMenu = true
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Calibration Suite (Temp Tower, PA, Flow, Max Speed)...") },
                        onClick = {
                            showMainMenu = false
                            showCalibrationMenu = true
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Save Project (.3mf)") },
                        onClick = {
                            showMainMenu = false
                            onExportClick(ExportFormat.PROJECT_3MF)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.IosShare, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Export G-code (.gcode)") },
                        onClick = {
                            showMainMenu = false
                            onExportClick(ExportFormat.GCODE)
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.ViewInAr, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Export Plate Mesh (.stl)") },
                        onClick = {
                            showMainMenu = false
                            onExportClick(ExportFormat.BINARY_STL)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Printer & Extruder Settings...") },
                        onClick = {
                            showMainMenu = false
                            onOpenPrinterSettings()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = OrcaTeal) },
                        text = { Text("Filament & Price Calculator Settings...") },
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
                }

                // Sub-dropdown for Add Primitive
                DropdownMenu(expanded = showPrimitiveMenu, onDismissRequest = { showPrimitiveMenu = false }) {
                    BuiltInPrimitiveType.entries.forEach { prim ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.ViewInAr, contentDescription = null, tint = OrcaTeal) },
                            text = { Text(prim.title) },
                            onClick = {
                                showPrimitiveMenu = false
                                onAddPrimitive(prim)
                            }
                        )
                    }
                }

                // Sub-dropdown for Calibration
                DropdownMenu(expanded = showCalibrationMenu, onDismissRequest = { showCalibrationMenu = false }) {
                    CalibrationTestType.entries.forEach { test ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, tint = OrcaTeal) },
                            text = { Text(test.title) },
                            onClick = {
                                showCalibrationMenu = false
                                onLaunchCalibration(test)
                            }
                        )
                    }
                }
            }

            // Vertical Separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(OrcaBorder)
            )

            // 1:1 WORKSPACE TABS: Prepare | Preview | Device | Project
            val tabIcons = mapOf(
                WorkspaceTab.PREPARE to Icons.Default.ViewInAr,
                WorkspaceTab.PREVIEW to Icons.Default.Layers,
                WorkspaceTab.DEVICE to Icons.Default.DesktopWindows,
                WorkspaceTab.PROJECT to Icons.AutoMirrored.Filled.FormatListBulleted
            )
            WorkspaceTab.entries.forEach { tab ->
                val active = state.activeWorkspaceTab == tab
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(if (active) OrcaActiveTabBg else Color.Transparent)
                        .clickable { onSelectWorkspaceTab(tab) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = tabIcons[tab] ?: Icons.Default.ViewInAr,
                        contentDescription = tab.title,
                        tint = if (active) Color.White else OrcaTextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) Color.White else OrcaTextPrimary
                    )
                }
            }

            // Vertical Separator after Project tab
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(OrcaBorder)
            )
        }

        Spacer(Modifier.width(12.dp))

        // RIGHT: DolphinSlicer Brand + Server Sync + Import + Slice Plate + Export
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // DolphinSlicer Brand Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF26282B))
                    .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OrcaWhaleVectorIcon(modifier = Modifier.size(16.dp))
                Text(
                    text = "DolphinSlicer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = OrcaTeal
                )
            }

            // Sync All Profiles from Server Button
            TopMenuIconPill(
                icon = Icons.Default.CloudDownload,
                label = if (state.isSyncingServerProfiles) "Syncing..." else "Sync Server",
                onClick = onSyncServerProfiles
            )

            // Quick Import File Button
            TopMenuIconPill(
                icon = Icons.Default.FileOpen,
                label = "Import",
                onClick = onImportClick
            )

            // Slice Plate Button (#00796B)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (!state.isSlicing && state.sceneObjects.isNotEmpty()) OrcaActiveTabBg else OrcaCardBg)
                    .border(1.dp, OrcaTeal.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .clickable(enabled = !state.isSlicing && state.sceneObjects.isNotEmpty()) { onSliceClick() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (state.isSlicing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Text("Slicing...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Slice plate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // Export Dropdown Button
            Box {
                TopMenuIconPill(
                    icon = Icons.Default.IosShare,
                    label = "Export",
                    onClick = { showExportMenu = true }
                )
                DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                    ExportFormat.entries.forEach { fmt ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = OrcaTeal) },
                            text = { Text(fmt.label) },
                            onClick = {
                                showExportMenu = false
                                onExportClick(fmt)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopMenuIconPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(OrcaCardBg)
            .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = OrcaTextPrimary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = OrcaTextPrimary
        )
    }
}

@Composable
private fun ViewportWithTouchOverlays(
    state: OrcaUiState,
    viewModel: OrcaViewModel,
    isLandscape: Boolean
) {
    val isPreview = state.activeWorkspaceTab == WorkspaceTab.PREVIEW
    var showTopGizmoBar by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Hardware-Accelerated OpenGL ES 3.0 3D Viewport (#38393B background + neutral grid)
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
            onObjectSelected = viewModel::selectObject
        )

        // 2. Top-Left 1:1 OrcaSlicer `‹›` Sidebar Collapse Button + Expandable Gizmo Toolbar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Exact square `‹›` collapse/expand button flush against the left border of the 3D viewport
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF434548), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .border(1.dp, Color(0xFF56585C), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .clickable { viewModel.toggleDockExpanded() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Collapse or Expand Left Sidebar",
                    tint = Color(0xFFD8DADF),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expandable 3D Manipulation Toolbar (toggled via the bottom-right `☰` button or when gizmo is active)
            if (showTopGizmoBar || state.showFloatingGizmoSheet) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xE6262729))
                        .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TouchToolbarIconButton(
                        icon = Icons.Default.OpenWith,
                        label = "Move",
                        active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.MOVE,
                        onClick = { viewModel.selectGizmoMode(TransformGizmoMode.MOVE) }
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.AspectRatio,
                        label = "Scale",
                        active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.SCALE,
                        onClick = { viewModel.selectGizmoMode(TransformGizmoMode.SCALE) }
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.Rotate90DegreesCcw,
                        label = "Rotate",
                        active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.ROTATE,
                        onClick = { viewModel.selectGizmoMode(TransformGizmoMode.ROTATE) }
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.VerticalAlignBottom,
                        label = "Lay Flat",
                        onClick = viewModel::laySelectedObjectFlat
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.ContentCut,
                        label = "Cut",
                        active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.CUT,
                        onClick = { viewModel.selectGizmoMode(TransformGizmoMode.CUT) }
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.Flip,
                        label = "Mirror",
                        active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.MIRROR,
                        onClick = { viewModel.selectGizmoMode(TransformGizmoMode.MIRROR) }
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.Straighten,
                        label = "Measure",
                        active = state.showFloatingGizmoSheet && state.activeGizmoMode == TransformGizmoMode.MEASURE,
                        onClick = { viewModel.selectGizmoMode(TransformGizmoMode.MEASURE) }
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.GridView,
                        label = "Arrange",
                        onClick = viewModel::autoArrangeObjects
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Clone",
                        onClick = { viewModel.duplicateSelectedObject(1) }
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.FilterCenterFocus,
                        label = "Center",
                        onClick = viewModel::centerSelectedObjectOnBed
                    )
                    TouchToolbarIconButton(
                        icon = Icons.Default.WarningAmber,
                        label = "Overhang",
                        active = state.showOverhangHighlight,
                        onClick = viewModel::toggleOverhangHighlight
                    )
                    if (state.selectedObject != null) {
                        TouchToolbarIconButton(
                            icon = Icons.Default.DeleteOutline,
                            label = "Delete",
                            tint = Color(0xFFEF5350),
                            onClick = viewModel::deleteSelectedObject
                        )
                    }
                }
            }
        }

        // 3. Bottom-Right 1:1 OrcaSlicer 3D Orientation ViewCube (`Top` / `Front` / `Left` + RGB X/Y/Z axes)
        //    + 2 Circular Buttons (`☰` Tools & `🔍` Fit Zoom)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Orca3DViewCubeWidget(
                onSelectPreset = viewModel::setCameraPreset
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular `☰` Button (toggles 3D tools bar)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (showTopGizmoBar) OrcaActiveTabBg else Color(0xFF2B2C2F))
                        .border(1.dp, Color(0xFF45474B), CircleShape)
                        .clickable { showTopGizmoBar = !showTopGizmoBar },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Toggle 3D Tools",
                        tint = Color(0xFFE0E2E5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Circular `🔍` Button (Fit / Reset Camera View)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2C2F))
                        .border(1.dp, Color(0xFF45474B), CircleShape)
                        .clickable { viewModel.resetCameraView() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Fit Camera to Plate",
                        tint = Color(0xFFE0E2E5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. Floating Interactive Gizmo Sheet (Move / Scale / Rotate / Cut / Mirror / Measure)
        val selObj = state.selectedObject
        if (!isPreview && state.showFloatingGizmoSheet && selObj != null) {
            FloatingGizmoControlCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp, end = 180.dp),
                state = state,
                selected = selObj,
                viewModel = viewModel
            )
        }

        // 5. Bottom Layer & Nozzle Scrubber when in Preview mode
        val slice = state.sliceResult
        if (isPreview && slice != null && slice.layers.isNotEmpty()) {
            val totalLayers = slice.layers.size
            val topLayer = slice.layers[state.maxVisibleLayer.coerceIn(0, totalLayers - 1)]

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE81A1D24))
                    .border(1.dp, OrcaBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = OrcaTeal, modifier = Modifier.size(16.dp))
                        Text(
                            text = String.format(
                                Locale.US,
                                "Layer %d / %d (Z = %.2f mm)",
                                state.maxVisibleLayer + 1,
                                totalLayers,
                                topLayer.zHeightMm
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTeal
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViewportMiniBtn(
                            label = if (state.showTravelMoves) "Travel: ON" else "Travel: OFF",
                            active = state.showTravelMoves,
                            onClick = viewModel::toggleTravelMoves
                        )
                        Text(
                            text = "${slice.statistics.formattedTotalTime} • ${String.format(Locale.US, "%.1fg", slice.statistics.filamentWeightGrams)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OrcaTextPrimary
                        )
                    }
                }

                // Layer scrubber slider
                Slider(
                    value = state.maxVisibleLayer.toFloat(),
                    onValueChange = { v ->
                        viewModel.updateLayerRange(state.minVisibleLayer, v.toInt())
                    },
                    valueRange = 0f..(totalLayers - 1).coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(thumbColor = OrcaTeal, activeTrackColor = OrcaTeal),
                    modifier = Modifier.height(26.dp)
                )

                // Single-layer G-code toolpath progress slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nozzle ${(state.activeLayerProgress * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = OrcaTextSecondary,
                        modifier = Modifier.width(85.dp)
                    )
                    Slider(
                        value = state.activeLayerProgress,
                        onValueChange = viewModel::updateActiveLayerProgress,
                        valueRange = 0.02f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                    )
                }

                if (isLandscape) {
                    Spacer(Modifier.height(4.dp))
                    ToolpathLegendRow()
                }
            }
        }
    }
}

/**
 * 1:1 OrcaSlicer Interactive 3D Orientation ViewCube (`Top` / `Front` / `Left` with RGB X/Y/Z axes).
 * Tapping Top, Left, or Front snaps the 3D OpenGL camera to that view; tapping the center corner snaps to Isometric.
 */
@Composable
private fun Orca3DViewCubeWidget(
    onSelectPreset: (ViewportCameraPreset) -> Unit
) {
    Box(
        modifier = Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.50f
            val cy = size.height * 0.54f
            val r = size.width * 0.34f
            val dx = r * 0.866f // cos(30°)
            val dy = r * 0.500f // sin(30°)

            val pCenter = Offset(cx, cy)
            val pTopBack = Offset(cx, cy - r)
            val pTopLeft = Offset(cx - dx, cy - dy)
            val pTopRight = Offset(cx + dx, cy - dy)
            val pBottomCenter = Offset(cx, cy + r)
            val pBottomLeft = Offset(cx - dx, cy + dy)
            val pBottomRight = Offset(cx + dx, cy + dy)

            // 1. Top Diamond Face (#787A7E)
            val topFace = Path().apply {
                moveTo(pTopBack.x, pTopBack.y)
                lineTo(pTopRight.x, pTopRight.y)
                lineTo(pCenter.x, pCenter.y)
                lineTo(pTopLeft.x, pTopLeft.y)
                close()
            }
            drawPath(topFace, color = Color(0xFF75777B))

            // 2. Left Face ("Left", #5A5C60)
            val leftFace = Path().apply {
                moveTo(pTopLeft.x, pTopLeft.y)
                lineTo(pCenter.x, pCenter.y)
                lineTo(pBottomCenter.x, pBottomCenter.y)
                lineTo(pBottomLeft.x, pBottomLeft.y)
                close()
            }
            drawPath(leftFace, color = Color(0xFF5A5C60))

            // 3. Right Face ("Front", #66686C)
            val rightFace = Path().apply {
                moveTo(pCenter.x, pCenter.y)
                lineTo(pTopRight.x, pTopRight.y)
                lineTo(pBottomRight.x, pBottomRight.y)
                lineTo(pBottomCenter.x, pBottomCenter.y)
                close()
            }
            drawPath(rightFace, color = Color(0xFF66686C))

            // Cube dark edge outlines
            val edgeColor = Color(0xFF232426)
            drawLine(edgeColor, pTopBack, pTopRight, strokeWidth = 2f)
            drawLine(edgeColor, pTopBack, pTopLeft, strokeWidth = 2f)
            drawLine(edgeColor, pTopLeft, pCenter, strokeWidth = 2f)
            drawLine(edgeColor, pTopRight, pCenter, strokeWidth = 2f)
            drawLine(edgeColor, pCenter, pBottomCenter, strokeWidth = 2f)
            drawLine(edgeColor, pTopLeft, pBottomLeft, strokeWidth = 2f)
            drawLine(edgeColor, pTopRight, pBottomRight, strokeWidth = 2f)

            // RGB Coordinate Axes originating from bottom-front vertex (pBottomCenter)
            // Red X axis -> along bottom-right edge
            val xAxisEnd = Offset(pBottomRight.x + 8f, pBottomRight.y - 4.5f)
            drawLine(Color(0xFFE53935), pBottomCenter, xAxisEnd, strokeWidth = 3.5f)
            drawCircle(Color(0xFFE53935), radius = 6.5f, center = xAxisEnd)

            // Green Y axis -> along bottom-left edge
            val yAxisEnd = Offset(pBottomLeft.x - 8f, pBottomLeft.y - 4.5f)
            drawLine(Color(0xFF43A047), pBottomCenter, yAxisEnd, strokeWidth = 3.5f)
            drawCircle(Color(0xFF43A047), radius = 6.5f, center = yAxisEnd)

            // Blue Z axis -> vertical from pBottomLeft up to pTopLeft
            val zAxisEnd = Offset(pTopLeft.x, pTopLeft.y - 8f)
            drawLine(Color(0xFF1E88E5), pBottomLeft, zAxisEnd, strokeWidth = 3.5f)
            drawCircle(Color(0xFF1E88E5), radius = 6.5f, center = zAxisEnd)
        }

        // Face Labels + Clickable Hotspots
        // Top Face Label ("Top")
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onSelectPreset(ViewportCameraPreset.TOP) }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Top",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1F21)
            )
        }

        // Left Face Label ("Left")
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 15.dp, top = 16.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onSelectPreset(ViewportCameraPreset.RIGHT) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Left",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1E20)
            )
        }

        // Right Face Label ("Front")
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 13.dp, top = 16.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onSelectPreset(ViewportCameraPreset.FRONT) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Front",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1E20)
            )
        }

        // Center Isometric Hotspot
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable { onSelectPreset(ViewportCameraPreset.ISOMETRIC) }
        )
    }
}

@Composable
private fun TouchToolbarIconButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    tint: Color = OrcaTextPrimary,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) OrcaTeal else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color(0xFF041F18) else tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) Color(0xFF041F18) else OrcaTextSecondary
        )
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
            .border(1.dp, OrcaTeal.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF01A1D24))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.activeGizmoMode.title.uppercase()} — ${selected.name}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OrcaTeal
                )
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
                    val objHeight = selected.mesh.localBounds.size.z.coerceAtLeast(2f)
                    TouchNumberSetting(
                        label = "Horizontal Cut Plane Z",
                        valueText = String.format(Locale.US, "%.1f mm", state.cutPlaneHeightMm.coerceIn(1f, objHeight)),
                        value = state.cutPlaneHeightMm.coerceIn(1f, objHeight),
                        range = 1.0f..objHeight,
                        step = 0.5f,
                        onValueChange = viewModel::updateCutPlaneHeight
                    )
                    Button(
                        onClick = { viewModel.cutSelectedObjectAtZ(state.cutPlaneHeightMm) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrcaTeal, contentColor = Color(0xFF041F18)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Perform Cut into Lower & Upper Objects", fontWeight = FontWeight.Bold)
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
    viewModel: OrcaViewModel
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
                    ViewportMiniBtn("Clone ×3") { viewModel.duplicateSelectedObject(3) }
                    ViewportMiniBtn("Arrange", onClick = viewModel::autoArrangeObjects)
                }
            }

            state.sceneObjects.forEach { obj ->
                val isSel = obj.id == state.selectedObjectId
                val dims = obj.worldBoundingBox().size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFF1A3A35) else OrcaPanelBg)
                        .border(1.dp, if (isSel) OrcaTeal else OrcaBorder, RoundedCornerShape(8.dp))
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
                                tint = if (obj.visible) OrcaTeal else OrcaTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = obj.sourceFormat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTeal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolpathLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolpathType.entries.forEach { t ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(t.r, t.g, t.b))
                )
                Text(text = t.displayName, fontSize = 10.sp, color = OrcaTextSecondary)
            }
        }
    }
}

@Composable
private fun OrcaWhaleVectorIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Leaping Orca Whale Body + Dorsal Fin
        val bodyPath = Path().apply {
            moveTo(w * 0.10f, h * 0.65f)
            cubicTo(w * 0.18f, h * 0.32f, w * 0.42f, h * 0.22f, w * 0.60f, h * 0.24f)
            // Dorsal fin
            cubicTo(w * 0.65f, h * 0.08f, w * 0.75f, h * 0.05f, w * 0.78f, h * 0.08f)
            cubicTo(w * 0.73f, h * 0.18f, w * 0.72f, h * 0.26f, w * 0.76f, h * 0.32f)
            // Nose / Snout
            cubicTo(w * 0.90f, h * 0.38f, w * 0.95f, h * 0.52f, w * 0.92f, h * 0.64f)
            cubicTo(w * 0.85f, h * 0.72f, w * 0.65f, h * 0.66f, w * 0.45f, h * 0.70f)
            cubicTo(w * 0.30f, h * 0.74f, w * 0.18f, h * 0.82f, w * 0.08f, h * 0.84f)
            close()
        }
        drawPath(bodyPath, color = OrcaTeal)

        // White Orca Eye Patch
        drawCircle(
            color = Color.White,
            radius = w * 0.09f,
            center = Offset(w * 0.76f, h * 0.44f)
        )

        // White Belly Highlight
        val bellyPath = Path().apply {
            moveTo(w * 0.32f, h * 0.66f)
            cubicTo(w * 0.48f, h * 0.56f, w * 0.68f, h * 0.56f, w * 0.86f, h * 0.63f)
            cubicTo(w * 0.72f, h * 0.69f, w * 0.50f, h * 0.67f, w * 0.32f, h * 0.66f)
            close()
        }
        drawPath(bellyPath, color = Color.White)
    }
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
                text = "Configure spool price and weight below, then tap 'Slice Plate' to compute exact filament meters, weight (g), electricity cost, and quote price.",
                fontSize = 11.sp,
                color = OrcaTextSecondary
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
                range = 5f..150f,
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

            // Top Headline Row: Time, Weight, Material Cost, Total Cost
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

            // Detailed Spool & Production Cost Breakdown Box
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
                        color = OrcaTeal
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

            // Live Filament Price/kg Stepper right inside the summary card
            TouchNumberSetting(
                label = "Filament Price (${cur} / kg)",
                valueText = String.format(Locale.US, "%s%.2f/kg", cur, f.costPerKgUsd),
                value = f.costPerKgUsd,
                range = 5.0f..150.0f,
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

@Composable
private fun DeviceNetworkPanel(
    state: OrcaUiState,
    viewModel: OrcaViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeaderRow(Icons.Default.Router, "Wi-Fi / LAN 3D Printer Connection")
                Text(
                    text = "Connect directly to Klipper / Moonraker (Mainsail, Fluidd, Creality K1, Voron) or OctoPrint to monitor temperatures and send sliced G-code over Wi-Fi.",
                    fontSize = 12.sp,
                    color = OrcaTextSecondary
                )

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.printerHost,
                        onValueChange = { viewModel.updatePrinterNetworkConfig(host = it) },
                        label = { Text("Printer IP / Hostname") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = state.printerPort,
                        onValueChange = { viewModel.updatePrinterNetworkConfig(port = it) },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(0.3f)
                    )
                }

                OutlinedTextField(
                    value = state.printerApiKey,
                    onValueChange = { viewModel.updatePrinterNetworkConfig(apiKey = it) },
                    label = { Text("API Key / Access Code (Optional for Moonraker LAN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = viewModel::testPrinterConnection,
                        enabled = !state.isNetworkBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrcaTeal,
                            contentColor = Color(0xFF041F18)
                        )
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Query Status", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.sendGCodeToNetworkPrinter(startImmediately = false) },
                        enabled = !state.isNetworkBusy
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload G-Code")
                    }

                    Button(
                        onClick = { viewModel.sendGCodeToNetworkPrinter(startImmediately = true) },
                        enabled = !state.isNetworkBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF041F18)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload & Start Print", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        state.printerTelemetry?.let { tel ->
            Card(
                colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionHeaderRow(Icons.Default.Analytics, "Live Printer Telemetry")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMetricItem("State", tel.stateText)
                        StatMetricItem(
                            "Nozzle Temp",
                            String.format(Locale.US, "%.1f°C / %.0f°C", tel.nozzleTempC, tel.nozzleTargetC)
                        )
                        StatMetricItem(
                            "Bed Temp",
                            String.format(Locale.US, "%.1f°C / %.0f°C", tel.bedTempC, tel.bedTargetC)
                        )
                        StatMetricItem("Progress", "${tel.progressPercent}%")
                    }
                }
            }
        }
    }
}
