package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.viewmodel.EngineViewModel
import com.example.ui.StudioPanels
import com.example.ui.TopEngineToolbar
import com.example.ui.Viewport3DCanvas
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                T3DEngineApp()
            }
        }
    }
}

@Composable
fun T3DEngineApp(viewModel: EngineViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkObsidian)
        ) {
            // Top Toolbar & Telemetry
            TopEngineToolbar(
                viewModel = viewModel,
                uiState = uiState
            )

            // 3D Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Viewport3DCanvas(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }

            // Bottom Studio Panels (Hierarchy, Inspector, AI Studio, Scripting, Sculpt, Profiler, Export, Leaderboard)
            StudioPanels(
                viewModel = viewModel,
                uiState = uiState
            )
        }
    }
}
