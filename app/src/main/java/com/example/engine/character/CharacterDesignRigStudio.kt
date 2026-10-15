package com.example.engine.character

import com.example.engine.core.*
import kotlin.math.*

enum class CharacterArchetype(
    val displayName: String,
    val description: String,
    val defaultPrimaryColor: Color4,
    val defaultAccentColor: Color4,
    val defaultGlowColor: Color4
) {
    CYBER_NINJA(
        "Cyber Ninja X",
        "High-agility assassin with carbon-fiber armor and dual photon blades",
        Color4(0.1f, 0.12f, 0.16f, 1f),
        Color4(0f, 0.95f, 0.85f, 1f),
        Color4(0f, 1.0f, 0.8f, 1f)
    ),
    MECH_WARRIOR(
        "Titan Mech Heavy",
        "Armored exoskeleton with hydraulic shoulder plates and heavy blast plating",
        Color4(0.22f, 0.25f, 0.3f, 1f),
        Color4(1.0f, 0.45f, 0.05f, 1f),
        Color4(1.0f, 0.6f, 0.1f, 1f)
    ),
    SPACE_SCOUT(
        "Orbital Scout Explorer",
        "Aero-pressurized exploration suit with jet thrusters and panoramic gold visor",
        Color4(0.85f, 0.88f, 0.92f, 1f),
        Color4(0.15f, 0.5f, 0.95f, 1f),
        Color4(1.0f, 0.8f, 0.2f, 1f)
    ),
    CYBER_DROID(
        "Synthetica Droid 07",
        "Polished chrome chassis with glowing exposed energy core and articulated servos",
        Color4(0.7f, 0.72f, 0.75f, 1f),
        Color4(0.7f, 0.2f, 0.95f, 1f),
        Color4(0.85f, 0.3f, 1.0f, 1f)
    )
}

enum class ProceduralPose(val displayName: String, val speed: Float) {
    IDLE_BREATHING("Idle Ready Stance", 1.0f),
    WALK_PATROL("Tactical Walk Gait", 1.2f),
    SPRINT_RUN("High-Speed Sprint", 2.0f),
    HERO_LANDING("Epic 3-Point Landing", 0.8f),
    COMBAT_SLASH("Melee Katana Slash", 1.5f),
    VICTORY_SALUTE("Victory Salute", 1.0f)
}

data class CharacterRigProportions(
    var overallScale: Float = 1.0f,
    var shoulderWidth: Float = 1.0f,
    var torsoHeight: Float = 1.0f,
    var armLength: Float = 1.0f,
    var legLength: Float = 1.0f,
    var headScale: Float = 1.0f
)

/**
 * Character Design & Skeletal Rigging Studio Subsystem.
 * Generates custom 3D humanoid character meshes, attaches skeletal hierarchy,
 * and computes real-time procedural bone animations and poses.
 */
class CharacterDesignRigStudio {
    var activeArchetype: CharacterArchetype = CharacterArchetype.CYBER_NINJA
    var activePose: ProceduralPose = ProceduralPose.IDLE_BREATHING
    var proportions: CharacterRigProportions = CharacterRigProportions()

    var primaryColor: Color4 = activeArchetype.defaultPrimaryColor
    var accentColor: Color4 = activeArchetype.defaultAccentColor
    var glowColor: Color4 = activeArchetype.defaultGlowColor

    var animTime: Float = 0f
    var showBoneLabels: Boolean = true

    fun setArchetype(arch: CharacterArchetype) {
        activeArchetype = arch
        primaryColor = arch.defaultPrimaryColor
        accentColor = arch.defaultAccentColor
        glowColor = arch.defaultGlowColor
    }

