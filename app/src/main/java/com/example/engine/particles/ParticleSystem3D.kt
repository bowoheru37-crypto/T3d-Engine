package com.example.engine.particles

import com.example.engine.core.*
import kotlin.math.*
import kotlin.random.Random

enum class EmitterShape {
    POINT,
    BOX,
    CONE,
    SPHERE,
    RING
}

enum class ParticlePreset(val displayName: String) {
    CYBER_PLASMA_SPARKS("Cyber Plasma Sparks"),
    MAGMA_VOLCANO_BURST("Magma Volcano Burst"),
    MAGIC_PORTAL_AURA("Magic Portal Aura"),
    BLIZZARD_SNOWFALL("Blizzard Snowfall"),
    JET_PLASMA_THRUSTER("Jet Plasma Thruster"),
    WARP_SPEED_STARDUST("Warp Speed Stardust"),
    CYBER_SMOKE_PLUME("Cyber Smoke Plume"),
    STAGE_CONCERT_HAZE("MMD Stage Concert Haze"),
    ROOM_AMBIENT_DUST("Room Ambient Dust Motes"),
    CYBER_FLOOR_STEAM("Cyber Floor Steam Mist"),
    GROUND_IMPACT_DUST("Ground Landing Dust")
}

data class Particle(
    var position: Vector3 = Vector3.ZERO,
    var velocity: Vector3 = Vector3.ZERO,
    var startColor: Color4 = Color4.CYAN,
    var endColor: Color4 = Color4(0f, 0.2f, 1f, 0f),
    var currentColor: Color4 = Color4.CYAN,
    var size: Float = 0.15f,
    var age: Float = 0f,
    var lifetime: Float = 1.5f,
    var isAlive: Boolean = false
)

data class ParticleEmitterConfig(
    var name: String = "ParticleEmitter",
    var shape: EmitterShape = EmitterShape.CONE,
    var emissionRate: Float = 45f, // particles per second
    var burstCount: Int = 30,
    var lifetimeMin: Float = 0.8f,
    var lifetimeMax: Float = 1.8f,
    var speedMin: Float = 1.5f,
    var speedMax: Float = 3.5f,
    var startSize: Float = 0.18f,
    var endSize: Float = 0.04f,
    var gravity: Vector3 = Vector3(0f, -2.5f, 0f),
    var spreadAngleDeg: Float = 35f,
    var startColor: Color4 = Color4(0f, 0.95f, 1f, 1f),
    var endColor: Color4 = Color4(0.8f, 0.1f, 0.9f, 0f),
    var groundBounce: Boolean = true,
    var bounceDamping: Float = 0.45f
)

