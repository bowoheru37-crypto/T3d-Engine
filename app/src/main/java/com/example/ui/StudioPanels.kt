package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ai.AIAssetType
import com.example.engine.animation.*
import com.example.engine.cgi.*
import com.example.engine.character.*
import com.example.engine.core.*
import com.example.engine.data.LeaderboardEntry
import com.example.engine.ecs.Entity
import com.example.engine.ecs.EntityTag
import com.example.engine.export.ExportTarget
import com.example.engine.generators.*
import com.example.engine.particles.*
import com.example.engine.physics.*
import com.example.engine.profiler.DeviceTier
import com.example.engine.profiler.QualityPreset
import com.example.engine.spatial.*
import com.example.engine.terrain.*
import com.example.engine.world.*
import com.example.engine.tools.SculptBrushType
import com.example.engine.viewmodel.EngineUiState
import com.example.engine.viewmodel.EngineViewModel
import com.example.engine.viewmodel.StudioTab
import com.example.ui.theme.*

@Composable
fun StudioPanels(
    viewModel: EngineViewModel,
    uiState: EngineUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        color = DarkNavy,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab Selector Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkObsidian.copy(alpha = 0.6f))
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabNavButton(
                    label = "Scene",
                    icon = Icons.Default.AccountTree,
                    isSelected = uiState.selectedTab == StudioTab.HIERARCHY,
                    onClick = { viewModel.setTab(StudioTab.HIERARCHY) }
                )
                TabNavButton(
                    label = "Inspector",
                    icon = Icons.Default.Tune,
                    isSelected = uiState.selectedTab == StudioTab.INSPECTOR,
                    onClick = { viewModel.setTab(StudioTab.INSPECTOR) }
                )
                TabNavButton(
                    label = "World & Stage",
                    icon = Icons.Default.Landscape,
                    isSelected = uiState.selectedTab == StudioTab.TERRAIN_GEN,
                    onClick = { viewModel.setTab(StudioTab.TERRAIN_GEN) }
                )
                TabNavButton(
                    label = "Asset Forge",
                    icon = Icons.Default.Category,
                    isSelected = uiState.selectedTab == StudioTab.ASSET_FORGE,
                    onClick = { viewModel.setTab(StudioTab.ASSET_FORGE) }
                )
                TabNavButton(
                    label = "Particles",
                    icon = Icons.Default.Flare,
                    isSelected = uiState.selectedTab == StudioTab.PARTICLES,
                    onClick = { viewModel.setTab(StudioTab.PARTICLES) }
                )
                TabNavButton(
                    label = "Timeline",
                    icon = Icons.Default.Animation,
                    isSelected = uiState.selectedTab == StudioTab.ANIMATION,
                    onClick = { viewModel.setTab(StudioTab.ANIMATION) }
                )
                TabNavButton(
                    label = "Physics Lab",
                    icon = Icons.Default.Science,
                    isSelected = uiState.selectedTab == StudioTab.PHYSICS_LAB,
                    onClick = { viewModel.setTab(StudioTab.PHYSICS_LAB) }
                )
                TabNavButton(
                    label = "CGI Movie",
                    icon = Icons.Default.MovieFilter,
                    isSelected = uiState.selectedTab == StudioTab.CGI_MOVIE,
                    onClick = { viewModel.setTab(StudioTab.CGI_MOVIE) }
                )
                TabNavButton(
                    label = "Character & Rig",
                    icon = Icons.Default.Accessibility,
                    isSelected = uiState.selectedTab == StudioTab.CHARACTER_RIG,
                    onClick = { viewModel.setTab(StudioTab.CHARACTER_RIG) }
                )
                TabNavButton(
                    label = "AI Studio",
                    icon = Icons.Default.AutoAwesome,
                    isSelected = uiState.selectedTab == StudioTab.AI_STUDIO,
                    onClick = { viewModel.setTab(StudioTab.AI_STUDIO) }
                )
                TabNavButton(
                    label = "Scripting",
                    icon = Icons.Default.Code,
                    isSelected = uiState.selectedTab == StudioTab.SCRIPTING,
                    onClick = { viewModel.setTab(StudioTab.SCRIPTING) }
                )
                TabNavButton(
                    label = "Sculpt/Rig",
                    icon = Icons.Default.Brush,
                    isSelected = uiState.selectedTab == StudioTab.SCULPT_RIG,
                    onClick = { viewModel.setTab(StudioTab.SCULPT_RIG) }
                )
                TabNavButton(
                    label = "Profiler",
                    icon = Icons.Default.Speed,
                    isSelected = uiState.selectedTab == StudioTab.PROFILER,
                    onClick = { viewModel.setTab(StudioTab.PROFILER) }
                )
                TabNavButton(
                    label = "Export",
                    icon = Icons.Default.SystemUpdateAlt,
                    isSelected = uiState.selectedTab == StudioTab.EXPORT,
                    onClick = { viewModel.setTab(StudioTab.EXPORT) }
                )
                TabNavButton(
                    label = "Leaderboard",
                    icon = Icons.Default.Leaderboard,
                    isSelected = uiState.selectedTab == StudioTab.LEADERBOARD,
                    onClick = { viewModel.setTab(StudioTab.LEADERBOARD) }
                )
            }

            Divider(color = DarkBorder, thickness = 1.dp)

            // Panel Body Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .padding(8.dp)
            ) {
                when (uiState.selectedTab) {
                    StudioTab.HIERARCHY -> HierarchyPanel(viewModel, uiState)
                    StudioTab.INSPECTOR -> InspectorPanel(viewModel, uiState)
                    StudioTab.TERRAIN_GEN -> TerrainGenPanel(viewModel, uiState)
                    StudioTab.ASSET_FORGE -> AssetForgePanel(viewModel, uiState)
                    StudioTab.PARTICLES -> ParticlesPanel(viewModel, uiState)
                    StudioTab.ANIMATION -> AnimationTimelinePanel(viewModel, uiState)
                    StudioTab.PHYSICS_LAB -> PhysicsLabPanel(viewModel, uiState)
                    StudioTab.CGI_MOVIE -> CgiMoviePanel(viewModel, uiState)
                    StudioTab.CHARACTER_RIG -> CharacterRigPanel(viewModel, uiState)
                    StudioTab.AI_STUDIO -> AiStudioPanel(viewModel, uiState)
                    StudioTab.SCRIPTING -> ScriptingPanel(viewModel, uiState)
                    StudioTab.SCULPT_RIG -> SculptRigPanel(viewModel, uiState)
                    StudioTab.PROFILER -> ProfilerPanel(viewModel, uiState)
                    StudioTab.EXPORT -> ExportPanel(viewModel, uiState)
                    StudioTab.LEADERBOARD -> LeaderboardPanel(viewModel, uiState)
                }
            }
        }
    }
}

