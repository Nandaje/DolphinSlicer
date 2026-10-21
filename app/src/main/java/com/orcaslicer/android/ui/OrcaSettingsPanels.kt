package com.orcaslicer.android.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.orcaslicer.android.model.BrimType
import com.orcaslicer.android.model.FuzzySkinType
import com.orcaslicer.android.model.GCodeFlavor
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.IroningType
import com.orcaslicer.android.model.PrintSequence
import com.orcaslicer.android.model.ScarfSeamType
import com.orcaslicer.android.model.SeamPosition
import com.orcaslicer.android.model.SupportStyle
import com.orcaslicer.android.model.SupportType
import com.orcaslicer.android.model.SurfacePattern
import com.orcaslicer.android.model.WallGeneratorType
import com.orcaslicer.android.model.ZHopType
import java.util.Locale

// Exact 1:1 OrcaSlicer Desktop Dark Palette from reference screenshot
internal val OrcaTeal = Color(0xFF009688)
internal val OrcaTealBright = Color(0xFF00BFA5)
internal val OrcaTopBarBg = Color(0xFF1F2022)
internal val OrcaDarkBg = Color(0xFF262729)
internal val OrcaPanelBg = Color(0xFF262729)
internal val OrcaCardBg = Color(0xFF2B2C2E)
internal val OrcaInputBg = Color(0xFF1E1F21)
internal val OrcaElevatedBg = Color(0xFF323437)
internal val OrcaBorder = Color(0xFF3E4044)
internal val OrcaDivider = Color(0xFF35373A)
internal val OrcaTextPrimary = Color(0xFFE6E8EA)
internal val OrcaTextSecondary = Color(0xFF9AA0A6)
internal val OrcaUnitText = Color(0xFF6E7276)

/**
 * 1:1 OrcaSlicer Section Header with Left Icon, Bold Title, and Horizontal Divider Line extending right.
 */
@Composable
fun OrcaSectionGroupHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1B3B36))
                .border(1.dp, OrcaTeal.copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrcaTealBright,
                modifier = Modifier.size(11.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OrcaTextPrimary
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OrcaDivider)
        )
    }
}

/**
 * 1:1 OrcaSlicer 2-Column Numeric Parameter Row:
 * Left: indented parameter label
 * Right: dark input box `[0.24          mm]` that expands a touch stepper + slider when tapped!
 */
@Composable
fun OrcaParamNumberRow(
    label: String,
    value: Float,
    unitText: String,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    decimals: Int = 2,
    boldLabel: Boolean = false,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    var expandedTouchAdjuster by remember { mutableStateOf(false) }
    val formattedVal = when (decimals) {
        0 -> value.toInt().toString()
        1 -> String.format(Locale.US, "%.1f", value)
        3 -> String.format(Locale.US, "%.3f", value)
        else -> String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            .let { if (it.isEmpty()) "0" else it }
    }
    var textInput by remember(formattedVal) { mutableStateOf(formattedVal) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (boldLabel) FontWeight.Bold else FontWeight.Normal,
                color = if (!enabled) OrcaUnitText else if (boldLabel) Color.White else OrcaTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(0.54f)
                    .padding(start = 10.dp, end = 6.dp)
            )

            Row(
                modifier = Modifier
                    .weight(0.46f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (enabled) OrcaInputBg else Color(0xFF232427))
                    .border(
                        width = 1.dp,
                        color = if (expandedTouchAdjuster) OrcaTealBright else OrcaBorder,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = enabled) { expandedTouchAdjuster = !expandedTouchAdjuster }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicTextField(
                    value = textInput,
                    onValueChange = { raw ->
                        textInput = raw
                        raw.removeSuffix("%").trim().toFloatOrNull()?.let { parsed ->
                            onValueChange(parsed.coerceIn(range.start, range.endInclusive))
                        }
                    },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (enabled) OrcaTextPrimary else OrcaUnitText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(OrcaTealBright),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = unitText,
                    fontSize = 10.sp,
                    color = OrcaUnitText,
                    maxLines = 1
                )
            }
        }

        // Touch-friendly expandable [- / Slider / +] bar when row is tapped
        if (expandedTouchAdjuster && enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaCardBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrcaElevatedBg)
                        .clickable { onValueChange((value - step).coerceIn(range.start, range.endInclusive)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = OrcaTextPrimary, modifier = Modifier.size(14.dp))
                }
                Slider(
                    value = value.coerceIn(range.start, range.endInclusive),
                    onValueChange = onValueChange,
                    valueRange = range,
                    colors = SliderDefaults.colors(
                        thumbColor = OrcaTealBright,
                        activeTrackColor = OrcaTeal,
                        inactiveTrackColor = OrcaInputBg
                    ),
                    modifier = Modifier.weight(1f).height(24.dp)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrcaElevatedBg)
                        .clickable { onValueChange((value + step).coerceIn(range.start, range.endInclusive)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = OrcaTextPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * 1:1 OrcaSlicer Integer Layers Stepper Row with Up/Down arrows on the left of the input box
 * (matches `Elephant foot compensation layers [ ^v 1      layers ]` in screenshot).
 */
@Composable
fun OrcaParamIntStepperRow(
    label: String,
    value: Int,
    unitText: String,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = OrcaTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.54f)
                .padding(start = 10.dp, end = 6.dp)
        )

        Row(
            modifier = Modifier
                .weight(0.46f)
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OrcaInputBg)
                .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Increment",
                        tint = OrcaTextSecondary,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onValueChange((value + 1).coerceIn(range)) }
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrement",
                        tint = OrcaTextSecondary,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onValueChange((value - 1).coerceIn(range)) }
                    )
                }
                Text(
                    text = value.toString(),
                    fontSize = 12.sp,
                    color = OrcaTextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = unitText, fontSize = 10.sp, color = OrcaUnitText)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(OrcaElevatedBg)
                        .clickable { onValueChange((value + 1).coerceIn(range)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 11.sp, color = OrcaTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 1:1 OrcaSlicer Square Teal Checkbox Row (`Staggered inner seams`, `Role base wipe speed`, `Arc fitting`, `Precise wall`).
 */
@Composable
fun OrcaParamCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = OrcaTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.54f)
                .padding(start = 10.dp, end = 6.dp)
        )

        Box(modifier = Modifier.weight(0.46f)) {
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (checked) OrcaTeal else OrcaInputBg)
                    .border(
                        width = 1.dp,
                        color = if (checked) OrcaTealBright else Color(0xFF52555A),
                        shape = RoundedCornerShape(3.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * 1:1 OrcaSlicer Dropdown Parameter Row (`Seam position [ v Aligned ]`, `Scarf joint seam (beta) [ v None ]`).
 */
@Composable
fun <T> OrcaParamDropdownRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = OrcaTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.54f)
                .padding(start = 10.dp, end = 6.dp)
        )

        Box(modifier = Modifier.weight(0.46f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = OrcaTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = optionLabel(selected),
                    fontSize = 12.sp,
                    color = OrcaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(opt), fontSize = 12.sp) },
                        onClick = {
                            expanded = false
                            onSelect(opt)
                        }
                    )
                }
            }
        }
    }
}

