package com.example.engine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ai.*
import com.example.engine.animation.*
import com.example.engine.cgi.*
import com.example.engine.character.*
import com.example.engine.core.*
import com.example.engine.data.*
import com.example.engine.ecs.*
import com.example.engine.export.*
import com.example.engine.generators.*
import com.example.engine.particles.*
import com.example.engine.physics.*
import com.example.engine.profiler.*
import com.example.engine.scripting.*
import com.example.engine.spatial.*
import com.example.engine.terrain.*
import com.example.engine.tools.*
import com.example.engine.world.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

enum class StudioTab {
    HIERARCHY,
    INSPECTOR,
    TERRAIN_GEN,
    ASSET_FORGE,
    PARTICLES,
    ANIMATION,
    PHYSICS_LAB,
    CGI_MOVIE,
    CHARACTER_RIG,
    AI_STUDIO,
    SCRIPTING,
    SCULPT_RIG,
    PROFILER,
    EXPORT,
    LEADERBOARD
}

data class EngineUiState(
    val isSimulating: Boolean = true,
    val simulationSpeed: Float = 1.0f,
    val activeCameraPreset: CameraPresetView = CameraPresetView.PERSPECTIVE_DEFAULT,
    val selectedTab: StudioTab = StudioTab.HIERARCHY,
    val gizmoMode: GizmoMode = GizmoMode.TRANSLATE,
    val selectedEntityId: String? = null,
    val isAiGenerating: Boolean = false,
    val aiPrompt: String = "Cyberpunk Hover Car with glowing neon trim",
    val aiAssetType: AIAssetType = AIAssetType.MODEL_3D,
    val aiComplexity: String = "Medium",
    val lastGeneratedAsset: AIGeneratedAssetResult? = null,
    val aiErrorMessage: String? = null,
    val exportTarget: ExportTarget = ExportTarget.SKETCHWARE_PRO_V7,
    val activeExportPackage: ExportPackage? = null,
    val showWireframeOverlay: Boolean = false,
    val showSkeleton: Boolean = true,
    val isBenchmarkRunning: Boolean = false,
    val benchmarkScore: String? = null,
    val ramBenchmarkResult: BenchmarkResult? = null,
    val assetLoadBenchmarkResult: BenchmarkResult? = null,
    val notificationMessage: String? = null,
    val activeScriptText: String = "",
    val cloudSyncStatus: String = "Offline Ready (AES-256 Synced)",
    val previewRotationAngle: Float = 0f,
    // Terrain Generation UI State
    val selectedTerrainBiome: TerrainBiome = TerrainBiome.VERDANT_FOREST,
    val terrainErosionEnabled: Boolean = true,
    val terrainScatterEnabled: Boolean = true,
    // Asset Forge UI State
    val selectedAssetCategory: ProceduralAssetCategory = ProceduralAssetCategory.L_SYSTEM_FLORA,
    // Particle VFX UI State
    val activeParticlePreset: ParticlePreset = ParticlePreset.CYBER_PLASMA_SPARKS,
    val isParticleEmitting: Boolean = true,
    // Animation Timeline UI State
    val isAnimationPlaying: Boolean = true,
    val animationPlaybackSpeed: Float = 1.0f,
    val animationCurrentTime: Float = 0f,
    // Physics & Simulation Polish
    val showPhysicsColliders: Boolean = true,
    val physicsCollisionCount: Int = 0,
    val lastCollisionEventDesc: String? = null,
    // CGI Movie Director UI State
    val isCinematicMode: Boolean = false,
    val selectedCgiShot: CameraRigShot = CameraRigShot.ORBIT_CINEMATIC,
    val selectedCgiAspect: AspectRatioPreset = AspectRatioPreset.CINEMASCOPE_21_9,
    val selectedCgiLut: ColorGradingLut = ColorGradingLut.CYBERPUNK_NEON,
    // Character Rig & Design UI State
    val selectedCharacterArchetype: CharacterArchetype = CharacterArchetype.CYBER_NINJA,
    val selectedCharacterPose: ProceduralPose = ProceduralPose.IDLE_BREATHING,
    // World, Room, Floor & MMD Stage Studio
    val worldStudioMode: WorldStudioMode = WorldStudioMode.MMD_ROOM_STAGE,
    val selectedRoomPreset: RoomPreset = RoomPreset.MMD_DANCE_STAGE,
    val selectedFloorStyle: FloorStyle = FloorStyle.MMD_CHECKERBOARD,
    val roomWidth: Float = 12f,
    val roomLength: Float = 12f,
    val roomHeight: Float = 4.2f,
    val isRoomCeilingEnabled: Boolean = false,
    val isRoomStageTrussesEnabled: Boolean = true,
    val isRoomSpotlightsEnabled: Boolean = true,
    val isRoomAtmosphereEnabled: Boolean = true,
    val isItelA70Optimized: Boolean = true
)

class EngineViewModel(application: Application) : AndroidViewModel(application) {

    val scene: Scene3D = Scene3D.createDefaultDemoScene()
    val renderer: Renderer3D = Renderer3D()
    val profiler: DeviceProfiler = DeviceProfiler()
    val scriptEngine: T3DScriptEngine = T3DScriptEngine()
    val sculptTools: SculptTools = SculptTools()
    val particleSystem: ParticleSystem3D = ParticleSystem3D()
    val animationEngine: AnimationTimelineEngine = AnimationTimelineEngine()
    val octree: SpatialOctree3D = SpatialOctree3D()

    // Lightweight 3D Physics Simulation with Kotlin/Coroutines
    val coroutinePhysics: CoroutinePhysicsEngine = CoroutinePhysicsEngine(viewModelScope)

    // CGI & Movie Director
    val movieDirector: CgiMovieDirector = CgiMovieDirector()

    // Character Design & Rigging Studio
    val characterStudio: CharacterDesignRigStudio = CharacterDesignRigStudio()

    // World, Room, Floor & MMD Stage Studio
    val worldStudio: WorldRoomStageStudio = WorldRoomStageStudio()

    // Configs for Procedural Generators
    var terrainNoiseSettings: TerrainNoiseSettings = TerrainNoiseSettings()
    var lsystemConfig: LSystemTreeConfig = LSystemTreeConfig()
    var buildingConfig: BuildingGrammarConfig = BuildingGrammarConfig()
    var weaponConfig: ProceduralWeaponConfig = ProceduralWeaponConfig()
    var dungeonConfig: DungeonConfig = DungeonConfig()
    var splineConfig: SplineExtrusionConfig = SplineExtrusionConfig()