    /**
     * Synthesizes a high-detail 3D humanoid character mesh with attached skeleton rig.
     */
    fun generateCharacterMesh(): Mesh {
        val mesh = Mesh("Hero_${activeArchetype.name}")
        val p = proportions

        val mainCol = primaryColor
        val accCol = accentColor
        val glowCol = glowColor
        val jointCol = Color4(0.18f, 0.2f, 0.24f, 1f)

        // 1. Head & Visor
        val headH = 0.28f * p.headScale
        val headW = 0.2f * p.headScale
        val headY = 1.75f * p.torsoHeight
        addSegmentBox(mesh, Vector3(0f, headY, 0f), Vector3(headW, headH, headW * 1.1f), mainCol, boneIndex = 4)
        // Neon Visor Strip
        addSegmentBox(mesh, Vector3(0f, headY + 0.04f, headW * 0.55f), Vector3(headW * 0.85f, headH * 0.3f, 0.04f), glowCol, boneIndex = 4)

        // 2. Torso & Chest Plate
        val chestH = 0.5f * p.torsoHeight
        val chestW = 0.38f * p.shoulderWidth
        val chestY = 1.35f * p.torsoHeight
        addSegmentBox(mesh, Vector3(0f, chestY, 0f), Vector3(chestW, chestH, 0.24f), mainCol, boneIndex = 2)
        // Chest Glowing Energy Core
        addSegmentBox(mesh, Vector3(0f, chestY + 0.08f, 0.13f), Vector3(0.12f, 0.12f, 0.02f), glowCol, boneIndex = 2)

        // 3. Pelvis / Hips
        val hipY = 0.95f * p.torsoHeight
        addSegmentBox(mesh, Vector3(0f, hipY, 0f), Vector3(chestW * 0.8f, 0.2f, 0.2f), accCol, boneIndex = 1)

        // 4. Shoulder Pauldrons
        val sOffset = chestW * 0.6f + 0.08f
        addSegmentBox(mesh, Vector3(-sOffset, chestY + 0.15f, 0f), Vector3(0.16f, 0.12f, 0.2f), accCol, boneIndex = 5)
        addSegmentBox(mesh, Vector3(sOffset, chestY + 0.15f, 0f), Vector3(0.16f, 0.12f, 0.2f), accCol, boneIndex = 8)

        // 5. Left Arm & Gauntlet
        val armLen = 0.38f * p.armLength
        addSegmentBox(mesh, Vector3(-sOffset, chestY - 0.12f, 0f), Vector3(0.12f, armLen, 0.12f), jointCol, boneIndex = 6)
        addSegmentBox(mesh, Vector3(-sOffset, chestY - 0.12f - armLen, 0f), Vector3(0.13f, armLen, 0.13f), accCol, boneIndex = 7)

        // 6. Right Arm & Gauntlet
        addSegmentBox(mesh, Vector3(sOffset, chestY - 0.12f, 0f), Vector3(0.12f, armLen, 0.12f), jointCol, boneIndex = 9)
        addSegmentBox(mesh, Vector3(sOffset, chestY - 0.12f - armLen, 0f), Vector3(0.13f, armLen, 0.13f), accCol, boneIndex = 10)

        // 7. Left Leg & Thruster Boot
        val legH = 0.45f * p.legLength
        val lOffset = 0.16f * p.shoulderWidth
        addSegmentBox(mesh, Vector3(-lOffset, hipY - 0.28f, 0f), Vector3(0.14f, legH, 0.15f), mainCol, boneIndex = 12)
        addSegmentBox(mesh, Vector3(-lOffset, hipY - 0.28f - legH, 0f), Vector3(0.15f, legH, 0.16f), accCol, boneIndex = 13)
        // Boot Foot
        addSegmentBox(mesh, Vector3(-lOffset, hipY - 0.28f - legH * 2f + 0.05f, 0.06f), Vector3(0.15f, 0.1f, 0.28f), jointCol, boneIndex = 14)

        // 8. Right Leg & Thruster Boot
        addSegmentBox(mesh, Vector3(lOffset, hipY - 0.28f, 0f), Vector3(0.14f, legH, 0.15f), mainCol, boneIndex = 15)
        addSegmentBox(mesh, Vector3(lOffset, hipY - 0.28f - legH, 0f), Vector3(0.15f, legH, 0.16f), accCol, boneIndex = 16)
        // Boot Foot
        addSegmentBox(mesh, Vector3(lOffset, hipY - 0.28f - legH * 2f + 0.05f, 0.06f), Vector3(0.15f, 0.1f, 0.28f), jointCol, boneIndex = 17)

        // 9. Back Jetpack / Energy Katana Sheath
        addSegmentBox(mesh, Vector3(0f, chestY + 0.05f, -0.18f), Vector3(0.24f, 0.32f, 0.1f), accCol, boneIndex = 2)
        // Jetpack thruster nozzles
        addSegmentBox(mesh, Vector3(-0.08f, chestY - 0.14f, -0.18f), Vector3(0.06f, 0.08f, 0.06f), glowCol, boneIndex = 2)
        addSegmentBox(mesh, Vector3(0.08f, chestY - 0.14f, -0.18f), Vector3(0.06f, 0.08f, 0.06f), glowCol, boneIndex = 2)

        // Build Skeleton Hierarchy
        mesh.skeleton = buildHumanoidSkeleton(p)
        mesh.recalculateNormals()
        mesh.recalculateAABB()
        return mesh
    }

