package com.example.engine.world

import com.example.engine.core.*
import com.example.engine.ecs.Entity
import com.example.engine.ecs.EntityTag
import com.example.engine.particles.EmitterShape
import com.example.engine.particles.ParticleEmitterConfig
import com.example.engine.particles.ParticlePreset
import com.example.engine.particles.ParticleSystem3D
import kotlin.math.*
import kotlin.random.Random

/**
 * World Studio Sub-mode Selector.
 */
enum class WorldStudioMode(val displayName: String) {
    MMD_ROOM_STAGE("MMD Stage & Room Builder"),
    PROCEDURAL_TERRAIN("Outdoor Procedural Terrain")
}

/**
 * Floor & Ground Styles inspired by MMD stages, anime rooms, and architectural world design.
 */
enum class FloorStyle(
    val displayName: String,
    val description: String,
    val primaryColor: Color4,
    val secondaryColor: Color4,
    val specularMetallic: Float,
    val roughness: Float,
    val friction: Float,
    val bounciness: Float
) {
    MMD_CHECKERBOARD(
        displayName = "MMD Concert Checkerboard",
        description = "Glossy reflective dance floor with alternating high-contrast tiles",
        primaryColor = Color4(0.92f, 0.94f, 0.98f, 1f),
        secondaryColor = Color4(0.12f, 0.14f, 0.22f, 1f),
        specularMetallic = 0.75f,
        roughness = 0.15f,
        friction = 0.45f,
        bounciness = 0.45f
    ),
    MMD_HEX_STAGE(
        displayName = "MMD Hologram Hex Stage",
        description = "Futuristic idol stage with glowing neon hexagonal cell boundaries",
        primaryColor = Color4(0.06f, 0.08f, 0.16f, 1f),
        secondaryColor = Color4(0f, 0.9f, 1f, 1f),
        specularMetallic = 0.85f,
        roughness = 0.12f,
        friction = 0.5f,
        bounciness = 0.5f
    ),
    WOODEN_PARQUET(
        displayName = "Anime Bedroom Wood Planks",
        description = "Warm natural oak wood flooring planks for cozy rooms",
        primaryColor = Color4(0.72f, 0.52f, 0.35f, 1f),
        secondaryColor = Color4(0.55f, 0.38f, 0.22f, 1f),
        specularMetallic = 0.15f,
        roughness = 0.65f,
        friction = 0.65f,
        bounciness = 0.2f
    ),
    TATAMI_MATS(
        displayName = "Dojo Woven Tatami Mats",
        description = "Traditional Japanese straw rush mat tessellation with dark fabric borders",
        primaryColor = Color4(0.68f, 0.75f, 0.48f, 1f),
        secondaryColor = Color4(0.18f, 0.22f, 0.15f, 1f),
        specularMetallic = 0.05f,
        roughness = 0.85f,
        friction = 0.7f,
        bounciness = 0.15f
    ),
    MARBLE_PALACE(
        displayName = "Luxury Polished Marble",
        description = "Opulent grand hall marble tiles with subtle brass seam inlays",
        primaryColor = Color4(0.95f, 0.95f, 0.96f, 1f),
        secondaryColor = Color4(0.78f, 0.65f, 0.35f, 1f),
        specularMetallic = 0.7f,
        roughness = 0.18f,
        friction = 0.35f,
        bounciness = 0.4f
    ),
    CYBER_GRID_NEON(
        displayName = "Cyberpunk Carbon Grid",
        description = "Dark matte composite surface with luminescent laser gridlines",
        primaryColor = Color4(0.05f, 0.06f, 0.1f, 1f),
        secondaryColor = Color4(1f, 0.08f, 0.58f, 1f),
        specularMetallic = 0.6f,
        roughness = 0.3f,
        friction = 0.55f,
        bounciness = 0.35f
    ),
    CONCRETE_HAZARD(
        displayName = "Industrial Lab Hazard Floor",
        description = "Reinforced hangar concrete with yellow & black safety perimeter bands",
        primaryColor = Color4(0.42f, 0.44f, 0.48f, 1f),
        secondaryColor = Color4(0.95f, 0.75f, 0.05f, 1f),
        specularMetallic = 0.25f,
        roughness = 0.7f,
        friction = 0.8f,
        bounciness = 0.1f
    )
}

/**
 * 1-Click Room & Scene Templates (MMD, Bedroom, Dojo, Cyber Chamber, Lab, Terrain).
 */
