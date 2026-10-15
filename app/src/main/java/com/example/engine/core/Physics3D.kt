package com.example.engine.core

import kotlin.math.*

enum class BodyType {
    STATIC,
    DYNAMIC,
    KINEMATIC
}

enum class ColliderType {
    BOX,
    SPHERE,
    CAPSULE,
    MESH_CONVEX
}

enum class CollisionDetectionMode {
    DISCRETE,
    CONTINUOUS_CCD
}

enum class RestitutionCombineMode {
    AVERAGE,
    MINIMUM,
    MAXIMUM,
    MULTIPLY
}

enum class PhysicsLayer(val mask: Int) {
    DEFAULT(1 shl 0),
    PLAYER(1 shl 1),
    ENEMY(1 shl 2),
    TERRAIN(1 shl 3),
    PROP(1 shl 4),
    TRIGGER(1 shl 5),
    PROJECTILE(1 shl 6)
}

data class PhysicsMaterial(
    var friction: Float = 0.5f,
    var bounciness: Float = 0.5f,
    var dynamicFriction: Float = 0.4f
)

class RigidBody3D(
    var mass: Float = 1.0f,
    var bodyType: BodyType = BodyType.DYNAMIC,
    var velocity: Vector3 = Vector3.ZERO,
    var angularVelocity: Vector3 = Vector3.ZERO,
    var useGravity: Boolean = true,
    var isGrounded: Boolean = false,
    var linearDamping: Float = 0.05f,
    var angularDamping: Float = 0.1f,
    var material: PhysicsMaterial = PhysicsMaterial(),
    var layer: PhysicsLayer = PhysicsLayer.DEFAULT,
    var collisionMask: Int = 0xFFFF // Collide with all layers by default
) {
    fun applyForce(force: Vector3) {
        if (bodyType == BodyType.DYNAMIC && mass > 0f) {
            velocity += force / mass
        }
    }

    fun applyImpulse(impulse: Vector3) {
        if (bodyType == BodyType.DYNAMIC && mass > 0f) {
            velocity += impulse / mass
        }
    }

    fun applyTorqueImpulse(torque: Vector3) {
        if (bodyType == BodyType.DYNAMIC && mass > 0f) {
            angularVelocity += torque / mass
        }
    }
}

class Collider3D(
    var type: ColliderType = ColliderType.BOX,
    var size: Vector3 = Vector3.ONE,
    var radius: Float = 0.5f,
    var center: Vector3 = Vector3.ZERO,
    var isTrigger: Boolean = false
) {
    fun getAABB(transformPosition: Vector3, transformScale: Vector3): AABB {
        val half = when (type) {
            ColliderType.BOX -> size * transformScale * 0.5f
            ColliderType.SPHERE -> Vector3(radius * transformScale.x, radius * transformScale.y, radius * transformScale.z)
            ColliderType.CAPSULE, ColliderType.MESH_CONVEX -> Vector3(radius * transformScale.x, size.y * transformScale.y * 0.5f, radius * transformScale.z)
        }
        val c = transformPosition + center
        return AABB(c - half, c + half)
    }
}

data class RaycastHit(
    val point: Vector3,
    val normal: Vector3,
    val distance: Float,
    val entity: com.example.engine.ecs.Entity
)