// Keep compatibility helpers for modal dialogs
@Composable
fun TouchNumberSetting(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    subtitle: String? = null
) {
    OrcaParamNumberRow(
        label = if (subtitle != null) "$label ($subtitle)" else label,
        value = value,
        unitText = valueText.substringAfterLast(" "),
        range = range,
        step = step,
        onValueChange = onValueChange
    )
}

@Composable
fun TouchToggleSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    OrcaParamCheckboxRow(
        label = if (subtitle != null) "$label — $subtitle" else label,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun <T> TouchEnumSelector(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    OrcaParamDropdownRow(
        label = label,
        options = options,
        selected = selected,
        optionLabel = optionLabel,
        onSelect = onSelect
    )
}

@Composable
fun SectionHeaderRow(icon: ImageVector, title: String) {
    OrcaSectionGroupHeader(icon = icon, title = title)
}

/**
 * 1:1 OrcaSlicer Upper-Left Sidebar:
 * - Printer Section (`[3D Printer Box | v Ender5P]` + `[Nozzle | v 0.4]`)
 * - Filament Section (`[1] [v Elegoo PLA Mod] [Edit]`)
 * - Process Section (`Global | Objects` pill, Atom icon, Green toggle, `[v Production 0.24 0.4Noz] [Save] [-] [Search]`)
 */
@Composable
fun FullPresetSelectorsCard(
    state: OrcaUiState,
    viewModel: OrcaViewModel,
    onImportProfileClick: () -> Unit = {}
) {
    var showPrinterBrowser by remember { mutableStateOf(false) }
    var showFilamentBrowser by remember { mutableStateOf(false) }
    var showNozzleMenu by remember { mutableStateOf(false) }
    var showProcessMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrcaPanelBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // =====================================================================
        // 1. PRINTER HEADER & SIDE-BY-SIDE PRINTER + NOZZLE BOXES
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Print, contentDescription = null, tint = OrcaTealBright, modifier = Modifier.size(16.dp))
                Text("Printer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OrcaTextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Cloud Server Sync icon button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { viewModel.syncProfilesFromOrcaServer() }
                        .padding(2.dp)
                ) {
                    if (state.isSyncingServerProfiles) {
                        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = OrcaTealBright)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Import All Profiles from OrcaSlicer Server",
                            tint = OrcaTealBright,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Wi-Fi Printer",
                    tint = OrcaTealBright,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { viewModel.selectWorkspaceTab(WorkspaceTab.DEVICE) }
                )
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Printer Settings",
                    tint = OrcaTextSecondary,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { viewModel.setPrinterSettingsDialogVisible(true) }
                )
            }
        }

        // Printer Box + Nozzle Box Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Printer Selector Box with miniature wireframe printer icon
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(5.dp))
                    .clickable { showPrinterBrowser = true }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Miniature 3D Printer Frame Graphic matching screenshot
                PrinterWireframeThumbnail(modifier = Modifier.size(34.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = OrcaTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = state.selectedPrinter.name.substringBefore(" ("),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = OrcaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Nozzle Selector Box (`Nozzle` / `v 0.4` / `-`)
            Box {
                Column(
                    modifier = Modifier
                        .width(66.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaBorder, RoundedCornerShape(5.dp))
                        .clickable { showNozzleMenu = true }
                        .padding(vertical = 3.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nozzle", fontSize = 10.sp, color = OrcaTextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = OrcaTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${state.selectedPrinter.nozzleDiameterMm}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrcaTextPrimary
                        )
                    }
                    Text("-", fontSize = 9.sp, color = OrcaUnitText)
                }
                DropdownMenu(expanded = showNozzleMenu, onDismissRequest = { showNozzleMenu = false }) {
                    listOf(0.2f, 0.4f, 0.6f, 0.8f).forEach { nd ->
                        DropdownMenuItem(
                            text = { Text("$nd mm Nozzle") },
                            onClick = {
                                showNozzleMenu = false
                                viewModel.updatePrinter { it.copy(nozzleDiameterMm = nd) }
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = OrcaDivider, modifier = Modifier.padding(vertical = 2.dp))

        // =====================================================================
        // 2. FILAMENT HEADER & NUMBERED FILAMENT SLOTS
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = OrcaTealBright, modifier = Modifier.size(16.dp))
                Text("Filament", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OrcaTextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Filament Slot",
                    tint = OrcaTextSecondary,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { viewModel.addFilamentSlot() }
                )
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Filaments from Server",
                    tint = OrcaTealBright,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { viewModel.syncProfilesFromOrcaServer() }
                )
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filament Settings",
                    tint = OrcaTextSecondary,
                    modifier = Modifier
                        .size(17.dp)
                        .clickable { viewModel.setFilamentSettingsDialogVisible(true) }
                )
            }
        }

        // Render each Filament Slot (`[1] [v Elegoo PLA Mod] [Edit]`)
        state.filamentSlots.forEachIndexed { idx, slotFil ->
            val isSlotActive = idx == state.activeFilamentSlotIndex
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Numbered colored square `[ 1 ]`
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(slotFil.colorHex))
                        .border(
                            width = if (isSlotActive) 1.5.dp else 0.5.dp,
                            color = if (isSlotActive) Color.White else OrcaBorder,
                            shape = RoundedCornerShape(3.dp)
                        )
                        .clickable { viewModel.selectFilamentSlot(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${idx + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A1916)
                    )
                }

                // Filament Dropdown Box (`v Elegoo PLA Mod`)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, if (isSlotActive) OrcaTeal else OrcaBorder, RoundedCornerShape(3.dp))
                        .clickable {
                            viewModel.selectFilamentSlot(idx)
                            showFilamentBrowser = true
                        }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = OrcaTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = slotFil.name,
                        fontSize = 12.sp,
                        color = OrcaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right square Edit button `[ ✎ ]`
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaBorder, RoundedCornerShape(3.dp))
                        .clickable {
                            viewModel.selectFilamentSlot(idx)
                            viewModel.setFilamentSettingsDialogVisible(true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Filament",
                        tint = OrcaTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = OrcaDivider, modifier = Modifier.padding(vertical = 2.dp))

        // =====================================================================
        // 3. PROCESS HEADER (`Process` | `Global / Objects` | Atom | Green Toggle | Icons)
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = OrcaTealBright, modifier = Modifier.size(16.dp))
                Text("Process", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OrcaTextPrimary)

                // `Global | Objects` Capsule Switch
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1B1D))
                        .border(1.dp, OrcaBorder, RoundedCornerShape(12.dp))
                        .padding(1.dp)
                ) {
                    val isGlobal = state.isGlobalProcessMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGlobal) OrcaTeal else Color.Transparent)
                            .clickable { viewModel.setGlobalProcessMode(true) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Global", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isGlobal) Color.White else OrcaTextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isGlobal) OrcaTeal else Color.Transparent)
                            .clickable { viewModel.setGlobalProcessMode(false) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Objects", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (!isGlobal) Color.White else OrcaTextSecondary)
                    }
                }
            }

            // Right Process Icons: Atom + Green Toggle (`• — ●`) + List
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Advanced Mode",
                    tint = OrcaTealBright,
                    modifier = Modifier.size(15.dp)
                )
                // OrcaSlicer 3-dot green pill switch (`• • ●`)
                Row(
                    modifier = Modifier
                        .width(36.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.showAdvancedSettings) OrcaTeal else OrcaInputBg)
                        .clickable { viewModel.toggleAdvancedSettings() }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                }
                Icon(
                    imageVector = Icons.Default.ListAlt,
                    contentDescription = "Compare Presets",
                    tint = OrcaTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Process Preset Dropdown Row (`[v Production 0.24 0.4Noz] [Save] [-] [Search]`)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaBorder, RoundedCornerShape(3.dp))
                        .clickable { showProcessMenu = true }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = OrcaTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = state.currentProcess.name,
                        fontSize = 12.sp,
                        color = OrcaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(expanded = showProcessMenu, onDismissRequest = { showProcessMenu = false }) {
                    state.processPresets.forEach { proc ->
                        DropdownMenuItem(
                            text = { Text(proc.name, fontSize = 12.sp) },
                            onClick = {
                                showProcessMenu = false
                                viewModel.selectProcessPreset(proc)
                            }
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save Preset",
                tint = OrcaTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Reset Preset",
                tint = OrcaTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Settings",
                tint = if (state.showProcessSearchBar) OrcaTealBright else OrcaTextSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { viewModel.toggleProcessSearchBar() }
            )
        }
    }

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
                onImportProfileClick()
            },
            onDismiss = { showPrinterBrowser = false }
        )
    }

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
                onImportProfileClick()
            },
            onDismiss = { showFilamentBrowser = false }
        )
    }
}

