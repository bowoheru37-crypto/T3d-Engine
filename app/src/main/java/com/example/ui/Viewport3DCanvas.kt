package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.core.GizmoMode
import com.example.engine.core.Vector3
import com.example.engine.ecs.CameraPresetView
import com.example.engine.viewmodel.EngineUiState
import com.example.engine.viewmodel.EngineViewModel
import com.example.ui.theme.*
import kotlin.math.sin

@Composable
fun Viewport3DCanvas(
    viewModel: EngineViewModel,
    uiState: EngineUiState,
    modifier: Modifier = Modifier
) {
    val scene = viewModel.scene
    val renderer = viewModel.renderer
    val selectedEntity = viewModel.getSelectedEntity()

    // Smooth render tick triggered by EngineViewModel loop
    var sculptTapPos by remember { mutableStateOf(Vector3.ZERO) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        // Main 3D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("viewport_3d_canvas")
                .pointerInput(uiState.gizmoMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            scene.camera.updateOrbit(0f, 0f, (zoom - 1.0f) * 8f)
                        } else {
                            scene.camera.updateOrbit(pan.x * 0.35f, -pan.y * 0.35f, 0f)
                        }
                    }
                }
                .pointerInput(uiState.gizmoMode, selectedEntity) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (uiState.gizmoMode == GizmoMode.SCULPT) {
                                // Apply sculpt brush at ground center
                                viewModel.performSculptAtCenter(Vector3(0f, 0f, 0f), 0.05f)
                            } else {
                                // Tap selection cycle or raycast entity
                                if (scene.entities.isNotEmpty()) {
                                    val currentIdx = scene.entities.indexOfFirst { it.id == uiState.selectedEntityId }
                                    val nextIdx = (currentIdx + 1) % scene.entities.size
                                    viewModel.selectEntity(scene.entities[nextIdx].id)
                                }
                            }
                        }
                    )
                }
        ) {
            // Render 3D Scene
            renderer.renderScene(
                drawScope = this,
                scene = scene,
                selectedEntity = selectedEntity,
                gizmoMode = uiState.gizmoMode,
                sculptRadius = viewModel.sculptTools.brushRadius,
                sculptCenter = Vector3(0f, 0f, 0f),
                isSculpting = viewModel.sculptTools.isSculpting,
                wireframeOverlay = uiState.showWireframeOverlay,
                showSkeleton = uiState.showSkeleton,
                particleSystem = viewModel.particleSystem,
                movieDirector = if (uiState.isCinematicMode || uiState.selectedTab == com.example.engine.viewmodel.StudioTab.CGI_MOVIE) viewModel.movieDirector else null,
                showPhysicsColliders = uiState.showPhysicsColliders,
                characterRigStudio = viewModel.characterStudio
            )
        }

        // Top Left Overlay: Real-Time Viewport Telemetry HUD
        Surface(
            color = DarkNavy.copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    text = "${scene.name} • ${scene.entities.size} Entities",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Polys: ${renderer.polygonCount} | Verts: ${renderer.vertexCount} | Calls: ${renderer.drawCallCount}",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
                Text(
                    text = "Physics: ${scene.physicsWorld.simulatedBodiesCount} Bodies | ${scene.physicsWorld.collisionCount} Col",
                    color = CyberAmber,
                    fontSize = 9.sp
                )
            }
        }

        // Top Right Overlay: Camera Presets & Speed Controls
        Column(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Camera Presets Bar
            Surface(
                color = DarkNavy.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.focusCameraOnSelected() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Focus Selected",
                            tint = CyberGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setCameraPreset(CameraPresetView.PERSPECTIVE_DEFAULT) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Default Cam",
                            tint = if (uiState.activeCameraPreset == CameraPresetView.PERSPECTIVE_DEFAULT) CyberCyan else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setCameraPreset(CameraPresetView.TOP) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("TOP", color = if (uiState.activeCameraPreset == CameraPresetView.TOP) CyberCyan else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { viewModel.setCameraPreset(CameraPresetView.FRONT) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("FRN", color = if (uiState.activeCameraPreset == CameraPresetView.FRONT) CyberCyan else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { viewModel.setCameraPreset(CameraPresetView.ISOMETRIC) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("ISO", color = if (uiState.activeCameraPreset == CameraPresetView.ISOMETRIC) CyberCyan else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { scene.showGrid = !scene.showGrid },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Grid",
                            tint = if (scene.showGrid) CyberGreen else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Simulation Speed Selector
            Surface(
                color = DarkNavy.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.5f to "0.5x", 1.0f to "1.0x", 2.0f to "2.0x").forEach { (speed, label) ->
                        val isSelected = uiState.simulationSpeed == speed
                        Surface(
                            color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.setSimulationSpeed(speed) }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) CyberCyan else TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Left Floating 3D Transform D-Pad Controller (When an entity is selected)
        if (selectedEntity != null) {
            Surface(
                color = DarkNavy.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.BottomStart)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "3D STEP: ${selectedEntity.name.take(10)}",
                        color = CyberCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // X-Axis Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.stepTransformSelected(-0.25f, 0f, 0f) }
                        ) {
                            Text("-X", color = CyberRed, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text("X", color = CyberRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.stepTransformSelected(0.25f, 0f, 0f) }
                        ) {
                            Text("+X", color = CyberRed, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    // Y-Axis Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.stepTransformSelected(0f, -0.25f, 0f) }
                        ) {
                            Text("-Y", color = CyberGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text("Y", color = CyberGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.stepTransformSelected(0f, 0.25f, 0f) }
                        ) {
                            Text("+Y", color = CyberGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    // Z-Axis Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.stepTransformSelected(0f, 0f, -0.25f) }
                        ) {
                            Text("-Z", color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text("Z", color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.stepTransformSelected(0f, 0f, 0.25f) }
                        ) {
                            Text("+Z", color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // Bottom Right Virtual Game Controls (Jump, Forward Push, Impulse)
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.BottomEnd)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Forward push button
                    Surface(
                        color = DarkNavy.copy(alpha = 0.9f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { viewModel.applyImpulseToSelected(Vector3(0f, 2f, 4f)) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Push", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Jump button
                    FloatingActionButton(
                        onClick = {
                            viewModel.applyImpulseToSelected(Vector3(0f, 6.5f, 0f))
                        },
                        containerColor = CyberCyan,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("jump_impulse_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Jump Physics",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "PHYSICS IMPULSE",
                    color = CyberCyan,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Notification Toast Bar
        if (uiState.notificationMessage != null) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = uiState.notificationMessage,
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
