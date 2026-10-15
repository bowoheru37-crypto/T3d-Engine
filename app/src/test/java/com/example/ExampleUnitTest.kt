package com.example

import com.example.engine.core.*
import com.example.engine.ecs.*
import com.example.engine.physics.*
import com.example.engine.character.*
import com.example.engine.cgi.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun coroutinePhysics_gravityAndCollision_detected() = runBlocking {
        val physicsEngine = CoroutinePhysicsEngine().apply {
            gravity = Vector3(0f, -9.8f, 0f)
            substeps = 2
        }

        // Create two overlapping dynamic sphere bodies
        val entity1 = Entity("BallA").apply {
            transform.position = Vector3(0f, 2f, 0f)
            collider = Collider3D(type = ColliderType.SPHERE, radius = 1.0f)
            rigidBody = RigidBody3D(mass = 1.0f, bodyType = BodyType.DYNAMIC)
        }
        val entity2 = Entity("BallB").apply {
            transform.position = Vector3(0f, 2.5f, 0f)
            collider = Collider3D(type = ColliderType.SPHERE, radius = 1.0f)
            rigidBody = RigidBody3D(mass = 1.0f, bodyType = BodyType.DYNAMIC)
        }

        val entities = mutableListOf(entity1, entity2)

        // Execute asynchronous step
        physicsEngine.stepAsync(entities, 0.016f)
        val stats = physicsEngine.simulationStats.value

        assertEquals(2, stats.activeRigidBodies)
        assertTrue("Colliders should be registered", stats.totalColliders >= 2)
    }

    @Test
    fun characterDesignStudio_generatesRiggedMesh_validVertices() {
        val studio = CharacterDesignRigStudio()
        studio.setArchetype(CharacterArchetype.CYBER_NINJA)
        val mesh = studio.generateCharacterMesh()

        assertTrue("Generated mesh should have vertices", mesh.vertices.isNotEmpty())
        assertTrue("Generated mesh should have triangles", mesh.triangles.isNotEmpty())
        assertNotNull("Generated character mesh should have skeleton", mesh.skeleton)
        assertEquals("Humanoid character rig should have 19 skeleton bones", 19, mesh.skeleton?.bones?.size)
        assertTrue("Root hips or pelvis bone should be present", mesh.skeleton?.bones?.any { it.name == "Pelvis" || it.name == "Root" } == true)
    }

    @Test
    fun cgiMovieDirector_presetsAndOptics_correctCalculations() {
        val director = CgiMovieDirector()
        director.activeAspect = AspectRatioPreset.CINEMASCOPE_21_9
        director.activeShot = CameraRigShot.ORBIT_CINEMATIC

        val camera = Camera3D()
        val target = Vector3(0f, 1f, 0f)

        director.update(0.1f, camera, target)

        assertEquals(2.39f, director.activeAspect.ratio, 0.01f)
        assertTrue("Camera position should have moved during orbit", camera.position.length() > 0.1f)
    }

    @Test
    fun worldRoomStageStudio_buildMmdStage_createsValidEntitiesAndColliders() {
        val studio = com.example.engine.world.WorldRoomStageStudio().apply {
            activePreset = com.example.engine.world.RoomPreset.MMD_DANCE_STAGE
            activeFloorStyle = com.example.engine.world.FloorStyle.MMD_CHECKERBOARD
            isLowEndOptimized = true
        }

        val entities = studio.buildRoomWorldEntities()

        assertTrue("Entities list should contain floor and stage components", entities.isNotEmpty())
        val floorEntity = entities.find { it.name.contains("Floor") }
        assertNotNull("Floor entity must be present", floorEntity)
        assertNotNull("Floor must have a static collider", floorEntity?.collider)
        assertEquals(BodyType.STATIC, floorEntity?.rigidBody?.bodyType)
        assertTrue("Backdrop or stage truss should be generated", entities.any { it.name.contains("Backdrop") || it.name.contains("Truss") })
    }

    @Test
    fun worldRoomStageStudio_floorMesh_itelA70BudgetOptimized() {
        val studio = com.example.engine.world.WorldRoomStageStudio()

        // Generate floor with itel A70 aggressive budget tiering
        val floorMesh = studio.generateFloorMesh(
            width = 12f,
            length = 12f,
            style = com.example.engine.world.FloorStyle.MMD_CHECKERBOARD,
            repeatX = 8,
            repeatZ = 8,
            lowEndOptimized = true
        )

        assertTrue("Floor mesh vertices should not be empty", floorMesh.vertices.isNotEmpty())
        assertTrue("Floor mesh triangles should not be empty", floorMesh.triangles.isNotEmpty())
        // Verify low-poly vertex budget for itel A70 (must stay under 600 vertices)
        assertTrue("Vertex count must stay lean for itel A70: ${floorMesh.vertices.size}", floorMesh.vertices.size < 600)
    }

    @Test
    fun worldRoomStageStudio_buildBedroom_hasPhysicsCollidersForProps() {
        val studio = com.example.engine.world.WorldRoomStageStudio().apply {
            activePreset = com.example.engine.world.RoomPreset.COZY_ANIME_BEDROOM
            activeFloorStyle = com.example.engine.world.FloorStyle.WOODEN_PARQUET
        }

        val entities = studio.buildRoomWorldEntities()

        val desk = entities.find { it.name.contains("Desk") }
        val bed = entities.find { it.name.contains("Bed") }

        assertNotNull("Study desk prop should be in bedroom scene", desk)
        assertNotNull("Bed prop should be in bedroom scene", bed)
        assertNotNull("Desk must have a collider for rigid body interaction", desk?.collider)
        assertNotNull("Bed must have a collider for rigid body interaction", bed?.collider)
    }
}