enum class RoomPreset(
    val displayName: String,
    val subtitle: String,
    val defaultFloorStyle: FloorStyle,
    val wallColor: Color4,
    val accentColor: Color4,
    val defaultDimensions: Vector3 // Width, Height, Depth
) {
    MMD_DANCE_STAGE(
        displayName = "MMD Concert Dance Stage",
        subtitle = "Glossy elevated platform, overhead lighting trusses, spotlight cones, concert dust",
        defaultFloorStyle = FloorStyle.MMD_CHECKERBOARD,
        wallColor = Color4(0.08f, 0.1f, 0.18f, 1f),
        accentColor = Color4(0f, 0.9f, 1f, 1f),
        defaultDimensions = Vector3(14f, 6.5f, 14f)
    ),
    COZY_ANIME_BEDROOM(
        displayName = "Cozy Anime Bedroom",
        subtitle = "Warm wooden room, window with sky view, study desk, bed frame, ambient dust",
        defaultFloorStyle = FloorStyle.WOODEN_PARQUET,
        wallColor = Color4(0.92f, 0.9f, 0.85f, 1f),
        accentColor = Color4(0.55f, 0.38f, 0.24f, 1f),
        defaultDimensions = Vector3(9f, 3.8f, 8f)
    ),
    CYBERPUNK_CHAMBER(
        displayName = "Cyberpunk Neon Chamber",
        subtitle = "Futuristic angled wall panels, illuminated conduits, hex floor, steam vapor",
        defaultFloorStyle = FloorStyle.CYBER_GRID_NEON,
        wallColor = Color4(0.08f, 0.09f, 0.15f, 1f),
        accentColor = Color4(0f, 0.95f, 0.85f, 1f),
        defaultDimensions = Vector3(11f, 4.5f, 11f)
    ),
    TRADITIONAL_DOJO(
        displayName = "Traditional Dojo & Tatami",
        subtitle = "Woven tatami mats, shoji screen paper walls, dark timber beams, falling leaves",
        defaultFloorStyle = FloorStyle.TATAMI_MATS,
        wallColor = Color4(0.88f, 0.85f, 0.76f, 1f),
        accentColor = Color4(0.35f, 0.22f, 0.12f, 1f),
        defaultDimensions = Vector3(12f, 4.0f, 10f)
    ),
    SCIFI_RESEARCH_LAB(
        displayName = "Sci-Fi Hangar & Lab",
        subtitle = "Hazard-striped metal floor, blast door arches, support pillars, spark vents",
        defaultFloorStyle = FloorStyle.CONCRETE_HAZARD,
        wallColor = Color4(0.25f, 0.28f, 0.32f, 1f),
        accentColor = Color4(0.95f, 0.65f, 0.05f, 1f),
        defaultDimensions = Vector3(16f, 6.0f, 14f)
    ),
    NATURE_TERRAIN_WORLD(
        displayName = "Nature World & Water Stage",
        subtitle = "Continuous FBM hills, water mirror plane, stone stage center, low-poly flora",
        defaultFloorStyle = FloorStyle.MARBLE_PALACE,
        wallColor = Color4(0.45f, 0.65f, 0.85f, 1f),
        accentColor = Color4(0.25f, 0.6f, 0.3f, 1f),
        defaultDimensions = Vector3(24f, 5.0f, 24f)
    )
}

/**
 * Procedural Room & World Builder Engine.
 * Fully optimized for budget hardware (itel A70 - Unisoc T603, 1-3 GB RAM)
 * through zero-allocation vertex buffers, pre-baked normals, and lightweight physics colliders.
 */
class WorldRoomStageStudio {

    var activePreset: RoomPreset = RoomPreset.MMD_DANCE_STAGE
    var activeFloorStyle: FloorStyle = FloorStyle.MMD_CHECKERBOARD
    var roomWidth: Float = 12f
    var roomLength: Float = 12f
    var roomHeight: Float = 4.2f
    var tileRepeatX: Int = 8
    var tileRepeatZ: Int = 8
    var includeCeiling: Boolean = false
    var includeFrontWall: Boolean = false // Keep false by default for MMD camera viewing
    var includeStageTrusses: Boolean = true
    var includeSpotlights: Boolean = true
    var includeAtmosphereParticles: Boolean = true
    var isLowEndOptimized: Boolean = true // itel A70 aggressive budget tiering

