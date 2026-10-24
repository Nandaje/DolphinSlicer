package com.orcaslicer.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.orcaslicer.android.model.GCodeFlavor
import com.orcaslicer.android.model.ZHopType
import java.util.Locale

/**
 * ============================================================================
 * Printer & Filament Profile Modals + Calibration Suite (`OrcaProfileModals.kt`)
 * ============================================================================
 * Contains all modal dialogs and workspace panels for browsing, searching,
 * syncing, and editing Printer & Filament presets, plus the procedural
 * Calibration test generator panel:
 * 1. [PrinterProfileBrowserModal] — Searchable vendor catalog for 3D printers
 * 2. [FilamentProfileBrowserModal] — Searchable vendor catalog for filaments
 * 3. [CalibrationWorkspacePanel] — Generates Temp Tower, Flow Rate, PA, Retraction, etc.
 * 4. [PrinterSettingsModalDialog] — Edits bed size, nozzle diameter, G-code flavor, & electricity cost
 * 5. [FilamentSettingsModalDialog] — Edits filament color, spool price, currency, temperatures, & PA
 */

@Composable
internal fun PrinterProfileBrowserModal(
    state: OrcaUiState,
    onSelectPrinter: (com.orcaslicer.android.model.PrinterProfile) -> Unit,
    onSyncServerClick: () -> Unit,
    onImportProfileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedVendor by remember { mutableStateOf("All") }
    val vendors = remember(state.printers) {
        listOf("All") + state.printers.map { it.vendor }.distinct()
    }
    val filtered = remember(state.printers, searchQuery, selectedVendor) {
        state.printers.filter { p ->
            (selectedVendor == "All" || p.vendor.equals(selectedVendor, ignoreCase = true)) &&
                (searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.vendor.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = OrcaCardBg
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, OrcaBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Compact single-line header row (never wraps or stretches vertically)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = OrcaTealBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Select Printer (${filtered.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sync Server compact pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.isSyncingServerProfiles) OrcaElevatedBg else OrcaTeal)
                                .clickable(enabled = !state.isSyncingServerProfiles, onClick = onSyncServerClick)
                                .padding(horizontal = 8.dp, vertical = 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (state.isSyncingServerProfiles) "Sync..." else "Sync",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Import .json compact pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(OrcaInputBg)
                                .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                                .clickable(onClick = onImportProfileClick)
                                .padding(horizontal = 7.dp, vertical = 4.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ .json",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrcaTextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(OrcaInputBg)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = OrcaTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Compact 32dp single-line search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaBorder, RoundedCornerShape(7.dp))
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = OrcaTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search printer (Bambu, Creality, Voron, Prusa, K1...)",
                                fontSize = 10.5.sp,
                                color = OrcaUnitText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = OrcaTextPrimary, fontSize = 11.sp),
                            cursorBrush = SolidColor(OrcaTealBright),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = OrcaTextSecondary,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                // Compact vendor filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    vendors.forEach { v ->
                        val active = selectedVendor == v
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (active) OrcaTeal else OrcaInputBg)
                                .border(1.dp, if (active) OrcaTealBright else OrcaBorder, RoundedCornerShape(5.dp))
                                .clickable { selectedVendor = v }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = v,
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Scrollable printer list
                Column(
                    modifier = Modifier
                        .height(340.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filtered.forEach { p ->
                        val isSel = p.id == state.selectedPrinter.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF1A3A35) else OrcaInputBg)
                                .border(1.dp, if (isSel) OrcaTealBright else OrcaBorder, RoundedCornerShape(6.dp))
                                .clickable { onSelectPrinter(p) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                Text(
                                    text = p.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrcaTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${p.vendor} • ${p.bedWidthX.toInt()}×${p.bedDepthY.toInt()}×${p.maxPrintHeightZ.toInt()}mm • Nozzle ${p.nozzleDiameterMm}mm • ${p.gcodeFlavor.label}",
                                    fontSize = 9.5.sp,
                                    color = OrcaTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = OrcaTealBright,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FilamentProfileBrowserModal(
    state: OrcaUiState,
    onSelectFilament: (com.orcaslicer.android.model.FilamentProfile) -> Unit,
    onSyncServerClick: () -> Unit,
    onImportProfileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedVendor by remember { mutableStateOf("All") }
    val vendors = remember(state.filaments) {
        listOf("All") + state.filaments.map { it.vendor }.distinct()
    }
    val filtered = remember(state.filaments, searchQuery, selectedVendor) {
        state.filaments.filter { f ->
            (selectedVendor == "All" || f.vendor.equals(selectedVendor, ignoreCase = true)) &&
                (searchQuery.isBlank() ||
                    f.name.contains(searchQuery, ignoreCase = true) ||
                    f.materialType.contains(searchQuery, ignoreCase = true) ||
                    f.vendor.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = OrcaCardBg
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, OrcaBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Compact single-line header row (never wraps or stretches vertically)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = OrcaTealBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Select Filament (${filtered.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sync Server compact pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.isSyncingServerProfiles) OrcaElevatedBg else OrcaTeal)
                                .clickable(enabled = !state.isSyncingServerProfiles, onClick = onSyncServerClick)
                                .padding(horizontal = 8.dp, vertical = 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (state.isSyncingServerProfiles) "Sync..." else "Sync",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Import .json compact pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(OrcaInputBg)
                                .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                                .clickable(onClick = onImportProfileClick)
                                .padding(horizontal = 7.dp, vertical = 4.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ .json",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrcaTextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(OrcaInputBg)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = OrcaTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Compact 32dp single-line search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaBorder, RoundedCornerShape(7.dp))
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = OrcaTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search filament (Bambu, eSUN, PLA, PETG, ABS, TPU...)",
                                fontSize = 10.5.sp,
                                color = OrcaUnitText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = OrcaTextPrimary, fontSize = 11.sp),
                            cursorBrush = SolidColor(OrcaTealBright),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = OrcaTextSecondary,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                // Compact vendor filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    vendors.forEach { v ->
                        val active = selectedVendor == v
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (active) OrcaTeal else OrcaInputBg)
                                .border(1.dp, if (active) OrcaTealBright else OrcaBorder, RoundedCornerShape(5.dp))
                                .clickable { selectedVendor = v }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = v,
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Scrollable filament list
                Column(
                    modifier = Modifier
                        .height(340.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filtered.forEach { f ->
                        val isSel = f.id == state.selectedFilament.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF1A3A35) else OrcaInputBg)
                                .border(1.dp, if (isSel) OrcaTealBright else OrcaBorder, RoundedCornerShape(6.dp))
                                .clickable { onSelectFilament(f) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(f.colorHex))
                                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                )
                                Column {
                                    Text(
                                        text = f.name,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrcaTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${f.vendor} • ${f.materialType} • ${f.nozzleTempOtherC}°C / Bed ${f.bedTempOtherC}°C • ${f.currencySymbol}${String.format(Locale.US, "%.2f", f.costPerKgUsd)}/kg",
                                        fontSize = 9.5.sp,
                                        color = OrcaTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = OrcaTealBright,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalibrationWorkspacePanel(
    viewModel: OrcaViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Science, contentDescription = null, tint = OrcaTealBright, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = "DOLPHINSLICER CALIBRATION & PROJECT SUITE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OrcaTealBright
                )
                Text(
                    text = "Generate calibration test geometries or sync all vendor profiles from the official OrcaSlicer server.",
                    fontSize = 12.sp,
                    color = OrcaTextSecondary
                )
            }
        }

        CalibrationTestType.entries.forEach { test ->
            Card(
                colors = CardDefaults.cardColors(containerColor = OrcaCardBg),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(test.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OrcaTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(test.description, fontSize = 12.sp, color = OrcaTextSecondary)
                    }
                    Button(
                        onClick = { viewModel.launchCalibrationTest(test) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrcaTeal, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Generate", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterSettingsModalDialog(
    state: OrcaUiState,
    viewModel: OrcaViewModel,
    onDismiss: () -> Unit
) {
    val p = state.selectedPrinter
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(12.dp),
            color = OrcaCardBg
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = OrcaTealBright, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Printer Settings (${p.name})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OrcaInputBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OrcaTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }

                HorizontalDivider(color = OrcaDivider)

                OrcaSectionGroupHeader(Icons.Default.Memory, "Printable Area & G-Code Flavor")
                OrcaParamDropdownRow(
                    label = "G-code flavor",
                    options = GCodeFlavor.entries,
                    selected = p.gcodeFlavor,
                    optionLabel = { it.label },
                    onSelect = { gf -> viewModel.updatePrinter { it.copy(gcodeFlavor = gf) } }
                )
                OrcaParamNumberRow(
                    label = "Printable width (X)",
                    value = p.bedWidthX,
                    unitText = "mm",
                    range = 80f..600f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(bedWidthX = v) } }
                )
                OrcaParamNumberRow(
                    label = "Printable depth (Y)",
                    value = p.bedDepthY,
                    unitText = "mm",
                    range = 80f..600f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(bedDepthY = v) } }
                )
                OrcaParamNumberRow(
                    label = "Printable height (Z)",
                    value = p.maxPrintHeightZ,
                    unitText = "mm",
                    range = 80f..600f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(maxPrintHeightZ = v) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Build, "Extruder & Retraction")
                OrcaParamDropdownRow(
                    label = "Nozzle diameter",
                    options = listOf(0.2f, 0.4f, 0.6f, 0.8f),
                    selected = p.nozzleDiameterMm,
                    optionLabel = { "$it mm" },
                    onSelect = { nd -> viewModel.updatePrinter { it.copy(nozzleDiameterMm = nd) } }
                )
                OrcaParamNumberRow(
                    label = "Retraction length",
                    value = p.retractionLengthMm,
                    unitText = "mm",
                    range = 0.0f..8.0f,
                    step = 0.1f,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(retractionLengthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Z hop when retracting",
                    value = p.zHopMm,
                    unitText = "mm",
                    range = 0.0f..2.0f,
                    step = 0.1f,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(zHopMm = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Z hop type",
                    options = ZHopType.entries,
                    selected = p.zHopType,
                    optionLabel = { it.label },
                    onSelect = { zh -> viewModel.updatePrinter { it.copy(zHopType = zh) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Speed, "Electricity & Machine Cost")
                OrcaParamNumberRow(
                    label = "Power consumption",
                    value = p.powerConsumptionWatts,
                    unitText = "W",
                    range = 40f..800f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(powerConsumptionWatts = v) } }
                )
                OrcaParamNumberRow(
                    label = "Electricity rate",
                    value = p.electricityRatePerKwh,
                    unitText = "/kWh",
                    range = 0.01f..1.50f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(electricityRatePerKwh = v) } }
                )
                OrcaParamNumberRow(
                    label = "Machine hourly rate",
                    value = p.hourlyMachineRate,
                    unitText = "/hr",
                    range = 0.0f..15.0f,
                    step = 0.10f,
                    onValueChange = { v -> viewModel.updatePrinter { it.copy(hourlyMachineRate = v) } }
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OrcaTeal, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilamentSettingsModalDialog(
    state: OrcaUiState,
    viewModel: OrcaViewModel,
    onDismiss: () -> Unit
) {
    val f = state.selectedFilament
    val colorSwatches = listOf(
        0xFF29ABE2L, // DolphinSlicer Logo Azure Blue
        0xFF00C896L,
        0xFF2898FFL,
        0xFFFF6E40L,
        0xFFFFB300L,
        0xFFE040FBL,
        0xFFEF5350L,
        0xFFECEFF1L
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(12.dp),
            color = OrcaCardBg
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = OrcaTealBright, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Filament Settings (${f.name})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrcaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OrcaInputBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OrcaTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }

                HorizontalDivider(color = OrcaDivider)

                Text("Filament Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OrcaTextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorSwatches.forEach { hex ->
                        val isSel = f.colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(hex))
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) Color.White else OrcaBorder,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.updateFilament { it.copy(colorHex = hex) } }
                        )
                    }
                }

                OrcaSectionGroupHeader(Icons.Default.Tune, "Spool Price & Custom Currency System")
                CustomCurrencySelectorSection(
                    currentSymbol = f.currencySymbol,
                    onCurrencyChange = { cur -> viewModel.setGlobalCurrencySymbol(cur) }
                )
                OrcaParamDropdownRow(
                    label = "Spool weight",
                    options = listOf(250f, 500f, 750f, 1000f, 2000f, 3000f, 5000f),
                    selected = f.spoolWeightGrams,
                    optionLabel = { "${it.toInt()} g" },
                    onSelect = { sw -> viewModel.updateFilament { it.copy(spoolWeightGrams = sw) } }
                )
                OrcaParamNumberRow(
                    label = "Price (per kg)",
                    value = f.costPerKgUsd,
                    unitText = "${f.currencySymbol}/kg",
                    range = 1.0f..1000000.0f,
                    step = 1.0f,
                    onValueChange = { v -> viewModel.updateFilament { it.copy(costPerKgUsd = v) } }
                )
                OrcaParamNumberRow(
                    label = "Density",
                    value = f.densityGPerCm3,
                    unitText = "g/cm³",
                    range = 0.80f..2.20f,
                    step = 0.01f,
                    onValueChange = { v -> viewModel.updateFilament { it.copy(densityGPerCm3 = v) } }
                )
                OrcaParamNumberRow(
                    label = "Flow ratio",
                    value = f.flowRatio,
                    unitText = "",
                    range = 0.70f..1.20f,
                    step = 0.01f,
                    decimals = 3,
                    onValueChange = { v -> viewModel.updateFilament { it.copy(flowRatio = v) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Thermostat, "Print Temperatures")
                OrcaParamNumberRow(
                    label = "Nozzle (initial layer)",
                    value = f.nozzleTempInitialC.toFloat(),
                    unitText = "°C",
                    range = 170f..320f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateFilament { it.copy(nozzleTempInitialC = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Nozzle (other layers)",
                    value = f.nozzleTempOtherC.toFloat(),
                    unitText = "°C",
                    range = 170f..320f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateFilament { it.copy(nozzleTempOtherC = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Textured PEI Plate",
                    value = f.bedTempOtherC.toFloat(),
                    unitText = "°C",
                    range = 0f..130f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v ->
                        val t = v.toInt()
                        viewModel.updateFilament { it.copy(bedTempOtherC = t, bedTempInitialC = t) }
                    }
                )

                OrcaSectionGroupHeader(Icons.Default.Air, "Cooling & Pressure Advance")
                OrcaParamNumberRow(
                    label = "Part cooling fan",
                    value = f.fanSpeedPercent.toFloat(),
                    unitText = "%",
                    range = 0f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateFilament { it.copy(fanSpeedPercent = v.toInt()) } }
                )
                OrcaParamCheckboxRow(
                    label = "Enable pressure advance",
                    checked = f.enablePressureAdvance,
                    onCheckedChange = { b -> viewModel.updateFilament { it.copy(enablePressureAdvance = b) } }
                )
                if (f.enablePressureAdvance) {
                    OrcaParamNumberRow(
                        label = "Pressure advance",
                        value = f.pressureAdvance,
                        unitText = "",
                        range = 0.00f..0.20f,
                        step = 0.002f,
                        decimals = 3,
                        onValueChange = { v -> viewModel.updateFilament { it.copy(pressureAdvance = v) } }
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OrcaTeal, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