    private fun addSegmentBox(mesh: Mesh, center: Vector3, size: Vector3, color: Color4, boneIndex: Int) {
        val hx = size.x * 0.5f
        val hy = size.y * 0.5f
        val hz = size.z * 0.5f
        val base = mesh.vertices.size

        val p = arrayOf(
            center + Vector3(-hx, -hy,  hz),
            center + Vector3( hx, -hy,  hz),
            center + Vector3( hx,  hy,  hz),
            center + Vector3(-hx,  hy,  hz),
            center + Vector3( hx, -hy, -hz),
            center + Vector3(-hx, -hy, -hz),
            center + Vector3(-hx,  hy, -hz),
            center + Vector3( hx,  hy, -hz)
        )

        for (pt in p) {
            mesh.vertices.add(
                Vertex(
                    position = pt,
                    color = color,
                    boneIndices = intArrayOf(boneIndex, 0, 0, 0),
                    boneWeights = floatArrayOf(1.0f, 0f, 0f, 0f)
                )
            )
        }

        // Triangles for 6 faces
        val indices = intArrayOf(
            0, 1, 2,  0, 2, 3, // Front
            1, 4, 7,  1, 7, 2, // Right
            4, 5, 6,  4, 6, 7, // Back
            5, 0, 3,  5, 3, 6, // Left
            3, 2, 7,  3, 7, 6, // Top
            5, 4, 1,  5, 1, 0  // Bottom
        )
        for (i in indices.indices step 3) {
            mesh.triangles.add(Triangle(base + indices[i], base + indices[i + 1], base + indices[i + 2]))
        }
    }

    private fun buildHumanoidSkeleton(p: CharacterRigProportions): Skeleton {
        val skel = Skeleton()
        skel.addBone("Root", -1, Vector3(0f, 0f, 0f), 0.5f) // 0
        skel.addBone("Pelvis", 0, Vector3(0f, 0.95f * p.torsoHeight, 0f), 0.25f) // 1
        skel.addBone("Spine", 1, Vector3(0f, 1.25f * p.torsoHeight, 0f), 0.3f) // 2
        skel.addBone("Chest", 2, Vector3(0f, 1.55f * p.torsoHeight, 0f), 0.3f) // 3
        skel.addBone("Head", 3, Vector3(0f, 1.75f * p.torsoHeight, 0f), 0.3f) // 4

        // Left Arm
        skel.addBone("LeftShoulder", 3, Vector3(-0.35f * p.shoulderWidth, 1.5f * p.torsoHeight, 0f), 0.2f) // 5
        skel.addBone("LeftArm", 5, Vector3(-0.55f * p.shoulderWidth, 1.2f * p.torsoHeight, 0f), 0.35f) // 6
        skel.addBone("LeftHand", 6, Vector3(-0.55f * p.shoulderWidth, 0.85f * p.torsoHeight, 0f), 0.2f) // 7

        // Right Arm
        skel.addBone("RightShoulder", 3, Vector3(0.35f * p.shoulderWidth, 1.5f * p.torsoHeight, 0f), 0.2f) // 8
        skel.addBone("RightArm", 8, Vector3(0.55f * p.shoulderWidth, 1.2f * p.torsoHeight, 0f), 0.35f) // 9
        skel.addBone("RightHand", 9, Vector3(0.55f * p.shoulderWidth, 0.85f * p.torsoHeight, 0f), 0.2f) // 10

        // Left Leg
        skel.addBone("LeftHip", 1, Vector3(-0.16f * p.shoulderWidth, 0.9f * p.torsoHeight, 0f), 0.2f) // 11
        skel.addBone("LeftThigh", 11, Vector3(-0.16f * p.shoulderWidth, 0.5f * p.legLength, 0f), 0.4f) // 12
        skel.addBone("LeftCalf", 12, Vector3(-0.16f * p.shoulderWidth, 0.1f * p.legLength, 0f), 0.4f) // 13
        skel.addBone("LeftFoot", 13, Vector3(-0.16f * p.shoulderWidth, 0.0f, 0.08f), 0.2f) // 14

        // Right Leg
        skel.addBone("RightHip", 1, Vector3(0.16f * p.shoulderWidth, 0.9f * p.torsoHeight, 0f), 0.2f) // 15
        skel.addBone("RightThigh", 15, Vector3(0.16f * p.shoulderWidth, 0.5f * p.legLength, 0f), 0.4f) // 16
        skel.addBone("RightCalf", 16, Vector3(0.16f * p.shoulderWidth, 0.1f * p.legLength, 0f), 0.4f) // 17
        skel.addBone("RightFoot", 17, Vector3(0.16f * p.shoulderWidth, 0.0f, 0.08f), 0.2f) // 18

        return skel
    }