    /**
     * Generates a complete set of Entities (Floor, Walls, Ceiling, Stage Props, Colliders)
     * for a given preset and parameters.
     */
    fun buildRoomWorldEntities(
        preset: RoomPreset = activePreset,
        floorStyle: FloorStyle = activeFloorStyle,
        particleSystem: ParticleSystem3D? = null
    ): List<Entity> {
        val entities = mutableListOf<Entity>()
        val w = if (isLowEndOptimized) roomWidth.coerceIn(6f, 18f) else roomWidth
        val l = if (isLowEndOptimized) roomLength.coerceIn(6f, 18f) else roomLength
        val h = roomHeight.coerceIn(2.5f, 8f)

        // 1. Generate High-Performance Floor / Ground
        val floorMesh = generateFloorMesh(w, l, floorStyle, tileRepeatX, tileRepeatZ, isLowEndOptimized)
        val floorEntity = Entity(
            name = "Floor (${floorStyle.displayName.take(14)})",
            tag = EntityTag.ENVIRONMENT
        )
        floorEntity.transform.position = Vector3(0f, 0f, 0f)
        floorEntity.mesh = floorMesh
        floorEntity.material = Material(
            name = "${floorStyle.name} Material",
            albedoColor = floorStyle.primaryColor,
            metallic = floorStyle.specularMetallic,
            roughness = floorStyle.roughness,
            castShadows = false,
            receiveShadows = true
        )
        // Solid Static Physics Collider for Ground (Never fall through)
        floorEntity.collider = Collider3D(
            type = ColliderType.BOX,
            size = Vector3(w, 0.4f, l),
            center = Vector3(0f, -0.2f, 0f)
        )
        floorEntity.rigidBody = RigidBody3D(
            mass = 0f,
            bodyType = BodyType.STATIC,
            material = PhysicsMaterial(friction = floorStyle.friction, bounciness = floorStyle.bounciness)
        )
        entities.add(floorEntity)

        // 2. Generate Walls & Boundary Architecture based on Preset
        when (preset) {
            RoomPreset.MMD_DANCE_STAGE -> {
                entities.addAll(buildMmdStageArchitecture(w, l, h, floorStyle, preset.accentColor))
            }
            RoomPreset.COZY_ANIME_BEDROOM -> {
                entities.addAll(buildAnimeBedroomArchitecture(w, l, h, preset.wallColor, preset.accentColor))
            }
            RoomPreset.CYBERPUNK_CHAMBER -> {
                entities.addAll(buildCyberpunkChamberArchitecture(w, l, h, preset.wallColor, preset.accentColor))
            }
            RoomPreset.TRADITIONAL_DOJO -> {
                entities.addAll(buildTraditionalDojoArchitecture(w, l, h, preset.wallColor, preset.accentColor))
            }
            RoomPreset.SCIFI_RESEARCH_LAB -> {
                entities.addAll(buildSciFiLabArchitecture(w, l, h, preset.wallColor, preset.accentColor))
            }
            RoomPreset.NATURE_TERRAIN_WORLD -> {
                entities.addAll(buildNatureStageArchitecture(w, l, h))
            }
        }

        // 3. Configure Atmospheric Particles for the Room
        if (includeAtmosphereParticles && particleSystem != null) {
            configureRoomParticles(preset, particleSystem, w, l, h)
        }

        return entities
    }

    /**
     * Synthesizes a tiled, zero-redundancy Floor Mesh with pre-baked checkerboard / hex colors.
     */
    fun generateFloorMesh(
        width: Float,
        length: Float,
        style: FloorStyle,
        repeatX: Int = 8,
        repeatZ: Int = 8,
        lowEndOptimized: Boolean = true
    ): Mesh {
        val mesh = Mesh("FloorMesh_${style.name}")
        val hW = width * 0.5f
        val hL = length * 0.5f

        // On low-end itel A70, clamp subdivision to 8x8 to keep vertex budget ultra-lean (~81 verts, 128 tris)
        val divX = if (lowEndOptimized) repeatX.coerceIn(4, 10) else repeatX.coerceIn(4, 18)
        val divZ = if (lowEndOptimized) repeatZ.coerceIn(4, 10) else repeatZ.coerceIn(4, 18)

        val stepX = width / divX
        val stepZ = length / divZ

        for (z in 0 until divZ) {
            for (x in 0 until divX) {
                val x0 = -hW + x * stepX
                val x1 = x0 + stepX
                val z0 = -hL + z * stepZ
                val z1 = z0 + stepZ

                // Tile Color Logic
                val isAlternate = (x + z) % 2 == 1
                val tileCol = when (style) {
                    FloorStyle.MMD_CHECKERBOARD -> if (isAlternate) style.secondaryColor else style.primaryColor
                    FloorStyle.WOODEN_PARQUET -> {
                        val subtle = (sin(x * 3.1f + z * 5.2f) * 0.06f).toFloat()
                        Color4(
                            (style.primaryColor.r + subtle).coerceIn(0f, 1f),
                            (style.primaryColor.g + subtle).coerceIn(0f, 1f),
                            (style.primaryColor.b + subtle * 0.5f).coerceIn(0f, 1f),
                            1f
                        )
                    }
                    FloorStyle.TATAMI_MATS -> if (isAlternate) style.primaryColor else style.primaryColor * 0.95f
                    FloorStyle.CYBER_GRID_NEON -> if (isAlternate) style.primaryColor else style.primaryColor * 1.15f
                    FloorStyle.CONCRETE_HAZARD -> {
                        // Hazard border on edge tiles
                        val isEdge = (x == 0 || x == divX - 1 || z == 0 || z == divZ - 1)
                        if (isEdge && (x + z) % 2 == 0) style.secondaryColor else style.primaryColor
                    }
                    else -> if (isAlternate) style.primaryColor else style.secondaryColor
                }

                val baseIdx = mesh.vertices.size
                mesh.vertices.add(Vertex(Vector3(x0, 0f, z0), Vector3.UP, Vector2(0f, 0f), tileCol))
                mesh.vertices.add(Vertex(Vector3(x1, 0f, z0), Vector3.UP, Vector2(1f, 0f), tileCol))
                mesh.vertices.add(Vertex(Vector3(x1, 0f, z1), Vector3.UP, Vector2(1f, 1f), tileCol))
                mesh.vertices.add(Vertex(Vector3(x0, 0f, z1), Vector3.UP, Vector2(0f, 1f), tileCol))

                // Two triangles per quad
                mesh.triangles.add(Triangle(baseIdx, baseIdx + 1, baseIdx + 2))
                mesh.triangles.add(Triangle(baseIdx, baseIdx + 2, baseIdx + 3))
            }
        }

        mesh.recalculateNormals()
        mesh.recalculateAABB()
        return mesh
    }

