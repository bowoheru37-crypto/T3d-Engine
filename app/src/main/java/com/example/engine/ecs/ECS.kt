package com.example.engine.ecs

import com.example.engine.core.*
import kotlin.math.*

enum class LightType {
    DIRECTIONAL,
    POINT,
    SPOT,
    AMBIENT
}

data class Light(
    var type: LightType = LightType.DIRECTIONAL,
    var color: Color4 = Color4(1f, 0.98f, 0.9f, 1f),
    var intensity: Float = 1.2f,
    var range: Float = 15f,
    var spotAngle: Float = 45f,
    var castShadows: Boolean = true
)

enum class CameraMode {
    ORBIT,
    FREECAM,
    FIRST_PERSON,
    TOP_DOWN,
    ISOMETRIC
}

enum class CameraPresetView {
    PERSPECTIVE_DEFAULT,
    TOP,
    FRONT,
    SIDE,
    ISOMETRIC
}

class Camera3D(
    var position: Vector3 = Vector3(0f, 4f, -8f),
    var target: Vector3 = Vector3(0f, 1f, 0f),
    var fov: Float = 60f,
    var near: Float = 0.1f,
    var far: Float = 100f,
    var mode: CameraMode = CameraMode.ORBIT,
    // Orbit camera parameters
    var distance: Float = 9f,
    var yaw: Float = 0f,
    var pitch: Float = 25f
) {
    fun setPresetView(preset: CameraPresetView) {
        when (preset) {
            CameraPresetView.TOP -> {
                yaw = 0f
                pitch = 88f
                distance = 14f
            }
            CameraPresetView.FRONT -> {
                yaw = 0f
                pitch = 5f
                distance = 10f
            }
            CameraPresetView.SIDE -> {
                yaw = 90f
                pitch = 5f
                distance = 10f
            }
            CameraPresetView.ISOMETRIC -> {
                yaw = 45f
                pitch = 35.264f
                distance = 11f
            }
            CameraPresetView.PERSPECTIVE_DEFAULT -> {
                yaw = 0f
                pitch = 25f
                distance = 9f
                target = Vector3(0f, 1f, 0f)
            }
        }
        updateOrbit(0f, 0f, 0f)
    }

    fun focusOn(point: Vector3, boundingRadius: Float = 2.0f) {
        target = point
        distance = (boundingRadius * 2.8f).coerceIn(3.5f, 25f)
        updateOrbit(0f, 0f, 0f)
    }

    fun updateOrbit(deltaYaw: Float, deltaPitch: Float, zoomDelta: Float) {
        yaw = (yaw + deltaYaw) % 360f
        pitch = (pitch + deltaPitch).coerceIn(-85f, 85f)
        distance = (distance - zoomDelta).coerceIn(2f, 40f)

        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()

        val x = target.x + distance * cos(pitchRad) * sin(yawRad)
        val y = target.y + distance * sin(pitchRad)
        val z = target.z - distance * cos(pitchRad) * cos(yawRad)

        position = Vector3(x, y, z)
    }

    fun getViewMatrix(): Matrix4x4 {
        return Matrix4x4.lookAt(position, target, Vector3.UP)
    }

    fun getProjectionMatrix(aspectRatio: Float): Matrix4x4 {
        return Matrix4x4.perspective(fov, aspectRatio, near, far)
    }
}

class Transform(
    var position: Vector3 = Vector3.ZERO,
    var rotation: Vector3 = Vector3.ZERO, // Euler angles in degrees
    var scale: Vector3 = Vector3.ONE
) {
    fun getLocalMatrix(): Matrix4x4 {
        val t = Matrix4x4.translation(position)
        val r = Matrix4x4.rotationEuler(rotation.y, rotation.x, rotation.z)
        val s = Matrix4x4.scale(scale)
        return t * r * s
    }

    fun getWorldMatrix(): Matrix4x4 = getLocalMatrix()

    fun forward(): Vector3 {
        val radY = Math.toRadians(rotation.y.toDouble()).toFloat()
        val radX = Math.toRadians(rotation.x.toDouble()).toFloat()
        return Vector3(
            sin(radY) * cos(radX),
            -sin(radX),
            cos(radY) * cos(radX)
        ).normalized()
    }
}

enum class EntityTag {
    DEFAULT,
    PLAYER,
    ENEMY,
    ENVIRONMENT,
    LIGHT,
    PHYSICS_PROP,
    COLLECTIBLE,
    TRIGGER_ZONE,
    PROJECTILE,
    WEAPON
}