class ParticleSystem3D(
    var maxParticles: Int = 200,
    var config: ParticleEmitterConfig = ParticleEmitterConfig()
) {
    val particles: Array<Particle> = Array(maxParticles) { Particle() }
    var emitterPosition: Vector3 = Vector3(0f, 1.5f, 0f)
    private var emissionTimer: Float = 0f
    private val random = Random(1337)
    var isEmitting: Boolean = true

    fun applyPreset(preset: ParticlePreset) {
        when (preset) {
            ParticlePreset.CYBER_PLASMA_SPARKS -> {
                config = ParticleEmitterConfig(
                    name = "Cyber Sparks",
                    shape = EmitterShape.SPHERE,
                    emissionRate = 60f,
                    speedMin = 2.0f,
                    speedMax = 5.0f,
                    lifetimeMin = 0.5f,
                    lifetimeMax = 1.2f,
                    gravity = Vector3(0f, -4.0f, 0f),
                    startColor = Color4(0f, 1f, 0.9f, 1f),
                    endColor = Color4(0.1f, 0.3f, 1f, 0f),
                    startSize = 0.15f,
                    endSize = 0.02f,
                    groundBounce = true
                )
            }
            ParticlePreset.MAGMA_VOLCANO_BURST -> {
                config = ParticleEmitterConfig(
                    name = "Magma Burst",
                    shape = EmitterShape.CONE,
                    emissionRate = 50f,
                    speedMin = 3.0f,
                    speedMax = 6.5f,
                    lifetimeMin = 1.0f,
                    lifetimeMax = 2.0f,
                    gravity = Vector3(0f, -6.0f, 0f),
                    spreadAngleDeg = 25f,
                    startColor = Color4(1f, 0.8f, 0.1f, 1f),
                    endColor = Color4(0.8f, 0.05f, 0.01f, 0f),
                    startSize = 0.22f,
                    endSize = 0.06f,
                    groundBounce = true
                )
            }
            ParticlePreset.MAGIC_PORTAL_AURA -> {
                config = ParticleEmitterConfig(
                    name = "Portal Aura",
                    shape = EmitterShape.RING,
                    emissionRate = 40f,
                    speedMin = 0.8f,
                    speedMax = 1.8f,
                    lifetimeMin = 1.2f,
                    lifetimeMax = 2.4f,
                    gravity = Vector3(0f, 1.2f, 0f),
                    startColor = Color4(0.85f, 0.2f, 1f, 0.9f),
                    endColor = Color4(0.1f, 0.8f, 1f, 0f),
                    startSize = 0.2f,
                    endSize = 0.05f,
                    groundBounce = false
                )
            }
            ParticlePreset.BLIZZARD_SNOWFALL -> {
                config = ParticleEmitterConfig(
                    name = "Blizzard Snow",
                    shape = EmitterShape.BOX,
                    emissionRate = 70f,
                    speedMin = 0.5f,
                    speedMax = 1.5f,
                    lifetimeMin = 2.0f,
                    lifetimeMax = 3.5f,
                    gravity = Vector3(0.5f, -1.2f, 0.2f),
                    startColor = Color4(0.9f, 0.95f, 1f, 0.85f),
                    endColor = Color4(0.7f, 0.85f, 1f, 0f),
                    startSize = 0.12f,
                    endSize = 0.08f,
                    groundBounce = false
                )
            }
            ParticlePreset.JET_PLASMA_THRUSTER -> {
                config = ParticleEmitterConfig(
                    name = "Plasma Thruster",
                    shape = EmitterShape.CONE,
                    emissionRate = 80f,
                    speedMin = 4.0f,
                    speedMax = 8.0f,
                    lifetimeMin = 0.3f,
                    lifetimeMax = 0.7f,
                    gravity = Vector3.ZERO,
                    spreadAngleDeg = 15f,
                    startColor = Color4(0.1f, 0.9f, 1f, 1f),
                    endColor = Color4(0.8f, 0.1f, 1f, 0f),
                    startSize = 0.25f,
                    endSize = 0.02f,
                    groundBounce = false
                )
            }
            ParticlePreset.WARP_SPEED_STARDUST -> {
                config = ParticleEmitterConfig(
                    name = "Warp Stardust",
                    shape = EmitterShape.POINT,
                    emissionRate = 60f,
                    speedMin = 5.0f,
                    speedMax = 10.0f,
                    lifetimeMin = 0.6f,
                    lifetimeMax = 1.4f,
                    gravity = Vector3.ZERO,
                    startColor = Color4.WHITE,
                    endColor = Color4(0.3f, 0.8f, 1f, 0f),
                    startSize = 0.14f,
                    endSize = 0.02f,
                    groundBounce = false
                )
            }
            ParticlePreset.CYBER_SMOKE_PLUME -> {
                config = ParticleEmitterConfig(
                    name = "Smoke Plume",
                    shape = EmitterShape.CONE,
                    emissionRate = 30f,
                    speedMin = 0.6f,
                    speedMax = 1.6f,
                    lifetimeMin = 1.8f,
                    lifetimeMax = 3.0f,
                    gravity = Vector3(0.1f, 0.8f, 0.05f),
                    spreadAngleDeg = 30f,
                    startColor = Color4(0.25f, 0.28f, 0.35f, 0.7f),
                    endColor = Color4(0.08f, 0.09f, 0.12f, 0f),
                    startSize = 0.15f,
                    endSize = 0.45f,
                    groundBounce = false
                )
            }
            ParticlePreset.STAGE_CONCERT_HAZE -> {
                config = ParticleEmitterConfig(
                    name = "Stage Haze",
                    shape = EmitterShape.RING,
                    emissionRate = 35f,
                    speedMin = 0.3f,
                    speedMax = 1.4f,
                    lifetimeMin = 2.0f,
                    lifetimeMax = 3.6f,
                    gravity = Vector3(0f, 0.3f, 0f),
                    startColor = Color4(0.35f, 0.9f, 1f, 0.75f),
                    endColor = Color4(0.85f, 0.3f, 1f, 0f),
                    startSize = 0.14f,
                    endSize = 0.03f,
                    groundBounce = true
                )
            }
            ParticlePreset.ROOM_AMBIENT_DUST -> {
                config = ParticleEmitterConfig(
                    name = "Room Dust",
                    shape = EmitterShape.BOX,
                    emissionRate = 22f,
                    speedMin = 0.08f,
                    speedMax = 0.4f,
                    lifetimeMin = 3.0f,
                    lifetimeMax = 5.0f,
                    gravity = Vector3(0.04f, -0.06f, 0.02f),
                    startColor = Color4(1f, 0.96f, 0.88f, 0.65f),
                    endColor = Color4(1f, 0.96f, 0.88f, 0f),
                    startSize = 0.08f,
                    endSize = 0.01f,
                    groundBounce = false
                )
            }
            ParticlePreset.CYBER_FLOOR_STEAM -> {
                config = ParticleEmitterConfig(
                    name = "Floor Steam",
                    shape = EmitterShape.BOX,
                    emissionRate = 30f,
                    speedMin = 0.6f,
                    speedMax = 1.8f,
                    lifetimeMin = 1.0f,
                    lifetimeMax = 2.2f,
                    gravity = Vector3(0.15f, 0.45f, 0f),
                    startColor = Color4(0f, 0.95f, 1f, 0.7f),
                    endColor = Color4(0.8f, 0.1f, 0.9f, 0f),
                    startSize = 0.18f,
                    endSize = 0.04f,
                    groundBounce = true
                )
            }
            ParticlePreset.GROUND_IMPACT_DUST -> {
                config = ParticleEmitterConfig(
                    name = "Ground Landing Dust",
                    shape = EmitterShape.RING,
                    emissionRate = 0f,
                    burstCount = 20,
                    speedMin = 1.5f,
                    speedMax = 3.8f,
                    lifetimeMin = 0.4f,
                    lifetimeMax = 0.9f,
                    gravity = Vector3(0f, -1.5f, 0f),
                    startColor = Color4(0.85f, 0.8f, 0.75f, 0.8f),
                    endColor = Color4(0.6f, 0.58f, 0.55f, 0f),
                    startSize = 0.16f,
                    endSize = 0.02f,
                    groundBounce = true
                )
            }
        }
    }

    fun triggerBurst(count: Int = config.burstCount) {
        var spawned = 0
        for (p in particles) {
            if (!p.isAlive) {
                spawnParticle(p)
                spawned++
                if (spawned >= count) break
            }
        }
    }

    private fun spawnParticle(p: Particle) {
        p.isAlive = true
        p.age = 0f
        p.lifetime = config.lifetimeMin + random.nextFloat() * (config.lifetimeMax - config.lifetimeMin)
        p.startColor = config.startColor
        p.endColor = config.endColor
        p.currentColor = config.startColor

        // Position & Velocity by Emitter Shape
        val speed = config.speedMin + random.nextFloat() * (config.speedMax - config.speedMin)
        when (config.shape) {
            EmitterShape.POINT -> {
                p.position = emitterPosition.copy()
                val dir = Vector3(
                    (random.nextFloat() - 0.5f) * 2f,
                    (random.nextFloat() - 0.5f) * 2f,
                    (random.nextFloat() - 0.5f) * 2f
                ).normalized()
                p.velocity = dir * speed
            }
            EmitterShape.BOX -> {
                val boxR = 2.0f
                p.position = emitterPosition + Vector3(
                    (random.nextFloat() - 0.5f) * boxR,
                    random.nextFloat() * 1.5f,
                    (random.nextFloat() - 0.5f) * boxR
                )
                p.velocity = Vector3((random.nextFloat() - 0.5f) * 0.4f, -speed, (random.nextFloat() - 0.5f) * 0.4f)
            }
            EmitterShape.CONE -> {
                p.position = emitterPosition.copy()
                val rad = Math.toRadians(config.spreadAngleDeg.toDouble()).toFloat()
                val phi = random.nextFloat() * 2.0f * PI.toFloat()
                val theta = random.nextFloat() * rad
                val vx = sin(theta) * cos(phi)
                val vy = cos(theta)
                val vz = sin(theta) * sin(phi)
                p.velocity = Vector3(vx, vy, vz).normalized() * speed
            }
            EmitterShape.SPHERE -> {
                val r = 0.2f
                val u = random.nextFloat()
                val v = random.nextFloat()
                val theta = u * 2.0f * PI.toFloat()
                val phi = acos(2.0f * v - 1.0f)
                val dx = sin(phi) * cos(theta)
                val dy = sin(phi) * sin(theta)
                val dz = cos(phi)
                p.position = emitterPosition + Vector3(dx * r, dy * r, dz * r)
                p.velocity = Vector3(dx, dy, dz) * speed
            }
            EmitterShape.RING -> {
                val ringR = 1.2f
                val theta = random.nextFloat() * 2.0f * PI.toFloat()
                val rx = cos(theta) * ringR
                val rz = sin(theta) * ringR
                p.position = emitterPosition + Vector3(rx, 0f, rz)
                p.velocity = Vector3(0f, speed, 0f)
            }
        }
    }

    fun update(dt: Float) {
        // Continuous emission
        if (isEmitting && config.emissionRate > 0) {
            emissionTimer += dt
            val interval = 1.0f / config.emissionRate
            while (emissionTimer >= interval) {
                emissionTimer -= interval
                val freeP = particles.firstOrNull { !it.isAlive }
                if (freeP != null) {
                    spawnParticle(freeP)
                }
            }
        }

        // Particle dynamics update
        for (p in particles) {
            if (p.isAlive) {
                p.age += dt
                if (p.age >= p.lifetime) {
                    p.isAlive = false
                    continue
                }

                val t = (p.age / p.lifetime).coerceIn(0f, 1f)

                // Velocity & Gravity
                p.velocity += config.gravity * dt
                p.position += p.velocity * dt

                // Ground Collision
                if (config.groundBounce && p.position.y <= 0f) {
                    p.position.y = 0f
                    p.velocity.y = -p.velocity.y * config.bounceDamping
                    p.velocity.x *= 0.8f
                    p.velocity.z *= 0.8f
                }

                // Color & Size Interpolation
                p.currentColor = p.startColor.lerp(p.endColor, t)
                p.size = config.startSize + (config.endSize - config.startSize) * t
            }
        }
    }

    fun getActiveParticleCount(): Int = particles.count { it.isAlive }
}
