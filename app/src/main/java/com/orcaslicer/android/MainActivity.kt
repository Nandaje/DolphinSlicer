package com.orcaslicer.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.orcaslicer.android.ui.OrcaSlicerScreen
import com.orcaslicer.android.ui.OrcaViewModel

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