data class ScriptComponent(
    var scriptName: String = "MovementScript",
    var scriptSource: String = """
        // T3D Engine Script
        on_start:
          speed = 4.0;
          jump_force = 6.5;
        
        on_update:
          rotate_y(15 * dt);
          if (touch_tap) {
            apply_impulse(0, jump_force, 0);
          }
    """.trimIndent(),
    var isRunning: Boolean = true,
    var isEnabled: Boolean = true,
    var localVariables: MutableMap<String, Any> = mutableMapOf()
)

data class AIBehaviorComponent(
    var aiType: String = "PatrolAndChase",
    var detectionRadius: Float = 6.0f,
    var attackRadius: Float = 1.5f,
    var movementSpeed: Float = 2.5f,
    var currentTargetId: String? = null,
    var state: String = "PATROL" // PATROL, CHASE, ATTACK, FLEE
)

class Entity(
    val id: String = java.util.UUID.randomUUID().toString().substring(0, 8),
    var name: String = "Entity",
    var tag: EntityTag = EntityTag.DEFAULT,
    var enabled: Boolean = true,
    var isSelected: Boolean = false
) {
    val transform: Transform = Transform()
    var mesh: Mesh? = null
    var material: Material = Material()
    var rigidBody: RigidBody3D? = null
    var collider: Collider3D? = null
    var light: Light? = null
    var script: ScriptComponent? = null
    var aiBehavior: AIBehaviorComponent? = null
    val children: MutableList<Entity> = mutableListOf()
    var parent: Entity? = null

    fun getGlobalMatrix(): Matrix4x4 {
        val parentMatrix = parent?.getGlobalMatrix() ?: Matrix4x4.identity()
        return parentMatrix * transform.getLocalMatrix()
    }

    fun getWorldMatrix(): Matrix4x4 = getGlobalMatrix()

    fun copy(
        id: String = java.util.UUID.randomUUID().toString().substring(0, 8),
        name: String = "${this.name}_Copy",
        tag: EntityTag = this.tag,
        enabled: Boolean = this.enabled
    ): Entity {
        val c = clone()
        return Entity(id = id, name = name, tag = tag, enabled = enabled).apply {
            transform.position = c.transform.position.copy()
            transform.rotation = c.transform.rotation.copy()
            transform.scale = c.transform.scale.copy()
            mesh = c.mesh
            material = c.material.copy()
            rigidBody = c.rigidBody
            collider = c.collider
            light = c.light
            script = c.script
        }
    }

    fun clone(): Entity {
        val newEntity = Entity(
            id = java.util.UUID.randomUUID().toString().substring(0, 8),
            name = "$name (Copy)",
            tag = tag,
            enabled = enabled
        )
        newEntity.transform.position = transform.position.copy() + Vector3(0.5f, 0f, 0.5f)
        newEntity.transform.rotation = transform.rotation.copy()
        newEntity.transform.scale = transform.scale.copy()
        newEntity.mesh = mesh?.clone()
        newEntity.material = material.copy()
        if (rigidBody != null) {
            newEntity.rigidBody = RigidBody3D(
                mass = rigidBody!!.mass,
                bodyType = rigidBody!!.bodyType,
                useGravity = rigidBody!!.useGravity,
                material = rigidBody!!.material.copy()
            )
        }
        if (collider != null) {
            newEntity.collider = Collider3D(
                type = collider!!.type,
                size = collider!!.size.copy(),
                radius = collider!!.radius,
                isTrigger = collider!!.isTrigger
            )
        }
        if (light != null) {
            newEntity.light = light!!.copy()
        }
        if (script != null) {
            newEntity.script = script!!.copy()
        }
        if (aiBehavior != null) {
            newEntity.aiBehavior = aiBehavior!!.copy()
        }
        return newEntity
    }
}

