package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.core.*
import com.example.engine.ecs.*
import com.example.engine.export.ExportTarget
import com.example.engine.export.ProjectExporter
import com.example.engine.profiler.DeviceProfiler
import com.example.engine.profiler.DeviceTier
import com.example.engine.profiler.QualityPreset
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("T3D Engine", appName)
    }

    @Test
    fun `physics world steps and resolves collisions`() {
        val physicsWorld = PhysicsWorld3D(gravity = Vector3(0f, -9.81f, 0f), substeps = 4)
        val entities = mutableListOf<Entity>()

        val ground = Entity(name = "Ground")
        ground.transform.position = Vector3(0f, 0f, 0f)
        ground.collider = Collider3D(ColliderType.BOX, size = Vector3(10f, 0.2f, 10f))
        ground.rigidBody = RigidBody3D(bodyType = BodyType.STATIC, layer = PhysicsLayer.TERRAIN)
        entities.add(ground)

        val dynamicBox = Entity(name = "FallingBox")
        dynamicBox.transform.position = Vector3(0f, 2.0f, 0f)
        dynamicBox.collider = Collider3D(ColliderType.BOX, size = Vector3(1f, 1f, 1f))
        dynamicBox.rigidBody = RigidBody3D(mass = 1.0f, bodyType = BodyType.DYNAMIC, layer = PhysicsLayer.DEFAULT)
        entities.add(dynamicBox)

        for (i in 0 until 60) {
            physicsWorld.step(entities, 1f / 60f)
        }

        assertTrue("Dynamic box should have fallen towards ground", dynamicBox.transform.position.y < 2.0f)
        assertTrue("Dynamic box should rest on or above ground level", dynamicBox.transform.position.y >= 0.3f)
    }

    @Test
    fun `mesh procedural generators create valid geometry`() {
        val vehicle = Mesh.createHoverVehicle()
        assertTrue("Vehicle mesh has vertices", vehicle.vertices.isNotEmpty())
        assertTrue("Vehicle mesh has triangles", vehicle.triangles.isNotEmpty())
        assertTrue("Vehicle mesh has normals", vehicle.hasNormals)

        val tower = Mesh.createSciFiTower()
        assertTrue("Tower mesh has vertices", tower.vertices.isNotEmpty())
        assertTrue("Tower mesh has triangles", tower.triangles.isNotEmpty())

        val crystals = Mesh.createCrystalCluster()
        assertTrue("Crystal cluster has vertices", crystals.vertices.isNotEmpty())
    }

    @Test
    fun `procedural textures are generated with valid properties`() {
        val presets = ProceduralTexture.presets()
        assertTrue("Presets include multiple procedural textures", presets.size >= 5)

        val lava = presets.first { it.type == ProceduralTextureType.LAVA_GLOW }
        assertEquals("Magma Core Lava", lava.name)
        assertTrue("Lava texture is emissive", lava.isEmissive)
    }

    @Test
    fun `device profiler correctly tracks memory and budget for 1GB RAM Itel A70`() {
        val profiler = DeviceProfiler()
        profiler.applyPreset(QualityPreset.LOW_ITEL_A70_60FPS)

        assertEquals("Itel A70 60FPS Preset is active", QualityPreset.LOW_ITEL_A70_60FPS.name, profiler.activePreset.name)
        assertTrue("Target FPS is 60", profiler.activePreset.targetFps == 60)
        assertTrue("Total RAM is recognized", profiler.totalRamGB.contains("GB"))

        val ramTest = profiler.runRamPressureBenchmark()
        assertTrue("RAM benchmark returns result", ramTest.primaryMetric.isNotEmpty())

        val loadTest = profiler.runAssetLoadBenchmark()
        assertTrue("Load benchmark returns throughput metric", loadTest.primaryMetric.contains("MB/s"))
    }

    @Test
    fun `project exporter packages for Android and Unity targets`() {
        val scene = Scene3D("TestProject")
        val cube = Entity(name = "HeroCube")
        cube.mesh = Mesh.createCube(1f)
        scene.addEntity(cube)

        val swpPkg = ProjectExporter.generateExportPackage(scene, ExportTarget.SKETCHWARE_PRO_V7)
        assertTrue("Sketchware Pro export contains config", swpPkg.files.any { it.relativePath.contains("project_config.json") })

        val unityPkg = ProjectExporter.generateExportPackage(scene, ExportTarget.UNITY_3D)
        assertTrue("Unity export contains package manifest", unityPkg.files.any { it.relativePath.contains("package.json") })
    }
}

