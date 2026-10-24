package com.orcaslicer.android.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orcaslicer.android.model.ToolpathType
import com.orcaslicer.android.network.NetworkProtocol
import java.util.Locale

/**
 * ============================================================================
 * Device Web Browser & Cost Calculator Panels (`DeviceNetworkPanel.kt`)
 * ============================================================================
 * Contains:
 * 1. [DeviceNetworkPanel]: Full-featured embedded Android `WebView` browser
 *    for Klipper (Fluidd / Mainsail) and OctoPrint web UIs, plus direct
 *    Moonraker / OctoPrint REST API G-code upload and live telemetry.
 * 2. [FilamentQuickCalculatorCard] & [SliceStatisticsSummaryCard]: Filament
 *    weight, length, electricity, machine wear, and quote price calculators.
 *
 * Human Maintainer Guide:
 * - Printer IP and web URL persistence uses Android `SharedPreferences`
 *   (`"dolphin_slicer_prefs"`) synced via [OrcaViewModel.updatePrinterNetworkConfig].
 * - To add new quick bookmark chips on the Device start screen, edit the
 *   `listOf(...)` inside [DeviceNetworkPanel].
 */

/**
 * Quick pre-slice card for configuring spool price and currency symbol.
 */
@Composable
internal fun FilamentQuickCalculatorCard(
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

/**
 * Detailed post-slice breakdown card showing print time, filament usage,
 * electricity cost, machine depreciation, and feature-by-feature time bars.
 */
@Composable
internal fun SliceStatisticsSummaryCard(
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
internal fun CostLineRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = OrcaTextSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OrcaTextPrimary)
    }
}

@Composable
internal fun StatMetricItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = OrcaTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrcaTextPrimary)
    }
}

/**
 * Full-screen `DEVICE` tab panel combining:
 * 1. Top Web Browser Address Bar (Back, Forward, Reload, IP/URL input, GO, Send G-Code, API drawer toggle)
 * 2. Collapsible Moonraker / OctoPrint API & Direct Upload Drawer
 * 3. Hardware-accelerated Android `WebView` with persistent IP/URL restoration and connection diagnostics
 */
@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun DeviceNetworkPanel(
    state: OrcaUiState,
    viewModel: OrcaViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember(context) {
        viewModel.initPreferences(context.applicationContext)
        context.applicationContext.getSharedPreferences("dolphin_slicer_prefs", android.content.Context.MODE_PRIVATE)
    }
    var showApiDrawer by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasNavigatedUrl by remember {
        val hasSavedPref = prefs.contains("printer_web_url") || prefs.contains("printer_host")
        val nonDefaultState = state.printerWebUrl.isNotBlank() ||
            (state.printerHost.trim() != "192.168.1.100" && state.printerHost.isNotBlank())
        mutableStateOf(hasSavedPref || nonDefaultState)
    }
    var pageLoadError by remember { mutableStateOf<String?>(null) }

    var addressBarText by remember {
        val savedWebUrl = prefs.getString("printer_web_url", null)?.trim().orEmpty()
        val savedHost = prefs.getString("printer_host", null)?.trim().orEmpty()
        val initTarget = savedWebUrl
            .ifEmpty { state.printerWebUrl.trim() }
            .ifEmpty { savedHost }
            .ifEmpty { state.printerHost.trim() }
            .ifEmpty { "192.168.1.100" }
        mutableStateOf(
            if (initTarget.startsWith("http://", ignoreCase = true) || initTarget.startsWith("https://", ignoreCase = true)) {
                initTarget
            } else {
                "http://$initTarget"
            }
        )
    }

    /**
     * Saves the user's IP/hostname and full normalized URL to `SharedPreferences`
     * immediately so closing and reopening the app restores the exact printer address.
     */
    fun persistAddressInput(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return
        val normalizedUrl = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val hostOnly = normalizedUrl.substringAfter("://").substringBefore("/").substringBefore(":")
        if (hostOnly.isNotBlank()) {
            viewModel.updatePrinterNetworkConfig(host = hostOnly, webUrl = normalizedUrl)
        } else {
            viewModel.updatePrinterNetworkConfig(webUrl = normalizedUrl)
        }
    }

    /**
     * Normalizes the input address, persists it, and directs the embedded `WebView` to load it.
     */
    fun navigateToAddress(rawInput: String) {
        focusManager.clearFocus()
        val trimmed = rawInput.trim().ifEmpty { "192.168.1.100" }
        val normalizedUrl = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "http://$trimmed"
        }
        addressBarText = normalizedUrl
        persistAddressInput(normalizedUrl)
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
                    onValueChange = {
                        addressBarText = it
                        persistAddressInput(it)
                    },
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
                                val nextUrl = "http://${it.trim()}"
                                viewModel.updatePrinterNetworkConfig(host = it, webUrl = nextUrl)
                                addressBarText = nextUrl
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
                                    persistAddressInput(url)
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
                                onValueChange = {
                                    addressBarText = it
                                    persistAddressInput(it)
                                },
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