class Scene3D(
    var name: String = "DefaultScene",
    val entities: MutableList<Entity> = mutableListOf(),
    val camera: Camera3D = Camera3D(),
    val directionalLight: Light = Light(LightType.DIRECTIONAL, Color4(1f, 0.95f, 0.88f, 1f), 1.3f),
    var ambientColor: Color4 = Color4(0.18f, 0.22f, 0.32f, 1f),
    var skyTopColor: Color4 = Color4(0.08f, 0.12f, 0.24f, 1f),
    var skyBottomColor: Color4 = Color4(0.02f, 0.04f, 0.08f, 1f),
    var showGrid: Boolean = true,
    var gridSpacing: Float = 1.0f,
    var gridSize: Int = 10,
    var fogEnabled: Boolean = true,
    var fogDensity: Float = 0.02f
) {
    val physicsWorld: PhysicsWorld3D = PhysicsWorld3D()

    fun addEntity(entity: Entity) {
        entities.add(entity)
    }

    fun removeEntity(entity: Entity) {
        entities.remove(entity)
    }

    fun findEntityById(id: String): Entity? {
        return entities.find { it.id == id }
    }

    fun findEntityByTag(tag: EntityTag): Entity? {
        return entities.find { it.tag == tag }
    }

    companion object {
        fun createDefaultDemoScene(): Scene3D {
            val scene = Scene3D("Cyberpunk City Demo")

            // 1. Terrain ground
            val terrainEntity = Entity(name = "Cyber Grid Ground", tag = EntityTag.ENVIRONMENT)
            terrainEntity.mesh = Mesh.createTerrain(width = 24f, depth = 24f, gridX = 14, gridZ = 14)
            terrainEntity.material = Material(
                name = "Grid Neon",
                albedoColor = Color4(0.07f, 0.12f, 0.2f, 1f),
                wireframe = true,
                wireframeColor = Color4(0f, 0.85f, 0.95f, 1f),
                shadingMode = ShadingMode.WIREFRAME
            )
            terrainEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(24f, 0.2f, 24f))
            terrainEntity.rigidBody = RigidBody3D(bodyType = BodyType.STATIC)
            scene.addEntity(terrainEntity)

            // 2. Character Model (Player)
            val playerEntity = Entity(name = "Player Droid", tag = EntityTag.PLAYER)
            playerEntity.transform.position = Vector3(0f, 0f, 0f)
            playerEntity.mesh = Mesh.createCharacterModel()
            playerEntity.material = Material(
                name = "Droid Metal",
                albedoColor = Color4(0.12f, 0.6f, 0.95f, 1f),
                metallic = 0.7f,
                roughness = 0.3f
            )
            playerEntity.rigidBody = RigidBody3D(mass = 2.0f, bodyType = BodyType.DYNAMIC)
            playerEntity.collider = Collider3D(ColliderType.CAPSULE, size = Vector3(0.8f, 1.8f, 0.8f))
            playerEntity.isSelected = true
            scene.addEntity(playerEntity)

            // 3. Physics Cube Prop
            val cubeEntity = Entity(name = "Neon Energy Cube", tag = EntityTag.PHYSICS_PROP)
            cubeEntity.transform.position = Vector3(-2.5f, 3.0f, 1.5f)
            cubeEntity.mesh = Mesh.createCube(size = 1.2f, color = Color4(1.0f, 0.45f, 0.05f, 1f))
            cubeEntity.material = Material(
                name = "Orange Plasma",
                albedoColor = Color4(1.0f, 0.5f, 0.1f, 1f),
                metallic = 0.5f,
                roughness = 0.2f
            )
            cubeEntity.rigidBody = RigidBody3D(mass = 1.0f, bodyType = BodyType.DYNAMIC, material = PhysicsMaterial(friction = 0.4f, bounciness = 0.6f))
            cubeEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(1.2f, 1.2f, 1.2f))
            scene.addEntity(cubeEntity)

            // 4. Torus Plasma Ring
            val torusEntity = Entity(name = "Portal Plasma Ring", tag = EntityTag.ENVIRONMENT)
            torusEntity.transform.position = Vector3(3.0f, 2.0f, 0.5f)
            torusEntity.transform.rotation = Vector3(45f, 30f, 0f)
            torusEntity.mesh = Mesh.createTorus(r1 = 1.0f, r2 = 0.25f, rings = 16, sides = 12)
            torusEntity.material = Material(
                name = "Portal Glow",
                albedoColor = Color4(0.7f, 0.2f, 0.95f, 1f),
                emissionColor = Color4(0.8f, 0.3f, 1.0f, 1f),
                emissionIntensity = 0.8f
            )
            torusEntity.script = ScriptComponent(
                scriptName = "HoverAndSpin",
                scriptSource = "on_update:\n  rotate_y(40 * dt);\n  rotate_x(20 * dt);"
            )
            scene.addEntity(torusEntity)

            // 5. Cylinder Pillar
            val pillarEntity = Entity(name = "Data Tower Pillar", tag = EntityTag.ENVIRONMENT)
            pillarEntity.transform.position = Vector3(2.5f, 1.5f, -3.0f)
            pillarEntity.mesh = Mesh.createCylinder(radius = 0.6f, height = 3.0f)
            pillarEntity.material = Material(
                name = "Data Pillar",
                albedoColor = Color4(0.2f, 0.8f, 0.5f, 1f),
                metallic = 0.8f,
                roughness = 0.2f
            )
            pillarEntity.collider = Collider3D(ColliderType.BOX, size = Vector3(1.2f, 3.0f, 1.2f))
            pillarEntity.rigidBody = RigidBody3D(bodyType = BodyType.STATIC)
            scene.addEntity(pillarEntity)

            return scene
        }
    }
}