@Composable
private fun PrinterWireframeThumbnail(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val frameColor = Color(0xFF8A8E94)
        // Outer gantry cube wireframe
        drawRect(
            color = frameColor,
            topLeft = Offset(w * 0.18f, h * 0.14f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.72f),
            style = Stroke(width = 1.4f)
        )
        // X-axis rail
        drawLine(
            color = frameColor,
            start = Offset(w * 0.18f, h * 0.42f),
            end = Offset(w * 0.82f, h * 0.42f),
            strokeWidth = 1.4f
        )
        // Bed plate
        drawLine(
            color = OrcaTealBright,
            start = Offset(w * 0.24f, h * 0.74f),
            end = Offset(w * 0.76f, h * 0.74f),
            strokeWidth = 2.0f
        )
        // Toolhead dot
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = w * 0.07f,
            center = Offset(w * 0.52f, h * 0.42f)
        )
    }
}

/**
 * 1:1 OrcaSlicer Process Settings Table:
 * - Underline Category Tabs (`Quality` | `Strength` | `Speed` | `Support` | `Multimaterial` | `Others`)
 * - 2-Column Parameter Table matching the uploaded screenshot line-for-line!
 */
@Composable
fun FullProcessSettingsCard(
    state: OrcaUiState,
    viewModel: OrcaViewModel
) {
    val proc = state.currentProcess
    val query = state.settingsSearchQuery.trim().lowercase(Locale.ROOT)
    fun matches(vararg keywords: String): Boolean {
        if (query.isEmpty()) return true
        return keywords.any { it.lowercase(Locale.ROOT).contains(query) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrcaPanelBg)
            .padding(horizontal = 8.dp)
    ) {
        // Optional Search Input Box (when magnifying glass is clicked)
        if (state.showProcessSearchBar) {
            OutlinedTextField(
                value = state.settingsSearchQuery,
                onValueChange = viewModel::updateSettingsSearchQuery,
                placeholder = { Text("Search parameter...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleProcessSearchBar() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Search", modifier = Modifier.size(15.dp))
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        // 1:1 OrcaSlicer Underline Tab Bar: Quality | Strength | Speed | Support | Multimaterial | Others
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProcessCategoryTab.entries.forEach { tab ->
                val active = state.activeProcessTab == tab
                Column(
                    modifier = Modifier
                        .clickable { viewModel.selectProcessTab(tab) }
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) Color.White else OrcaTextSecondary,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(2.5.dp)
                            .background(if (active) OrcaTealBright else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = OrcaDivider)

        // If user switched to `Objects` tab in Process header, show Per-Object Table
        if (!state.isGlobalProcessMode) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Per-Object Process Settings (${state.sceneObjects.size} objects on plate)",
                fontSize = 11.sp,
                color = OrcaTealBright,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // =====================================================================
        // TAB 1: QUALITY (Matches reference screenshot 1:1!)
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.QUALITY || query.isNotEmpty()) {
            if (matches("layer height", "first layer height")) {
                OrcaSectionGroupHeader(Icons.Default.Layers, "Layer height")
                OrcaParamNumberRow(
                    label = "Layer height",
                    value = proc.layerHeightMm,
                    unitText = "mm",
                    range = 0.04f..0.60f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(layerHeightMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer height",
                    value = proc.initialLayerHeightMm,
                    unitText = "mm",
                    range = 0.08f..0.60f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(initialLayerHeightMm = v) } }
                )
            }

            if (matches("line width", "default", "outer wall", "inner wall", "top surface", "sparse infill", "internal solid", "support", "bridge")) {
                OrcaSectionGroupHeader(Icons.Default.LinearScale, "Line width")
                OrcaParamNumberRow(
                    label = "Default",
                    value = proc.lineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    boldLabel = true,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(lineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer",
                    value = proc.initialLayerLineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(initialLayerLineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Outer wall",
                    value = proc.outerWallLineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(outerWallLineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Inner wall",
                    value = proc.innerWallLineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(innerWallLineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top surface",
                    value = proc.topSurfaceLineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topSurfaceLineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Sparse infill",
                    value = proc.sparseInfillLineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(sparseInfillLineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Internal solid infill",
                    value = proc.internalSolidLineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalSolidLineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Support",
                    value = proc.supportLineWidthMm,
                    unitText = "mm or %",
                    range = 0.20f..1.20f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportLineWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bridge",
                    value = proc.bridgeLineWidthPercent.toFloat(),
                    unitText = "% mm or %",
                    range = 50f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bridgeLineWidthPercent = v.toInt()) } }
                )
            }

            if (matches("seam", "staggered", "scarf", "wipe")) {
                OrcaSectionGroupHeader(Icons.Default.AutoAwesome, "Seam")
                OrcaParamDropdownRow(
                    label = "Seam position",
                    options = SeamPosition.entries,
                    selected = proc.seamPosition,
                    optionLabel = { it.label },
                    onSelect = { pos -> viewModel.updateProcess { it.copy(seamPosition = pos) } }
                )
                OrcaParamCheckboxRow(
                    label = "Staggered inner seams",
                    checked = proc.staggeredInnerSeams,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(staggeredInnerSeams = b) } }
                )
                OrcaParamNumberRow(
                    label = "Seam gap",
                    value = proc.seamGapPercent.toFloat(),
                    unitText = "% mm or %",
                    range = 0f..30f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(seamGapPercent = v.toInt()) } }
                )
                OrcaParamDropdownRow(
                    label = "Scarf joint seam (beta)",
                    options = ScarfSeamType.entries,
                    selected = proc.scarfJointSeam,
                    optionLabel = { it.label },
                    onSelect = { sc -> viewModel.updateProcess { it.copy(scarfJointSeam = sc) } }
                )
                OrcaParamCheckboxRow(
                    label = "Role base wipe speed",
                    checked = proc.roleBaseWipeSpeed,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(roleBaseWipeSpeed = b) } }
                )
                OrcaParamNumberRow(
                    label = "Wipe speed",
                    value = proc.wipeSpeedPercent.toFloat(),
                    unitText = "% mm/s or %",
                    range = 10f..200f,
                    step = 5f,
                    decimals = 0,
                    enabled = !proc.roleBaseWipeSpeed,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(wipeSpeedPercent = v.toInt()) } }
                )
                OrcaParamCheckboxRow(
                    label = "Wipe on loops",
                    checked = proc.wipeOnLoops,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(wipeOnLoops = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Wipe before external loop",
                    checked = proc.wipeBeforeExternalLoop,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(wipeBeforeExternalLoop = b) } }
                )
            }

            if (matches("precision", "slice gap", "resolution", "arc fitting", "hole", "contour", "elephant", "precise")) {
                OrcaSectionGroupHeader(Icons.Default.GridOn, "Precision")
                OrcaParamNumberRow(
                    label = "Slice gap closing radius",
                    value = proc.sliceGapClosingRadiusMm,
                    unitText = "mm",
                    range = 0.001f..0.200f,
                    step = 0.005f,
                    decimals = 3,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(sliceGapClosingRadiusMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Resolution",
                    value = proc.resolutionMm,
                    unitText = "mm",
                    range = 0.002f..0.100f,
                    step = 0.002f,
                    decimals = 3,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(resolutionMm = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Arc fitting",
                    checked = proc.arcFittingG2G3,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(arcFittingG2G3 = b) } }
                )
                OrcaParamNumberRow(
                    label = "X-Y hole compensation",
                    value = proc.xyHoleCompensationMm,
                    unitText = "mm",
                    range = -1.0f..1.0f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(xyHoleCompensationMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "X-Y contour compensation",
                    value = proc.xyContourCompensationMm,
                    unitText = "mm",
                    range = -1.0f..1.0f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(xyContourCompensationMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Elephant foot compensation",
                    value = proc.elephantFootCompensationMm,
                    unitText = "mm",
                    range = 0.0f..1.0f,
                    step = 0.05f,
                    boldLabel = true,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(elephantFootCompensationMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Elephant foot layers density",
                    value = proc.elephantFootLayersDensityPercent.toFloat(),
                    unitText = "%",
                    range = 10f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(elephantFootLayersDensityPercent = v.toInt()) } }
                )
                OrcaParamIntStepperRow(
                    label = "Elephant foot compensation layers",
                    value = proc.elephantFootCompensationLayers,
                    unitText = "layers",
                    range = 1..10,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(elephantFootCompensationLayers = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Precise wall",
                    checked = proc.preciseWall,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(preciseWall = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Precise Z height",
                    checked = proc.preciseZHeight,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(preciseZHeight = b) } }
                )
            }

            if (matches("ironing", "wall generator", "arachne")) {
                OrcaSectionGroupHeader(Icons.Default.Build, "Ironing & Wall generator")
                OrcaParamDropdownRow(
                    label = "Wall generator",
                    options = WallGeneratorType.entries,
                    selected = proc.wallGenerator,
                    optionLabel = { it.label },
                    onSelect = { wg -> viewModel.updateProcess { it.copy(wallGenerator = wg) } }
                )
                OrcaParamDropdownRow(
                    label = "Ironing type",
                    options = IroningType.entries,
                    selected = proc.ironingType,
                    optionLabel = { it.label },
                    onSelect = { ir -> viewModel.updateProcess { it.copy(ironingType = ir) } }
                )
                OrcaParamCheckboxRow(
                    label = "Only one wall on top surfaces",
                    checked = proc.onlyOneWallOnTop,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(onlyOneWallOnTop = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Avoid crossing walls",
                    checked = proc.avoidCrossingWalls,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(avoidCrossingWalls = b) } }
                )
            }
        }

        // =====================================================================
        // TAB 2: STRENGTH
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.STRENGTH || query.isNotEmpty()) {
            if (matches("wall loops", "thin walls", "top shell", "bottom shell")) {
                OrcaSectionGroupHeader(Icons.Default.Shield, "Walls")
                OrcaParamIntStepperRow(
                    label = "Wall loops",
                    value = proc.wallLoops,
                    unitText = "loops",
                    range = 1..12,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(wallLoops = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Alternate extra wall",
                    checked = proc.alternateExtraWall,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(alternateExtraWall = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Detect thin walls",
                    checked = proc.detectThinWalls,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(detectThinWalls = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Layers, "Top/bottom shells")
                OrcaParamDropdownRow(
                    label = "Top surface pattern",
                    options = SurfacePattern.entries,
                    selected = proc.topSurfacePattern,
                    optionLabel = { it.label },
                    onSelect = { sp -> viewModel.updateProcess { it.copy(topSurfacePattern = sp) } }
                )
                OrcaParamIntStepperRow(
                    label = "Top shell layers",
                    value = proc.topShellLayers,
                    unitText = "layers",
                    range = 0..15,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topShellLayers = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top shell thickness",
                    value = proc.topShellThicknessMm,
                    unitText = "mm",
                    range = 0.0f..3.0f,
                    step = 0.2f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topShellThicknessMm = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Bottom surface pattern",
                    options = SurfacePattern.entries,
                    selected = proc.bottomSurfacePattern,
                    optionLabel = { it.label },
                    onSelect = { sp -> viewModel.updateProcess { it.copy(bottomSurfacePattern = sp) } }
                )
                OrcaParamIntStepperRow(
                    label = "Bottom shell layers",
                    value = proc.bottomShellLayers,
                    unitText = "layers",
                    range = 0..15,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bottomShellLayers = v) } }
                )
            }

            if (matches("sparse infill", "gyroid", "overlap")) {
                OrcaSectionGroupHeader(Icons.Default.GridOn, "Sparse infill")
                OrcaParamNumberRow(
                    label = "Sparse infill density",
                    value = proc.sparseInfillDensityPercent.toFloat(),
                    unitText = "%",
                    range = 0f..100f,
                    step = 5f,
                    decimals = 0,
                    boldLabel = true,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(sparseInfillDensityPercent = v.toInt()) } }
                )
                OrcaParamDropdownRow(
                    label = "Sparse infill pattern",
                    options = InfillPattern.entries,
                    selected = proc.sparseInfillPattern,
                    optionLabel = { it.label },
                    onSelect = { pat -> viewModel.updateProcess { it.copy(sparseInfillPattern = pat) } }
                )
                OrcaParamNumberRow(
                    label = "Infill/wall overlap",
                    value = proc.infillWallOverlapPercent.toFloat(),
                    unitText = "%",
                    range = 0f..50f,
                    step = 2f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(infillWallOverlapPercent = v.toInt()) } }
                )
                OrcaParamCheckboxRow(
                    label = "Infill combination",
                    checked = proc.infillCombination,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(infillCombination = b) } }
                )
            }
        }

        // =====================================================================
        // TAB 3: SPEED
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.SPEED || query.isNotEmpty()) {
            if (matches("speed", "outer wall", "inner wall", "infill", "travel", "acceleration")) {
                OrcaSectionGroupHeader(Icons.Default.Speed, "First layer speed")
                OrcaParamNumberRow(
                    label = "Initial layer",
                    value = proc.initialLayerSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(initialLayerSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Initial layer infill",
                    value = proc.initialLayerInfillSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..250f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(initialLayerInfillSpeedMmS = v) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Speed, "Other layers speed")
                OrcaParamNumberRow(
                    label = "Outer wall",
                    value = proc.outerWallSpeedMmS,
                    unitText = "mm/s",
                    range = 20f..500f,
                    step = 10f,
                    decimals = 0,
                    boldLabel = true,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(outerWallSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Inner wall",
                    value = proc.innerWallSpeedMmS,
                    unitText = "mm/s",
                    range = 20f..500f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(innerWallSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Sparse infill",
                    value = proc.sparseInfillSpeedMmS,
                    unitText = "mm/s",
                    range = 30f..600f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(sparseInfillSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Internal solid infill",
                    value = proc.internalSolidInfillSpeedMmS,
                    unitText = "mm/s",
                    range = 30f..500f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalSolidInfillSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top surface",
                    value = proc.topSurfaceSpeedMmS,
                    unitText = "mm/s",
                    range = 20f..350f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topSurfaceSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bridge",
                    value = proc.bridgeSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bridgeSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Travel",
                    value = proc.travelSpeedMmS,
                    unitText = "mm/s",
                    range = 100f..700f,
                    step = 20f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(travelSpeedMmS = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Slow down for overhangs",
                    checked = proc.slowDownForOverhangs,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(slowDownForOverhangs = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Speed, "Acceleration")
                OrcaParamNumberRow(
                    label = "Normal printing",
                    value = proc.defaultAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 500f..30000f,
                    step = 500f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(defaultAccelerationMmS2 = v) } }
                )
                OrcaParamNumberRow(
                    label = "Outer wall",
                    value = proc.outerWallAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 500f..20000f,
                    step = 500f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(outerWallAccelerationMmS2 = v) } }
                )
            }
        }

        // =====================================================================
        // TAB 4: SUPPORT
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.SUPPORT || query.isNotEmpty()) {
            if (matches("support", "tree", "raft", "overhang")) {
                OrcaSectionGroupHeader(Icons.Default.Compress, "Support")
                OrcaParamCheckboxRow(
                    label = "Enable support",
                    checked = proc.enableSupport,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(enableSupport = b) } }
                )
                OrcaParamDropdownRow(
                    label = "Type",
                    options = SupportType.entries,
                    selected = proc.supportType,
                    optionLabel = { it.label },
                    onSelect = { st -> viewModel.updateProcess { it.copy(supportType = st) } }
                )
                OrcaParamDropdownRow(
                    label = "Style",
                    options = SupportStyle.entries,
                    selected = proc.supportStyle,
                    optionLabel = { it.label },
                    onSelect = { ss -> viewModel.updateProcess { it.copy(supportStyle = ss) } }
                )
                OrcaParamNumberRow(
                    label = "Threshold angle",
                    value = proc.supportThresholdAngleDeg.toFloat(),
                    unitText = "°",
                    range = 10f..80f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportThresholdAngleDeg = v.toInt()) } }
                )
                OrcaParamCheckboxRow(
                    label = "On build plate only",
                    checked = proc.supportOnBuildPlateOnly,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(supportOnBuildPlateOnly = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Remove small overhangs",
                    checked = proc.removeSmallOverhangs,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(removeSmallOverhangs = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Layers, "Raft & Interface")
                OrcaParamIntStepperRow(
                    label = "Raft layers",
                    value = proc.raftLayers,
                    unitText = "layers",
                    range = 0..8,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(raftLayers = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top Z distance",
                    value = proc.supportTopZDistanceMm,
                    unitText = "mm",
                    range = 0.05f..0.50f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportTopZDistanceMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Support/object xy distance",
                    value = proc.supportObjectXyDistanceMm,
                    unitText = "mm",
                    range = 0.15f..1.20f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportObjectXyDistanceMm = v) } }
                )
            }
        }

        // =====================================================================
        // TAB 5: MULTIMATERIAL
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.MULTIMATERIAL || query.isNotEmpty()) {
            if (matches("prime tower", "flush", "ooze")) {
                OrcaSectionGroupHeader(Icons.Default.ColorLens, "Prime tower")
                OrcaParamCheckboxRow(
                    label = "Enable",
                    checked = proc.enablePrimeTower,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(enablePrimeTower = b) } }
                )
                OrcaParamNumberRow(
                    label = "Width",
                    value = proc.primeTowerWidthMm,
                    unitText = "mm",
                    range = 10f..80f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(primeTowerWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Prime volume",
                    value = proc.primeVolumeMm3,
                    unitText = "mm³",
                    range = 10f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(primeVolumeMm3 = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Flush into objects' infill",
                    checked = proc.flushIntoInfill,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(flushIntoInfill = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Flush into objects' support",
                    checked = proc.flushIntoSupport,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(flushIntoSupport = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Enable ooze shield",
                    checked = proc.enableOozeShield,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(enableOozeShield = b) } }
                )
            }
        }

        // =====================================================================
        // TAB 6: OTHERS
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.OTHERS || query.isNotEmpty()) {
            if (matches("skirt", "brim", "fuzzy", "vase", "sequence", "exclude")) {
                OrcaSectionGroupHeader(Icons.Default.Build, "Bed adhesion")
                OrcaParamIntStepperRow(
                    label = "Skirt loops",
                    value = proc.skirtLoops,
                    unitText = "loops",
                    range = 0..6,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(skirtLoops = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Brim type",
                    options = BrimType.entries,
                    selected = proc.brimType,
                    optionLabel = { it.label },
                    onSelect = { bt -> viewModel.updateProcess { it.copy(brimType = bt) } }
                )
                OrcaParamNumberRow(
                    label = "Brim width",
                    value = proc.brimWidthMm,
                    unitText = "mm",
                    range = 1f..20f,
                    step = 1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(brimWidthMm = v) } }
                )

                OrcaSectionGroupHeader(Icons.Default.AutoAwesome, "Special mode")
                OrcaParamDropdownRow(
                    label = "Print sequence",
                    options = PrintSequence.entries,
                    selected = proc.printSequence,
                    optionLabel = { it.label },
                    onSelect = { ps -> viewModel.updateProcess { it.copy(printSequence = ps) } }
                )
                OrcaParamCheckboxRow(
                    label = "Spiral vase",
                    checked = proc.spiralVaseMode,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(spiralVaseMode = b) } }
                )
                OrcaParamDropdownRow(
                    label = "Fuzzy skin",
                    options = FuzzySkinType.entries,
                    selected = proc.fuzzySkinType,
                    optionLabel = { it.label },
                    onSelect = { fs -> viewModel.updateProcess { it.copy(fuzzySkinType = fs) } }
                )
                OrcaParamCheckboxRow(
                    label = "Exclude objects",
                    checked = proc.excludeObjectGcode,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(excludeObjectGcode = b) } }
                )
            }
        }
    }
}