    /**
     * Updates real-time procedural animations based on active pose.
     */
    fun updatePose(dt: Float, mesh: Mesh?) {
        val skeleton = mesh?.skeleton ?: return
        animTime += dt * activePose.speed

        val t = animTime

        when (activePose) {
            ProceduralPose.IDLE_BREATHING -> {
                // Subtle chest breathing & arm sway
                val breath = sin(t * 2.5f) * 0.03f
                findBone(skeleton, "Chest")?.localPosition?.y = (1.55f * proportions.torsoHeight) + breath
                findBone(skeleton, "Head")?.localRotation = Vector3(sin(t * 1.5f) * 3f, 0f, 0f)
                findBone(skeleton, "LeftArm")?.localRotation = Vector3(0f, 0f, 15f + sin(t * 2.5f) * 3f)
                findBone(skeleton, "RightArm")?.localRotation = Vector3(0f, 0f, -15f - sin(t * 2.5f) * 3f)
            }
            ProceduralPose.WALK_PATROL -> {
                // Alternating stride
                val legSwing = sin(t * 4.5f) * 22f
                val armSwing = -legSwing * 0.8f

                findBone(skeleton, "LeftThigh")?.localRotation = Vector3(legSwing, 0f, 0f)
                findBone(skeleton, "RightThigh")?.localRotation = Vector3(-legSwing, 0f, 0f)

                findBone(skeleton, "LeftArm")?.localRotation = Vector3(armSwing, 0f, 10f)
                findBone(skeleton, "RightArm")?.localRotation = Vector3(-armSwing, 0f, -10f)

                // Pelvis vertical bounce
                findBone(skeleton, "Pelvis")?.localPosition?.y = (0.95f * proportions.torsoHeight) + abs(sin(t * 4.5f)) * 0.06f
            }
            ProceduralPose.SPRINT_RUN -> {
                // High-speed run stride with forward lean
                val runSwing = sin(t * 8.0f) * 45f
                findBone(skeleton, "Spine")?.localRotation = Vector3(18f, 0f, 0f)
                findBone(skeleton, "LeftThigh")?.localRotation = Vector3(runSwing, 0f, 0f)
                findBone(skeleton, "RightThigh")?.localRotation = Vector3(-runSwing, 0f, 0f)

                findBone(skeleton, "LeftArm")?.localRotation = Vector3(-runSwing * 1.2f, 0f, 20f)
                findBone(skeleton, "RightArm")?.localRotation = Vector3(runSwing * 1.2f, 0f, -20f)
            }
            ProceduralPose.HERO_LANDING -> {
                // Dynamic 3-point crouch pose
                findBone(skeleton, "Pelvis")?.localPosition?.y = 0.45f
                findBone(skeleton, "LeftThigh")?.localRotation = Vector3(65f, -15f, 0f)
                findBone(skeleton, "RightThigh")?.localRotation = Vector3(-30f, 35f, 0f)
                findBone(skeleton, "Spine")?.localRotation = Vector3(45f, 0f, 0f)
                findBone(skeleton, "LeftHand")?.localRotation = Vector3(-70f, 0f, 0f)
                findBone(skeleton, "Head")?.localRotation = Vector3(-20f, 10f, 0f)
            }
            ProceduralPose.COMBAT_SLASH -> {
                val slashT = sin(t * 5.0f)
                findBone(skeleton, "RightArm")?.localRotation = Vector3(slashT * 70f, slashT * 50f, -30f)
                findBone(skeleton, "Spine")?.localRotation = Vector3(10f, -slashT * 25f, 0f)
            }
            ProceduralPose.VICTORY_SALUTE -> {
                findBone(skeleton, "RightArm")?.localRotation = Vector3(0f, 0f, -140f)
                findBone(skeleton, "Head")?.localRotation = Vector3(-15f, 0f, 0f)
            }
        }
    }

    private fun findBone(skeleton: Skeleton, name: String): Bone? {
        return skeleton.bones.find { it.name.equals(name, ignoreCase = true) }
    }
}
