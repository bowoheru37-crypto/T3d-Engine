package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.core.GizmoMode
import com.example.engine.viewmodel.EngineUiState
import com.example.engine.viewmodel.EngineViewModel
import com.example.ui.theme.*

@Composable
fun TopEngineToolbar(
    viewModel: EngineViewModel,
    uiState: EngineUiState,
    modifier: Modifier = Modifier
) {
    val profiler = viewModel.profiler
    val scrollState = rememberScrollState()

    Surface(
        color = DarkNavy,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Top Row: Engine Brand, FPS/Memory Telemetry, and Main Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "T3D Logo",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "T3D ENGINE",
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (profiler.isThermalThrottling) "THERMAL THROTTLE" else "${profiler.activePreset.name.take(16)}",
                            color = if (profiler.isThermalThrottling) CyberRed else TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                // Telemetry Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // FPS Badge
                    Surface(
                        color = if (profiler.frameBudget.currentFps >= 50f) CyberGreen.copy(alpha = 0.15f) else CyberAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (profiler.frameBudget.currentFps >= 50f) CyberGreen.copy(alpha = 0.5f) else CyberAmber.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${profiler.frameBudget.currentFps.toInt()} FPS",
                            color = if (profiler.frameBudget.currentFps >= 50f) CyberGreen else CyberAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // RAM Badge
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${profiler.memoryBudget.usedMB.toInt()}M RAM",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Play / Pause / Reset & Save Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleSimulation() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Simulation",
                            tint = if (uiState.isSimulating) CyberGreen else CyberAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.resetScene() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("reset_scene_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Scene",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.saveProjectLocally() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("save_project_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Project",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Row: Gizmo Selector & Quick View Mode Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GizmoChip(
                    label = "Translate",
                    icon = Icons.Default.OpenWith,
                    isSelected = uiState.gizmoMode == GizmoMode.TRANSLATE,
                    onClick = { viewModel.setGizmoMode(GizmoMode.TRANSLATE) }
                )
                GizmoChip(
                    label = "Rotate",
                    icon = Icons.Default.Sync,
                    isSelected = uiState.gizmoMode == GizmoMode.ROTATE,
                    onClick = { viewModel.setGizmoMode(GizmoMode.ROTATE) }
                )
                GizmoChip(
                    label = "Scale",
                    icon = Icons.Default.AspectRatio,
                    isSelected = uiState.gizmoMode == GizmoMode.SCALE,
                    onClick = { viewModel.setGizmoMode(GizmoMode.SCALE) }
                )
                GizmoChip(
                    label = "Sculpt 3D",
                    icon = Icons.Default.Brush,
                    isSelected = uiState.gizmoMode == GizmoMode.SCULPT,
                    onClick = { viewModel.setGizmoMode(GizmoMode.SCULPT) }
                )
                GizmoChip(
                    label = "Free Orbit",
                    icon = Icons.Default.Visibility,
                    isSelected = uiState.gizmoMode == GizmoMode.VIEW_ONLY,
                    onClick = { viewModel.setGizmoMode(GizmoMode.VIEW_ONLY) }
                )
                // Cinema Movie Quick Toggle
                Surface(
                    color = if (uiState.isCinematicMode) CyberPurple.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(14.dp),
                    border = if (uiState.isCinematicMode) androidx.compose.foundation.BorderStroke(1.dp, CyberPurple) else null,
                    modifier = Modifier.clickable { viewModel.toggleCinematicMode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = "CGI Cinema Mode",
                            tint = if (uiState.isCinematicMode) CyberPurple else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isCinematicMode) "21:9 Cinema ON" else "CGI Cine",
                            color = if (uiState.isCinematicMode) CyberPurple else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (uiState.isCinematicMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                // Physics Colliders Quick Toggle
                Surface(
                    color = if (uiState.showPhysicsColliders) CyberGreen.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(14.dp),
                    border = if (uiState.showPhysicsColliders) androidx.compose.foundation.BorderStroke(1.dp, CyberGreen) else null,
                    modifier = Modifier.clickable { viewModel.togglePhysicsColliders() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Physics Colliders",
                            tint = if (uiState.showPhysicsColliders) CyberGreen else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.showPhysicsColliders) "Colliders ON" else "Colliders",
                            color = if (uiState.showPhysicsColliders) CyberGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (uiState.showPhysicsColliders) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GizmoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkCard,
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) CyberCyan else TextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) CyberCyan else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