@Composable
private fun PrinterProfileBrowserModal(
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
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = OrcaCardBg
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = OrcaTealBright)
                        Text(
                            text = "Select Printer (${filtered.size} Models)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrcaTextPrimary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onSyncServerClick,
                            enabled = !state.isSyncingServerProfiles,
                            colors = ButtonDefaults.buttonColors(containerColor = OrcaTeal, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (state.isSyncingServerProfiles) "Syncing..." else "Sync Server", fontSize = 11.sp)
                        }
                        OutlinedButton(onClick = onImportProfileClick, shape = RoundedCornerShape(6.dp)) {
                            Text("Import .json", fontSize = 11.sp)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = OrcaTextSecondary)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search printer (Ender5P, Bambu, K1, Voron, Prusa, SV08...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vendors.forEach { v ->
                        val active = selectedVendor == v
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) OrcaTeal else OrcaInputBg)
                                .border(1.dp, if (active) OrcaTealBright else OrcaBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedVendor = v }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = v,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .height(360.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OrcaTextPrimary)
                                Text(
                                    "${p.vendor} • ${p.bedWidthX.toInt()}×${p.bedDepthY.toInt()}×${p.maxPrintHeightZ.toInt()}mm • Nozzle ${p.nozzleDiameterMm}mm • ${p.gcodeFlavor.label}",
                                    fontSize = 11.sp,
                                    color = OrcaTextSecondary
                                )
                            }
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = OrcaTealBright)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilamentProfileBrowserModal(
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
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = OrcaCardBg
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = OrcaTealBright)
                        Text(
                            text = "Select Filament (${filtered.size} Profiles)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrcaTextPrimary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onSyncServerClick,
                            enabled = !state.isSyncingServerProfiles,
                            colors = ButtonDefaults.buttonColors(containerColor = OrcaTeal, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (state.isSyncingServerProfiles) "Syncing..." else "Sync Server", fontSize = 11.sp)
                        }
                        OutlinedButton(onClick = onImportProfileClick, shape = RoundedCornerShape(6.dp)) {
                            Text("Import .json", fontSize = 11.sp)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = OrcaTextSecondary)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search filament (Elegoo PLA Mod, PLA, PETG, ABS, ASA, TPU...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vendors.forEach { v ->
                        val active = selectedVendor == v
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) OrcaTeal else OrcaInputBg)
                                .border(1.dp, if (active) OrcaTealBright else OrcaBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedVendor = v }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(v, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .height(360.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(f.colorHex))
                                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                )
                                Column {
                                    Text(f.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OrcaTextPrimary)
                                    Text(
                                        "${f.vendor} • ${f.materialType} • ${f.nozzleTempOtherC}°C / Bed ${f.bedTempOtherC}°C • ${f.currencySymbol}${String.format(Locale.US, "%.2f", f.costPerKgUsd)}/kg",
                                        fontSize = 11.sp,
                                        color = OrcaTextSecondary
                                    )
                                }
                            }
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = OrcaTealBright)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = OrcaTealBright)
                        Text("Printer Settings (${p.name})", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = OrcaTextPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OrcaTextSecondary)
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
        0xFF1CA392L, // Orca/Dolphin Teal (Screenshot exact match)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = OrcaTealBright)
                        Text("Filament Settings (${f.name})", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = OrcaTextPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OrcaTextSecondary)
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

                OrcaSectionGroupHeader(Icons.Default.Tune, "Spool Price & Cost Calculation")
                OrcaParamDropdownRow(
                    label = "Currency",
                    options = listOf("$", "€", "£", "¥", "฿", "Rp", "₹", "A$", "C$"),
                    selected = f.currencySymbol,
                    optionLabel = { it },
                    onSelect = { cur -> viewModel.updateFilament { it.copy(currencySymbol = cur) } }
                )
                OrcaParamDropdownRow(
                    label = "Spool weight",
                    options = listOf(250f, 500f, 750f, 1000f, 2000f, 3000f),
                    selected = f.spoolWeightGrams,
                    optionLabel = { "${it.toInt()} g" },
                    onSelect = { sw -> viewModel.updateFilament { it.copy(spoolWeightGrams = sw) } }
                )
                OrcaParamNumberRow(
                    label = "Price (per kg)",
                    value = f.costPerKgUsd,
                    unitText = "${f.currencySymbol}/kg",
                    range = 5.0f..200.0f,
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