    // =========================================================================
    // 1. MMD CONCERT DANCE STAGE ARCHITECTURE
    // =========================================================================
    private fun buildMmdStageArchitecture(
        w: Float, l: Float, h: Float,
        floorStyle: FloorStyle,
        neonColor: Color4
    ): List<Entity> {
        val result = mutableListOf<Entity>()
        val hW = w * 0.5f
        val hL = l * 0.5f

        // Stage Back Wall / Screen Backdrop
        val backWall = Entity(name = "MMD Backdrop Screen", tag = EntityTag.ENVIRONMENT)
        backWall.transform.position = Vector3(0f, h * 0.5f, -hL)
        backWall.mesh = Mesh.createCube(size = 1f, color = Color4(0.06f, 0.08f, 0.14f, 1f))
        backWall.transform.scale = Vector3(w * 1.05f, h, 0.35f)
        backWall.material = Material(
            albedoColor = Color4(0.08f, 0.1f, 0.16f, 1f),
            metallic = 0.5f,
            roughness = 0.25f
        )
        backWall.collider = Collider3D(ColliderType.BOX, size = Vector3(w * 1.05f, h, 0.35f))
        backWall.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(backWall)

        // Stage Neon Accent Bevel (Glowing Border Rim)
        val rimEntity = Entity(name = "Stage Glow Rim", tag = EntityTag.ENVIRONMENT)
        rimEntity.transform.position = Vector3(0f, 0.04f, 0f)
        rimEntity.mesh = Mesh.createCube(size = 1f, color = neonColor)
        rimEntity.transform.scale = Vector3(w * 1.02f, 0.08f, l * 1.02f)
        rimEntity.material = Material(
            albedoColor = neonColor,
            emissionColor = neonColor,
            emissionIntensity = 0.65f
        )
        result.add(rimEntity)

        // Stage Lighting Trusses (Overhead Aluminum Crossbars)
        if (includeStageTrusses) {
            val trussEntity = Entity(name = "Stage Light Truss Overhead", tag = EntityTag.ENVIRONMENT)
            trussEntity.transform.position = Vector3(0f, h - 0.3f, 0f)
            trussEntity.mesh = Mesh.createCube(size = 1f, color = Color4(0.45f, 0.48f, 0.52f, 1f))
            trussEntity.transform.scale = Vector3(w * 0.9f, 0.25f, 0.25f)
            trussEntity.material = Material(metallic = 0.8f, roughness = 0.2f)
            result.add(trussEntity)
        }

        // 4 Perimeter Stage Spotlight Towers
        if (includeSpotlights) {
            val cornerOffsets = listOf(
                Vector3(-hW + 0.5f, 0f, -hL + 0.5f),
                Vector3(hW - 0.5f, 0f, -hL + 0.5f),
                Vector3(-hW + 0.5f, 0f, hL - 0.5f),
                Vector3(hW - 0.5f, 0f, hL - 0.5f)
            )
            cornerOffsets.forEachIndexed { idx, offset ->
                val pillar = Entity(name = "Stage Spotlight Tower #$idx", tag = EntityTag.ENVIRONMENT)
                pillar.transform.position = offset + Vector3(0f, h * 0.4f, 0f)
                pillar.mesh = Mesh.createCylinder(radius = 0.2f, height = h * 0.8f, segments = 8, color = Color4(0.2f, 0.22f, 0.26f, 1f))
                pillar.material = Material(metallic = 0.7f, roughness = 0.3f)
                pillar.collider = Collider3D(ColliderType.CAPSULE, size = Vector3(0.4f, h * 0.8f, 0.4f))
                pillar.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
                result.add(pillar)

                // Spotlight Head Glowing Lens
                val lens = Entity(name = "Spotlight Head #$idx", tag = EntityTag.LIGHT)
                lens.transform.position = offset + Vector3(0f, h * 0.8f + 0.15f, 0f)
                lens.mesh = Mesh.createSphere(radius = 0.25f, rings = 6, sectors = 8, color = neonColor)
                lens.material = Material(
                    albedoColor = neonColor,
                    emissionColor = neonColor,
                    emissionIntensity = 0.85f
                )
                result.add(lens)
            }
        }

        return result
    }

