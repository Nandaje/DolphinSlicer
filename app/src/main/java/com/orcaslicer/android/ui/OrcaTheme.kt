package com.orcaslicer.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ============================================================================
 * DolphinSlicer Theme & Color Palette (`OrcaTheme.kt`)
 * ============================================================================
 * Centralizes all dark-charcoal surfaces and Dolphin Azure / Teal accents used
 * across the 3D viewport, top bar, bottom drawer, and process settings panels.
 *
 * Human Maintainer Guide:
 * - To change the primary accent color across the entire app, edit [OrcaTeal]
 *   and [OrcaTealBright].
 * - To adjust dark mode surface contrast, edit [OrcaTopBarBg], [OrcaDarkBg],
 *   [OrcaPanelBg], [OrcaCardBg], and [OrcaInputBg].
 */

// Primary Brand Accents (Dolphin Azure / Teal)
internal val OrcaTeal = Color(0xFF1D88B8)           // Deep Azure primary fill
internal val OrcaTealBright = Color(0xFF29ABE2)     // Bright Azure highlight & borders
internal val OrcaActiveTabBg = Color(0xFF1D88B8)    // Active top navigation tab fill

// Dark Mode Workspace Surfaces
internal val OrcaTopBarBg = Color(0xFF18191C)       // Top app bar & browser bar background
internal val OrcaDarkBg = Color(0xFF1E1F23)         // Main 3D workspace & drawer background
internal val OrcaPanelBg = Color(0xFF232428)        // Left sidebar & settings panel background
internal val OrcaCardBg = Color(0xFF28292E)         // Elevated card & modal container
internal val OrcaInputBg = Color(0xFF2C2E33)        // Parameter text-field & chip background
internal val OrcaElevatedBg = Color(0xFF33353B)     // Secondary button & hover surface

// Borders, Dividers & Glass Overlays
internal val OrcaBorder = Color(0xFF3D3F46)         // Standard 1dp border for inputs & cards
internal val OrcaDivider = Color(0xFF2E3036)        // Subtle horizontal section divider
internal val OrcaGlassBg = Color(0xEB151822)        // Translucent dark slate for 3D overlays
internal val OrcaGlassBorder = Color(0x18FFFFFF)    // Subtle white rim on floating glass cards
internal val OrcaActiveBtnBg = Color(0x3329ABE2)    // Active tool button translucent fill
internal val OrcaActiveBtnBorder = Color(0x8829ABE2)// Active tool button border

// Typography Colors
internal val OrcaTextPrimary = Color(0xFFE6E8EC)    // Primary high-contrast label text
internal val OrcaTextSecondary = Color(0xFFA2A6B0)  // Secondary description & subtitle text
internal val OrcaUnitText = Color(0xFF7D828D)       // Measurement unit suffix text (mm, °C, %)

// Status & Telemetry Accents
internal val EmeraldGreen = Color(0xFF10B981)       // Klipper connected / ready badge
internal val EmeraldDimBg = Color(0x2210B981)       // Connected status pill background
internal val EmeraldBorder = Color(0x4D10B981)      // Connected status pill border

internal val OrcaColorScheme = darkColorScheme(
    primary = OrcaTeal,
    onPrimary = Color(0xFF04141E),
    secondary = OrcaTealBright,
    background = OrcaDarkBg,
    surface = OrcaPanelBg,
    surfaceVariant = OrcaCardBg,
    onBackground = OrcaTextPrimary,
    onSurface = OrcaTextPrimary
)

/**
 * Root Material3 wrapper applying the DolphinSlicer dark color scheme.
 */
@Composable
fun OrcaSlicerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrcaColorScheme,
        content = content
    )
}
