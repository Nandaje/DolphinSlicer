package com.orcaslicer.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.orcaslicer.android.ui.OrcaSlicerScreen
import com.orcaslicer.android.ui.OrcaViewModel

/**
 * ============================================================================
 * Android Entry Point (`MainActivity.kt`)
 * ============================================================================
 * Hosts the single-activity Jetpack Compose UI ([OrcaSlicerScreen]) and wires:
 * 1. Persistent user preferences (`dolphin_slicer_prefs`) via [OrcaViewModel.initPreferences]
 *    so the printer's IP address, port, and web URL are restored on startup.
 * 2. Incoming Android `ACTION_VIEW` file intents (`.stl`, `.3mf`, `.obj`, `.gcode`)
 *    opened from system file managers or browser downloads.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: OrcaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initPreferences(applicationContext)

        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        setContent {
            OrcaSlicerScreen(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            viewModel.importFromUri(contentResolver, uri)
        }
    }
}
