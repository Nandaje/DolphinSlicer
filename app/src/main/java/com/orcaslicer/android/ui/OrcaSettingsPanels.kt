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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import com.orcaslicer.android.model.BridgeCounterboreMode
import com.orcaslicer.android.model.BrimType
import com.orcaslicer.android.model.CombingMode
import com.orcaslicer.android.model.DraftShieldMode
import com.orcaslicer.android.model.ExtraBridgeLayersMode
import com.orcaslicer.android.model.FilterSmallBridgesMode
import com.orcaslicer.android.model.FuzzySkinType
import com.orcaslicer.android.model.GCodeFlavor
import com.orcaslicer.android.model.GapFillPlacement
import com.orcaslicer.android.model.InfillPattern
import com.orcaslicer.android.model.IntraLayerOrder
import com.orcaslicer.android.model.IroningType
import com.orcaslicer.android.model.PrintSequence
import com.orcaslicer.android.model.ScarfSeamType
import com.orcaslicer.android.model.SeamPosition
import com.orcaslicer.android.model.SkirtType
import com.orcaslicer.android.model.SlicingModeType
import com.orcaslicer.android.model.SupportBasePattern
import com.orcaslicer.android.model.SupportInterfacePattern
import com.orcaslicer.android.model.SupportStyle
import com.orcaslicer.android.model.SupportType
import com.orcaslicer.android.model.SurfacePattern
import com.orcaslicer.android.model.TopOneWallMode
import com.orcaslicer.android.model.VerticalShellThicknessMode
import com.orcaslicer.android.model.WallGeneratorType
import com.orcaslicer.android.model.WallLoopDirection
import com.orcaslicer.android.model.WallsPrintingOrder
import com.orcaslicer.android.model.WipeTowerWallType
import com.orcaslicer.android.model.ZHopType
import java.util.Locale

// DolphinSlicer Dark Charcoal + Azure/Teal Palette
internal val OrcaTeal = Color(0xFF1D88B8)          // DolphinSlicer Deep Azure
internal val OrcaTealBright = Color(0xFF29ABE2)    // DolphinSlicer Bright Azure
internal val OrcaTopBarBg = Color(0xFF18191C)      // Top app bar
internal val OrcaDarkBg = Color(0xFF1E1F23)        // Main workspace background
internal val OrcaPanelBg = Color(0xFF232428)       // Left settings panel background
internal val OrcaCardBg = Color(0xFF28292E)        // Sub-card container
internal val OrcaInputBg = Color(0xFF2C2E33)       // Parameter input box background
internal val OrcaElevatedBg = Color(0xFF33353B)    // Hover / secondary button background
internal val OrcaBorder = Color(0xFF3D3F46)        // Input box & card border
internal val OrcaDivider = Color(0xFF2E3036)       // Section horizontal rule
internal val OrcaTextPrimary = Color(0xFFE6E8EC)   // Main label text
internal val OrcaTextSecondary = Color(0xFFA2A6B0) // Secondary label text
internal val OrcaUnitText = Color(0xFF7D828D)      // Unit text inside input boxes

/**
 * 1:1 OrcaSlicer Section Header (`Layers and perimeters`, `Line width`, `Seam`, `Precision`, `Ironing`, etc.)
 */
@Composable
fun OrcaSectionGroupHeader(
    icon: ImageVector,
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrcaTealBright,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = OrcaTextPrimary
            )
        }
        HorizontalDivider(color = OrcaDivider, thickness = 1.dp)
    }
}