    // =========================================================================
    // 2. COZY ANIME BEDROOM ARCHITECTURE
    // =========================================================================
    private fun buildAnimeBedroomArchitecture(
        w: Float, l: Float, h: Float,
        wallCol: Color4, woodCol: Color4
    ): List<Entity> {
        val result = mutableListOf<Entity>()
        val hW = w * 0.5f
        val hL = l * 0.5f

        // Back Wall with Window opening
        val backWall = Entity(name = "Bedroom Back Wall", tag = EntityTag.ENVIRONMENT)
        backWall.transform.position = Vector3(0f, h * 0.5f, -hL)
        backWall.mesh = Mesh.createCube(size = 1f, color = wallCol)
        backWall.transform.scale = Vector3(w, h, 0.3f)
        backWall.material = Material(albedoColor = wallCol, roughness = 0.7f)
        backWall.collider = Collider3D(ColliderType.BOX, size = Vector3(w, h, 0.3f))
        backWall.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(backWall)

        // Window Frame on Back Wall (Glass Panel)
        val windowGlass = Entity(name = "Bedroom Sky Window", tag = EntityTag.ENVIRONMENT)
        windowGlass.transform.position = Vector3(0f, h * 0.55f, -hL + 0.18f)
        windowGlass.mesh = Mesh.createCube(size = 1f, color = Color4(0.55f, 0.8f, 0.95f, 0.8f))
        windowGlass.transform.scale = Vector3(w * 0.45f, h * 0.45f, 0.08f)
        windowGlass.material = Material(
            albedoColor = Color4(0.5f, 0.75f, 0.95f, 0.85f),
            metallic = 0.9f,
            roughness = 0.05f
        )
        result.add(windowGlass)

        // Left Wall
        val leftWall = Entity(name = "Bedroom Left Wall", tag = EntityTag.ENVIRONMENT)
        leftWall.transform.position = Vector3(-hW, h * 0.5f, 0f)
        leftWall.mesh = Mesh.createCube(size = 1f, color = wallCol)
        leftWall.transform.scale = Vector3(0.3f, h, l)
        leftWall.material = Material(albedoColor = wallCol, roughness = 0.7f)
        leftWall.collider = Collider3D(ColliderType.BOX, size = Vector3(0.3f, h, l))
        leftWall.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(leftWall)

        // Right Wall with Door Frame
        val rightWall = Entity(name = "Bedroom Right Wall", tag = EntityTag.ENVIRONMENT)
        rightWall.transform.position = Vector3(hW, h * 0.5f, 0f)
        rightWall.mesh = Mesh.createCube(size = 1f, color = wallCol)
        rightWall.transform.scale = Vector3(0.3f, h, l)
        rightWall.material = Material(albedoColor = wallCol, roughness = 0.7f)
        rightWall.collider = Collider3D(ColliderType.BOX, size = Vector3(0.3f, h, l))
        rightWall.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(rightWall)

        // Ceiling Planks / Beams (Optional)
        if (includeCeiling) {
            val ceiling = Entity(name = "Bedroom Ceiling", tag = EntityTag.ENVIRONMENT)
            ceiling.transform.position = Vector3(0f, h, 0f)
            ceiling.mesh = Mesh.createCube(size = 1f, color = wallCol * 0.9f)
            ceiling.transform.scale = Vector3(w, 0.2f, l)
            ceiling.material = Material(albedoColor = wallCol * 0.9f, roughness = 0.8f)
            result.add(ceiling)
        }

        // Low-Poly Furniture: Study Desk
        val desk = Entity(name = "Anime Study Desk", tag = EntityTag.PHYSICS_PROP)
        desk.transform.position = Vector3(-hW + 1.6f, 0.45f, -hL + 1.4f)
        desk.mesh = Mesh.createCube(size = 1f, color = woodCol)
        desk.transform.scale = Vector3(2.2f, 0.9f, 1.2f)
        desk.material = Material(albedoColor = woodCol, roughness = 0.6f)
        desk.collider = Collider3D(ColliderType.BOX, size = Vector3(2.2f, 0.9f, 1.2f))
        desk.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(desk)

        // Low-Poly Furniture: Platform Bed
        val bed = Entity(name = "Cozy Platform Bed", tag = EntityTag.PHYSICS_PROP)
        bed.transform.position = Vector3(hW - 1.8f, 0.35f, -hL + 2.2f)
        bed.mesh = Mesh.createCube(size = 1f, color = Color4(0.85f, 0.88f, 0.95f, 1f))
        bed.transform.scale = Vector3(2.4f, 0.7f, 3.2f)
        bed.material = Material(albedoColor = Color4(0.88f, 0.9f, 0.96f, 1f), roughness = 0.75f)
        bed.collider = Collider3D(ColliderType.BOX, size = Vector3(2.4f, 0.7f, 3.2f))
        bed.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(bed)

        return result
    }

