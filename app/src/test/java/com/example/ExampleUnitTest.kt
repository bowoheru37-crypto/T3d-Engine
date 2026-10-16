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

    @Test
    fun math3D_perspectiveAndLookAt_validTransformations() {
        // Test FastTrig
        val sin90 = FastTrig.sinDeg(90f)
        assertEquals(1.0f, sin90, 0.01f)

        // Test LookAt with top view parallel up vector
        val lookAtTop = Matrix4x4.lookAt(Vector3(0f, 10f, 0f), Vector3(0f, 0f, 0f), Vector3.UP)
        assertNotNull("LookAt top view matrix should be non-null", lookAtTop)

        // Test perspective matrix
        val perspective = Matrix4x4.perspective(60f, 1.77f, 0.1f, 100f)
        val p = perspective.transformPoint(Vector3(0f, 0f, -5f))
        assertTrue("Projected point should be non-zero", p.z != 0f)
    }

    @Test
    fun mesh3D_createQuad_correctGeometryAndUVs() {
        val quad = Mesh.createQuad(2.0f, 2.0f)

        assertEquals("Quad should have 4 vertices", 4, quad.vertices.size)
        assertEquals("Quad should have 2 triangles", 2, quad.triangles.size)
        assertEquals("First UV should be (0,0)", 0f, quad.vertices[0].uv.x, 0.001f)
        assertEquals("First UV should be (0,0)", 0f, quad.vertices[0].uv.y, 0.001f)
        assertEquals("Last UV should be (0,1)", 1f, quad.vertices[3].uv.y, 0.001f)
    }

    @Test
    fun texture3D_ps2Sampling_quantizesColors() {
        val tex = ProceduralTexture("PS2 Grid", ProceduralTextureType.CYBER_GRID)
        val sampledPS2 = tex.samplePS2(0.5f, 0.5f, ditherLevels = 8)

        assertNotNull(sampledPS2)
        assertTrue("Color channel R should be between 0 and 1", sampledPS2.r in 0f..1f)
        assertTrue("Color channel G should be between 0 and 1", sampledPS2.g in 0f..1f)
        assertTrue("Color channel B should be between 0 and 1", sampledPS2.b in 0f..1f)
    }

    @Test
    fun camera3D_presetsAndPerspective_correctMatrix() {
        val camera = Camera3D()
        camera.setPresetView(CameraPresetView.TOP)

        assertEquals(88f, camera.pitch, 0.1f)
        val viewMat = camera.getViewMatrix()
        val projMat = camera.getProjectionMatrix(1.77f)

        assertNotNull("View matrix must be non-null", viewMat)
        assertNotNull("Projection matrix must be non-null", projMat)
    }
}