private fun formatOrcaNumber(value: Float, decimals: Int): String = when (decimals) {
    0 -> value.toInt().toString()
    1 -> String.format(Locale.US, "%.1f", value)
    3 -> String.format(Locale.US, "%.3f", value)
    else -> String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        .let { if (it.isEmpty()) "0" else it }
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
    var isFocused by remember { mutableStateOf(false) }
    val formattedVal = formatOrcaNumber(value, decimals)
    var textInput by remember { mutableStateOf(formattedVal) }

    LaunchedEffect(formattedVal, isFocused) {
        if (!isFocused) {
            textInput = formattedVal
        }
    }

    val minAllowed = if (range.start >= 0f) 0f else range.start
    val maxAllowed = maxOf(range.endInclusive, 10_000_000f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (boldLabel) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) OrcaTextPrimary else OrcaUnitText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(0.56f)
                    .padding(start = 6.dp, end = 6.dp)
            )

            Row(
                modifier = Modifier
                    .weight(0.44f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (enabled) OrcaInputBg else Color(0xFF232427))
                    .border(
                        width = 1.dp,
                        color = if (isFocused || expandedTouchAdjuster) OrcaTealBright else OrcaBorder,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = enabled) { expandedTouchAdjuster = !expandedTouchAdjuster }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicTextField(
                    value = textInput,
                    onValueChange = { raw ->
                        textInput = raw
                        val cleaned = raw.removeSuffix("%").replace(',', '.').trim()
                        cleaned.toFloatOrNull()?.let { parsed ->
                            onValueChange(parsed.coerceIn(minAllowed, maxAllowed))
                        }
                    },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (enabled) OrcaTextPrimary else OrcaUnitText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(OrcaTealBright),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (!focusState.isFocused) {
                                textInput = formatOrcaNumber(value, decimals)
                            }
                        }
                )
                Text(
                    text = unitText,
                    fontSize = 9.sp,
                    color = OrcaUnitText,
                    maxLines = 1,
                    modifier = Modifier.clickable(enabled = enabled) { expandedTouchAdjuster = !expandedTouchAdjuster }
                )
            }
        }

        // Touch-friendly expandable [- / Slider / +] bar when row is tapped
        if (expandedTouchAdjuster && enabled) {
            val sliderMin = minOf(range.start, value)
            val baseMax = if (range.endInclusive > 2000f) maxOf(500f, value * 1.5f) else range.endInclusive
            val sliderMax = maxOf(baseMax, value, sliderMin + step)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, bottom = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(OrcaCardBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrcaElevatedBg)
                        .clickable {
                            val next = (value - step).coerceIn(minAllowed, maxAllowed)
                            onValueChange(next)
                            textInput = formatOrcaNumber(next, decimals)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = OrcaTextPrimary, modifier = Modifier.size(12.dp))
                }
                Slider(
                    value = value.coerceIn(sliderMin, sliderMax),
                    onValueChange = { next ->
                        onValueChange(next)
                        textInput = formatOrcaNumber(next, decimals)
                    },
                    valueRange = sliderMin..sliderMax,
                    colors = SliderDefaults.colors(
                        thumbColor = OrcaTealBright,
                        activeTrackColor = OrcaTeal,
                        inactiveTrackColor = OrcaInputBg
                    ),
                    modifier = Modifier.weight(1f).height(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrcaElevatedBg)
                        .clickable {
                            val next = (value + step).coerceIn(minAllowed, maxAllowed)
                            onValueChange(next)
                            textInput = formatOrcaNumber(next, decimals)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = OrcaTextPrimary, modifier = Modifier.size(12.dp))
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
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = OrcaTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.56f)
                .padding(start = 6.dp, end = 6.dp)
        )

        Row(
            modifier = Modifier
                .weight(0.44f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OrcaInputBg)
                .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                            .size(10.dp)
                            .clickable { onValueChange((value + 1).coerceIn(range)) }
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrement",
                        tint = OrcaTextSecondary,
                        modifier = Modifier
                            .size(10.dp)
                            .clickable { onValueChange((value - 1).coerceIn(range)) }
                    )
                }
                Text(
                    text = value.toString(),
                    fontSize = 11.sp,
                    color = OrcaTextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = unitText, fontSize = 9.sp, color = OrcaUnitText)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(OrcaElevatedBg)
                        .clickable { onValueChange((value + 1).coerceIn(range)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 10.sp, color = OrcaTextPrimary, fontWeight = FontWeight.Bold)
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
            .padding(vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = OrcaTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.56f)
                .padding(start = 6.dp, end = 6.dp)
        )

        Box(modifier = Modifier.weight(0.44f)) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (checked) OrcaTeal else OrcaInputBg)
                    .border(
                        width = 1.dp,
                        color = if (checked) OrcaTealBright else Color(0xFF555862),
                        shape = RoundedCornerShape(3.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

/**
 * 1:1 OrcaSlicer Dropdown Parameter Row (`Seam position [Aligned    v]`).
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
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = OrcaTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.56f)
                .padding(start = 6.dp, end = 6.dp)
        )

        Box(modifier = Modifier.weight(0.44f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(OrcaInputBg)
                    .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = optionLabel(selected),
                    fontSize = 11.sp,
                    color = OrcaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = OrcaTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(opt), fontSize = 11.sp) },
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

@Composable
fun OrcaParamTextRow(
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit
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

        Box(
            modifier = Modifier
                .weight(0.46f)
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OrcaInputBg)
                .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, fontSize = 11.sp, color = OrcaUnitText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = OrcaTextPrimary, fontSize = 12.sp),
                cursorBrush = SolidColor(OrcaTealBright),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun OrcaParamMultiLineTextRow(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(OrcaInputBg)
            .border(1.dp, OrcaBorder, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 11.sp, color = OrcaUnitText)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = OrcaTextPrimary, fontSize = 11.sp),
            cursorBrush = SolidColor(OrcaTealBright),
            modifier = Modifier.fillMaxSize()
        )
    }
}

val BuiltInCurrencyPresets = listOf(
    "$" to "$ (USD)",
    "€" to "€ (EUR)",
    "£" to "£ (GBP)",
    "¥" to "¥ (JPY/CNY)",
    "Rp" to "Rp (IDR)",
    "₹" to "₹ (INR)",
    "₱" to "₱ (PHP)",
    "฿" to "฿ (THB)",
    "RM" to "RM (MYR)",
    "S$" to "S$ (SGD)",
    "₫" to "₫ (VND)",
    "₩" to "₩ (KRW)",
    "R$" to "R$ (BRL)",
    "CA$" to "CA$ (CAD)",
    "A$" to "A$ (AUD)",
    "CHF " to "CHF (Swiss)",
    "zł" to "zł (PLN)",
    "kr" to "kr (SEK/NOK)",
    "₺" to "₺ (TRY)",
    "AED " to "AED (Dirham)",
    "SAR " to "SAR (Riyal)"
)

@Composable
fun CustomCurrencySelectorSection(
    currentSymbol: String,
    onCurrencyChange: (String) -> Unit
) {
    var customInput by remember(currentSymbol) { mutableStateOf(currentSymbol) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OrcaParamDropdownRow(
            label = "Currency preset",
            options = BuiltInCurrencyPresets,
            selected = BuiltInCurrencyPresets.find { it.first == currentSymbol } ?: (currentSymbol to "Custom ($currentSymbol)"),
            optionLabel = { it.second },
            onSelect = { pair ->
                customInput = pair.first
                onCurrencyChange(pair.first)
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom currency symbol / code",
                fontSize = 12.sp,
                color = OrcaTextPrimary,
                modifier = Modifier
                    .weight(0.54f)
                    .padding(start = 10.dp, end = 6.dp)
            )
            Row(
                modifier = Modifier.weight(0.46f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrcaInputBg)
                        .border(1.dp, OrcaTealBright.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (customInput.isEmpty()) {
                        Text("e.g. $, Rp, €, IDR", fontSize = 11.sp, color = OrcaUnitText)
                    }
                    BasicTextField(
                        value = customInput,
                        onValueChange = { raw ->
                            customInput = raw.take(8)
                            if (customInput.isNotBlank()) {
                                onCurrencyChange(customInput)
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = OrcaTealBright, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(OrcaTealBright),
                        modifier = Modifier.fillMaxWidth()
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
        // TAB 1: QUALITY (Complete 1:1 OrcaSlicer Quality Settings from PDF)
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
                    unitText = "%",
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
                    unitText = "%",
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
                    unitText = "%",
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

            if (matches("precision", "slice gap", "resolution", "arc fitting", "hole", "contour", "elephant", "precise", "polyholes")) {
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
                OrcaParamCheckboxRow(
                    label = "Convert holes to polyholes",
                    checked = proc.convertHolesToPolyholes,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(convertHolesToPolyholes = b) } }
                )
            }

            if (matches("ironing")) {
                OrcaSectionGroupHeader(Icons.Default.Build, "Ironing")
                OrcaParamDropdownRow(
                    label = "Ironing type",
                    options = IroningType.entries,
                    selected = proc.ironingType,
                    optionLabel = { it.label },
                    onSelect = { ir -> viewModel.updateProcess { it.copy(ironingType = ir) } }
                )
                if (proc.ironingType != IroningType.NONE) {
                    OrcaParamNumberRow(
                        label = "Ironing speed",
                        value = proc.ironingSpeedMmS,
                        unitText = "mm/s",
                        range = 5f..150f,
                        step = 5f,
                        decimals = 0,
                        onValueChange = { v -> viewModel.updateProcess { it.copy(ironingSpeedMmS = v) } }
                    )
                    OrcaParamNumberRow(
                        label = "Ironing flow",
                        value = proc.ironingFlowPercent.toFloat(),
                        unitText = "%",
                        range = 1f..50f,
                        step = 1f,
                        decimals = 0,
                        onValueChange = { v -> viewModel.updateProcess { it.copy(ironingFlowPercent = v.toInt()) } }
                    )
                }
            }

            if (matches("z compounding")) {
                OrcaSectionGroupHeader(Icons.Default.Layers, "Z compounding")
                OrcaParamCheckboxRow(
                    label = "Z compounding enabled",
                    checked = proc.zCompoundingEnabled,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(zCompoundingEnabled = b) } }
                )
            }

            if (matches("wall generator", "arachne", "transition", "minimum wall", "feature size", "deviation")) {
                OrcaSectionGroupHeader(Icons.Default.Build, "Wall generator")
                OrcaParamDropdownRow(
                    label = "Wall generator",
                    options = WallGeneratorType.entries,
                    selected = proc.wallGenerator,
                    optionLabel = { it.label },
                    onSelect = { wg -> viewModel.updateProcess { it.copy(wallGenerator = wg) } }
                )
                OrcaParamNumberRow(
                    label = "Wall transitioning threshold angle",
                    value = proc.wallTransitionThresholdAngleDeg.toFloat(),
                    unitText = "°",
                    range = 1f..60f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(wallTransitionThresholdAngleDeg = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Wall transitioning filter margin",
                    value = proc.wallTransitionFilterMarginPercent.toFloat(),
                    unitText = "%",
                    range = 0f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(wallTransitionFilterMarginPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Wall transition length",
                    value = proc.wallTransitionLengthPercent.toFloat(),
                    unitText = "%",
                    range = 10f..300f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(wallTransitionLengthPercent = v.toInt()) } }
                )
                OrcaParamIntStepperRow(
                    label = "Wall distribution count",
                    value = proc.wallDistributionCount,
                    unitText = "",
                    range = 1..6,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(wallDistributionCount = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer minimum wall width",
                    value = proc.firstLayerMinWallWidthPercent.toFloat(),
                    unitText = "%",
                    range = 30f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(firstLayerMinWallWidthPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Minimum wall width",
                    value = proc.minWallWidthPercent.toFloat(),
                    unitText = "%",
                    range = 30f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(minWallWidthPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Minimum feature size",
                    value = proc.minFeatureSizePercent.toFloat(),
                    unitText = "%",
                    range = 1f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(minFeatureSizePercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Minimum wall length",
                    value = proc.minWallLengthMm,
                    unitText = "mm",
                    range = 0.0f..5.0f,
                    step = 0.1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(minWallLengthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Minimum wall volume",
                    value = proc.minWallVolumeMm3,
                    unitText = "mm³",
                    range = 0.0f..2.0f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(minWallVolumeMm3 = v) } }
                )
                OrcaParamNumberRow(
                    label = "Maximum wall deviation",
                    value = proc.maxWallDeviationMm,
                    unitText = "mm",
                    range = 0.001f..0.200f,
                    step = 0.005f,
                    decimals = 3,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(maxWallDeviationMm = v) } }
                )
            }

            if (matches("walls and surfaces", "printing order", "infill first", "loop direction", "flow ratio", "only one wall", "avoid crossing", "detour", "small area")) {
                OrcaSectionGroupHeader(Icons.Default.Layers, "Walls and surfaces")
                OrcaParamDropdownRow(
                    label = "Walls printing order",
                    options = WallsPrintingOrder.entries,
                    selected = proc.wallsPrintingOrder,
                    optionLabel = { it.label },
                    onSelect = { wo -> viewModel.updateProcess { it.copy(wallsPrintingOrder = wo) } }
                )
                OrcaParamCheckboxRow(
                    label = "Print infill first",
                    checked = proc.printInfillFirst,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(printInfillFirst = b) } }
                )
                OrcaParamDropdownRow(
                    label = "Wall loop direction",
                    options = WallLoopDirection.entries,
                    selected = proc.wallLoopDirection,
                    optionLabel = { it.label },
                    onSelect = { wd -> viewModel.updateProcess { it.copy(wallLoopDirection = wd) } }
                )
                OrcaParamNumberRow(
                    label = "Top surface flow ratio",
                    value = proc.topSurfaceFlowRatio,
                    unitText = "",
                    range = 0.50f..1.50f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topSurfaceFlowRatio = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bottom surface flow ratio",
                    value = proc.bottomSurfaceFlowRatio,
                    unitText = "",
                    range = 0.50f..1.50f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bottomSurfaceFlowRatio = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Set other flow ratios",
                    checked = proc.setOtherFlowRatios,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(setOtherFlowRatios = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Only one wall on first layer",
                    checked = proc.onlyOneWallFirstLayer,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(onlyOneWallFirstLayer = b) } }
                )
                OrcaParamDropdownRow(
                    label = "Only one wall on top surfaces",
                    options = TopOneWallMode.entries,
                    selected = proc.topOneWallMode,
                    optionLabel = { it.label },
                    onSelect = { tm ->
                        viewModel.updateProcess {
                            it.copy(
                                topOneWallMode = tm,
                                onlyOneWallOnTop = tm != TopOneWallMode.NOT_APPLIED
                            )
                        }
                    }
                )
                OrcaParamCheckboxRow(
                    label = "Avoid crossing walls",
                    checked = proc.avoidCrossingWalls,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(avoidCrossingWalls = b) } }
                )
                OrcaParamNumberRow(
                    label = "Avoid crossing walls - Max detour length",
                    value = proc.avoidCrossingWallsMaxDetourMm,
                    unitText = "mm or %",
                    range = 0f..500f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(avoidCrossingWallsMaxDetourMm = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Small area flow compensation (beta)",
                    checked = proc.smallAreaFlowCompensation,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(smallAreaFlowCompensation = b) } }
                )
            }

            if (matches("bridging", "bridge flow", "bridge density", "thick", "counterbore")) {
                OrcaSectionGroupHeader(Icons.Default.LinearScale, "Bridging")
                OrcaParamNumberRow(
                    label = "Bridge flow ratio",
                    value = proc.bridgeFlowRatio,
                    unitText = "",
                    range = 0.50f..1.50f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bridgeFlowRatio = v) } }
                )
                OrcaParamNumberRow(
                    label = "Internal bridge flow ratio",
                    value = proc.internalBridgeFlowRatio,
                    unitText = "",
                    range = 0.50f..1.50f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalBridgeFlowRatio = v) } }
                )
                OrcaParamNumberRow(
                    label = "External bridge density",
                    value = proc.externalBridgeDensityPercent.toFloat(),
                    unitText = "%",
                    range = 10f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(externalBridgeDensityPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Internal bridge density",
                    value = proc.internalBridgeDensityPercent.toFloat(),
                    unitText = "%",
                    range = 10f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalBridgeDensityPercent = v.toInt()) } }
                )
                OrcaParamCheckboxRow(
                    label = "Thick external bridges",
                    checked = proc.thickExternalBridges,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(thickExternalBridges = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Thick internal bridges",
                    checked = proc.thickInternalBridges,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(thickInternalBridges = b) } }
                )
                OrcaParamDropdownRow(
                    label = "Extra bridge layers (beta)",
                    options = ExtraBridgeLayersMode.entries,
                    selected = proc.extraBridgeLayersMode,
                    optionLabel = { it.label },
                    onSelect = { eb -> viewModel.updateProcess { it.copy(extraBridgeLayersMode = eb) } }
                )
                OrcaParamDropdownRow(
                    label = "Filter out small internal bridges",
                    options = FilterSmallBridgesMode.entries,
                    selected = proc.filterOutSmallInternalBridges,
                    optionLabel = { it.label },
                    onSelect = { fb -> viewModel.updateProcess { it.copy(filterOutSmallInternalBridges = fb) } }
                )
                OrcaParamDropdownRow(
                    label = "Bridge counterbore holes",
                    options =BridgeCounterboreMode.entries,
                    selected = proc.bridgeCounterboreHoles,
                    optionLabel = { it.label },
                    onSelect = { bc -> viewModel.updateProcess { it.copy(bridgeCounterboreHoles = bc) } }
                )
            }

            if (matches("overhangs", "detect overhang", "printable", "extra perimeters", "reverse on even")) {
                OrcaSectionGroupHeader(Icons.Default.AutoAwesome, "Overhangs")
                OrcaParamCheckboxRow(
                    label = "Detect overhang walls",
                    checked = proc.detectOverhangWalls,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(detectOverhangWalls = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Make overhangs printable",
                    checked = proc.makeOverhangsPrintable,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(makeOverhangsPrintable = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Extra perimeters on overhangs",
                    checked = proc.extraPerimetersOnOverhangs,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(extraPerimetersOnOverhangs = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Reverse on even",
                    checked = proc.reverseOnEven,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(reverseOnEven = b) } }
                )
            }
        }

        // =====================================================================
        // TAB 2: STRENGTH (Complete 1:1 OrcaSlicer Strength Settings from PDF)
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.STRENGTH || query.isNotEmpty()) {
            if (matches("wall loops", "thin walls", "alternate extra wall")) {
                OrcaSectionGroupHeader(Icons.Default.Shield, "Walls")
                OrcaParamIntStepperRow(
                    label = "Wall loops",
                    value = proc.wallLoops,
                    unitText = "",
                    range = 1..15,
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
            }

            if (matches("top shell", "bottom shell", "surface pattern", "surface density", "overlap")) {
                OrcaSectionGroupHeader(Icons.Default.Layers, "Top/bottom shells")
                OrcaParamIntStepperRow(
                    label = "Top shell layers",
                    value = proc.topShellLayers,
                    unitText = "layers",
                    range = 0..20,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topShellLayers = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top shell thickness",
                    value = proc.topShellThicknessMm,
                    unitText = "mm",
                    range = 0.0f..5.0f,
                    step = 0.2f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topShellThicknessMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top surface density",
                    value = proc.topSurfaceDensityPercent.toFloat(),
                    unitText = "%",
                    range = 0f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topSurfaceDensityPercent = v.toInt()) } }
                )
                OrcaParamDropdownRow(
                    label = "Top surface pattern",
                    options = SurfacePattern.entries,
                    selected = proc.topSurfacePattern,
                    optionLabel = { it.label },
                    onSelect = { sp -> viewModel.updateProcess { it.copy(topSurfacePattern = sp) } }
                )
                OrcaParamIntStepperRow(
                    label = "Bottom shell layers",
                    value = proc.bottomShellLayers,
                    unitText = "layers",
                    range = 0..20,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bottomShellLayers = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bottom shell thickness",
                    value = proc.bottomShellThicknessMm,
                    unitText = "mm",
                    range = 0.0f..5.0f,
                    step = 0.2f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bottomShellThicknessMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bottom surface density",
                    value = proc.bottomSurfaceDensityPercent.toFloat(),
                    unitText = "%",
                    range = 0f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bottomSurfaceDensityPercent = v.toInt()) } }
                )
                OrcaParamDropdownRow(
                    label = "Bottom surface pattern",
                    options = SurfacePattern.entries,
                    selected = proc.bottomSurfacePattern,
                    optionLabel = { it.label },
                    onSelect = { sp -> viewModel.updateProcess { it.copy(bottomSurfacePattern = sp) } }
                )
                OrcaParamNumberRow(
                    label = "Top/bottom solid infill/wall overlap",
                    value = proc.topBottomSolidInfillWallOverlapPercent.toFloat(),
                    unitText = "%",
                    range = 0f..80f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topBottomSolidInfillWallOverlapPercent = v.toInt()) } }
                )
            }

            if (matches("infill", "sparse infill", "gyroid", "cross hatch", "multiline", "anchor", "gap fill", "overlap")) {
                OrcaSectionGroupHeader(Icons.Default.GridOn, "Infill")
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
                OrcaParamIntStepperRow(
                    label = "Fill Multiline",
                    value = proc.fillMultiline,
                    unitText = "",
                    range = 1..6,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(fillMultiline = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Sparse infill pattern",
                    options = InfillPattern.entries,
                    selected = proc.sparseInfillPattern,
                    optionLabel = { it.label },
                    onSelect = { pat -> viewModel.updateProcess { it.copy(sparseInfillPattern = pat) } }
                )
                OrcaParamNumberRow(
                    label = "Sparse infill direction",
                    value = proc.infillDirectionDeg.toFloat(),
                    unitText = "°",
                    range = 0f..360f,
                    step = 15f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(infillDirectionDeg = v.toInt()) } }
                )
                OrcaParamTextRow(
                    label = "Sparse infill rotation template",
                    value = proc.sparseInfillRotationTemplate,
                    placeholder = "e.g. 0, 90",
                    onValueChange = { s -> viewModel.updateProcess { it.copy(sparseInfillRotationTemplate = s) } }
                )
                OrcaParamNumberRow(
                    label = "Maximum length of the infill anchor",
                    value = proc.maxInfillAnchorLengthMm,
                    unitText = "mm or %",
                    range = 0f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(maxInfillAnchorLengthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Sparse infill anchor length",
                    value = proc.sparseInfillAnchorLengthPercent.toFloat(),
                    unitText = "%",
                    range = 0f..1000f,
                    step = 50f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(sparseInfillAnchorLengthPercent = v.toInt()) } }
                )
                OrcaParamDropdownRow(
                    label = "Internal solid infill pattern",
                    options = SurfacePattern.entries,
                    selected = proc.internalSolidInfillPattern,
                    optionLabel = { it.label },
                    onSelect = { pat -> viewModel.updateProcess { it.copy(internalSolidInfillPattern = pat) } }
                )
                OrcaParamNumberRow(
                    label = "Solid infill direction",
                    value = proc.solidInfillDirectionDeg.toFloat(),
                    unitText = "°",
                    range = 0f..360f,
                    step = 15f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(solidInfillDirectionDeg = v.toInt()) } }
                )
                OrcaParamTextRow(
                    label = "Solid infill rotation template",
                    value = proc.solidInfillRotationTemplate,
                    placeholder = "e.g. 45, 135",
                    onValueChange = { s -> viewModel.updateProcess { it.copy(solidInfillRotationTemplate = s) } }
                )
                OrcaParamDropdownRow(
                    label = "Apply gap fill",
                    options = GapFillPlacement.entries,
                    selected = proc.applyGapFill,
                    optionLabel = { it.label },
                    onSelect = { gf -> viewModel.updateProcess { it.copy(applyGapFill = gf) } }
                )
                OrcaParamNumberRow(
                    label = "Filter out tiny gaps",
                    value = proc.filterOutTinyGapsMm,
                    unitText = "mm",
                    range = 0f..5f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(filterOutTinyGapsMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Infill/wall overlap",
                    value = proc.infillWallOverlapPercent.toFloat(),
                    unitText = "%",
                    range = 0f..80f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(infillWallOverlapPercent = v.toInt()) } }
                )
            }

            if (matches("advanced", "align infill", "infill solid layers", "bridge infill", "threshold", "combination", "vertical shell")) {
                OrcaSectionGroupHeader(Icons.Default.Tune, "Advanced")
                OrcaParamCheckboxRow(
                    label = "Align infill direction to model",
                    checked = proc.alignInfillDirectionToModel,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(alignInfillDirectionToModel = b) } }
                )
                OrcaParamIntStepperRow(
                    label = "Infill solid layers",
                    value = proc.infillSolidLayersInterval,
                    unitText = "layers",
                    range = 0..200,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(infillSolidLayersInterval = v) } }
                )
                OrcaParamNumberRow(
                    label = "External bridge infill direction",
                    value = proc.externalBridgeInfillDirectionDeg.toFloat(),
                    unitText = "°",
                    range = 0f..360f,
                    step = 15f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(externalBridgeInfillDirectionDeg = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Internal bridge infill direction",
                    value = proc.internalBridgeInfillDirectionDeg.toFloat(),
                    unitText = "°",
                    range = 0f..360f,
                    step = 15f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalBridgeInfillDirectionDeg = v.toInt()) } }
                )
                OrcaParamCheckboxRow(
                    label = "Relative bridge angle",
                    checked = proc.relativeBridgeAngle,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(relativeBridgeAngle = b) } }
                )
                OrcaParamNumberRow(
                    label = "Minimum sparse infill threshold",
                    value = proc.minSparseInfillThresholdMm2,
                    unitText = "mm²",
                    range = 0f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(minSparseInfillThresholdMm2 = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Infill combination",
                    checked = proc.infillCombination,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(infillCombination = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Detect narrow internal solid infill",
                    checked = proc.detectNarrowInternalSolidInfill,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(detectNarrowInternalSolidInfill = b) } }
                )
                OrcaParamDropdownRow(
                    label = "Ensure vertical shell thickness",
                    options = VerticalShellThicknessMode.entries,
                    selected = proc.ensureVerticalShellThickness,
                    optionLabel = { it.label },
                    onSelect = { vs -> viewModel.updateProcess { it.copy(ensureVerticalShellThickness = vs) } }
                )
            }
        }

        // =====================================================================
        // TAB 3: SPEED (Complete 1:1 OrcaSlicer Speed Settings from PDF)
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.SPEED || query.isNotEmpty()) {
            if (matches("first layer speed", "slow layers")) {
                OrcaSectionGroupHeader(Icons.Default.Speed, "First layer speed")
                OrcaParamNumberRow(
                    label = "First layer",
                    value = proc.initialLayerSpeedMmS,
                    unitText = "mm/s",
                    range = 5f..250f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(initialLayerSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer infill",
                    value = proc.initialLayerInfillSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..300f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(initialLayerInfillSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer travel speed",
                    value = proc.initialLayerTravelSpeedPercent.toFloat(),
                    unitText = "%",
                    range = 10f..200f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(initialLayerTravelSpeedPercent = v.toInt()) } }
                )
                OrcaParamIntStepperRow(
                    label = "Number of slow layers",
                    value = proc.numberOfSlowLayers,
                    unitText = "",
                    range = 0..10,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(numberOfSlowLayers = v) } }
                )
            }

            if (matches("other layers speed", "outer wall", "inner wall", "small perimeters", "sparse infill", "internal solid", "top surface", "gap infill", "support")) {
                OrcaSectionGroupHeader(Icons.Default.Speed, "Other layers speed")
                OrcaParamNumberRow(
                    label = "Outer wall",
                    value = proc.outerWallSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..600f,
                    step = 10f,
                    decimals = 0,
                    boldLabel = true,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(outerWallSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Inner wall",
                    value = proc.innerWallSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..600f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(innerWallSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Small perimeters",
                    value = proc.smallPerimeterSpeedPercent.toFloat(),
                    unitText = "%",
                    range = 10f..150f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(smallPerimeterSpeedPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Small perimeters threshold",
                    value = proc.smallPerimeterThresholdMm,
                    unitText = "mm",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(smallPerimeterThresholdMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Sparse infill",
                    value = proc.sparseInfillSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..700f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(sparseInfillSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Internal solid infill",
                    value = proc.internalSolidInfillSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..600f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalSolidInfillSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top surface",
                    value = proc.topSurfaceSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..400f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topSurfaceSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Gap infill",
                    value = proc.gapInfillSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..400f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(gapInfillSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Support",
                    value = proc.supportSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..400f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Support interface",
                    value = proc.supportInterfaceSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..300f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportInterfaceSpeedMmS = v) } }
                )
            }

            if (matches("overhang speed", "curled perimeters", "bridge")) {
                OrcaSectionGroupHeader(Icons.Default.Speed, "Overhang speed")
                OrcaParamCheckboxRow(
                    label = "Slow down for overhangs",
                    checked = proc.slowDownForOverhangs,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(slowDownForOverhangs = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Slow down for curled perimeters",
                    checked = proc.slowDownForCurledPerimeters,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(slowDownForCurledPerimeters = b) } }
                )
                OrcaParamNumberRow(
                    label = "Overhang speed (10%-25%)",
                    value = proc.overhangSpeed10MmS,
                    unitText = "mm/s",
                    range = 0f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(overhangSpeed10MmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Overhang speed (25%-50%)",
                    value = proc.overhangSpeed25MmS,
                    unitText = "mm/s",
                    range = 0f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(overhangSpeed25MmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Overhang speed (50%-75%)",
                    value = proc.overhangSpeed50MmS,
                    unitText = "mm/s",
                    range = 0f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(overhangSpeed50MmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Overhang speed (75%-100%)",
                    value = proc.overhangSpeed75MmS,
                    unitText = "mm/s",
                    range = 0f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(overhangSpeed75MmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bridge",
                    value = proc.bridgeSpeedMmS,
                    unitText = "mm/s",
                    range = 5f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bridgeSpeedMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Internal bridge",
                    value = proc.internalBridgeSpeedPercent.toFloat(),
                    unitText = "%",
                    range = 20f..300f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalBridgeSpeedPercent = v.toInt()) } }
                )
            }

            if (matches("travel speed", "travel", "combing", "retract before outer wall", "avoid printed parts", "avoid supports", "z hop", "layer start")) {
                OrcaSectionGroupHeader(Icons.Default.Speed, "Travel & Combing Mode")
                OrcaParamNumberRow(
                    label = "Travel speed",
                    value = proc.travelSpeedMmS,
                    unitText = "mm/s",
                    range = 50f..800f,
                    step = 20f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(travelSpeedMmS = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Combing mode",
                    options = CombingMode.entries,
                    selected = proc.combingMode,
                    optionLabel = { it.label },
                    onSelect = { cm -> viewModel.updateProcess { it.copy(combingMode = cm) } }
                )
                if (proc.combingMode != CombingMode.OFF) {
                    OrcaParamNumberRow(
                        label = "Max comb distance with no retract",
                        value = proc.maxCombDistanceWithNoRetractMm,
                        unitText = "mm",
                        range = 0f..200f,
                        step = 5f,
                        decimals = 1,
                        onValueChange = { v -> viewModel.updateProcess { it.copy(maxCombDistanceWithNoRetractMm = v) } }
                    )
                    OrcaParamCheckboxRow(
                        label = "Retract before outer wall",
                        checked = proc.retractBeforeOuterWall,
                        onCheckedChange = { b -> viewModel.updateProcess { it.copy(retractBeforeOuterWall = b) } }
                    )
                    OrcaParamCheckboxRow(
                        label = "Avoid printed parts when traveling",
                        checked = proc.avoidPrintedPartsWhenTraveling,
                        onCheckedChange = { b -> viewModel.updateProcess { it.copy(avoidPrintedPartsWhenTraveling = b) } }
                    )
                    OrcaParamCheckboxRow(
                        label = "Avoid supports when traveling",
                        checked = proc.avoidSupportsWhenTraveling,
                        onCheckedChange = { b -> viewModel.updateProcess { it.copy(avoidSupportsWhenTraveling = b) } }
                    )
                    if (proc.avoidPrintedPartsWhenTraveling || proc.avoidSupportsWhenTraveling) {
                        OrcaParamNumberRow(
                            label = "Travel avoid distance",
                            value = proc.travelAvoidDistanceMm,
                            unitText = "mm",
                            range = 0.1f..5.0f,
                            step = 0.125f,
                            decimals = 3,
                            onValueChange = { v -> viewModel.updateProcess { it.copy(travelAvoidDistanceMm = v) } }
                        )
                    }
                }
                OrcaParamNumberRow(
                    label = "Layer start X",
                    value = proc.layerStartXMm,
                    unitText = "mm",
                    range = -300f..300f,
                    step = 5f,
                    decimals = 1,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(layerStartXMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Layer start Y",
                    value = proc.layerStartYMm,
                    unitText = "mm",
                    range = -300f..300f,
                    step = 5f,
                    decimals = 1,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(layerStartYMm = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Z hop when retracted",
                    checked = proc.zHopWhenRetracted,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(zHopWhenRetracted = b) } }
                )
                if (proc.zHopWhenRetracted) {
                    OrcaParamCheckboxRow(
                        label = "Z hop only over printed parts",
                        checked = proc.zHopOnlyOverPrintedParts,
                        onCheckedChange = { b -> viewModel.updateProcess { it.copy(zHopOnlyOverPrintedParts = b) } }
                    )
                    OrcaParamNumberRow(
                        label = "Z hop height",
                        value = proc.zHopHeightMm,
                        unitText = "mm",
                        range = 0.05f..3.0f,
                        step = 0.05f,
                        decimals = 2,
                        onValueChange = { v -> viewModel.updateProcess { it.copy(zHopHeightMm = v) } }
                    )
                }
            }

            if (matches("acceleration", "normal printing")) {
                OrcaSectionGroupHeader(Icons.Default.Speed, "Acceleration")
                OrcaParamNumberRow(
                    label = "Normal printing",
                    value = proc.defaultAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 100f..30000f,
                    step = 500f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(defaultAccelerationMmS2 = v) } }
                )
                OrcaParamNumberRow(
                    label = "Outer wall",
                    value = proc.outerWallAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 100f..30000f,
                    step = 500f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(outerWallAccelerationMmS2 = v) } }
                )
                OrcaParamNumberRow(
                    label = "Inner wall",
                    value = proc.innerWallAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 100f..30000f,
                    step = 500f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(innerWallAccelerationMmS2 = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bridge",
                    value = proc.bridgeAccelerationPercent.toFloat(),
                    unitText = "%",
                    range = 10f..200f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(bridgeAccelerationPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Sparse infill",
                    value = proc.sparseInfillAccelerationPercent.toFloat(),
                    unitText = "%",
                    range = 10f..200f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(sparseInfillAccelerationPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Internal solid infill",
                    value = proc.internalSolidInfillAccelerationPercent.toFloat(),
                    unitText = "%",
                    range = 10f..200f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(internalSolidInfillAccelerationPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "First layer",
                    value = proc.firstLayerAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 100f..10000f,
                    step = 100f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(firstLayerAccelerationMmS2 = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer travel",
                    value = proc.firstLayerTravelAccelerationPercent.toFloat(),
                    unitText = "%",
                    range = 10f..200f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(firstLayerTravelAccelerationPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Top surface",
                    value = proc.topSurfaceAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 100f..15000f,
                    step = 200f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(topSurfaceAccelerationMmS2 = v) } }
                )
                OrcaParamNumberRow(
                    label = "Travel",
                    value = proc.travelAccelerationMmS2,
                    unitText = "mm/s²",
                    range = 100f..30000f,
                    step = 500f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(travelAccelerationMmS2 = v) } }
                )
            }

            if (matches("jerk", "extrusion rate smoothing")) {
                OrcaSectionGroupHeader(Icons.Default.Speed, "Jerk (XY)")
                OrcaParamNumberRow(
                    label = "Default",
                    value = proc.jerkDefaultMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkDefaultMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Outer wall",
                    value = proc.jerkOuterWallMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkOuterWallMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Inner wall",
                    value = proc.jerkInnerWallMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkInnerWallMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Infill",
                    value = proc.jerkInfillMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkInfillMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Top surface",
                    value = proc.jerkTopSurfaceMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkTopSurfaceMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer",
                    value = proc.jerkFirstLayerMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkFirstLayerMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "First layer travel",
                    value = proc.jerkFirstLayerTravelMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkFirstLayerTravelMmS = v) } }
                )
                OrcaParamNumberRow(
                    label = "Travel",
                    value = proc.jerkTravelMmS,
                    unitText = "mm/s",
                    range = 0f..50f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(jerkTravelMmS = v) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Tune, "Advanced")
                OrcaParamNumberRow(
                    label = "Extrusion rate smoothing",
                    value = proc.extrusionRateSmoothingMm3S2,
                    unitText = "mm³/s²",
                    range = 0f..500f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(extrusionRateSmoothingMm3S2 = v) } }
                )
            }
        }

        // =====================================================================
        // TAB 4: SUPPORT (Complete 1:1 OrcaSlicer Support Settings from PDF)
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.SUPPORT || query.isNotEmpty()) {
            if (matches("support", "tree", "raft", "overhang", "interface")) {
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
                    range = 5f..85f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportThresholdAngleDeg = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Threshold overlap",
                    value = proc.supportThresholdOverlapPercent.toFloat(),
                    unitText = "%",
                    range = 0f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportThresholdOverlapPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "First layer density",
                    value = proc.supportFirstLayerDensityPercent.toFloat(),
                    unitText = "%",
                    range = 10f..100f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportFirstLayerDensityPercent = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "First layer expansion",
                    value = proc.supportFirstLayerExpansionMm,
                    unitText = "mm",
                    range = 0f..15f,
                    step = 0.5f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportFirstLayerExpansionMm = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "On build plate only",
                    checked = proc.supportOnBuildPlateOnly,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(supportOnBuildPlateOnly = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Ignore small overhangs",
                    checked = proc.removeSmallOverhangs,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(removeSmallOverhangs = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Layers, "Raft")
                OrcaParamIntStepperRow(
                    label = "Raft layers",
                    value = proc.raftLayers,
                    unitText = "layers",
                    range = 0..10,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(raftLayers = v) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Palette, "Filament for Supports")
                val filSlotChoices = listOf("Default") + state.filamentSlots.mapIndexed { i, f -> "${i + 1}: ${f.name}" }
                OrcaParamDropdownRow(
                    label = "Support/raft base",
                    options = filSlotChoices,
                    selected = proc.supportFilamentBase,
                    optionLabel = { it },
                    onSelect = { sf -> viewModel.updateProcess { it.copy(supportFilamentBase = sf) } }
                )
                OrcaParamDropdownRow(
                    label = "Support/raft interface",
                    options = filSlotChoices,
                    selected = proc.supportFilamentInterface,
                    optionLabel = { it },
                    onSelect = { sf -> viewModel.updateProcess { it.copy(supportFilamentInterface = sf) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Build, "Support ironing")
                OrcaParamCheckboxRow(
                    label = "Ironing Support Interface",
                    checked = proc.ironingSupportInterface,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(ironingSupportInterface = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Tune, "Advanced")
                OrcaParamNumberRow(
                    label = "Top Z distance",
                    value = proc.supportTopZDistanceMm,
                    unitText = "mm",
                    range = 0.0f..0.80f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportTopZDistanceMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bottom Z distance",
                    value = proc.supportBottomZDistanceMm,
                    unitText = "mm",
                    range = 0.0f..0.80f,
                    step = 0.02f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportBottomZDistanceMm = v) } }
                )
                OrcaParamIntStepperRow(
                    label = "Support wall loops",
                    value = proc.supportWallLoops,
                    unitText = "",
                    range = 0..6,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportWallLoops = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Base pattern",
                    options = SupportBasePattern.entries,
                    selected = proc.supportBasePattern,
                    optionLabel = { it.label },
                    onSelect = { bp -> viewModel.updateProcess { it.copy(supportBasePattern = bp) } }
                )
                OrcaParamNumberRow(
                    label = "Base pattern spacing",
                    value = proc.supportBasePatternSpacingMm,
                    unitText = "mm",
                    range = 0.5f..15.0f,
                    step = 0.5f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportBasePatternSpacingMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Pattern angle",
                    value = proc.supportPatternAngleDeg.toFloat(),
                    unitText = "°",
                    range = 0f..360f,
                    step = 15f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportPatternAngleDeg = v.toInt()) } }
                )
                OrcaParamIntStepperRow(
                    label = "Top interface layers",
                    value = proc.supportTopInterfaceLayers,
                    unitText = "layers",
                    range = 0..8,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportTopInterfaceLayers = v) } }
                )
                OrcaParamIntStepperRow(
                    label = "Bottom interface layers",
                    value = proc.supportBottomInterfaceLayers,
                    unitText = "layers",
                    range = 0..8,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportBottomInterfaceLayers = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Interface pattern",
                    options = SupportInterfacePattern.entries,
                    selected = proc.supportInterfacePattern,
                    optionLabel = { it.label },
                    onSelect = { ip -> viewModel.updateProcess { it.copy(supportInterfacePattern = ip) } }
                )
                OrcaParamNumberRow(
                    label = "Top interface spacing",
                    value = proc.supportTopInterfaceSpacingMm,
                    unitText = "mm",
                    range = 0.0f..3.0f,
                    step = 0.1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportTopInterfaceSpacingMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Bottom interface spacing",
                    value = proc.supportBottomInterfaceSpacingMm,
                    unitText = "mm",
                    range = 0.0f..3.0f,
                    step = 0.1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportBottomInterfaceSpacingMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Normal support expansion",
                    value = proc.normalSupportExpansionMm,
                    unitText = "mm",
                    range = -5.0f..5.0f,
                    step = 0.2f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(normalSupportExpansionMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Support/object XY distance",
                    value = proc.supportObjectXyDistanceMm,
                    unitText = "mm",
                    range = 0.10f..2.00f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportObjectXyDistanceMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Support/object first layer gap",
                    value = proc.supportObjectFirstLayerGapMm,
                    unitText = "mm",
                    range = 0.0f..2.0f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(supportObjectFirstLayerGapMm = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Don't support bridges",
                    checked = proc.dontSupportBridges,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(dontSupportBridges = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.AutoAwesome, "Tree support")
                OrcaParamNumberRow(
                    label = "Tip diameter",
                    value = proc.treeSupportTipDiameterMm,
                    unitText = "mm",
                    range = 0.4f..3.0f,
                    step = 0.1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(treeSupportTipDiameterMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Branch distance",
                    value = proc.treeSupportBranchDistanceMm,
                    unitText = "mm",
                    range = 0.2f..10.0f,
                    step = 0.5f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(treeSupportBranchDistanceMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Branch diameter",
                    value = proc.treeSupportBranchDiameterMm,
                    unitText = "mm",
                    range = 1.0f..10.0f,
                    step = 0.5f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(treeSupportBranchDiameterMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Branch angle",
                    value = proc.treeSupportBranchAngleDeg.toFloat(),
                    unitText = "°",
                    range = 15f..70f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(treeSupportBranchAngleDeg = v.toInt()) } }
                )
                OrcaParamNumberRow(
                    label = "Branch diameter angle",
                    value = proc.treeSupportBranchDiameterAngleDeg.toFloat(),
                    unitText = "°",
                    range = 0f..20f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(treeSupportBranchDiameterAngleDeg = v.toInt()) } }
                )
                OrcaParamIntStepperRow(
                    label = "Tree support wall loops",
                    value = proc.treeSupportWallLoops,
                    unitText = "loops",
                    range = 1..4,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(treeSupportWallLoops = v) } }
                )
                OrcaParamNumberRow(
                    label = "Tree support brim width",
                    value = proc.treeSupportBrimWidthMm,
                    unitText = "mm",
                    range = 0.0f..15.0f,
                    step = 0.5f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(treeSupportBrimWidthMm = v) } }
                )
            }
        }

        // =====================================================================
        // TAB 5: MULTIMATERIAL (Complete 1:1 OrcaSlicer Multimaterial Settings from PDF)
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.MULTIMATERIAL || query.isNotEmpty()) {
            if (matches("prime tower", "wipe tower", "flush", "ooze", "interlocking", "filament for features")) {
                val mm = proc.multimaterial
                OrcaSectionGroupHeader(Icons.Default.ColorLens, "Prime tower")
                OrcaParamCheckboxRow(
                    label = "Enable",
                    checked = mm.enablePrimeTower,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(enablePrimeTower = b)) } }
                )
                OrcaParamCheckboxRow(
                    label = "Skip points",
                    checked = mm.primeTowerSkipPoints,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerSkipPoints = b)) } }
                )
                OrcaParamCheckboxRow(
                    label = "Enable tower interface features",
                    checked = mm.primeTowerInterfaceFeatures,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerInterfaceFeatures = b)) } }
                )
                OrcaParamCheckboxRow(
                    label = "Internal ribs",
                    checked = mm.primeTowerInternalRibs,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerInternalRibs = b)) } }
                )
                OrcaParamNumberRow(
                    label = "Width",
                    value = mm.primeTowerWidthMm,
                    unitText = "mm",
                    range = 10f..120f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerWidthMm = v)) } }
                )
                OrcaParamNumberRow(
                    label = "Brim width",
                    value = mm.primeTowerBrimWidthMm,
                    unitText = "mm",
                    range = 0f..15f,
                    step = 1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerBrimWidthMm = v)) } }
                )
                OrcaParamNumberRow(
                    label = "Infill gap",
                    value = mm.primeTowerInfillGapPercent.toFloat(),
                    unitText = "%",
                    range = 50f..300f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerInfillGapPercent = v.toInt())) } }
                )
                OrcaParamNumberRow(
                    label = "Wipe tower rotation angle",
                    value = mm.primeTowerRotationAngleDeg.toFloat(),
                    unitText = "°",
                    range = -180f..180f,
                    step = 15f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerRotationAngleDeg = v.toInt())) } }
                )
                OrcaParamNumberRow(
                    label = "Maximal bridging distance",
                    value = mm.primeTowerMaxBridgingDistMm,
                    unitText = "mm",
                    range = 1f..50f,
                    step = 2f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerMaxBridgingDistMm = v)) } }
                )
                OrcaParamNumberRow(
                    label = "Wipe tower purge lines spacing",
                    value = mm.primeTowerPurgeLinesSpacingPercent.toFloat(),
                    unitText = "%",
                    range = 50f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerPurgeLinesSpacingPercent = v.toInt())) } }
                )
                OrcaParamNumberRow(
                    label = "Extra flow for purging",
                    value = mm.primeTowerExtraFlowPurgingPercent.toFloat(),
                    unitText = "%",
                    range = 50f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerExtraFlowPurgingPercent = v.toInt())) } }
                )
                OrcaParamNumberRow(
                    label = "Maximum wipe tower print speed",
                    value = mm.primeTowerMaxPrintSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..300f,
                    step = 10f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerMaxPrintSpeedMmS = v)) } }
                )
                OrcaParamDropdownRow(
                    label = "Wall type",
                    options = WipeTowerWallType.entries,
                    selected = mm.primeTowerWallType,
                    optionLabel = { it.label },
                    onSelect = { wt -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerWallType = wt)) } }
                )
                OrcaParamNumberRow(
                    label = "Extra rib length",
                    value = mm.primeTowerExtraRibLengthMm,
                    unitText = "mm",
                    range = 0f..50f,
                    step = 2f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerExtraRibLengthMm = v)) } }
                )
                OrcaParamNumberRow(
                    label = "Rib width",
                    value = mm.primeTowerRibWidthMm,
                    unitText = "mm",
                    range = 1f..30f,
                    step = 1f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerRibWidthMm = v)) } }
                )
                OrcaParamCheckboxRow(
                    label = "Fillet wall",
                    checked = mm.primeTowerFilletWall,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerFilletWall = b)) } }
                )
                OrcaParamCheckboxRow(
                    label = "No sparse layers (beta)",
                    checked = mm.primeTowerNoSparseLayers,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(primeTowerNoSparseLayers = b)) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Palette, "Filament for Features")
                val filChoices = listOf("Default") + state.filamentSlots.mapIndexed { i, f -> "${i + 1}: ${f.name}" }
                OrcaParamDropdownRow(
                    label = "Outer walls",
                    options = filChoices,
                    selected = mm.filamentOuterWalls,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(filamentOuterWalls = v)) } }
                )
                OrcaParamDropdownRow(
                    label = "Inner walls",
                    options = filChoices,
                    selected = mm.filamentInnerWalls,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(filamentInnerWalls = v)) } }
                )
                OrcaParamDropdownRow(
                    label = "Infill",
                    options = filChoices,
                    selected = mm.filamentInfill,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(filamentInfill = v)) } }
                )
                OrcaParamDropdownRow(
                    label = "Internal solid infill",
                    options = filChoices,
                    selected = mm.filamentInternalSolidInfill,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(filamentInternalSolidInfill = v)) } }
                )
                OrcaParamDropdownRow(
                    label = "Top surface",
                    options = filChoices,
                    selected = mm.filamentTopSurface,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(filamentTopSurface = v)) } }
                )
                OrcaParamDropdownRow(
                    label = "Bottom surface",
                    options = filChoices,
                    selected = mm.filamentBottomSurface,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(filamentBottomSurface = v)) } }
                )
                OrcaParamDropdownRow(
                    label = "Wipe tower",
                    options = filChoices,
                    selected = mm.filamentWipeTower,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(filamentWipeTower = v)) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Shield, "Ooze prevention")
                OrcaParamCheckboxRow(
                    label = "Enable",
                    checked = mm.enableOozeShield,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(enableOozeShield = b)) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Sync, "Flush options")
                OrcaParamCheckboxRow(
                    label = "Flush into objects' infill",
                    checked = mm.flushIntoInfill,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(flushIntoInfill = b)) } }
                )
                OrcaParamCheckboxRow(
                    label = "Flush into objects' support",
                    checked = mm.flushIntoSupport,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(flushIntoSupport = b)) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Tune, "Advanced")
                OrcaParamCheckboxRow(
                    label = "Use beam interlocking",
                    checked = mm.useBeamInterlocking,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(useBeamInterlocking = b)) } }
                )
                OrcaParamCheckboxRow(
                    label = "Interface shells",
                    checked = mm.interfaceShells,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(interfaceShells = b)) } }
                )
                OrcaParamNumberRow(
                    label = "Maximum width of a segmented region",
                    value = mm.maxWidthSegmentedRegionMm,
                    unitText = "mm",
                    range = 0f..50f,
                    step = 1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(maxWidthSegmentedRegionMm = v)) } }
                )
                OrcaParamNumberRow(
                    label = "Interlocking depth of a segmented region",
                    value = mm.interlockingDepthSegmentedRegionMm,
                    unitText = "mm",
                    range = 0f..20f,
                    step = 0.5f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(multimaterial = it.multimaterial.copy(interlockingDepthSegmentedRegionMm = v)) } }
                )
            }
        }

        // =====================================================================
        // TAB 6: OTHERS (Complete 1:1 OrcaSlicer Others Settings from PDF)
        // =====================================================================
        if (state.activeProcessTab == ProcessCategoryTab.OTHERS || query.isNotEmpty()) {
            if (matches("skirt", "draft shield", "brim", "fuzzy", "vase", "sequence", "slicing mode", "gcode", "post-processing", "notes")) {
                OrcaSectionGroupHeader(Icons.Default.Build, "Skirt")
                OrcaParamIntStepperRow(
                    label = "Skirt loops",
                    value = proc.skirtLoops,
                    unitText = "loops",
                    range = 0..10,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(skirtLoops = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Skirt type",
                    options = SkirtType.entries,
                    selected = proc.skirtType,
                    optionLabel = { it.label },
                    onSelect = { st -> viewModel.updateProcess { it.copy(skirtType = st) } }
                )
                OrcaParamNumberRow(
                    label = "Skirt minimum extrusion length",
                    value = proc.skirtMinExtrusionLengthMm,
                    unitText = "mm",
                    range = 0f..200f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(skirtMinExtrusionLengthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Skirt distance",
                    value = proc.skirtDistanceMm,
                    unitText = "mm",
                    range = 0.5f..30.0f,
                    step = 0.5f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(skirtDistanceMm = v) } }
                )
                OrcaParamIntStepperRow(
                    label = "Skirt height",
                    value = proc.skirtHeightLayers,
                    unitText = "layers",
                    range = 1..50,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(skirtHeightLayers = v) } }
                )
                OrcaParamNumberRow(
                    label = "Skirt speed",
                    value = proc.skirtSpeedMmS,
                    unitText = "mm/s",
                    range = 10f..250f,
                    step = 5f,
                    decimals = 0,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(skirtSpeedMmS = v) } }
                )
                OrcaParamDropdownRow(
                    label = "Draft shield",
                    options = DraftShieldMode.entries,
                    selected = proc.draftShieldMode,
                    optionLabel = { it.label },
                    onSelect = { ds -> viewModel.updateProcess { it.copy(draftShieldMode = ds) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Layers, "Brim")
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
                    range = 0f..30f,
                    step = 1f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(brimWidthMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Brim-object gap",
                    value = proc.brimObjectGapMm,
                    unitText = "mm",
                    range = 0.0f..2.0f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(brimObjectGapMm = v) } }
                )
                OrcaParamNumberRow(
                    label = "Brim flow ratio",
                    value = proc.brimFlowRatio,
                    unitText = "",
                    range = 0.5f..1.5f,
                    step = 0.05f,
                    onValueChange = { v -> viewModel.updateProcess { it.copy(brimFlowRatio = v) } }
                )
                OrcaParamCheckboxRow(
                    label = "Brim follows compensated outline",
                    checked = proc.brimFollowsCompensatedOutline,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(brimFollowsCompensatedOutline = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Combine brims",
                    checked = proc.combineBrims,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(combineBrims = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.AutoAwesome, "Special mode")
                OrcaParamDropdownRow(
                    label = "Slicing Mode",
                    options = SlicingModeType.entries,
                    selected = proc.slicingMode,
                    optionLabel = { it.label },
                    onSelect = { sm -> viewModel.updateProcess { it.copy(slicingMode = sm) } }
                )
                OrcaParamDropdownRow(
                    label = "Print sequence",
                    options = PrintSequence.entries,
                    selected = proc.printSequence,
                    optionLabel = { it.label },
                    onSelect = { ps -> viewModel.updateProcess { it.copy(printSequence = ps) } }
                )
                OrcaParamDropdownRow(
                    label = "Intra-layer order",
                    options = IntraLayerOrder.entries,
                    selected = proc.intraLayerOrder,
                    optionLabel = { it.label },
                    onSelect = { io -> viewModel.updateProcess { it.copy(intraLayerOrder = io) } }
                )
                OrcaParamCheckboxRow(
                    label = "Spiral vase",
                    checked = proc.spiralVaseMode,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(spiralVaseMode = b) } }
                )

                OrcaSectionGroupHeader(Icons.Default.GridOn, "Fuzzy skin")
                OrcaParamDropdownRow(
                    label = "Fuzzy skin",
                    options = FuzzySkinType.entries,
                    selected = proc.fuzzySkinType,
                    optionLabel = { it.label },
                    onSelect = { fs -> viewModel.updateProcess { it.copy(fuzzySkinType = fs) } }
                )
                if (proc.fuzzySkinType != FuzzySkinType.NONE) {
                    OrcaParamNumberRow(
                        label = "Fuzzy skin point distance",
                        value = proc.fuzzySkinPointDistMm,
                        unitText = "mm",
                        range = 0.1f..3.0f,
                        step = 0.1f,
                        onValueChange = { v -> viewModel.updateProcess { it.copy(fuzzySkinPointDistMm = v) } }
                    )
                    OrcaParamNumberRow(
                        label = "Fuzzy skin thickness",
                        value = proc.fuzzySkinThicknessMm,
                        unitText = "mm",
                        range = 0.05f..1.5f,
                        step = 0.05f,
                        onValueChange = { v -> viewModel.updateProcess { it.copy(fuzzySkinThicknessMm = v) } }
                    )
                }

                OrcaSectionGroupHeader(Icons.Default.Memory, "G-code output")
                OrcaParamCheckboxRow(
                    label = "Reduce infill retraction",
                    checked = proc.reduceInfillRetraction,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(reduceInfillRetraction = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Verbose G-code",
                    checked = proc.verboseGcode,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(verboseGcode = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Label objects",
                    checked = proc.labelObjects,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(labelObjects = b) } }
                )
                OrcaParamCheckboxRow(
                    label = "Exclude objects",
                    checked = proc.excludeObjectGcode,
                    onCheckedChange = { b -> viewModel.updateProcess { it.copy(excludeObjectGcode = b) } }
                )
                OrcaParamTextRow(
                    label = "Filename format",
                    value = proc.filenameFormat,
                    placeholder = "{input_filename_base}_{filament_type}_{print_time}.gcode",
                    onValueChange = { s -> viewModel.updateProcess { it.copy(filenameFormat = s) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Edit, "Change extrusion role G-code")
                OrcaParamMultiLineTextRow(
                    value = proc.changeExtrusionRoleGcode,
                    placeholder = "; Custom G-code executed when extrusion role changes",
                    onValueChange = { s -> viewModel.updateProcess { it.copy(changeExtrusionRoleGcode = s) } }
                )

                OrcaSectionGroupHeader(Icons.Default.Build, "Post-processing Scripts")
                OrcaParamMultiLineTextRow(
                    value = proc.postProcessingScripts,
                    placeholder = "; Post-processing scripts (one per line)",
                    onValueChange = { s -> viewModel.updateProcess { it.copy(postProcessingScripts = s) } }
                )

                OrcaSectionGroupHeader(Icons.Default.ListAlt, "Notes")
                OrcaParamMultiLineTextRow(
                    value = proc.notes,
                    placeholder = "Process profile notes...",
                    onValueChange = { s -> viewModel.updateProcess { it.copy(notes = s) } }
                )
            }
        }
    }
}

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