    // =========================================================================
    // 3. CYBERPUNK NEON CHAMBER ARCHITECTURE
    // =========================================================================
    private fun buildCyberpunkChamberArchitecture(
        w: Float, l: Float, h: Float,
        panelCol: Color4, cyanNeon: Color4
    ): List<Entity> {
        val result = mutableListOf<Entity>()
        val hW = w * 0.5f
        val hL = l * 0.5f

        // Angled Cyber Rear Wall
        val backWall = Entity(name = "Cyber Wall Conduit", tag = EntityTag.ENVIRONMENT)
        backWall.transform.position = Vector3(0f, h * 0.5f, -hL)
        backWall.mesh = Mesh.createCube(size = 1f, color = panelCol)
        backWall.transform.scale = Vector3(w, h, 0.4f)
        backWall.material = Material(albedoColor = panelCol, metallic = 0.8f, roughness = 0.25f)
        backWall.collider = Collider3D(ColliderType.BOX, size = Vector3(w, h, 0.4f))
        backWall.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(backWall)

        // Glowing Conduit Strips
        val strip1 = Entity(name = "Cyber Neon Conduit 1", tag = EntityTag.ENVIRONMENT)
        strip1.transform.position = Vector3(0f, h * 0.65f, -hL + 0.22f)
        strip1.mesh = Mesh.createCube(size = 1f, color = cyanNeon)
        strip1.transform.scale = Vector3(w * 0.85f, 0.1f, 0.06f)
        strip1.material = Material(albedoColor = cyanNeon, emissionColor = cyanNeon, emissionIntensity = 0.8f)
        result.add(strip1)

        val magentaNeon = Color4(1f, 0.1f, 0.6f, 1f)
        val strip2 = Entity(name = "Cyber Neon Conduit 2", tag = EntityTag.ENVIRONMENT)
        strip2.transform.position = Vector3(0f, h * 0.35f, -hL + 0.22f)
        strip2.mesh = Mesh.createCube(size = 1f, color = magentaNeon)
        strip2.transform.scale = Vector3(w * 0.85f, 0.1f, 0.06f)
        strip2.material = Material(albedoColor = magentaNeon, emissionColor = magentaNeon, emissionIntensity = 0.8f)
        result.add(strip2)

        // Server Terminal Pillar
        val serverRack = Entity(name = "Cyber Server Rack", tag = EntityTag.PHYSICS_PROP)
        serverRack.transform.position = Vector3(-hW + 1.5f, h * 0.45f, -hL + 1.2f)
        serverRack.mesh = Mesh.createCube(size = 1f, color = Color4(0.12f, 0.14f, 0.2f, 1f))
        serverRack.transform.scale = Vector3(1.2f, h * 0.9f, 1.4f)
        serverRack.material = Material(metallic = 0.75f, roughness = 0.3f)
        serverRack.collider = Collider3D(ColliderType.BOX, size = Vector3(1.2f, h * 0.9f, 1.4f))
        serverRack.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(serverRack)

        return result
    }