@Composable
fun TabNavButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) CyberCyan else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) CyberCyan else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ----------------------------------------------------
// 1. Hierarchy Panel (Scene Outliner)
// ----------------------------------------------------
@Composable
fun HierarchyPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Top action bar for spawning primitives
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val spawnItems = listOf(
                "Cube" to Icons.Default.Square,
                "Sphere" to Icons.Default.Circle,
                "Vehicle" to Icons.Default.DirectionsCar,
                "Tower" to Icons.Default.Apartment,
                "Crystal" to Icons.Default.Diamond,
                "Cylinder" to Icons.Default.ViewColumn,
                "Torus" to Icons.Default.DonutLarge,
                "Terrain" to Icons.Default.Terrain,
                "PhysicsProp" to Icons.Default.Inbox,
                "Light" to Icons.Default.Lightbulb
            )

            spawnItems.forEach { (type, icon) ->
                Button(
                    onClick = { viewModel.addPrimitive(type) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp).testTag("add_primitive_$type")
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ $type", fontSize = 10.sp)
                }
            }
        }

        Divider(color = DarkBorder, thickness = 0.5.dp)

        // Entity List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(viewModel.scene.entities) { entity ->
                val isSelected = entity.id == uiState.selectedEntityId
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectEntity(entity.id) }
                        .testTag("entity_item_${entity.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    entity.light != null -> Icons.Default.Lightbulb
                                    entity.rigidBody != null -> Icons.Default.SportsBaseball
                                    entity.script != null -> Icons.Default.Code
                                    else -> Icons.Default.ViewInAr
                                },
                                contentDescription = null,
                                tint = if (isSelected) CyberCyan else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = entity.name,
                                color = if (isSelected) CyberCyan else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (entity.rigidBody != null) {
                                Surface(
                                    color = CyberAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("PHYSICS", color = CyberAmber, fontSize = 7.sp, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                }
                            }
                            if (entity.script != null) {
                                Surface(
                                    color = CyberPurple.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("SCRIPT", color = CyberPurple, fontSize = 7.sp, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. Inspector Panel (Component & Transform Editor)
// ----------------------------------------------------
@Composable
fun InspectorPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val selected = viewModel.getSelectedEntity()
    val scrollState = rememberScrollState()

    if (selected == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an entity to inspect and modify properties", color = TextMuted, fontSize = 11.sp)
        }
        return
    }

    val entity = selected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entity.name,
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = entity.tag.name,
                        color = TextSecondary,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.duplicateSelectedEntity() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = { viewModel.deleteSelectedEntity() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberRed, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Section: Transform
        InspectorSection(title = "Transform 3D") {
            Vector3Row(label = "Pos", vec = entity.transform.position) { entity.transform.position = it }
            Vector3Row(label = "Rot", vec = entity.transform.rotation) { entity.transform.rotation = it }
            Vector3Row(label = "Scale", vec = entity.transform.scale) { entity.transform.scale = it }

            // Placement & Alignment Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = DarkNavy,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).clickable { viewModel.snapSelectedToGrid(0.5f) }
                ) {
                    Text("Grid Snap (0.5m)", color = CyberCyan, fontSize = 8.sp, modifier = Modifier.padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Surface(
                    color = DarkNavy,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).clickable { viewModel.groundSelectedToSurface() }
                ) {
                    Text("Ground Surface", color = CyberGreen, fontSize = 8.sp, modifier = Modifier.padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Surface(
                    color = DarkNavy,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).clickable { viewModel.alignSelectedToOrigin() }
                ) {
                    Text("Center Origin", color = CyberAmber, fontSize = 8.sp, modifier = Modifier.padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }

        // Section: Mesh & Geometry Stats
        if (entity.mesh != null) {
            val mesh = entity.mesh!!
            InspectorSection(title = "Mesh Geometry: ${mesh.name}") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vertices: ${mesh.vertices.size}", color = TextSecondary, fontSize = 10.sp)
                    Text("Polygons: ${mesh.triangles.size}", color = TextSecondary, fontSize = 10.sp)
                    Text("Normals: ${if (mesh.hasNormals) "Yes" else "No"}", color = CyberCyan, fontSize = 10.sp)
                }
            }
        }

        // Section: Material & Procedural Textures
        InspectorSection(title = "Material & PBR Shading") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Roughness: ${(entity.material.roughness * 100).toInt()}%", color = TextSecondary, fontSize = 10.sp)
                Slider(
                    value = entity.material.roughness,
                    onValueChange = { entity.material.roughness = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(140.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Metallic: ${(entity.material.metallic * 100).toInt()}%", color = TextSecondary, fontSize = 10.sp)
                Slider(
                    value = entity.material.metallic,
                    onValueChange = { entity.material.metallic = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(140.dp)
                )
            }

            // Quick Procedural Texture Palettes
            Text("Apply Procedural Texture:", color = TextMuted, fontSize = 9.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProceduralTexture.presets().forEach { tex ->
                    Surface(
                        color = DarkNavy,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tex.primaryColor.toComposeColor().copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { viewModel.applyTextureToSelectedEntity(tex) }
                    ) {
                        Text(
                            text = tex.name,
                            color = tex.primaryColor.toComposeColor(),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Section: Physics & RigidBody
        InspectorSection(title = "RigidBody 3D Physics") {
            if (entity.rigidBody != null) {
                val rb = entity.rigidBody!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Type: ${rb.bodyType}", color = TextSecondary, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        BodyType.values().forEach { type ->
                            Surface(
                                color = if (rb.bodyType == type) CyberAmber.copy(alpha = 0.2f) else DarkCard,
                                shape = RoundedCornerShape(4.dp),
                                border = if (rb.bodyType == type) androidx.compose.foundation.BorderStroke(1.dp, CyberAmber) else null,
                                modifier = Modifier.clickable { rb.bodyType = type }
                            ) {
                                Text(
                                    text = type.name.take(3),
                                    color = if (rb.bodyType == type) CyberAmber else TextMuted,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Layer: ${rb.layer.name}", color = TextSecondary, fontSize = 10.sp)
                    Text("Grounded: ${rb.isGrounded}", color = if (rb.isGrounded) CyberGreen else TextMuted, fontSize = 10.sp)
                }

                Button(
                    onClick = { viewModel.applyImpulseToSelected(Vector3(0f, 6.0f, 0f)) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                ) {
                    Text("Apply Upward Physics Impulse", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        entity.rigidBody = RigidBody3D(bodyType = BodyType.DYNAMIC)
                        entity.collider = Collider3D(ColliderType.BOX)
                        viewModel.showNotification("Added RigidBody3D to ${entity.name}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add RigidBody & Collider", fontSize = 10.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. Physics Lab Panel (Coroutine Physics Engine & Collisions)
// ----------------------------------------------------
@Composable
fun PhysicsLabPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val world = viewModel.scene.physicsWorld
    val coroutinePhysics = viewModel.coroutinePhysics
    val stats = coroutinePhysics.simulationStats.collectAsState().value
    val scrollState = rememberScrollState()
    var raycastResult by remember { mutableStateOf<String?>(null) }
    val selected = viewModel.getSelectedEntity()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Science, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(18.dp))
                Text("Coroutine 3D Physics Engine", color = CyberAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = CyberAmber.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${stats.activeRigidBodies} Bodies | ${stats.narrowphaseCollisions} Collisions (${String.format("%.2f", stats.simulationTimeMs)}ms)",
                    color = CyberAmber,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Live Collision Events HUD
        if (uiState.lastCollisionEventDesc != null) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(14.dp))
                    Text(
                        text = "SharedFlow Event: ${uiState.lastCollisionEventDesc}",
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Quick Action Tools: Spawn Cascade & Impulse
        InspectorSection(title = "Coroutine Physics Stress & Test Suite") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.dropPhysicsCascadeTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1.2f).height(30.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Drop 12-Body Cascade", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.applyImpulseToSelected(Vector3(0f, 6.0f, 0f)) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy, contentColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(30.dp)
                ) {
                    Icon(Icons.Default.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Apply Jump Force", fontSize = 9.sp)
                }
            }

            // Collider Wireframe Visualizer Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Collider Wireframes:", color = TextSecondary, fontSize = 10.sp)
                Button(
                    onClick = { viewModel.togglePhysicsColliders() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.showPhysicsColliders) CyberGreen.copy(alpha = 0.25f) else DarkNavy,
                        contentColor = if (uiState.showPhysicsColliders) CyberGreen else TextMuted
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(if (uiState.showPhysicsColliders) "Colliders ON (Green/Cyan)" else "Colliders OFF", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Scenario Preset Loaders
        InspectorSection(title = "Interactive Test Scenarios") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.loadPhysicsScenario("Dominoes") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("Dominoes", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.loadPhysicsScenario("Demolition") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("Demolition", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.loadPhysicsScenario("ZeroG") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("Zero-G", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Simulation Tuning Settings
        InspectorSection(title = "Simulation Parameters") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Gravity Y: ${String.format("%.1f", world.gravity.y)} m/s²", color = TextSecondary, fontSize = 10.sp)
                Slider(
                    value = world.gravity.y,
                    onValueChange = {
                        viewModel.updatePhysicsWorldSettings(gravityY = it)
                        coroutinePhysics.gravity = Vector3(0f, it, 0f)
                    },
                    valueRange = -25f..5f,
                    modifier = Modifier.width(140.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Substeps: ${world.substeps}x (${world.substeps * 60}Hz)", color = TextSecondary, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(1, 2, 4, 8).forEach { step ->
                        Surface(
                            color = if (world.substeps == step) CyberAmber.copy(alpha = 0.25f) else DarkNavy,
                            shape = RoundedCornerShape(4.dp),
                            border = if (world.substeps == step) androidx.compose.foundation.BorderStroke(1.dp, CyberAmber) else null,
                            modifier = Modifier.clickable {
                                viewModel.updatePhysicsWorldSettings(substeps = step)
                                coroutinePhysics.substeps = step
                            }
                        ) {
                            Text("${step}x", color = if (world.substeps == step) CyberAmber else TextMuted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Collision Mode: ${world.collisionMode.name}", color = TextSecondary, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    CollisionDetectionMode.values().forEach { mode ->
                        Surface(
                            color = if (world.collisionMode == mode) CyberCyan.copy(alpha = 0.25f) else DarkNavy,
                            shape = RoundedCornerShape(4.dp),
                            border = if (world.collisionMode == mode) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                            modifier = Modifier.clickable { viewModel.updatePhysicsWorldSettings(collisionMode = mode) }
                        ) {
                            Text(mode.name.take(4), color = if (world.collisionMode == mode) CyberCyan else TextMuted, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // Raycast Probe Tool
        InspectorSection(title = "Raycast Collision Query Tool") {
            Button(
                onClick = { raycastResult = viewModel.triggerRaycastQuery() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy, contentColor = CyberCyan),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            ) {
                Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fire Raycast from Camera Center", fontSize = 10.sp)
            }
            if (raycastResult != null) {
                Text(raycastResult!!, color = CyberGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ----------------------------------------------------
// CGI MOVIE DIRECTOR & CINEMATOGRAPHY PANEL
// ----------------------------------------------------
@Composable
fun CgiMoviePanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val director = viewModel.movieDirector
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Text("CGI Movie Director & Cinematography", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.toggleCinematicMode() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isCinematicMode) CyberPurple else DarkNavy,
                    contentColor = if (uiState.isCinematicMode) Color.White else CyberCyan
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(if (uiState.isCinematicMode) "Cinema Mode: ON" else "Cinema Mode: OFF", fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Camera Rig Preset Shots
        Text("Camera Rig Shot Presets:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CameraRigShot.values().forEach { shot ->
                val isSelected = uiState.selectedCgiShot == shot
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier.clickable { viewModel.setMovieShot(shot) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(shot.displayName, color = if (isSelected) CyberCyan else TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${shot.defaultFov.toInt()}° FOV • f/${shot.fStop}", color = TextMuted, fontSize = 7.sp)
                    }
                }
            }
        }

        // Aspect Ratio Matte Presets (CinemaScope, Anamorphic, IMAX)
        InspectorSection(title = "Aspect Ratio & Anamorphic Letterbox") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AspectRatioPreset.values().forEach { aspect ->
                    val isSelected = uiState.selectedCgiAspect == aspect
                    Surface(
                        color = if (isSelected) CyberPurple.copy(alpha = 0.25f) else DarkNavy,
                        shape = RoundedCornerShape(4.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberPurple) else null,
                        modifier = Modifier.clickable { viewModel.setMovieAspect(aspect) }
                    ) {
                        Text(
                            text = aspect.displayName,
                            color = if (isSelected) CyberPurple else TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Hollywood Color Grading LUT Curves
        InspectorSection(title = "Cinematic Color Grading & Film LUTs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ColorGradingLut.values().forEach { lut ->
                    val isSelected = uiState.selectedCgiLut == lut
                    Surface(
                        color = if (isSelected) CyberGreen.copy(alpha = 0.25f) else DarkNavy,
                        shape = RoundedCornerShape(4.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberGreen) else null,
                        modifier = Modifier.clickable { viewModel.setMovieLut(lut) }
                    ) {
                        Text(
                            text = lut.displayName,
                            color = if (isSelected) CyberGreen else TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CHARACTER DESIGN & SKELETAL RIG STUDIO PANEL
// ----------------------------------------------------
@Composable
fun CharacterRigPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val charStudio = viewModel.characterStudio
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Accessibility, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                Text("3D Character Design & Skeletal Rig", color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("18-Bone Humanoid Rig", color = TextMuted, fontSize = 9.sp)
        }

        // Archetype Selector
        Text("Character Archetypes:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CharacterArchetype.values().forEach { arch ->
                val isSelected = uiState.selectedCharacterArchetype == arch
                Surface(
                    color = if (isSelected) CyberGreen.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberGreen) else null,
                    modifier = Modifier.clickable { viewModel.setCharacterArchetype(arch) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(arch.displayName, color = if (isSelected) CyberGreen else TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(arch.description.take(28) + "...", color = TextMuted, fontSize = 7.sp)
                    }
                }
            }
        }

        // Real-Time Procedural Poses & Gaits
        InspectorSection(title = "Procedural Rig Poses & Gaits") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProceduralPose.values().forEach { pose ->
                    val isSelected = uiState.selectedCharacterPose == pose
                    Surface(
                        color = if (isSelected) CyberAmber.copy(alpha = 0.25f) else DarkNavy,
                        shape = RoundedCornerShape(4.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberAmber) else null,
                        modifier = Modifier.clickable { viewModel.setCharacterPose(pose) }
                    ) {
                        Text(
                            text = pose.displayName,
                            color = if (isSelected) CyberAmber else TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Action: Spawn Rigged Hero in Scene
        Button(
            onClick = { viewModel.spawnRiggedCharacterToScene() },
            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Spawn Rigged Hero in 3D Scene", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// 4. AI 3D Studio Panel (Objects, Models, Textures, Shaders)
// ----------------------------------------------------
@Composable
fun AiStudioPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    var promptInput by remember { mutableStateOf(uiState.aiPrompt) }
    var selectedType by remember { mutableStateOf(uiState.aiAssetType) }
    val scrollState = rememberScrollState()

    val promptPresets = listOf(
        "Cyberpunk Hover Car with glowing neon trim",
        "Floating Sci-Fi Island with crystal spires",
        "Alien Crystal Energy Monolith",
        "Combat Droid Robot with dual lasers",
        "Magma Core Lava Glow Procedural Texture",
        "Auto-Turret Defense Gameplay Script",
        "PBR Neon Hologram Shader"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gemini AI 3D Asset Generator", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = CyberCyan.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Gemini 3.5 Flash", color = CyberCyan, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }

        // Asset Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AIAssetType.values().forEach { type ->
                Surface(
                    color = if (selectedType == type) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (selectedType == type) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier.clickable { selectedType = type }
                ) {
                    Text(
                        text = when (type) {
                            AIAssetType.MODEL_3D -> "3D Object (.OBJ)"
                            AIAssetType.ENVIRONMENT_TERRAIN -> "Environment"
                            AIAssetType.TEXTURE_PROCEDURAL -> "Texture / PBR"
                            AIAssetType.GAMEPLAY_SCRIPT -> "Game Script"
                            AIAssetType.SHADER_MATERIAL -> "PBR Shader"
                            AIAssetType.NPC_DIALOGUE_QUEST -> "NPC Dialogue"
                        },
                        color = if (selectedType == type) CyberCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Prompt Input
        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            placeholder = { Text("Describe 3D model, terrain, texture, or script...", fontSize = 11.sp, color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyberCyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("ai_prompt_input"),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
        )

        // Prompt Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            promptPresets.forEach { preset ->
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { promptInput = preset }
                ) {
                    Text(
                        text = preset,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Generate Button
        Button(
            onClick = { viewModel.generateAiAsset(promptInput, selectedType, "Medium") },
            enabled = !uiState.isAiGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .testTag("generate_ai_asset_button")
        ) {
            if (uiState.isAiGenerating) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Synthesizing 3D Asset...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Asset with Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live 3D Render Preview Card
        if (uiState.lastGeneratedAsset != null) {
            val asset = uiState.lastGeneratedAsset
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = asset.title, color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { viewModel.addGeneratedAssetToScene() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Place in Scene", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(text = asset.description, color = TextSecondary, fontSize = 10.sp)

                    if (asset.generatedMesh != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Vertices: ${asset.generatedMesh.vertices.size}", color = CyberCyan, fontSize = 9.sp)
                            Text("Faces: ${asset.generatedMesh.triangles.size}", color = CyberCyan, fontSize = 9.sp)
                            Text("Topology: Watertight", color = CyberGreen, fontSize = 9.sp)
                        }
                    }

                    Text(
                        text = asset.rawContent.take(140) + if (asset.rawContent.length > 140) "..." else "",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. Scripting & Visual Logic Panel
// ----------------------------------------------------
@Composable
fun ScriptingPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    var scriptText by remember(uiState.activeScriptText) { mutableStateOf(uiState.activeScriptText) }
    var showVisualGraph by remember { mutableStateOf(false) }
    val visualNodes = remember { com.example.engine.scripting.T3DScriptEngine.createSampleVisualNodes() }

    val templates = listOf(
        "Coin Spinner" to "// Coin Spinner\non_update:\n    rotate_y(90 * dt)",
        "Sine Hover" to "// Hover Floating\non_update:\n    rotate_y(45 * dt)\n    rotate_x(15 * dt)",
        "Touch Jumper" to "// Touch Impulse\non_touch:\n    apply_impulse(0, 7.5, 0)\n    set_color(orange)",
        "Patrol Z" to "// Enemy Patrol\non_update:\n    rotate_y(30 * dt)\n    move_z(1.5 * dt)",
        "Neon Pulse" to "// Neon Pulse\non_update:\n    rotate_y(60 * dt)\n    set_color(cyan)"
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("T3D Script & Node Logic", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { showVisualGraph = !showVisualGraph },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(if (showVisualGraph) "Text Code" else "Visual Graph", fontSize = 9.sp)
                }

                Button(
                    onClick = { viewModel.updateSelectedScript(scriptText) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Apply & Compile", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Script Templates Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Templates:", color = TextMuted, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterVertically))
            templates.forEach { (label, code) ->
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable {
                        scriptText = code
                        viewModel.updateSelectedScript(code)
                    }
                ) {
                    Text(
                        text = label,
                        color = CyberCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (showVisualGraph) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(visualNodes) { node ->
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (node.type.name.startsWith("EVENT")) CyberAmber else CyberCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(node.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(node.type.name, color = TextMuted, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = scriptText,
                onValueChange = { scriptText = it },
                placeholder = { Text("// Write T3D Script: on_update, on_collision, on_touch...", fontSize = 10.sp, color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyberCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("script_editor_input"),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            )
        }
    }
}

// ----------------------------------------------------
// 6. Sculpt & Rigging Panel
// ----------------------------------------------------
@Composable
fun SculptRigPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val sculpt = viewModel.sculptTools
    val selected = viewModel.getSelectedEntity()
    var radius by remember { mutableStateOf(sculpt.brushRadius) }
    var strength by remember { mutableStateOf(sculpt.brushStrength) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("3D Sculpting & Bone Rigging", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        // Brush Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SculptBrushType.values().forEach { brush ->
                val isSelected = sculpt.brushType == brush
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier.clickable { sculpt.brushType = brush }
                ) {
                    Text(
                        text = brush.name.replace("_", " "),
                        color = if (isSelected) CyberCyan else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Brush Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Brush Radius: ${String.format("%.1f", radius)}m", color = TextSecondary, fontSize = 10.sp)
            Slider(
                value = radius,
                onValueChange = {
                    radius = it
                    sculpt.brushRadius = it
                },
                valueRange = 0.5f..5.0f,
                modifier = Modifier.width(160.dp)
            )
        }

        // Apply Sculpt Trigger Button
        Button(
            onClick = {
                viewModel.performSculptAtCenter(Vector3(0f, 0f, 0f), 0.1f)
                viewModel.showNotification("Applied ${sculpt.brushType} brush to mesh")
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().height(30.dp)
        ) {
            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Apply Sculpt Brush to ${selected?.name ?: "Selected"}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// 7. Profiler & Low-End Optimization (Itel A70 / 1GB RAM)
// ----------------------------------------------------
@Composable
fun ProfilerPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val profiler = viewModel.profiler
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hardware Profiler & Itel A70 Optimizer", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = if (profiler.batterySaverMode) CyberGreen.copy(alpha = 0.2f) else DarkCard,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { profiler.toggleBatterySaver() }
            ) {
                Text(
                    text = if (profiler.batterySaverMode) "Battery Saver ON" else "Battery Saver OFF",
                    color = if (profiler.batterySaverMode) CyberGreen else TextMuted,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Hardware Specifications Card
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Hardware: ${profiler.socName}", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("GPU: ${profiler.gpuModel} | RAM: ${profiler.totalRamGB}", color = TextSecondary, fontSize = 8.sp)
                Text("Battery Drain: ~${profiler.estimatedBatteryDrainPercentPerHour}%/hr (${profiler.powerConsumptionMilliwatts.toInt()}mW) | Temp: ${String.format("%.1f", profiler.deviceTemperatureCelsius)}°C", color = if (profiler.isThermalThrottling) CyberRed else CyberGreen, fontSize = 8.sp)
            }
        }

        // Quality Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                QualityPreset.LOW_ITEL_A70 to "Itel A70 (30FPS)",
                QualityPreset.LOW_ITEL_A70_60FPS to "Itel A70 (60FPS)",
                QualityPreset.MID_DEFAULT to "Balanced 60FPS",
                QualityPreset.HIGH_ULTRA to "Ultra HD"
            ).forEach { (preset, label) ->
                val isSelected = profiler.activePreset.name == preset.name
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { profiler.applyPreset(preset) }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) CyberCyan else TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // 16.67ms Frame Budget Visualizer
        Text("Frame Time Breakdown (${String.format("%.2f", profiler.frameBudget.totalFrameTimeMs)}ms / 60 FPS Target)", color = TextSecondary, fontSize = 9.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            Box(modifier = Modifier.weight(profiler.frameBudget.physicsMs.coerceAtLeast(0.1f)).fillMaxHeight().background(CyberAmber))
            Box(modifier = Modifier.weight(profiler.frameBudget.renderSubmitMs.coerceAtLeast(0.1f)).fillMaxHeight().background(CyberCyan))
            Box(modifier = Modifier.weight(profiler.frameBudget.gpuMs.coerceAtLeast(0.1f)).fillMaxHeight().background(CyberGreen))
            Box(modifier = Modifier.weight(profiler.frameBudget.miscMs.coerceAtLeast(0.1f)).fillMaxHeight().background(DarkBorder))
        }

        // 512MB RAM Budget Visualizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Memory Budget: ${profiler.memoryBudget.usedMB.toInt()}MB / ${profiler.memoryBudget.totalBudgetMB.toInt()}MB (${profiler.memoryBudget.usagePercent.toInt()}%)", color = TextSecondary, fontSize = 9.sp)
            Button(
                onClick = { viewModel.forceGarbageCollection() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy, contentColor = CyberGreen),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(22.dp)
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Clean RAM", fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        LinearProgressIndicator(
            progress = { profiler.memoryBudget.usagePercent / 100f },
            color = if (profiler.memoryBudget.usagePercent > 80f) CyberRed else CyberCyan,
            trackColor = DarkBorder,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )

        // Benchmarks Suite
        InspectorSection(title = "Low-End Baseline Performance Benchmarks") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.runRamBenchmark() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy, contentColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("1GB RAM & GC", fontSize = 8.sp)
                }
                Button(
                    onClick = { viewModel.runAssetLoadBenchmark() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy, contentColor = CyberGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("Load Speed", fontSize = 8.sp)
                }
                Button(
                    onClick = { viewModel.runPhysicsStressBenchmark() },
                    enabled = !uiState.isBenchmarkRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("60 FPS Test", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.ramBenchmarkResult != null) {
                Text("RAM GC: ${uiState.ramBenchmarkResult.primaryMetric} - ${uiState.ramBenchmarkResult.scoreGrade}", color = CyberCyan, fontSize = 9.sp)
            }
            if (uiState.assetLoadBenchmarkResult != null) {
                Text("Load Speed: ${uiState.assetLoadBenchmarkResult.primaryMetric}", color = CyberGreen, fontSize = 9.sp)
            }
            if (uiState.benchmarkScore != null) {
                Text("Stress: ${uiState.benchmarkScore}", color = CyberAmber, fontSize = 9.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 8. Export Panel
// ----------------------------------------------------
@Composable
fun ExportPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    var selectedTarget by remember { mutableStateOf(uiState.exportTarget) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Cross-Platform Export Pipeline", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        // Platform Target Buttons
        ExportTarget.values().forEach { target ->
            val isSelected = selectedTarget == target
            Surface(
                color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkCard,
                shape = RoundedCornerShape(6.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedTarget = target
                        viewModel.prepareExport(target)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(target.platformName, color = if (isSelected) CyberCyan else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(target.outputExtension, color = TextMuted, fontSize = 8.sp)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Export Action
        Button(
            onClick = {
                viewModel.prepareExport(selectedTarget)
                viewModel.showNotification("Generated export package for ${selectedTarget.platformName}")
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Build Package for ${selectedTarget.name}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // Live Generated Package File Tree Explorer & Code Inspector
        if (uiState.activeExportPackage != null) {
            val pkg = uiState.activeExportPackage
            var selectedFile by remember { mutableStateOf(pkg.files.firstOrNull()) }

            InspectorSection(title = "Generated Package Contents (${pkg.files.size} Files, ~${pkg.totalSizeBytes / 1024} KB)") {
                // File selection list
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pkg.files.forEach { file ->
                        val isFileActive = selectedFile?.relativePath == file.relativePath
                        Surface(
                            color = if (isFileActive) CyberCyan.copy(alpha = 0.25f) else DarkNavy,
                            shape = RoundedCornerShape(4.dp),
                            border = if (isFileActive) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                            modifier = Modifier.clickable { selectedFile = file }
                        ) {
                            Text(
                                text = file.relativePath.substringAfterLast("/"),
                                color = if (isFileActive) CyberCyan else TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = if (isFileActive) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (selectedFile != null) {
                    Surface(
                        color = DarkObsidian,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedFile!!.relativePath, color = CyberAmber, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text("${selectedFile!!.sizeBytes} bytes", color = TextMuted, fontSize = 8.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedFile!!.content.take(300) + if (selectedFile!!.content.length > 300) "\n... [truncated for preview]" else "",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 9. Leaderboard & Multi-User Panel
// ----------------------------------------------------
@Composable
fun LeaderboardPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    var playerNameInput by remember { mutableStateOf("ItelA70_Pro") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Real-Time Leaderboard & Multiplayer", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = CyberGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(uiState.cloudSyncStatus, color = CyberGreen, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = playerNameInput,
                onValueChange = { playerNameInput = it },
                placeholder = { Text("Player Handle", fontSize = 10.sp) },
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp)
            )
            Button(
                onClick = { viewModel.submitPlayerScore(playerNameInput, 15400L) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Submit Score", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Leaderboard Ranking List
        val entries = listOf(
            LeaderboardEntry("1", "CyberNinja99", 28900L, "14:20.1", "Itel A70 60FPS", 59.8f),
            LeaderboardEntry("2", "NeonRacer_X", 24500L, "15:02.4", "Itel A70 60FPS", 60.0f),
            LeaderboardEntry("3", "DroidMaster", 19200L, "16:45.0", "Balanced 60FPS", 58.4f),
            LeaderboardEntry("4", playerNameInput, 15400L, "18:10.2", "Itel A70 60FPS", 60.0f)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(entries) { entry ->
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${entry.id}", color = CyberAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(entry.playerName, color = TextPrimary, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${entry.score} pts", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${entry.fpsAvg.toInt()} FPS", color = CyberGreen, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// UI Helpers
// ----------------------------------------------------
@Composable
fun InspectorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun Vector3Row(label: String, vec: Vector3, onValueChange: (Vector3) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumericChip(label = "X", value = vec.x, color = CyberRed) { onValueChange(vec.copy(x = vec.x + it)) }
            NumericChip(label = "Y", value = vec.y, color = CyberGreen) { onValueChange(vec.copy(y = vec.y + it)) }
            NumericChip(label = "Z", value = vec.z, color = CyberCyan) { onValueChange(vec.copy(z = vec.z + it)) }
        }
    }
}

@Composable
fun NumericChip(label: String, value: Float, color: Color, onStep: (Float) -> Unit) {
    Surface(
        color = DarkNavy,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label:", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = String.format("%.2f", value), color = TextPrimary, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "+",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onStep(0.5f) }
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "-",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onStep(-0.5f) }
            )
        }
    }
}

// =========================================================================
// 1. PROCEDURAL 3D TERRAIN GENERATOR PANEL
// =========================================================================
@Composable
fun TerrainGenPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val scrollState = rememberScrollState()
    var heightMult by remember { mutableFloatStateOf(viewModel.terrainNoiseSettings.heightMultiplier) }
    var persistence by remember { mutableFloatStateOf(viewModel.terrainNoiseSettings.persistence) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mode Switcher Header: MMD Room/Stage vs Outdoor Terrain
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = if (uiState.worldStudioMode == WorldStudioMode.MMD_ROOM_STAGE) Icons.Default.MeetingRoom else Icons.Default.Terrain,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text("World, Stage & Room Studio", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Mode Toggle Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (mode in WorldStudioMode.values()) {
                    val isSelected = uiState.worldStudioMode == mode
                    Surface(
                        color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                        shape = RoundedCornerShape(6.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                        modifier = Modifier.clickable { viewModel.setWorldStudioMode(mode) }
                    ) {
                        Text(
                            text = if (mode == WorldStudioMode.MMD_ROOM_STAGE) "Stage / Room" else "Outdoor FBM",
                            color = if (isSelected) CyberCyan else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // MODE 1: MMD CONCERT STAGE & 3D ROOM/CHAMBER BUILDER
        // =====================================================================
        if (uiState.worldStudioMode == WorldStudioMode.MMD_ROOM_STAGE) {
            // itel A70 Budget Optimization Indicator & Toggle
            Surface(
                color = if (uiState.isItelA70Optimized) CyberGreen.copy(alpha = 0.15f) else DarkNavy,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isItelA70Optimized) CyberGreen.copy(alpha = 0.5f) else DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleItelA70Optimization() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = if (uiState.isItelA70Optimized) CyberGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "itel A70 Hardware Engine Optimization",
                                color = if (uiState.isItelA70Optimized) CyberGreen else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isItelA70Optimized) "Capped Polycount, Low-RAM VBO, Pre-baked Normals (Smooth 60 FPS)" else "High-Poly Native Desktop Subdivisions",
                                color = TextMuted,
                                fontSize = 8.sp
                            )
                        }
                    }
                    Text(
                        text = if (uiState.isItelA70Optimized) "ACTIVE" else "OFF",
                        color = if (uiState.isItelA70Optimized) CyberGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. Room / Stage Presets
            Text("Room & Stage Templates (MMD Style):", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (preset in RoomPreset.values()) {
                    val isSelected = uiState.selectedRoomPreset == preset
                    Surface(
                        color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                        shape = RoundedCornerShape(6.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                        modifier = Modifier.clickable { viewModel.setRoomPreset(preset) }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Text(
                                text = preset.displayName,
                                color = if (isSelected) CyberCyan else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${preset.defaultDimensions.x.toInt()}x${preset.defaultDimensions.z.toInt()}m",
                                color = TextMuted,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            // 2. Floor / Ground Surface Style
            Text("Floor / Ground Surface Style:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (style in FloorStyle.values()) {
                    val isSelected = uiState.selectedFloorStyle == style
                    Surface(
                        color = if (isSelected) CyberPurple.copy(alpha = 0.25f) else DarkCard,
                        shape = RoundedCornerShape(6.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberPurple) else null,
                        modifier = Modifier.clickable { viewModel.setFloorStyle(style) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(style.primaryColor.r, style.primaryColor.g, style.primaryColor.b, 1f), CircleShape)
                            )
                            Text(
                                text = style.displayName,
                                color = if (isSelected) CyberPurple else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 3. Room Dimensions Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Width: ${uiState.roomWidth.toInt()}m", color = TextSecondary, fontSize = 9.sp)
                    Slider(
                        value = uiState.roomWidth,
                        onValueChange = { viewModel.updateRoomDimensions(it, uiState.roomLength, uiState.roomHeight) },
                        valueRange = 6f..18f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Length: ${uiState.roomLength.toInt()}m", color = TextSecondary, fontSize = 9.sp)
                    Slider(
                        value = uiState.roomLength,
                        onValueChange = { viewModel.updateRoomDimensions(uiState.roomWidth, it, uiState.roomHeight) },
                        valueRange = 6f..18f,
                        colors = SliderDefaults.colors(thumbColor = CyberPurple, activeTrackColor = CyberPurple)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Height: ${String.format("%.1f", uiState.roomHeight)}m", color = TextSecondary, fontSize = 9.sp)
                    Slider(
                        value = uiState.roomHeight,
                        onValueChange = { viewModel.updateRoomDimensions(uiState.roomWidth, uiState.roomLength, it) },
                        valueRange = 2.5f..7.0f,
                        colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                    )
                }
            }

            // 4. Feature Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Trusses Toggle
                Surface(
                    color = if (uiState.isRoomStageTrussesEnabled) CyberCyan.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(4.dp),
                    border = if (uiState.isRoomStageTrussesEnabled) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier.weight(1f).clickable { viewModel.toggleRoomStageTrusses() }
                ) {
                    Text(
                        text = if (uiState.isRoomStageTrussesEnabled) "✓ Trusses" else "Trusses Off",
                        color = if (uiState.isRoomStageTrussesEnabled) CyberCyan else TextMuted,
                        fontSize = 9.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Spotlights Toggle
                Surface(
                    color = if (uiState.isRoomSpotlightsEnabled) CyberAmber.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(4.dp),
                    border = if (uiState.isRoomSpotlightsEnabled) androidx.compose.foundation.BorderStroke(1.dp, CyberAmber) else null,
                    modifier = Modifier.weight(1f).clickable { viewModel.toggleRoomSpotlights() }
                ) {
                    Text(
                        text = if (uiState.isRoomSpotlightsEnabled) "✓ Spotlights" else "Spotlights Off",
                        color = if (uiState.isRoomSpotlightsEnabled) CyberAmber else TextMuted,
                        fontSize = 9.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Ceiling Toggle
                Surface(
                    color = if (uiState.isRoomCeilingEnabled) CyberGreen.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(4.dp),
                    border = if (uiState.isRoomCeilingEnabled) androidx.compose.foundation.BorderStroke(1.dp, CyberGreen) else null,
                    modifier = Modifier.weight(1f).clickable { viewModel.toggleRoomCeiling() }
                ) {
                    Text(
                        text = if (uiState.isRoomCeilingEnabled) "✓ Ceiling" else "Open Ceiling",
                        color = if (uiState.isRoomCeilingEnabled) CyberGreen else TextMuted,
                        fontSize = 9.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Atmosphere Particles Toggle
                Surface(
                    color = if (uiState.isRoomAtmosphereEnabled) CyberPurple.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(4.dp),
                    border = if (uiState.isRoomAtmosphereEnabled) androidx.compose.foundation.BorderStroke(1.dp, CyberPurple) else null,
                    modifier = Modifier.weight(1f).clickable { viewModel.toggleRoomAtmosphere() }
                ) {
                    Text(
                        text = if (uiState.isRoomAtmosphereEnabled) "✓ Ambiance VFX" else "VFX Off",
                        color = if (uiState.isRoomAtmosphereEnabled) CyberPurple else TextMuted,
                        fontSize = 9.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // 5. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.buildRoomWorldScene(clearExistingWorld = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(40.dp)
                        .testTag("build_room_btn")
                ) {
                    Icon(Icons.Default.Roofing, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Build 3D Stage / Room", color = DarkObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val floorOnly = viewModel.worldStudio.generateFloorMesh(
                            uiState.roomWidth, uiState.roomLength, uiState.selectedFloorStyle,
                            lowEndOptimized = uiState.isItelA70Optimized
                        )
                        val floorEntity = Entity("Floor Only", tag = EntityTag.ENVIRONMENT).apply {
                            mesh = floorOnly
                            material = Material(albedoColor = uiState.selectedFloorStyle.primaryColor)
                            collider = Collider3D(ColliderType.BOX, size = Vector3(uiState.roomWidth, 0.4f, uiState.roomLength))
                            rigidBody = RigidBody3D(bodyType = BodyType.STATIC)
                        }
                        viewModel.scene.addEntity(floorEntity)
                        viewModel.showNotification("Added Floor (${uiState.selectedFloorStyle.displayName})")
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("build_floor_btn")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Floor Only", color = CyberPurple, fontSize = 11.sp)
                }
            }
        }

        // =====================================================================
        // MODE 2: PROCEDURAL 3D TERRAIN (FBM & HYDRAULIC EROSION)
        // =====================================================================
        if (uiState.worldStudioMode == WorldStudioMode.PROCEDURAL_TERRAIN) {
            Text("Select Terrain Biome:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (biome in TerrainBiome.values()) {
                    val isSelected = uiState.selectedTerrainBiome == biome
                    Surface(
                        color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                        shape = RoundedCornerShape(6.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                        modifier = Modifier.clickable { viewModel.setTerrainBiome(biome) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(biome.flatColor.r, biome.flatColor.g, biome.flatColor.b, 1f), CircleShape)
                            )
                            Text(
                                text = biome.displayName,
                                color = if (isSelected) CyberCyan else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Sliders for Noise Configuration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Height Scale: ${String.format("%.1f", heightMult)}m", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = heightMult,
                        onValueChange = {
                            heightMult = it
                            viewModel.terrainNoiseSettings.heightMultiplier = it
                        },
                        valueRange = 1.0f..10.0f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Persistence: ${String.format("%.2f", persistence)}", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = persistence,
                        onValueChange = {
                            persistence = it
                            viewModel.terrainNoiseSettings.persistence = it
                        },
                        valueRange = 0.1f..0.9f,
                        colors = SliderDefaults.colors(thumbColor = CyberPurple, activeTrackColor = CyberPurple)
                    )
                }
            }

            // Toggles for Hydraulic Erosion & Foliage Scatter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (uiState.terrainErosionEnabled) CyberGreen.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (uiState.terrainErosionEnabled) androidx.compose.foundation.BorderStroke(1.dp, CyberGreen) else null,
                    modifier = Modifier.weight(1f).clickable { viewModel.toggleTerrainErosion() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (uiState.terrainErosionEnabled) Icons.Default.WaterDrop else Icons.Default.WaterDamage,
                            contentDescription = null,
                            tint = if (uiState.terrainErosionEnabled) CyberGreen else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Hydraulic Erosion", color = TextPrimary, fontSize = 10.sp)
                    }
                }

                Surface(
                    color = if (uiState.terrainScatterEnabled) CyberAmber.copy(alpha = 0.2f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (uiState.terrainScatterEnabled) androidx.compose.foundation.BorderStroke(1.dp, CyberAmber) else null,
                    modifier = Modifier.weight(1f).clickable { viewModel.toggleTerrainScatter() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (uiState.terrainScatterEnabled) Icons.Default.Park else Icons.Default.Nature,
                            contentDescription = null,
                            tint = if (uiState.terrainScatterEnabled) CyberAmber else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Scatter Props", color = TextPrimary, fontSize = 10.sp)
                    }
                }
            }

            // Action Button: Synthesize & Apply Terrain
            Button(
                onClick = { viewModel.generateProceduralTerrainScene() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("generate_terrain_btn")
            ) {
                Icon(Icons.Default.Terrain, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate & Apply 3D Terrain", color = DarkObsidian, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// 2. PROCEDURAL ASSET FORGE PANEL (L-System, Shape Grammar, Weapons, BSP)
// =========================================================================
@Composable
fun AssetForgePanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Category, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(18.dp))
                Text("Procedural 3D Generator Forge", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Generator Category Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProceduralAssetCategory.values().forEach { cat ->
                val isSelected = uiState.selectedAssetCategory == cat
                Surface(
                    color = if (isSelected) CyberPurple.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberPurple) else null,
                    modifier = Modifier.clickable { viewModel.setAssetCategory(cat) }
                ) {
                    Text(
                        text = cat.displayName,
                        color = if (isSelected) CyberPurple else TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Category-Specific Controls
        when (uiState.selectedAssetCategory) {
            ProceduralAssetCategory.L_SYSTEM_FLORA -> {
                Text("L-System Rule: ${viewModel.lsystemConfig.axiom} (Iterations: ${viewModel.lsystemConfig.iterations})", color = TextSecondary, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Branch Angle: ${viewModel.lsystemConfig.branchAngleDeg.toInt()}° | Style: ${viewModel.lsystemConfig.treeStyle}", color = CyberCyan, fontSize = 10.sp)
                }
            }
            ProceduralAssetCategory.SHAPE_GRAMMAR_BUILDING -> {
                Text("Building Grammar: Floors: ${viewModel.buildingConfig.floors} | Setback Rate: ${viewModel.buildingConfig.setbackRate}", color = TextSecondary, fontSize = 10.sp)
                Text("Style: ${viewModel.buildingConfig.buildingStyle}", color = CyberGreen, fontSize = 10.sp)
            }
            ProceduralAssetCategory.PROCEDURAL_WEAPON -> {
                Text("Weapon Generator: Type = ${viewModel.weaponConfig.weaponType} | Length: ${viewModel.weaponConfig.barrelLength}m", color = TextSecondary, fontSize = 10.sp)
                Text("Archetype: Sci-Fi Hard-Light Weaponry", color = CyberAmber, fontSize = 10.sp)
            }
            ProceduralAssetCategory.DUNGEON_MAZE -> {
                Text("BSP Dungeon: Grid: ${viewModel.dungeonConfig.roomGridSize}x${viewModel.dungeonConfig.roomGridSize} | Wall Height: ${viewModel.dungeonConfig.wallHeight}m", color = TextSecondary, fontSize = 10.sp)
                Text("Architecture: Procedurally Connected Chambers & Halls", color = CyberCyan, fontSize = 10.sp)
            }
            ProceduralAssetCategory.SPLINE_EXTRUSION -> {
                Text("Spline Curve: Spiral Coils: ${viewModel.splineConfig.spiralCoils} | Radius: ${viewModel.splineConfig.radius}m", color = TextSecondary, fontSize = 10.sp)
                Text("Topology: Parametric Tubular Surface Sweep", color = CyberPurple, fontSize = 10.sp)
            }
        }

        // Action Button: Synthesize Asset
        Button(
            onClick = { viewModel.generateAndAddProceduralAsset() },
            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("generate_asset_btn")
        ) {
            Icon(Icons.Default.AddBox, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Synthesize & Spawn in Scene", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// 3. PARTICLE SIMULATION & VFX PANEL
// =========================================================================
@Composable
fun ParticlesPanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Flare, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(18.dp))
                Text("3D Particle Simulation System", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Active: ${viewModel.particleSystem.getActiveParticleCount()}/${viewModel.particleSystem.maxParticles}",
                color = CyberAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Particle Preset Chips
        Text("Particle Presets:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ParticlePreset.values().forEach { preset ->
                val isSelected = uiState.activeParticlePreset == preset
                Surface(
                    color = if (isSelected) CyberAmber.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberAmber) else null,
                    modifier = Modifier.clickable { viewModel.setParticlePreset(preset) }
                ) {
                    Text(
                        text = preset.displayName,
                        color = if (isSelected) CyberAmber else TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Emitter Shape & Ground Bounce Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Shape: ${viewModel.particleSystem.config.shape.name}", color = CyberCyan, fontSize = 10.sp)
            Text("Emission: ${viewModel.particleSystem.config.emissionRate.toInt()} /sec", color = TextSecondary, fontSize = 10.sp)
            Text("Ground Bounce: ${if (viewModel.particleSystem.config.groundBounce) "ON" else "OFF"}", color = CyberGreen, fontSize = 10.sp)
        }

        // Controls: Burst & Pause/Resume
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.triggerParticleBurst() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Burst Explosion", color = DarkObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.toggleParticleEmission() },
                border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isParticleEmitting) CyberGreen else CyberRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) {
                Icon(
                    if (uiState.isParticleEmitting) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (uiState.isParticleEmitting) CyberGreen else CyberRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.isParticleEmitting) "Pause Emission" else "Resume Emission",
                    color = if (uiState.isParticleEmitting) CyberGreen else CyberRed,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// =========================================================================
// 4. ANIMATION TIMELINE & KEYFRAME ENGINE PANEL
// =========================================================================
@Composable
fun AnimationTimelinePanel(viewModel: EngineViewModel, uiState: EngineUiState) {
    val scrollState = rememberScrollState()
    val activeClip = viewModel.animationEngine.activeClip

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Animation, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Text("Keyframe & Animation Timeline", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${String.format("%.2f", uiState.animationCurrentTime)}s / ${String.format("%.2f", activeClip.durationSec)}s",
                color = CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Clip Selectors
        Text("Animation Clips:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Walk Cycle", "Hover Pulse", "Bounce Impact").forEach { clipName ->
                val isSelected = activeClip.name.contains(clipName, ignoreCase = true)
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setAnimationClip(clipName) }
                ) {
                    Text(
                        text = clipName,
                        color = if (isSelected) CyberCyan else TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Timeline Scrub Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = uiState.animationCurrentTime.coerceIn(0f, activeClip.durationSec),
                onValueChange = { viewModel.animationEngine.currentTimeSec = it },
                valueRange = 0f..activeClip.durationSec,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
            )
        }

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.toggleAnimationPlay() },
                colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isAnimationPlaying) CyberPurple else CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(38.dp)
            ) {
                Icon(
                    if (uiState.isAnimationPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = DarkObsidian,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.isAnimationPlaying) "Pause Timeline" else "Play Timeline",
                    color = DarkObsidian,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Speed chips
            listOf(0.5f, 1.0f, 2.0f).forEach { speed ->
                val isSelected = uiState.animationPlaybackSpeed == speed
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier.clickable { viewModel.setAnimationPlaybackSpeed(speed) }
                ) {
                    Text(
                        text = "${speed}x",
                        color = if (isSelected) CyberCyan else TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