class PhysicsWorld3D(
    var gravity: Vector3 = Vector3(0f, -9.81f, 0f),
    var fixedTimeStep: Float = 1f / 60f,
    var substeps: Int = 2,
    var timeScale: Float = 1.0f,
    var airDrag: Float = 0.02f,
    var collisionMode: CollisionDetectionMode = CollisionDetectionMode.DISCRETE,
    var restitutionMode: RestitutionCombineMode = RestitutionCombineMode.AVERAGE,
    var solverIterations: Int = 4
) {
    var collisionCount: Int = 0
    var simulatedBodiesCount: Int = 0

    // 7x7 Layer Collision Matrix (default: all layers collide except trigger with trigger)
    val layerCollisionMatrix = Array(7) { BooleanArray(7) { true } }.apply {
        val triggerIdx = PhysicsLayer.TRIGGER.ordinal
        this[triggerIdx][triggerIdx] = false
    }

    fun setLayerCollision(layerA: PhysicsLayer, layerB: PhysicsLayer, enabled: Boolean) {
        layerCollisionMatrix[layerA.ordinal][layerB.ordinal] = enabled
        layerCollisionMatrix[layerB.ordinal][layerA.ordinal] = enabled
    }

    fun canLayersCollide(layerA: PhysicsLayer, layerB: PhysicsLayer): Boolean {
        return layerCollisionMatrix[layerA.ordinal][layerB.ordinal]
    }

    fun step(entities: List<com.example.engine.ecs.Entity>, dtRaw: Float) {
        val dt = (dtRaw * timeScale).coerceIn(0.001f, 0.05f)
        val stepDt = dt / substeps.coerceIn(1, 8)

        simulatedBodiesCount = 0
        collisionCount = 0

        val dynamicEntities = entities.filter { it.enabled && it.rigidBody?.bodyType == BodyType.DYNAMIC }
        val allCollidables = entities.filter { it.enabled && it.collider != null }
        simulatedBodiesCount = dynamicEntities.size

        for (sub in 0 until substeps) {
            // 1. Integration step
            for (entity in dynamicEntities) {
                val rb = entity.rigidBody ?: continue
                val tf = entity.transform

                if (rb.useGravity) {
                    rb.velocity += gravity * stepDt
                }

                // Apply air drag and damping
                val totalDamping = (1.0f - (rb.linearDamping + airDrag) * stepDt).coerceIn(0f, 1f)
                rb.velocity *= totalDamping
                rb.angularVelocity *= (1.0f - rb.angularDamping * stepDt).coerceIn(0f, 1f)

                // Continuous Collision Detection (CCD) Ray step check
                if (collisionMode == CollisionDetectionMode.CONTINUOUS_CCD && rb.velocity.length() > 5.0f) {
                    val predictMove = rb.velocity * stepDt
                    // Prevent tunneling through ground
                    if (tf.position.y + predictMove.y < 0f) {
                        tf.position.y = 0f
                        rb.velocity.y = -rb.velocity.y * rb.material.bounciness
                    }
                }

                // Predict position
                tf.position += rb.velocity * stepDt
                tf.rotation += rb.angularVelocity * (stepDt * 57.2958f)

                // Ground plane collision (Y = 0)
                val aabb = entity.collider?.getAABB(tf.position, tf.scale)
                    ?: AABB(tf.position - Vector3(0.5f, 0.5f, 0.5f), tf.position + Vector3(0.5f, 0.5f, 0.5f))

                if (aabb.min.y <= 0f) {
                    val penetration = -aabb.min.y
                    tf.position.y += penetration
                    if (rb.velocity.y < 0f) {
                        rb.velocity.y = -rb.velocity.y * rb.material.bounciness
                        if (abs(rb.velocity.y) < 0.2f) rb.velocity.y = 0f
                        // Ground friction
                        rb.velocity.x *= (1f - rb.material.friction * 0.15f)
                        rb.velocity.z *= (1f - rb.material.friction * 0.15f)
                    }
                    rb.isGrounded = true
                } else {
                    rb.isGrounded = false
                }
            }

            // 2. Inter-entity collision resolution
            for (iter in 0 until solverIterations) {
                for (i in dynamicEntities.indices) {
                    val eA = dynamicEntities[i]
                    val rbA = eA.rigidBody ?: continue
                    val colA = eA.collider ?: continue
                    val boxA = colA.getAABB(eA.transform.position, eA.transform.scale)

                    for (j in allCollidables.indices) {
                        val eB = allCollidables[j]
                        if (eA == eB) continue
                        val rbB = eB.rigidBody
                        val colB = eB.collider ?: continue

                        // Check Layer collision filtering
                        val layerA = rbA.layer
                        val layerB = rbB?.layer ?: PhysicsLayer.DEFAULT
                        if (!canLayersCollide(layerA, layerB)) continue

                        val boxB = colB.getAABB(eB.transform.position, eB.transform.scale)

                        if (boxA.intersects(boxB)) {
                            collisionCount++

                            if (colA.isTrigger || colB.isTrigger) {
                                // Trigger event without physical impulse
                                continue
                            }

                            resolveCollision(eA, eB, boxA, boxB, rbA, rbB)
                        }
                    }
                }
            }
        }
    }

    private fun resolveCollision(
        eA: com.example.engine.ecs.Entity,
        eB: com.example.engine.ecs.Entity,
        boxA: AABB,
        boxB: AABB,
        rbA: RigidBody3D,
        rbB: RigidBody3D?
    ) {
        val delta = eA.transform.position - eB.transform.position
        val normal = if (delta.lengthSquared() > 0.0001f) delta.normalized() else Vector3.UP

        val overlapX = (boxA.halfExtents.x + boxB.halfExtents.x) - abs(delta.x)
        val overlapY = (boxA.halfExtents.y + boxB.halfExtents.y) - abs(delta.y)
        val overlapZ = (boxA.halfExtents.z + boxB.halfExtents.z) - abs(delta.z)

        val minOverlap = min(overlapX, min(overlapY, overlapZ))
        if (minOverlap > 0f) {
            val separation = normal * (minOverlap * 0.5f)
            eA.transform.position += separation

            val relativeVelocity = rbA.velocity - (rbB?.velocity ?: Vector3.ZERO)
            val velAlongNormal = relativeVelocity.dot(normal)

            if (velAlongNormal < 0f) {
                val matA = rbA.material
                val matB = rbB?.material ?: PhysicsMaterial()
                val restitution = when (restitutionMode) {
                    RestitutionCombineMode.AVERAGE -> (matA.bounciness + matB.bounciness) * 0.5f
                    RestitutionCombineMode.MINIMUM -> min(matA.bounciness, matB.bounciness)
                    RestitutionCombineMode.MAXIMUM -> max(matA.bounciness, matB.bounciness)
                    RestitutionCombineMode.MULTIPLY -> matA.bounciness * matB.bounciness
                }

                val totalInvMass = (1f / rbA.mass) + if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) (1f / rbB.mass) else 0f
                if (totalInvMass > 0f) {
                    val impulseScalar = -(1f + restitution) * velAlongNormal / totalInvMass
                    val impulse = normal * impulseScalar
                    rbA.velocity += impulse / rbA.mass
                    if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) {
                        rbB.velocity -= impulse / rbB.mass
                    }
                }
            }
        }
    }

    fun raycast(ray: Ray, entities: List<com.example.engine.ecs.Entity>, maxDist: Float = 100f): RaycastHit? {
        var closestHit: RaycastHit? = null
        var minDistance = maxDist

        for (entity in entities) {
            if (!entity.enabled || entity.collider == null) continue
            val aabb = entity.collider!!.getAABB(entity.transform.position, entity.transform.scale)
            val t = ray.intersectsAABB(aabb)
            if (t != null && t < minDistance) {
                minDistance = t
                val hitPoint = ray.getPoint(t)
                val normal = (hitPoint - entity.transform.position).normalized()
                closestHit = RaycastHit(hitPoint, normal, t, entity)
            }
        }

        return closestHit
    }
}