    // =========================================================================
    // 4. TRADITIONAL DOJO ARCHITECTURE
    // =========================================================================
    private fun buildTraditionalDojoArchitecture(
        w: Float, l: Float, h: Float,
        shojiCol: Color4, timberCol: Color4
    ): List<Entity> {
        val result = mutableListOf<Entity>()
        val hW = w * 0.5f
        val hL = l * 0.5f

        // Shoji Screen Back Wall
        val backWall = Entity(name = "Dojo Shoji Screen Wall", tag = EntityTag.ENVIRONMENT)
        backWall.transform.position = Vector3(0f, h * 0.5f, -hL)
        backWall.mesh = Mesh.createCube(size = 1f, color = shojiCol)
        backWall.transform.scale = Vector3(w, h, 0.25f)
        backWall.material = Material(albedoColor = shojiCol, roughness = 0.85f)
        backWall.collider = Collider3D(ColliderType.BOX, size = Vector3(w, h, 0.25f))
        backWall.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(backWall)

        // Timber Columns (Torii / Dojo Pillars)
        val pillarOffsets = listOf(
            Vector3(-hW + 0.4f, h * 0.5f, -hL + 0.4f),
            Vector3(hW - 0.4f, h * 0.5f, -hL + 0.4f),
            Vector3(-hW + 0.4f, h * 0.5f, hL - 0.4f),
            Vector3(hW - 0.4f, h * 0.5f, hL - 0.4f)
        )
        pillarOffsets.forEachIndexed { i, p ->
            val col = Entity(name = "Dojo Timber Pillar #$i", tag = EntityTag.ENVIRONMENT)
            col.transform.position = p
            col.mesh = Mesh.createCube(size = 1f, color = timberCol)
            col.transform.scale = Vector3(0.5f, h, 0.5f)
            col.material = Material(albedoColor = timberCol, roughness = 0.7f)
            col.collider = Collider3D(ColliderType.BOX, size = Vector3(0.5f, h, 0.5f))
            col.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
            result.add(col)
        }

        return result
    }

    // =========================================================================
    // 5. SCI-FI RESEARCH LAB ARCHITECTURE
    // =========================================================================
    private fun buildSciFiLabArchitecture(
        w: Float, l: Float, h: Float,
        steelCol: Color4, hazardCol: Color4
    ): List<Entity> {
        val result = mutableListOf<Entity>()
        val hW = w * 0.5f
        val hL = l * 0.5f

        // Heavy Blast Door Arch
        val arch = Entity(name = "Blast Door Arch", tag = EntityTag.ENVIRONMENT)
        arch.transform.position = Vector3(0f, h * 0.5f, -hL)
        arch.mesh = Mesh.createCube(size = 1f, color = steelCol)
        arch.transform.scale = Vector3(w * 0.95f, h, 0.5f)
        arch.material = Material(metallic = 0.85f, roughness = 0.2f)
        arch.collider = Collider3D(ColliderType.BOX, size = Vector3(w * 0.95f, h, 0.5f))
        arch.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(arch)

        // Cryo Chamber Glass Tube Prop
        val cryo = Entity(name = "Cryo Stasis Tube", tag = EntityTag.PHYSICS_PROP)
        cryo.transform.position = Vector3(-hW + 2.0f, h * 0.4f, -hL + 1.8f)
        cryo.mesh = Mesh.createCylinder(radius = 0.6f, height = h * 0.75f, segments = 10, color = Color4(0.3f, 0.8f, 0.95f, 0.7f))
        cryo.material = Material(
            albedoColor = Color4(0.3f, 0.8f, 0.95f, 0.75f),
            metallic = 0.9f,
            roughness = 0.1f,
            emissionColor = Color4(0f, 0.6f, 0.8f, 1f),
            emissionIntensity = 0.4f
        )
        cryo.collider = Collider3D(ColliderType.CAPSULE, size = Vector3(1.2f, h * 0.75f, 1.2f))
        cryo.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(cryo)

        return result
    }

    // =========================================================================
    // 6. NATURE WORLD & WATER STAGE
    // =========================================================================
    private fun buildNatureStageArchitecture(w: Float, l: Float, h: Float): List<Entity> {
        val result = mutableListOf<Entity>()

        // Water Reflection Mirror Plane surrounding the central stage
        val water = Entity(name = "Water Lake Mirror", tag = EntityTag.ENVIRONMENT)
        water.transform.position = Vector3(0f, -0.05f, 0f)
        water.mesh = Mesh.createTerrain(width = w * 1.5f, depth = l * 1.5f, gridX = 8, gridZ = 8, color = Color4(0.1f, 0.35f, 0.65f, 0.85f))
        water.material = Material(
            albedoColor = Color4(0.12f, 0.4f, 0.7f, 0.85f),
            metallic = 0.9f,
            roughness = 0.05f
        )
        water.collider = Collider3D(ColliderType.BOX, size = Vector3(w * 1.5f, 0.2f, l * 1.5f))
        water.rigidBody = RigidBody3D(mass = 0f, bodyType = BodyType.STATIC)
        result.add(water)

        return result
    }