    private val repository: EngineRepository

    private val _uiState = MutableStateFlow(EngineUiState())
    val uiState: StateFlow<EngineUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var lastFrameTimeNanos: Long = System.nanoTime()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = EngineRepository(db)

        particleSystem.applyPreset(ParticlePreset.CYBER_PLASMA_SPARKS)
        selectEntity(scene.entities.firstOrNull()?.id)

        // Listen for Coroutine Physics Collision Events
        viewModelScope.launch {
            coroutinePhysics.collisionEvents.collect { event ->
                when (event) {
                    is CollisionEvent.CollisionEnter -> {
                        _uiState.value = _uiState.value.copy(
                            physicsCollisionCount = _uiState.value.physicsCollisionCount + 1,
                            lastCollisionEventDesc = "Hit: ${event.entityA.name} ↔ ${event.entityB.name} (Impulse: ${String.format("%.1f", event.impulse)})"
                        )
                        // Trigger impact spark particles at contact point if impulse is noticeable
                        if (event.impulse > 1.5f) {
                            particleSystem.emitterPosition = event.contactPoint
                            particleSystem.triggerBurst(12)
                        }
                    }
                    else -> {}
                }
            }
        }

        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive) {
                val now = System.nanoTime()
                val dtNanos = now - lastFrameTimeNanos
                lastFrameTimeNanos = now
                val dt = ((dtNanos / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)) * _uiState.value.simulationSpeed

                if (_uiState.value.isSimulating) {
                    // 1. Lightweight Coroutine Physics Simulation Step
                    coroutinePhysics.stepAsync(scene.entities, dt)
                    // Keep scene.physicsWorld telemetry in sync
                    scene.physicsWorld.simulatedBodiesCount = coroutinePhysics.simulationStats.value.activeRigidBodies
                    scene.physicsWorld.collisionCount = coroutinePhysics.simulationStats.value.narrowphaseCollisions

                    // 2. Scripts Execution
                    for (entity in scene.entities) {
                        if (entity.enabled && entity.script != null && entity.script!!.isEnabled) {
                            scriptEngine.executeScript(entity.script!!.scriptSource, entity, dt, false)
                        }
                    }

                    // 3. AI Behaviors
                    updateAIBehaviors(dt)

                    // 4. Particle System Simulation
                    particleSystem.update(dt)

                    // 5. Animation Timeline Engine
                    val selected = getSelectedEntity()
                    animationEngine.update(dt) { posOffset, rotOffset, scaleOffset ->
                        if (selected != null && _uiState.value.selectedTab == StudioTab.ANIMATION) {
                            selected.transform.position += posOffset * dt
                            selected.transform.rotation.y = (selected.transform.rotation.y + rotOffset.y * dt) % 360f
                        }
                    }

                    // 6. Character Studio Procedural Pose Animation
                    if (selected != null) {
                        characterStudio.updatePose(dt, selected.mesh)
                    }

                    // 7. Movie Director Cinematic Camera Tracking
                    if (_uiState.value.isCinematicMode || _uiState.value.selectedTab == StudioTab.CGI_MOVIE) {
                        val targetPos = selected?.transform?.position ?: Vector3.ZERO
                        movieDirector.update(dt, scene.camera, targetPos)
                    }

                    _uiState.value = _uiState.value.copy(
                        animationCurrentTime = animationEngine.currentTimeSec,
                        isParticleEmitting = particleSystem.isEmitting
                    )
                }

                // Update Performance Metrics
                profiler.updateMetrics(
                    entitiesCount = scene.entities.size,
                    polygonsCount = renderer.polygonCount,
                    drawCalls = renderer.drawCallCount,
                    dtSeconds = dt
                )

                delay(16) // Target 60 FPS (~16.6ms)
            }
        }
    }

    private fun updateAIBehaviors(dt: Float) {
        val player = scene.findEntityByTag(EntityTag.PLAYER) ?: return
        for (entity in scene.entities) {
            val ai = entity.aiBehavior ?: continue
            if (!entity.enabled) continue

            val dist = entity.transform.position.distanceTo(player.transform.position)
            if (dist <= ai.detectionRadius) {
                ai.state = if (dist <= ai.attackRadius) "ATTACK" else "CHASE"
                if (ai.state == "CHASE") {
                    val dir = (player.transform.position - entity.transform.position).normalized()
                    entity.transform.position += dir * (ai.movementSpeed * dt)
                    entity.transform.rotation.y = Math.toDegrees(atan2(dir.x.toDouble(), dir.z.toDouble())).toFloat()
                }
            } else {
                ai.state = "PATROL"
                entity.transform.rotation.y = (entity.transform.rotation.y + 20f * dt) % 360f
            }
        }
    }

    fun toggleSimulation() {
        _uiState.value = _uiState.value.copy(isSimulating = !_uiState.value.isSimulating)
    }

    fun resetScene() {
        val demoScene = Scene3D.createDefaultDemoScene()
        scene.entities.clear()
        scene.entities.addAll(demoScene.entities)
        scene.camera.position = Vector3(0f, 4f, -8f)
        scene.camera.target = Vector3(0f, 1f, 0f)
        scene.camera.yaw = 0f
        scene.camera.pitch = 25f
        scene.camera.distance = 9f
        selectEntity(scene.entities.firstOrNull()?.id)
        showNotification("Scene reset to default state")
    }

    fun setCameraPreset(preset: CameraPresetView) {
        scene.camera.setPresetView(preset)
        _uiState.value = _uiState.value.copy(activeCameraPreset = preset)
        showNotification("Camera view: ${preset.name}")
    }

    fun focusCameraOnSelected() {
        val selected = getSelectedEntity() ?: return
        val meshAABB = selected.mesh?.aabb
        val radius = meshAABB?.size?.length()?.times(0.5f) ?: 2.0f
        scene.camera.focusOn(selected.transform.position, radius)
        showNotification("Camera focused on ${selected.name}")
    }

    fun setSimulationSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(simulationSpeed = speed)
        showNotification("Simulation speed: ${speed}x")
    }

    fun stepTransformSelected(dx: Float, dy: Float, dz: Float) {
        val selected = getSelectedEntity() ?: return
        selected.transform.position.x += dx
        selected.transform.position.y += dy
        selected.transform.position.z += dz
    }

    fun stepRotateSelected(dYaw: Float, dPitch: Float, dRoll: Float) {
        val selected = getSelectedEntity() ?: return
        selected.transform.rotation.y = (selected.transform.rotation.y + dYaw) % 360f
        selected.transform.rotation.x = (selected.transform.rotation.x + dPitch) % 360f
        selected.transform.rotation.z = (selected.transform.rotation.z + dRoll) % 360f
    }

    fun stepScaleSelected(dScale: Float) {
        val selected = getSelectedEntity() ?: return
        val newScale = (selected.transform.scale.x + dScale).coerceIn(0.1f, 10f)
        selected.transform.scale = Vector3(newScale, newScale, newScale)
    }

    fun forceGarbageCollection() {
        val runtime = Runtime.getRuntime()
        val memBefore = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        System.gc()
        val memAfter = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val freed = (memBefore - memAfter).coerceAtLeast(0)
        showNotification("Garbage Collector Triggered: Freed ${freed}MB RAM")
    }

    fun setTab(tab: StudioTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setGizmoMode(mode: GizmoMode) {
        _uiState.value = _uiState.value.copy(gizmoMode = mode)
    }

    fun selectEntity(entityId: String?) {
        for (e in scene.entities) {
            e.isSelected = (e.id == entityId)
        }
        val selected = scene.entities.find { it.id == entityId }
        _uiState.value = _uiState.value.copy(
            selectedEntityId = entityId,
            activeScriptText = selected?.script?.scriptSource ?: ""
        )
    }

    fun getSelectedEntity(): Entity? {
        val id = _uiState.value.selectedEntityId ?: return null
        return scene.findEntityById(id)
    }

    fun addPrimitive(type: String) {
        val name = when (type) {
            "Cube" -> "New Cube"
            "Sphere" -> "New Sphere"
            "Cylinder" -> "New Cylinder"
            "Torus" -> "New Torus"
            "Vehicle" -> "Hover Car"
            "Tower" -> "Sci-Fi Spire"
            "Crystal" -> "Crystal Cluster"
            "Terrain" -> "New Terrain"
            "Light" -> "Point Light"
            "PhysicsProp" -> "Physics Box"
            else -> "Entity"
        }

        val newEntity = Entity(name = name, tag = EntityTag.DEFAULT)
        newEntity.transform.position = Vector3(0f, 2f, 0f)

        when (type) {
            "Cube" -> {
                newEntity.mesh = Mesh.createCube(1.0f)
                newEntity.material = Material(albedoColor = Color4(0.2f, 0.7f, 1.0f, 1f))
                newEntity.collider = Collider3D(ColliderType.BOX, size = Vector3.ONE)
                newEntity.rigidBody = RigidBody3D(bodyType = BodyType.DYNAMIC)
            }
            "Sphere" -> {
                newEntity.mesh = Mesh.createSphere(radius = 0.6f)
                newEntity.material = Material(albedoColor = Color4(0.9f, 0.3f, 0.8f, 1f))
                newEntity.collider = Collider3D(ColliderType.SPHERE, radius = 0.6f)
                newEntity.rigidBody = RigidBody3D(bodyType = BodyType.DYNAMIC)
            }
            "Cylinder" -> {
                newEntity.mesh = Mesh.createCylinder(radius = 0.5f, height = 1.6f)
                newEntity.material = Material(albedoColor = Color4(0.3f, 0.9f, 0.4f, 1f))
                newEntity.collider = Collider3D(ColliderType.CAPSULE, radius = 0.5f, size = Vector3(0.5f, 1.6f, 0.5f))
                newEntity.rigidBody = RigidBody3D(bodyType = BodyType.DYNAMIC)
            }
            "Torus" -> {
                newEntity.mesh = Mesh.createTorus(r1 = 0.7f, r2 = 0.2f)
                newEntity.material = Material(albedoColor = Color4(1.0f, 0.8f, 0.1f, 1f))
                newEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(1.4f, 0.4f, 1.4f))
                newEntity.rigidBody = RigidBody3D(bodyType = BodyType.DYNAMIC)
            }
            "Vehicle" -> {
                newEntity.mesh = Mesh.createHoverVehicle()
                newEntity.material = Material(albedoColor = Color4(0f, 0.9f, 1f, 1f), metallic = 0.7f)
                newEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(1.6f, 0.8f, 2.4f))
                newEntity.rigidBody = RigidBody3D(mass = 2.0f, bodyType = BodyType.DYNAMIC)
            }
            "Tower" -> {
                newEntity.mesh = Mesh.createSciFiTower()
                newEntity.material = Material(albedoColor = Color4(0.15f, 0.8f, 0.9f, 1f))
                newEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(2.0f, 4.8f, 2.0f))
                newEntity.rigidBody = RigidBody3D(bodyType = BodyType.STATIC)
            }
            "Crystal" -> {
                newEntity.mesh = Mesh.createCrystalCluster()
                newEntity.material = Material(albedoColor = Color4(0.85f, 0.25f, 0.95f, 1f), metallic = 0.8f)
                newEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(1.0f, 2.2f, 1.0f))
                newEntity.rigidBody = RigidBody3D(bodyType = BodyType.DYNAMIC)
            }
            "Terrain" -> {
                newEntity.mesh = Mesh.createTerrain(width = 16f, depth = 16f, gridX = 12, gridZ = 12)
                newEntity.material = Material(albedoColor = Color4(0.18f, 0.35f, 0.2f, 1f))
                newEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(16f, 1f, 16f))
                newEntity.rigidBody = RigidBody3D(bodyType = BodyType.STATIC, layer = PhysicsLayer.TERRAIN)
            }
            "Light" -> {
                newEntity.light = Light(type = LightType.POINT, color = Color4.CYAN, intensity = 1.5f)
            }
            "PhysicsProp" -> {
                newEntity.mesh = Mesh.createCube(0.8f)
                newEntity.material = Material(albedoColor = Color4(1f, 0.5f, 0.1f, 1f))
                newEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(0.8f, 0.8f, 0.8f))
                newEntity.rigidBody = RigidBody3D(mass = 1.5f, bodyType = BodyType.DYNAMIC, layer = PhysicsLayer.PROP)
            }
        }

        scene.addEntity(newEntity)
        selectEntity(newEntity.id)
        showNotification("Created $name in 3D scene")
    }

    fun deleteSelectedEntity() {
        val selected = getSelectedEntity() ?: return
        scene.removeEntity(selected)
        selectEntity(scene.entities.firstOrNull()?.id)
        showNotification("Deleted entity")
    }

    fun duplicateSelectedEntity() {
        val orig = getSelectedEntity() ?: return
        val copy = orig.copy(
            id = "entity_${System.currentTimeMillis()}",
            name = "${orig.name}_Copy"
        )
        copy.transform.position = orig.transform.position + Vector3(0.5f, 0.5f, 0.5f)
        scene.addEntity(copy)
        selectEntity(copy.id)
        showNotification("Duplicated ${orig.name}")
    }

    // Physics Playground Scenario Loaders
    fun loadPhysicsScenario(scenario: String) {
        scene.entities.clear()
        // Ground plane
        val ground = Entity(name = "Ground", tag = EntityTag.DEFAULT)
        ground.transform.position = Vector3(0f, 0f, 0f)
        ground.mesh = Mesh.createPlane(width = 20f, depth = 20f)
        ground.material = Material(albedoColor = Color4(0.12f, 0.15f, 0.2f, 1f))
        ground.collider = Collider3D(ColliderType.BOX, size = Vector3(20f, 0.2f, 20f))
        ground.rigidBody = RigidBody3D(bodyType = BodyType.STATIC, layer = PhysicsLayer.TERRAIN)
        scene.addEntity(ground)

        when (scenario) {
            "Dominoes" -> {
                for (i in 0 until 12) {
                    val domino = Entity(name = "Domino_$i", tag = EntityTag.PHYSICS_PROP)
                    domino.transform.position = Vector3(0f, 0.6f, -4f + i * 0.7f)
                    domino.mesh = Mesh.createCube(1.0f)
                    domino.transform.scale = Vector3(0.6f, 1.2f, 0.15f)
                    domino.material = Material(albedoColor = Color4(0f, 0.9f, 1f, 1f))
                    domino.collider = Collider3D(ColliderType.BOX, size = Vector3.ONE)
                    domino.rigidBody = RigidBody3D(mass = 0.5f, bodyType = BodyType.DYNAMIC, layer = PhysicsLayer.PROP)
                    scene.addEntity(domino)
                }
                // Starter ball
                val ball = Entity(name = "Starter Ball", tag = EntityTag.PHYSICS_PROP)
                ball.transform.position = Vector3(0f, 1.2f, -5.5f)
                ball.mesh = Mesh.createSphere(0.4f)
                ball.material = Material(albedoColor = Color4(1f, 0.2f, 0.2f, 1f))
                ball.collider = Collider3D(ColliderType.SPHERE, radius = 0.4f)
                ball.rigidBody = RigidBody3D(mass = 2.0f, bodyType = BodyType.DYNAMIC, layer = PhysicsLayer.PROJECTILE).apply {
                    velocity = Vector3(0f, 0f, 4.0f)
                }
                scene.addEntity(ball)
                showNotification("Loaded 'Domino Cascade' physics test!")
            }
            "Demolition" -> {
                // 4-layer pyramid
                for (row in 0 until 4) {
                    val count = 4 - row
                    for (c in 0 until count) {
                        val brick = Entity(name = "Brick_${row}_$c", tag = EntityTag.PHYSICS_PROP)
                        val offset = (count - 1) * 0.4f
                        brick.transform.position = Vector3(-offset + c * 0.8f, 0.35f + row * 0.7f, 0f)
                        brick.mesh = Mesh.createCube(0.7f)
                        brick.material = Material(albedoColor = Color4(1f, 0.4f + row * 0.15f, 0.1f, 1f))
                        brick.collider = Collider3D(ColliderType.BOX, size = Vector3(0.7f, 0.7f, 0.7f))
                        brick.rigidBody = RigidBody3D(mass = 1.0f, bodyType = BodyType.DYNAMIC, layer = PhysicsLayer.PROP)
                        scene.addEntity(brick)
                    }
                }
                // Wrecking projectile
                val wreckBall = Entity(name = "Wrecking Ball", tag = EntityTag.PROJECTILE)
                wreckBall.transform.position = Vector3(0f, 1.5f, -4.5f)
                wreckBall.mesh = Mesh.createSphere(0.6f)
                wreckBall.material = Material(albedoColor = Color4(0.85f, 0.2f, 0.95f, 1f))
                wreckBall.collider = Collider3D(ColliderType.SPHERE, radius = 0.6f)
                wreckBall.rigidBody = RigidBody3D(mass = 8.0f, bodyType = BodyType.DYNAMIC, layer = PhysicsLayer.PROJECTILE).apply {
                    velocity = Vector3(0f, 1.0f, 7.5f)
                }
                scene.addEntity(wreckBall)
                showNotification("Loaded 'Pyramid Demolition' physics test!")
            }
            "ZeroG" -> {
                scene.physicsWorld.gravity = Vector3.ZERO
                for (i in 0 until 15) {
                    val ast = Entity(name = "Asteroid_$i", tag = EntityTag.PHYSICS_PROP)
                    ast.transform.position = Vector3(
                        (i % 5 - 2) * 1.5f,
                        1.5f + (i % 3) * 1.2f,
                        (i / 5 - 1) * 1.5f
                    )
                    ast.mesh = if (i % 2 == 0) Mesh.createCrystalCluster() else Mesh.createCube(0.7f)
                    ast.material = Material(albedoColor = Color4(0.2f, 0.8f, 1.0f, 1f))
                    ast.collider = Collider3D(ColliderType.BOX, size = Vector3.ONE)
                    ast.rigidBody = RigidBody3D(mass = 1.0f, bodyType = BodyType.DYNAMIC, useGravity = false).apply {
                        velocity = Vector3((sin(i.toDouble()) * 1.2).toFloat(), (cos(i.toDouble()) * 0.8).toFloat(), (sin(i * 2.0) * 1.0).toFloat())
                        angularVelocity = Vector3(0.5f, 1.0f, 0.3f)
                    }
                    scene.addEntity(ast)
                }
                showNotification("Loaded 'Zero-G Asteroid Field' simulation!")
            }
        }
        selectEntity(scene.entities.firstOrNull { it.rigidBody?.bodyType == BodyType.DYNAMIC }?.id)
    }

    fun applyTextureToSelectedEntity(texture: ProceduralTexture) {
        val entity = getSelectedEntity() ?: return
        entity.material.name = "${texture.name} Mat"
        entity.material.albedoColor = texture.primaryColor
        entity.material.metallic = texture.metallic
        entity.material.roughness = texture.roughness
        entity.material.emissionColor = if (texture.isEmissive) texture.primaryColor else Color4.BLACK
        entity.material.emissionIntensity = if (texture.isEmissive) 1.5f else 0f
        showNotification("Applied procedural texture '${texture.name}' to ${entity.name}!")
    }

    fun updatePhysicsWorldSettings(
        gravityY: Float? = null,
        substeps: Int? = null,
        timeScale: Float? = null,
        collisionMode: CollisionDetectionMode? = null,
        restitutionMode: RestitutionCombineMode? = null
    ) {
        gravityY?.let { scene.physicsWorld.gravity = Vector3(0f, it, 0f) }
        substeps?.let { scene.physicsWorld.substeps = it }
        timeScale?.let { scene.physicsWorld.timeScale = it }
        collisionMode?.let { scene.physicsWorld.collisionMode = it }
        restitutionMode?.let { scene.physicsWorld.restitutionMode = it }
    }

    fun triggerRaycastQuery(): String {
        val ray = Ray(scene.camera.position, (scene.camera.target - scene.camera.position).normalized())
        val hit = scene.physicsWorld.raycast(ray, scene.entities)
        return if (hit != null) {
            selectEntity(hit.entity.id)
            "Raycast HIT: '${hit.entity.name}' at distance ${String.format("%.2f", hit.distance)}m"
        } else {
            "Raycast MISSED: No collider intersected along camera center"
        }
    }

    fun performSculptAtCenter(center: Vector3, dt: Float) {
        val selected = getSelectedEntity() ?: return
        val mesh = selected.mesh ?: return
        sculptTools.applySculpt(mesh, selected.transform.getWorldMatrix(), center, dt)
    }

    // --- Procedural Terrain Generation ---
    fun setTerrainBiome(biome: TerrainBiome) {
        _uiState.value = _uiState.value.copy(selectedTerrainBiome = biome)
    }

    fun toggleTerrainErosion() {
        _uiState.value = _uiState.value.copy(terrainErosionEnabled = !_uiState.value.terrainErosionEnabled)
    }

    fun toggleTerrainScatter() {
        _uiState.value = _uiState.value.copy(terrainScatterEnabled = !_uiState.value.terrainScatterEnabled)
    }

    fun generateProceduralTerrainScene() {
        val biome = _uiState.value.selectedTerrainBiome
        val (terrainMesh, scatterItems) = Terrain3DGenerator.generateTerrainMesh(
            width = 28f,
            depth = 28f,
            gridX = 18,
            gridZ = 18,
            biome = biome,
            noiseSettings = terrainNoiseSettings,
            enableErosion = _uiState.value.terrainErosionEnabled,
            erosionDroplets = 90
        )

        // Find or replace ground terrain
        val existingTerrain = scene.entities.find { it.name.contains("Ground", ignoreCase = true) || it.name.contains("Terrain", ignoreCase = true) }
        if (existingTerrain != null) {
            existingTerrain.name = "Terrain (${biome.displayName})"
            existingTerrain.mesh = terrainMesh
            existingTerrain.material = Material(
                name = "${biome.displayName} Material",
                albedoColor = biome.flatColor,
                roughness = 0.8f
            )
            existingTerrain.collider = Collider3D(ColliderType.BOX, size = Vector3(28f, terrainNoiseSettings.heightMultiplier, 28f))
        } else {
            val terrainEntity = Entity(name = "Terrain (${biome.displayName})", tag = EntityTag.ENVIRONMENT)
            terrainEntity.mesh = terrainMesh
            terrainEntity.material = Material(name = "${biome.displayName} Material", albedoColor = biome.flatColor, roughness = 0.8f)
            terrainEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(28f, terrainNoiseSettings.heightMultiplier, 28f))
            terrainEntity.rigidBody = RigidBody3D(bodyType = BodyType.STATIC)
            scene.addEntity(terrainEntity)
        }

        // Add scatter props if enabled
        if (_uiState.value.terrainScatterEnabled) {
            // Remove previous scatter props
            scene.entities.removeAll { it.tag == EntityTag.ENVIRONMENT && (it.name.contains("Tree") || it.name.contains("Beacon") || it.name.contains("Monolith") || it.name.contains("Spire")) }
            for (scatter in scatterItems.take(8)) {
                val prop = Entity(name = scatter.name, tag = EntityTag.ENVIRONMENT)
                prop.transform.position = scatter.position
                prop.transform.rotation.y = scatter.rotationY
                prop.transform.scale = scatter.scale
                prop.mesh = scatter.meshGenerator()
                prop.material = Material(name = "${scatter.name} Mat", albedoColor = biome.peakColor, metallic = 0.4f)
                prop.collider = Collider3D(ColliderType.BOX, size = Vector3(1f, 2f, 1f))
                prop.rigidBody = RigidBody3D(bodyType = BodyType.STATIC)
                scene.addEntity(prop)
            }
        }

        showNotification("Generated ${biome.displayName} Terrain (Erosion: ${_uiState.value.terrainErosionEnabled})")
    }

    // =========================================================================
    // WORLD, ROOM, FLOOR & MMD STAGE STUDIO CONTROLS
    // =========================================================================
    fun setWorldStudioMode(mode: WorldStudioMode) {
        _uiState.value = _uiState.value.copy(worldStudioMode = mode)
    }

    fun setRoomPreset(preset: RoomPreset) {
        worldStudio.activePreset = preset
        worldStudio.activeFloorStyle = preset.defaultFloorStyle
        _uiState.value = _uiState.value.copy(
            selectedRoomPreset = preset,
            selectedFloorStyle = preset.defaultFloorStyle,
            roomWidth = preset.defaultDimensions.x,
            roomHeight = preset.defaultDimensions.y,
            roomLength = preset.defaultDimensions.z
        )
        showNotification("Room Template: ${preset.displayName}")
    }

    fun setFloorStyle(style: FloorStyle) {
        worldStudio.activeFloorStyle = style
        _uiState.value = _uiState.value.copy(selectedFloorStyle = style)
        showNotification("Floor Style: ${style.displayName}")
    }

    fun updateRoomDimensions(w: Float, l: Float, h: Float) {
        worldStudio.roomWidth = w
        worldStudio.roomLength = l
        worldStudio.roomHeight = h
        _uiState.value = _uiState.value.copy(roomWidth = w, roomLength = l, roomHeight = h)
    }

    fun toggleRoomCeiling() {
        worldStudio.includeCeiling = !worldStudio.includeCeiling
        _uiState.value = _uiState.value.copy(isRoomCeilingEnabled = worldStudio.includeCeiling)
    }

    fun toggleRoomStageTrusses() {
        worldStudio.includeStageTrusses = !worldStudio.includeStageTrusses
        _uiState.value = _uiState.value.copy(isRoomStageTrussesEnabled = worldStudio.includeStageTrusses)
    }

    fun toggleRoomSpotlights() {
        worldStudio.includeSpotlights = !worldStudio.includeSpotlights
        _uiState.value = _uiState.value.copy(isRoomSpotlightsEnabled = worldStudio.includeSpotlights)
    }

    fun toggleRoomAtmosphere() {
        worldStudio.includeAtmosphereParticles = !worldStudio.includeAtmosphereParticles
        _uiState.value = _uiState.value.copy(isRoomAtmosphereEnabled = worldStudio.includeAtmosphereParticles)
    }

    fun toggleItelA70Optimization() {
        val next = !_uiState.value.isItelA70Optimized
        worldStudio.isLowEndOptimized = next
        _uiState.value = _uiState.value.copy(isItelA70Optimized = next)
        showNotification("itel A70 Budget Optimizer: ${if (next) "ACTIVE (Low RAM, Smooth 60fps)" else "FULL RES"}")
    }

    fun buildRoomWorldScene(clearExistingWorld: Boolean = true) {
        if (clearExistingWorld) {
            scene.entities.removeAll {
                it.tag == EntityTag.ENVIRONMENT || it.name.contains("Floor") || it.name.contains("Wall") ||
                it.name.contains("Stage") || it.name.contains("Room") || it.name.contains("Terrain") ||
                it.name.contains("Dojo") || it.name.contains("Bed") || it.name.contains("Desk") ||
                it.name.contains("Truss") || it.name.contains("Spotlight")
            }
        }

        val roomEntities = worldStudio.buildRoomWorldEntities(
            preset = _uiState.value.selectedRoomPreset,
            floorStyle = _uiState.value.selectedFloorStyle,
            particleSystem = particleSystem
        )

        for (e in roomEntities) {
            scene.addEntity(e)
        }

        showNotification("Built '${_uiState.value.selectedRoomPreset.displayName}' (${roomEntities.size} components)!")
    }

    // --- Procedural Asset Forge ---
    fun setAssetCategory(category: ProceduralAssetCategory) {
        _uiState.value = _uiState.value.copy(selectedAssetCategory = category)
    }

    fun generateAndAddProceduralAsset() {
        val cat = _uiState.value.selectedAssetCategory
        val (assetName, generatedMesh, mat) = when (cat) {
            ProceduralAssetCategory.L_SYSTEM_FLORA -> {
                val mesh = ProceduralAssetGenerator.generateLSystemTree(lsystemConfig)
                Triple("L-System Tree", mesh, Material(name = "Flora Material", albedoColor = lsystemConfig.leafColor, roughness = 0.7f))
            }
            ProceduralAssetCategory.SHAPE_GRAMMAR_BUILDING -> {
                val mesh = ProceduralAssetGenerator.generateProceduralBuilding(buildingConfig)
                Triple("Grammar Tower", mesh, Material(name = "Cyber Tower Mat", albedoColor = buildingConfig.wallColor, metallic = 0.8f, roughness = 0.2f))
            }
            ProceduralAssetCategory.PROCEDURAL_WEAPON -> {
                val mesh = ProceduralAssetGenerator.generateProceduralWeapon(weaponConfig)
                Triple(weaponConfig.weaponType, mesh, Material(name = "Weapon Mat", albedoColor = weaponConfig.primaryColor, metallic = 0.9f, roughness = 0.1f))
            }
            ProceduralAssetCategory.DUNGEON_MAZE -> {
                val mesh = ProceduralAssetGenerator.generateProceduralDungeon(dungeonConfig)
                Triple("BSP Dungeon", mesh, Material(name = "Stone Dungeon", albedoColor = dungeonConfig.wallColor, roughness = 0.9f))
            }
            ProceduralAssetCategory.SPLINE_EXTRUSION -> {
                val mesh = ProceduralAssetGenerator.generateSplineExtrusion(splineConfig)
                Triple("Spline Extrusion", mesh, Material(name = "Spline Mat", albedoColor = splineConfig.color, metallic = 0.7f))
            }
        }

        val entity = Entity(name = assetName, tag = EntityTag.DEFAULT)
        entity.transform.position = Vector3(0f, 2.0f, 0f)
        entity.mesh = generatedMesh
        entity.material = mat
        entity.collider = Collider3D(ColliderType.BOX, size = generatedMesh.aabb.size)
        entity.rigidBody = RigidBody3D(mass = 1.0f, bodyType = BodyType.DYNAMIC)

        scene.addEntity(entity)
        selectEntity(entity.id)
        showNotification("Added Procedural '$assetName' to Scene!")
    }

    // --- Particle System VFX Controls ---
    fun setParticlePreset(preset: ParticlePreset) {
        particleSystem.applyPreset(preset)
        _uiState.value = _uiState.value.copy(activeParticlePreset = preset)
        showNotification("Particle Preset: ${preset.displayName}")
    }

    fun triggerParticleBurst() {
        particleSystem.triggerBurst(35)
        showNotification("Triggered Particle Burst (35 particles)!")
    }

    fun toggleParticleEmission() {
        particleSystem.isEmitting = !particleSystem.isEmitting
        _uiState.value = _uiState.value.copy(isParticleEmitting = particleSystem.isEmitting)
        showNotification("Particle Emission: ${if (particleSystem.isEmitting) "Active" else "Paused"}")
    }

    // --- Animation Timeline Controls ---
    fun toggleAnimationPlay() {
        animationEngine.isPlaying = !animationEngine.isPlaying
        _uiState.value = _uiState.value.copy(isAnimationPlaying = animationEngine.isPlaying)
    }

    fun setAnimationClip(clipType: String) {
        when (clipType) {
            "Walk Cycle" -> animationEngine.activeClip = AnimationTimelineEngine.createWalkCycleClip()
            "Hover Pulse" -> animationEngine.activeClip = AnimationTimelineEngine.createHoverPulseClip()
            "Bounce Impact" -> animationEngine.activeClip = AnimationTimelineEngine.createBounceImpactClip()
        }
        animationEngine.currentTimeSec = 0f
        showNotification("Animation Clip: $clipType")
    }

    fun setAnimationPlaybackSpeed(speed: Float) {
        animationEngine.playbackSpeed = speed
        _uiState.value = _uiState.value.copy(animationPlaybackSpeed = speed)
    }

    // AI Studio Generation
    fun setAiPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(aiPrompt = prompt)
    }

    fun setAiAssetType(type: AIAssetType) {
        _uiState.value = _uiState.value.copy(aiAssetType = type)
    }

    fun setAiComplexity(complexity: String) {
        _uiState.value = _uiState.value.copy(aiComplexity = complexity)
    }

    fun generateAiAsset(prompt: String, type: AIAssetType, complexity: String = "Medium") {
        _uiState.value = _uiState.value.copy(aiPrompt = prompt, aiAssetType = type, aiComplexity = complexity)
        generateAssetWithGemini()
    }

    fun generateAssetWithGemini() {
        val prompt = _uiState.value.aiPrompt
        val type = _uiState.value.aiAssetType
        val complexity = _uiState.value.aiComplexity

        _uiState.value = _uiState.value.copy(
            isAiGenerating = true,
            aiErrorMessage = null,
            lastGeneratedAsset = null
        )

        viewModelScope.launch {
            val result = Gemini3DStudio.generate3DAsset(prompt, type, complexity)
            result.onSuccess { assetResult ->
                _uiState.value = _uiState.value.copy(
                    isAiGenerating = false,
                    lastGeneratedAsset = assetResult,
                    aiErrorMessage = null
                )
                showNotification("Generated 3D Asset: ${assetResult.title}")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isAiGenerating = false,
                    aiErrorMessage = "Generation failed: ${error.localizedMessage}"
                )
            }
        }
    }

    fun addGeneratedAssetToScene() {
        val asset = _uiState.value.lastGeneratedAsset ?: return
        val newEntity = Entity(
            name = "AI_${asset.title.take(15)}",
            tag = EntityTag.DEFAULT
        )
        newEntity.transform.position = Vector3(0f, 1.5f, 0f)

        if (asset.generatedMesh != null) {
            newEntity.mesh = asset.generatedMesh
            newEntity.material = Material(
                name = "${asset.title} Mat",
                albedoColor = Color4(0.2f, 0.85f, 0.95f, 1f),
                metallic = 0.5f,
                roughness = 0.3f
            )
            newEntity.collider = Collider3D(ColliderType.BOX, size = asset.generatedMesh.aabb.size)
            newEntity.rigidBody = RigidBody3D(mass = 1.5f, bodyType = BodyType.DYNAMIC)
        }

        if (asset.generatedTexture != null) {
            applyTextureToSelectedEntity(asset.generatedTexture)
        }

        if (asset.generatedScript != null) {
            newEntity.script = ScriptComponent(
                scriptName = "${asset.title}Script",
                scriptSource = asset.generatedScript
            )
        }

        scene.addEntity(newEntity)
        selectEntity(newEntity.id)
        showNotification("Added AI Asset '${asset.title}' to 3D Viewport!")
    }

    fun updateSelectedScript(code: String) {
        val selected = getSelectedEntity() ?: return
        if (selected.script == null) {
            selected.script = ScriptComponent(scriptName = "${selected.name}Script", scriptSource = code)
        } else {
            selected.script!!.scriptSource = code
        }
        _uiState.value = _uiState.value.copy(activeScriptText = code)
        showNotification("Applied script to ${selected.name}")
    }

    fun prepareExport(target: ExportTarget) {
        val pkg = ProjectExporter.generateExportPackage(scene, target, scene.name)
        _uiState.value = _uiState.value.copy(
            exportTarget = target,
            activeExportPackage = pkg
        )
    }

    // Benchmarks
    fun runRamBenchmark() {
        val result = profiler.runRamPressureBenchmark()
        _uiState.value = _uiState.value.copy(ramBenchmarkResult = result)
        showNotification("RAM GC Benchmark: ${result.primaryMetric}")
    }

    fun runAssetLoadBenchmark() {
        val result = profiler.runAssetLoadBenchmark()
        _uiState.value = _uiState.value.copy(assetLoadBenchmarkResult = result)
        showNotification("Asset Load Speed: ${result.primaryMetric}")
    }

    fun runPhysicsStressBenchmark() {
        _uiState.value = _uiState.value.copy(isBenchmarkRunning = true, benchmarkScore = null)

        viewModelScope.launch {
            // Spawn 30 physics cubes in a cascade
            for (i in 0 until 30) {
                val box = Entity(name = "BenchCube_$i", tag = EntityTag.PHYSICS_PROP)
                box.transform.position = Vector3(
                    (i % 5 - 2) * 0.8f,
                    3.0f + (i / 5) * 1.2f,
                    ((i % 3) - 1) * 0.8f
                )
                box.mesh = Mesh.createCube(0.6f, color = Color4(1f, 0.3f + (i * 0.02f), 0.1f, 1f))
                box.material = Material(albedoColor = Color4.ORANGE, metallic = 0.6f)
                box.collider = Collider3D(ColliderType.BOX, size = Vector3(0.6f, 0.6f, 0.6f))
                box.rigidBody = RigidBody3D(mass = 0.8f, bodyType = BodyType.DYNAMIC)
                scene.addEntity(box)
                delay(25)
            }

            delay(1500)

            val avgFps = profiler.frameBudget.currentFps
            val score = when {
                avgFps >= 55f -> "A+ (Smooth 60 FPS - Outstanding Stability)"
                avgFps >= 45f -> "A (50+ FPS - Optimal for Itel A70 / Unisoc T603)"
                avgFps >= 30f -> "B (30 FPS Lock - Stable Playable Experience)"
                else -> "C (Low Tier Preset Recommended)"
            }

            _uiState.value = _uiState.value.copy(
                isBenchmarkRunning = false,
                benchmarkScore = "$score | Avg FPS: ${avgFps.toInt()} | Active Bodies: ${scene.entities.size}"
            )
            showNotification("60 FPS Stress Benchmark: $score")
        }
    }

    fun submitPlayerScore(playerName: String, score: Long) {
        viewModelScope.launch {
            repository.submitLeaderboardScore(
                LeaderboardEntry(
                    id = "lb_${System.currentTimeMillis()}",
                    playerName = playerName.ifBlank { "Mobile_Player" },
                    score = score,
                    timeAchievedFormatted = "10:15.2",
                    deviceTier = profiler.activePreset.name,
                    fpsAvg = profiler.frameBudget.currentFps
                )
            )
            showNotification("Leaderboard score submitted!")
        }
    }

    fun showNotification(msg: String) {
        _uiState.value = _uiState.value.copy(notificationMessage = msg)
        viewModelScope.launch {
            delay(2500)
            if (_uiState.value.notificationMessage == msg) {
                _uiState.value = _uiState.value.copy(notificationMessage = null)
            }
        }
    }

    fun saveProjectLocally() {
        viewModelScope.launch {
            val proj = ProjectEntity(
                id = "proj_${scene.name.lowercase().replace(" ", "_")}",
                name = scene.name,
                description = "3D Game Scene with ${scene.entities.size} active entities",
                lastModifiedTimestamp = System.currentTimeMillis(),
                sceneJson = "{\"name\":\"${scene.name}\"}",
                entityCount = scene.entities.size,
                targetPlatform = "Android / iOS / Web",
                isEncrypted = true,
                checksumSha256 = java.util.UUID.randomUUID().toString()
            )
            repository.saveProject(proj)
            showNotification("Project '${scene.name}' encrypted and saved locally!")
        }
    }

    // =========================================================================
    // COROUTINE PHYSICS SIMULATION CONTROLS
    // =========================================================================
    fun togglePhysicsColliders() {
        _uiState.value = _uiState.value.copy(showPhysicsColliders = !_uiState.value.showPhysicsColliders)
        showNotification("Physics Collider Visualizer: ${if (_uiState.value.showPhysicsColliders) "ON" else "OFF"}")
    }

    fun applyImpulseToSelected(impulse: Vector3) {
        val selected = getSelectedEntity() ?: return
        val rb = selected.rigidBody
        if (rb != null) {
            rb.applyImpulse(impulse)
            showNotification("Applied impulse (${impulse.x}, ${impulse.y}, ${impulse.z}) to ${selected.name}")
        } else {
            showNotification("${selected.name} does not have a RigidBody3D component")
        }
    }

    fun dropPhysicsCascadeTest() {
        viewModelScope.launch {
            for (i in 0 until 12) {
                val box = Entity(name = "CascadeBox_$i", tag = EntityTag.PHYSICS_PROP)
                val posX = (i % 3 - 1) * 0.9f
                val posY = 4.0f + (i * 0.8f)
                val posZ = ((i / 3) % 3 - 1) * 0.9f
                box.transform.position = Vector3(posX, posY, posZ)
                box.transform.rotation = Vector3(15f * i, 20f * i, 0f)

                val colChoice = when (i % 3) {
                    0 -> ColliderType.BOX
                    1 -> ColliderType.SPHERE
                    else -> ColliderType.BOX
                }

                if (colChoice == ColliderType.SPHERE) {
                    box.mesh = Mesh.createSphere(radius = 0.5f, rings = 10, sectors = 10, color = Color4(0f, 0.9f, 1f, 1f))
                    box.collider = Collider3D(ColliderType.SPHERE, radius = 0.5f)
                } else {
                    box.mesh = Mesh.createCube(size = 0.8f, color = Color4(1f, 0.5f + (i * 0.04f), 0.1f, 1f))
                    box.collider = Collider3D(ColliderType.BOX, size = Vector3(0.8f, 0.8f, 0.8f))
                }

                box.material = Material(
                    albedoColor = if (colChoice == ColliderType.SPHERE) Color4(0f, 0.85f, 1f, 1f) else Color4(1f, 0.5f, 0.1f, 1f),
                    metallic = 0.6f,
                    roughness = 0.25f
                )
                box.rigidBody = RigidBody3D(
                    mass = 1.0f,
                    bodyType = BodyType.DYNAMIC,
                    material = PhysicsMaterial(friction = 0.45f, bounciness = 0.65f)
                )

                scene.addEntity(box)
                delay(30)
            }
            showNotification("Spawned 12 dynamic rigid bodies in Coroutine Physics Engine!")
        }
    }

    // =========================================================================
    // CGI MOVIE DIRECTOR CONTROLS
    // =========================================================================
    fun toggleCinematicMode() {
        val newMode = !_uiState.value.isCinematicMode
        _uiState.value = _uiState.value.copy(isCinematicMode = newMode)
        movieDirector.isShotPlaying = newMode
        showNotification("CGI Cinema Mode: ${if (newMode) "ACTIVE" else "STANDARD"}")
    }

    fun setMovieShot(shot: CameraRigShot) {
        movieDirector.activeShot = shot
        movieDirector.shotPlaybackTime = 0f
        _uiState.value = _uiState.value.copy(selectedCgiShot = shot)
        showNotification("CGI Camera Rig: ${shot.displayName}")
    }

    fun setMovieAspect(aspect: AspectRatioPreset) {
        movieDirector.activeAspect = aspect
        _uiState.value = _uiState.value.copy(selectedCgiAspect = aspect)
        showNotification("Aspect Ratio: ${aspect.displayName}")
    }

    fun setMovieLut(lut: ColorGradingLut) {
        movieDirector.activeLut = lut
        _uiState.value = _uiState.value.copy(selectedCgiLut = lut)
        showNotification("Color Grading: ${lut.displayName}")
    }

    // =========================================================================
    // CHARACTER DESIGN & RIGGING STUDIO CONTROLS
    // =========================================================================
    fun setCharacterArchetype(arch: CharacterArchetype) {
        characterStudio.setArchetype(arch)
        _uiState.value = _uiState.value.copy(selectedCharacterArchetype = arch)
        showNotification("Character Archetype: ${arch.displayName}")
    }

    fun setCharacterPose(pose: ProceduralPose) {
        characterStudio.activePose = pose
        _uiState.value = _uiState.value.copy(selectedCharacterPose = pose)
        showNotification("Procedural Rig Pose: ${pose.displayName}")
    }

    fun spawnRiggedCharacterToScene() {
        val arch = characterStudio.activeArchetype
        val charMesh = characterStudio.generateCharacterMesh()

        val charEntity = Entity(
            name = "${arch.displayName.take(12)}",
            tag = EntityTag.PLAYER
        )
        charEntity.transform.position = Vector3(0f, 0.05f, 0f)
        charEntity.mesh = charMesh
        charEntity.material = Material(
            name = "${arch.name} Mat",
            albedoColor = arch.defaultPrimaryColor,
            metallic = 0.6f,
            roughness = 0.3f,
            emissionColor = arch.defaultGlowColor,
            emissionIntensity = 0.5f
        )
        charEntity.collider = Collider3D(ColliderType.CAPSULE, size = Vector3(0.8f, 1.9f, 0.8f), center = Vector3(0f, 0.95f, 0f))
        charEntity.rigidBody = RigidBody3D(
            mass = 2.5f,
            bodyType = BodyType.DYNAMIC,
            material = PhysicsMaterial(friction = 0.6f, bounciness = 0.2f)
        )

        scene.addEntity(charEntity)
        selectEntity(charEntity.id)
        showNotification("Spawned Rigged Hero '${arch.displayName}' into Scene!")
    }
}
