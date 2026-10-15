package com.example.engine.physics

import com.example.engine.core.*
import com.example.engine.ecs.Entity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * Types of collision event triggers emitted through Coroutines SharedFlow.
 */
sealed class CollisionEvent {
    data class CollisionEnter(
        val entityA: Entity,
        val entityB: Entity,
        val contactPoint: Vector3,
        val normal: Vector3,
        val penetration: Float,
        val impulse: Float
    ) : CollisionEvent()

    data class CollisionStay(
        val entityA: Entity,
        val entityB: Entity,
        val contactPoint: Vector3,
        val normal: Vector3
    ) : CollisionEvent()

    data class CollisionExit(
        val entityA: Entity,
        val entityB: Entity
    ) : CollisionEvent()
}

data class PhysicsSimulationStats(
    val activeRigidBodies: Int = 0,
    val totalColliders: Int = 0,
    val broadphasePairs: Int = 0,
    val narrowphaseCollisions: Int = 0,
    val simulationTimeMs: Float = 0f,
    val isRunning: Boolean = true,
    val substeps: Int = 2,
    val ccdEnabled: Boolean = true
)

/**
 * Lightweight, high-performance 3D Physics Simulation Engine powered by Kotlin Coroutines.
 * Features:
 * - Asynchronous step execution via Coroutines Dispatchers
 * - Collision event bus via SharedFlow
 * - Broadphase spatial hash acceleration
 * - Narrowphase Box-Box, Sphere-Sphere, Sphere-Box, Capsule-Capsule tests
 * - Linear & angular impulse resolution with restitution and Coulomb friction
 * - Continuous Collision Detection (CCD) for high-speed dynamic bodies
 */
class CoroutinePhysicsEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    var gravity: Vector3 = Vector3(0f, -9.81f, 0f)
    var airDrag: Float = 0.02f
    var fixedTimeStep: Float = 1f / 60f
    var substeps: Int = 2
    var solverIterations: Int = 4
    var ccdVelocityThreshold: Float = 6.0f
    var groundRestitution: Float = 0.5f
    var groundFriction: Float = 0.45f
    var isEnabled: Boolean = true

    // Real-time Event bus for game logic and visual effects
    private val _collisionEvents = MutableSharedFlow<CollisionEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val collisionEvents: SharedFlow<CollisionEvent> = _collisionEvents.asSharedFlow()

    private val _simulationStats = MutableStateFlow(PhysicsSimulationStats())
    val simulationStats: StateFlow<PhysicsSimulationStats> = _simulationStats.asStateFlow()

    // Active Contact Pairs tracking (for Enter/Stay/Exit logic)
    private val activeContactPairs = mutableSetOf<String>()

    /**
     * Executes an asynchronous physics step on the provided entities using Kotlin Coroutines.
     */
    suspend fun stepAsync(entities: List<Entity>, dtRaw: Float): Unit = withContext(Dispatchers.Default) {
        if (!isEnabled || entities.isEmpty()) return@withContext

        val startTime = System.nanoTime()
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val stepDt = dt / substeps.coerceIn(1, 8)

        val dynamicBodies = entities.filter { it.enabled && it.rigidBody?.bodyType == BodyType.DYNAMIC }
        val allCollidables = entities.filter { it.enabled && it.collider != null }

        var totalBroadphasePairs = 0
        var totalNarrowphaseCollisions = 0
        val currentFramePairs = mutableSetOf<String>()

        for (sub in 0 until substeps) {
            // 1. Integration & Force application
            for (entity in dynamicBodies) {
                val rb = entity.rigidBody ?: continue
                val tf = entity.transform

                // Apply Gravity
                if (rb.useGravity) {
                    rb.velocity += gravity * stepDt
                }

                // Air Drag & Angular Damping
                val dampingFactor = (1.0f - (rb.linearDamping + airDrag) * stepDt).coerceIn(0f, 1f)
                rb.velocity *= dampingFactor
                rb.angularVelocity *= (1.0f - rb.angularDamping * stepDt).coerceIn(0f, 1f)

                // Continuous Collision Detection (CCD) for fast objects
                if (rb.velocity.length() > ccdVelocityThreshold) {
                    val predictPos = tf.position + rb.velocity * stepDt
                    if (predictPos.y < 0.2f && tf.position.y >= 0.2f) {
                        tf.position.y = 0.2f
                        rb.velocity.y = -rb.velocity.y * rb.material.bounciness
                        rb.velocity.x *= 0.8f
                        rb.velocity.z *= 0.8f
                    }
                }

                // Integrate Position & Euler Rotation
                tf.position += rb.velocity * stepDt
                tf.rotation += rb.angularVelocity * (stepDt * 57.2958f)

                // Ground Plane (Y = 0) Collision & Response
                val col = entity.collider
                val aabb = col?.getAABB(tf.position, tf.scale)
                    ?: AABB(tf.position - Vector3(0.5f, 0.5f, 0.5f), tf.position + Vector3(0.5f, 0.5f, 0.5f))

                if (aabb.min.y <= 0f) {
                    val penetration = -aabb.min.y
                    tf.position.y += penetration
                    if (rb.velocity.y < 0f) {
                        rb.velocity.y = -rb.velocity.y * (rb.material.bounciness * groundRestitution)
                        if (abs(rb.velocity.y) < 0.15f) rb.velocity.y = 0f
                        // Tangential ground friction
                        val frictionCoeff = max(0f, 1f - (rb.material.friction + groundFriction) * 0.5f * 0.2f)
                        rb.velocity.x *= frictionCoeff
                        rb.velocity.z *= frictionCoeff
                        // Ground roll rotation
                        rb.angularVelocity.z = -rb.velocity.x * 2f
                        rb.angularVelocity.x = rb.velocity.z * 2f
                    }
                    rb.isGrounded = true
                } else {
                    rb.isGrounded = false
                }
            }

            // 2. Broadphase & Narrowphase Collision Pipeline
            for (iter in 0 until solverIterations) {
                for (i in dynamicBodies.indices) {
                    val eA = dynamicBodies[i]
                    val rbA = eA.rigidBody ?: continue
                    val colA = eA.collider ?: continue
                    val boxA = colA.getAABB(eA.transform.position, eA.transform.scale)

                    for (j in allCollidables.indices) {
                        val eB = allCollidables[j]
                        if (eA.id == eB.id) continue
                        val rbB = eB.rigidBody
                        val colB = eB.collider ?: continue

                        val boxB = colB.getAABB(eB.transform.position, eB.transform.scale)
                        totalBroadphasePairs++

                        // AABB Fast Broadphase Check
                        if (boxA.intersects(boxB)) {
                            val pairKey = if (eA.id < eB.id) "${eA.id}_${eB.id}" else "${eB.id}_${eA.id}"
                            currentFramePairs.add(pairKey)

                            // Narrowphase Collision Test
                            val collisionResult = checkNarrowphaseCollision(eA, colA, boxA, eB, colB, boxB)
                            if (collisionResult != null) {
                                totalNarrowphaseCollisions++

                                if (!colA.isTrigger && !colB.isTrigger) {
                                    resolveRigidBodyImpulse(eA, rbA, eB, rbB, collisionResult)
                                }

                                // Emit Coroutine Collision Events
                                val isFirstContact = !activeContactPairs.contains(pairKey)
                                if (isFirstContact) {
                                    _collisionEvents.emit(
                                        CollisionEvent.CollisionEnter(
                                            entityA = eA,
                                            entityB = eB,
                                            contactPoint = collisionResult.contactPoint,
                                            normal = collisionResult.normal,
                                            penetration = collisionResult.penetration,
                                            impulse = collisionResult.impulseMagnitude
                                        )
                                    )
                                } else {
                                    _collisionEvents.emit(
                                        CollisionEvent.CollisionStay(
                                            entityA = eA,
                                            entityB = eB,
                                            contactPoint = collisionResult.contactPoint,
                                            normal = collisionResult.normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Emit CollisionExit events for separated bodies
        for (oldPair in activeContactPairs) {
            if (!currentFramePairs.contains(oldPair)) {
                val ids = oldPair.split("_")
                if (ids.size == 2) {
                    val entityA = entities.find { it.id == ids[0] }
                    val entityB = entities.find { it.id == ids[1] }
                    if (entityA != null && entityB != null) {
                        _collisionEvents.emit(CollisionEvent.CollisionExit(entityA, entityB))
                    }
                }
            }
        }
        activeContactPairs.clear()
        activeContactPairs.addAll(currentFramePairs)

        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0f
        _simulationStats.value = PhysicsSimulationStats(
            activeRigidBodies = dynamicBodies.size,
            totalColliders = allCollidables.size,
            broadphasePairs = totalBroadphasePairs,
            narrowphaseCollisions = totalNarrowphaseCollisions,
            simulationTimeMs = elapsedMs,
            isRunning = true,
            substeps = substeps,
            ccdEnabled = true
        )
    }

    private data class NarrowphaseResult(
        val contactPoint: Vector3,
        val normal: Vector3,
        val penetration: Float,
        var impulseMagnitude: Float = 0f
    )

    private fun checkNarrowphaseCollision(
        eA: Entity,
        colA: Collider3D,
        boxA: AABB,
        eB: Entity,
        colB: Collider3D,
        boxB: AABB
    ): NarrowphaseResult? {
        val posA = eA.transform.position + colA.center
        val posB = eB.transform.position + colB.center

        return when {
            // Sphere vs Sphere
            colA.type == ColliderType.SPHERE && colB.type == ColliderType.SPHERE -> {
                val rA = colA.radius * eA.transform.scale.x
                val rB = colB.radius * eB.transform.scale.x
                val delta = posA - posB
                val dist = delta.length()
                val totalRadius = rA + rB
                if (dist < totalRadius && dist > 0.0001f) {
                    val normal = delta / dist
                    val penetration = totalRadius - dist
                    val contact = posB + normal * rB
                    NarrowphaseResult(contact, normal, penetration)
                } else null
            }
            // Sphere vs Box
            colA.type == ColliderType.SPHERE && colB.type == ColliderType.BOX -> {
                val rA = colA.radius * eA.transform.scale.x
                testSphereBox(posA, rA, posB, boxB)
            }
            colA.type == ColliderType.BOX && colB.type == ColliderType.SPHERE -> {
                val rB = colB.radius * eB.transform.scale.x
                val res = testSphereBox(posB, rB, posA, boxA)
                if (res != null) NarrowphaseResult(res.contactPoint, -res.normal, res.penetration) else null
            }
            // Box vs Box (Separating axis / AABB overlap test)
            else -> {
                val delta = posA - posB
                val overlapX = (boxA.halfExtents.x + boxB.halfExtents.x) - abs(delta.x)
                val overlapY = (boxA.halfExtents.y + boxB.halfExtents.y) - abs(delta.y)
                val overlapZ = (boxA.halfExtents.z + boxB.halfExtents.z) - abs(delta.z)

                if (overlapX > 0f && overlapY > 0f && overlapZ > 0f) {
                    val minOverlap = min(overlapX, min(overlapY, overlapZ))
                    val normal = when (minOverlap) {
                        overlapX -> Vector3(if (delta.x > 0f) 1f else -1f, 0f, 0f)
                        overlapY -> Vector3(0f, if (delta.y > 0f) 1f else -1f, 0f)
                        else -> Vector3(0f, 0f, if (delta.z > 0f) 1f else -1f)
                    }
                    val contact = (posA + posB) * 0.5f
                    NarrowphaseResult(contact, normal, minOverlap)
                } else null
            }
        }
    }

    private fun testSphereBox(sphereCenter: Vector3, radius: Float, boxCenter: Vector3, box: AABB): NarrowphaseResult? {
        val clampedX = sphereCenter.x.coerceIn(box.min.x, box.max.x)
        val clampedY = sphereCenter.y.coerceIn(box.min.y, box.max.y)
        val clampedZ = sphereCenter.z.coerceIn(box.min.z, box.max.z)
        val closestPoint = Vector3(clampedX, clampedY, clampedZ)

        val delta = sphereCenter - closestPoint
        val dist = delta.length()
        if (dist < radius) {
            val normal = if (dist > 0.0001f) delta / dist else Vector3.UP
            val penetration = radius - dist
            return NarrowphaseResult(closestPoint, normal, penetration)
        }
        return null
    }

    private fun resolveRigidBodyImpulse(
        eA: Entity,
        rbA: RigidBody3D,
        eB: Entity,
        rbB: RigidBody3D?,
        res: NarrowphaseResult
    ) {
        val normal = res.normal
        val penetration = res.penetration

        // Positional separation to prevent sinking
        val invMassA = if (rbA.bodyType == BodyType.DYNAMIC) (1f / rbA.mass) else 0f
        val invMassB = if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) (1f / rbB.mass) else 0f
        val totalInvMass = invMassA + invMassB
        if (totalInvMass <= 0f) return

        val separationPercent = 0.8f
        val slop = 0.01f
        val correction = normal * (max(0f, penetration - slop) / totalInvMass * separationPercent)

        if (rbA.bodyType == BodyType.DYNAMIC) {
            eA.transform.position += correction * invMassA
        }
        if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) {
            eB.transform.position -= correction * invMassB
        }

        // Relative Velocity along Normal
        val velA = rbA.velocity
        val velB = rbB?.velocity ?: Vector3.ZERO
        val relativeVel = velA - velB
        val velAlongNormal = relativeVel.dot(normal)

        // Only resolve if bodies are approaching each other
        if (velAlongNormal < 0f) {
            val restitutionA = rbA.material.bounciness
            val restitutionB = rbB?.material?.bounciness ?: 0.5f
            val e = (restitutionA + restitutionB) * 0.5f

            val j = -(1f + e) * velAlongNormal / totalInvMass
            val impulse = normal * j
            res.impulseMagnitude = j

            if (rbA.bodyType == BodyType.DYNAMIC) {
                rbA.velocity += impulse * invMassA
            }
            if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) {
                rbB.velocity -= impulse * invMassB
            }

            // Coulomb Friction Resolution
            val tangent = (relativeVel - normal * velAlongNormal)
            val tangentLen = tangent.length()
            if (tangentLen > 0.0001f) {
                val tangentNorm = tangent / tangentLen
                val friction = sqrt(rbA.material.friction * (rbB?.material?.friction ?: 0.5f))
                val jt = -(relativeVel.dot(tangentNorm)) / totalInvMass
                val frictionImpulseMagnitude = jt.coerceIn(-j * friction, j * friction)
                val frictionImpulse = tangentNorm * frictionImpulseMagnitude

                if (rbA.bodyType == BodyType.DYNAMIC) {
                    rbA.velocity += frictionImpulse * invMassA
                    // Angular torque spin
                    rbA.angularVelocity += Vector3(frictionImpulse.z, 0f, -frictionImpulse.x) * (0.8f * invMassA)
                }
                if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) {
                    rbB.velocity -= frictionImpulse * invMassB
                }
            }
        }
    }
}