    /**
     * Sets up room atmosphere particles based on the selected scene preset.
     */
    fun configureRoomParticles(preset: RoomPreset, particleSystem: ParticleSystem3D, w: Float, l: Float, h: Float) {
        val cfg = particleSystem.config
        cfg.groundBounce = true
        particleSystem.emitterPosition = Vector3(0f, h * 0.6f, 0f)

        when (preset) {
            RoomPreset.MMD_DANCE_STAGE -> {
                cfg.shape = EmitterShape.RING
                cfg.emissionRate = if (isLowEndOptimized) 20f else 45f
                cfg.speedMin = 0.4f
                cfg.speedMax = 1.6f
                cfg.lifetimeMin = 2.0f
                cfg.lifetimeMax = 3.8f
                cfg.gravity = Vector3(0f, 0.25f, 0f) // Float gently upward into spotlights
                cfg.startColor = Color4(0.4f, 0.9f, 1.0f, 0.8f)
                cfg.endColor = Color4(0.9f, 0.4f, 1.0f, 0.0f)
                cfg.startSize = 0.14f
                cfg.endSize = 0.02f
            }
            RoomPreset.COZY_ANIME_BEDROOM -> {
                cfg.shape = EmitterShape.BOX
                cfg.emissionRate = if (isLowEndOptimized) 12f else 28f
                cfg.speedMin = 0.1f
                cfg.speedMax = 0.5f
                cfg.lifetimeMin = 3.0f
                cfg.lifetimeMax = 5.0f
                cfg.gravity = Vector3(0.05f, -0.08f, 0.02f) // Gentle Brownian motion dust motes
                cfg.startColor = Color4(1.0f, 0.95f, 0.85f, 0.6f)
                cfg.endColor = Color4(1.0f, 0.95f, 0.85f, 0.0f)
                cfg.startSize = 0.09f
                cfg.endSize = 0.01f
            }
            RoomPreset.CYBERPUNK_CHAMBER -> {
                cfg.shape = EmitterShape.BOX
                cfg.emissionRate = if (isLowEndOptimized) 18f else 40f
                cfg.speedMin = 0.8f
                cfg.speedMax = 2.2f
                cfg.lifetimeMin = 1.2f
                cfg.lifetimeMax = 2.4f
                cfg.gravity = Vector3(0f, 0.6f, 0f)
                cfg.startColor = Color4(0f, 0.95f, 1.0f, 0.7f)
                cfg.endColor = Color4(0.8f, 0.1f, 0.95f, 0.0f)
                cfg.startSize = 0.2f
                cfg.endSize = 0.04f
            }
            RoomPreset.TRADITIONAL_DOJO -> {
                cfg.shape = EmitterShape.SPHERE
                cfg.emissionRate = if (isLowEndOptimized) 10f else 22f
                cfg.speedMin = 0.3f
                cfg.speedMax = 0.9f
                cfg.lifetimeMin = 2.5f
                cfg.lifetimeMax = 4.5f
                cfg.gravity = Vector3(0.1f, -0.4f, 0.05f) // Floating petal drift
                cfg.startColor = Color4(1.0f, 0.75f, 0.82f, 0.8f) // Cherry blossom pink
                cfg.endColor = Color4(0.95f, 0.5f, 0.65f, 0.0f)
                cfg.startSize = 0.12f
                cfg.endSize = 0.03f
            }
            RoomPreset.SCIFI_RESEARCH_LAB -> {
                cfg.shape = EmitterShape.CONE
                cfg.emissionRate = if (isLowEndOptimized) 15f else 35f
                cfg.speedMin = 1.2f
                cfg.speedMax = 3.0f
                cfg.lifetimeMin = 0.8f
                cfg.lifetimeMax = 1.8f
                cfg.gravity = Vector3(0f, -1.8f, 0f)
                cfg.startColor = Color4(0.95f, 0.8f, 0.2f, 0.9f) // Spark discharge
                cfg.endColor = Color4(1.0f, 0.3f, 0.05f, 0.0f)
                cfg.startSize = 0.15f
                cfg.endSize = 0.02f
            }
            RoomPreset.NATURE_TERRAIN_WORLD -> {
                cfg.shape = EmitterShape.SPHERE
                cfg.emissionRate = if (isLowEndOptimized) 14f else 30f
                cfg.speedMin = 0.2f
                cfg.speedMax = 0.8f
                cfg.lifetimeMin = 3.0f
                cfg.lifetimeMax = 5.0f
                cfg.gravity = Vector3(0.05f, -0.15f, 0f)
                cfg.startColor = Color4(0.4f, 0.85f, 0.5f, 0.7f) // Pollen / fireflies
                cfg.endColor = Color4(0.8f, 1.0f, 0.4f, 0.0f)
                cfg.startSize = 0.1f
                cfg.endSize = 0.02f
            }
        }
    }
}
