package com.example.engine.generators

import com.example.engine.core.*
import kotlin.math.*
import kotlin.random.Random

enum class ProceduralAssetCategory(val displayName: String) {
    L_SYSTEM_FLORA("L-System Flora"),
    SHAPE_GRAMMAR_BUILDING("Grammar Tower"),
    PROCEDURAL_WEAPON("Sci-Fi Weapon"),
    DUNGEON_MAZE("BSP Dungeon"),
    SPLINE_EXTRUSION("Spline Extrusion")
}

data class LSystemTreeConfig(
    val axiom: String = "F",
    val iterations: Int = 3,
    val branchAngleDeg: Float = 28f,
    val branchLength: Float = 1.0f,
    val lengthReduction: Float = 0.72f,
    val trunkRadius: Float = 0.16f,
    val woodColor: Color4 = Color4(0.35f, 0.22f, 0.12f, 1f),
    val leafColor: Color4 = Color4(0.15f, 0.65f, 0.25f, 1f),
    val treeStyle: String = "Oak / Deciduous"
)

data class BuildingGrammarConfig(
    val floors: Int = 5,
    val baseWidth: Float = 3.0f,
    val baseDepth: Float = 3.0f,
    val floorHeight: Float = 0.8f,
    val setbackRate: Float = 0.85f,
    val hasAntenna: Boolean = true,
    val hasHelipad: Boolean = false,
    val wallColor: Color4 = Color4(0.12f, 0.16f, 0.24f, 1f),
    val windowGlowColor: Color4 = Color4(0f, 0.9f, 1f, 1f),
    val buildingStyle: String = "Cyberpunk Skyscraper"
)

data class ProceduralWeaponConfig(
    val weaponType: String = "Sci-Fi Plasma Rifle",
    val barrelLength: Float = 1.4f,
    val bodyScale: Float = 1.0f,
    val primaryColor: Color4 = Color4(0.1f, 0.12f, 0.16f, 1f),
    val accentColor: Color4 = Color4(0f, 0.95f, 1f, 1f),
    val emissiveCore: Boolean = true
)

data class DungeonConfig(
    val roomGridSize: Int = 4,
    val roomMinSize: Float = 2.0f,
    val roomMaxSize: Float = 4.0f,
    val wallHeight: Float = 2.0f,
    val wallColor: Color4 = Color4(0.35f, 0.32f, 0.3f, 1f),
    val floorColor: Color4 = Color4(0.2f, 0.18f, 0.16f, 1f)
)

data class SplineExtrusionConfig(
    val shapeProfile: String = "Hexagon Pipe", // Circle, Hexagon, Star, Square
    val radius: Float = 0.3f,
    val pointsCount: Int = 8,
    val spiralCoils: Float = 2.0f,
    val height: Float = 4.0f,
    val color: Color4 = Color4(0.85f, 0.3f, 0.95f, 1f)
)

object ProceduralAssetGenerator {

    /**
     * 1. L-System 3D Flora Generator (Lindenmayer Fractal Branching)
     */
    fun generateLSystemTree(config: LSystemTreeConfig = LSystemTreeConfig(), seed: Int = 42): Mesh {
        val mesh = Mesh("LSystem_${config.treeStyle.replace(" ", "_")}")
        val random = Random(seed)

        // Generate L-System string: F -> F[+F][-F][^F][&F]
        var currentString = config.axiom
        val rules = mapOf('F' to "F[+F][-F][*F][&F]")
        for (i in 0 until config.iterations) {
            val sb = StringBuilder()
            for (ch in currentString) {
                sb.append(rules[ch] ?: ch.toString())
            }
            currentString = sb.toString()
        }

        // Turtle Graphics 3D Stack
        data class TurtleState(
            var position: Vector3,
            var forward: Vector3,
            var up: Vector3,
            var right: Vector3,
            var length: Float,
            var radius: Float,
            var depth: Int
        )

        val stateStack = mutableListOf<TurtleState>()
        var state = TurtleState(
            position = Vector3(0f, 0f, 0f),
            forward = Vector3(0f, 1f, 0f),
            up = Vector3(0f, 0f, 1f),
            right = Vector3(1f, 0f, 0f),
            length = config.branchLength,
            radius = config.trunkRadius,
            depth = 0
        )

        val angleRad = Math.toRadians(config.branchAngleDeg.toDouble()).toFloat()

        fun rotateVector(vec: Vector3, axis: Vector3, rad: Float): Vector3 {
            val cosTheta = cos(rad)
            val sinTheta = sin(rad)
            return vec * cosTheta + axis.cross(vec) * sinTheta + axis * (axis.dot(vec) * (1f - cosTheta))
        }

        for (ch in currentString) {
            when (ch) {
                'F' -> {
                    // Draw branch segment
                    val nextPos = state.position + state.forward * state.length
                    addBranchCylinder(mesh, state.position, nextPos, state.radius, state.radius * config.lengthReduction, config.woodColor)

                    // Add leaf clusters at terminal twigs
                    if (state.depth >= config.iterations - 1 || state.length < 0.35f) {
                        addLeafCluster(mesh, nextPos, state.radius * 2.8f, config.leafColor, random)
                    }

                    state.position = nextPos
                    state.length *= config.lengthReduction
                    state.radius *= 0.75f
                }
                '[' -> {
                    // Push state
                    stateStack.add(TurtleState(
                        state.position.copy(),
                        state.forward.copy(),
                        state.up.copy(),
                        state.right.copy(),
                        state.length,
                        state.radius,
                        state.depth + 1
                    ))
                }
                ']' -> {
                    // Pop state
                    if (stateStack.isNotEmpty()) {
                        state = stateStack.removeAt(stateStack.size - 1)
                    }
                }
                '+' -> {
                    // Turn left around right axis
                    val jitter = (random.nextFloat() - 0.5f) * 0.15f
                    state.forward = rotateVector(state.forward, state.right, angleRad + jitter).normalized()
                    state.up = state.right.cross(state.forward).normalized()
                }
                '-' -> {
                    // Turn right around right axis
                    val jitter = (random.nextFloat() - 0.5f) * 0.15f
                    state.forward = rotateVector(state.forward, state.right, -angleRad + jitter).normalized()
                    state.up = state.right.cross(state.forward).normalized()
                }
                '*' -> {
                    // Pitch up around up axis
                    val jitter = (random.nextFloat() - 0.5f) * 0.15f
                    state.forward = rotateVector(state.forward, state.up, angleRad + jitter).normalized()
                    state.right = state.forward.cross(state.up).normalized()
                }
                '&' -> {
                    // Pitch down around up axis
                    val jitter = (random.nextFloat() - 0.5f) * 0.15f
                    state.forward = rotateVector(state.forward, state.up, -angleRad + jitter).normalized()
                    state.right = state.forward.cross(state.up).normalized()
                }
            }
        }

        mesh.recalculateNormals()
        mesh.recalculateAABB()
        return mesh
    }

    private fun addBranchCylinder(mesh: Mesh, start: Vector3, end: Vector3, rStart: Float, rEnd: Float, color: Color4) {
        val dir = (end - start).normalized()
        var arbitrary = if (abs(dir.y) < 0.9f) Vector3.UP else Vector3.FORWARD
        val right = dir.cross(arbitrary).normalized()
        val up = right.cross(dir).normalized()

        val baseIdx = mesh.vertices.size
        val sides = 5

        for (i in 0 until sides) {
            val theta = (i * 2.0 * PI / sides).toFloat()
            val cosT = cos(theta)
            val sinT = sin(theta)

            val p0 = start + (right * cosT + up * sinT) * rStart
            val p1 = end + (right * cosT + up * sinT) * rEnd

            mesh.vertices.add(Vertex(p0, color = color))
            mesh.vertices.add(Vertex(p1, color = color))
        }

        for (i in 0 until sides) {
            val i0 = baseIdx + i * 2
            val i1 = baseIdx + i * 2 + 1
            val i2 = baseIdx + ((i + 1) % sides) * 2
            val i3 = baseIdx + ((i + 1) % sides) * 2 + 1

            mesh.triangles.add(Triangle(i0, i1, i2))
            mesh.triangles.add(Triangle(i2, i1, i3))
        }
    }

    private fun addLeafCluster(mesh: Mesh, center: Vector3, radius: Float, color: Color4, random: Random) {
        val baseIdx = mesh.vertices.size
        val leafPositions = arrayOf(
            center + Vector3(0f, radius * 0.8f, 0f),
            center + Vector3(-radius, 0f, 0f),
            center + Vector3(radius, 0f, 0f),
            center + Vector3(0f, 0f, -radius),
            center + Vector3(0f, 0f, radius),
            center + Vector3(0f, -radius * 0.5f, 0f)
        )

        for (p in leafPositions) {
            mesh.vertices.add(Vertex(p, color = color))
        }

        mesh.triangles.add(Triangle(baseIdx + 0, baseIdx + 1, baseIdx + 3))
        mesh.triangles.add(Triangle(baseIdx + 0, baseIdx + 3, baseIdx + 2))
        mesh.triangles.add(Triangle(baseIdx + 0, baseIdx + 2, baseIdx + 4))
        mesh.triangles.add(Triangle(baseIdx + 0, baseIdx + 4, baseIdx + 1))
        mesh.triangles.add(Triangle(baseIdx + 5, baseIdx + 3, baseIdx + 1))
        mesh.triangles.add(Triangle(baseIdx + 5, baseIdx + 2, baseIdx + 3))
        mesh.triangles.add(Triangle(baseIdx + 5, baseIdx + 4, baseIdx + 2))
        mesh.triangles.add(Triangle(baseIdx + 5, baseIdx + 1, baseIdx + 4))
    }

    /**
     * 2. Shape Grammar Procedural Building & Skyscraper Generator
     */
    fun generateProceduralBuilding(config: BuildingGrammarConfig = BuildingGrammarConfig()): Mesh {
        val mesh = Mesh("Grammar_${config.buildingStyle.replace(" ", "_")}")
        var currentW = config.baseWidth
        var currentD = config.baseDepth
        var currentY = 0f

        for (floor in 0 until config.floors) {
            val floorH = config.floorHeight
            val hw = currentW * 0.5f
            val hd = currentD * 0.5f

            // Add floor box
            val baseIdx = mesh.vertices.size
            val positions = arrayOf(
                Vector3(-hw, currentY, hd), Vector3(hw, currentY, hd), Vector3(hw, currentY + floorH, hd), Vector3(-hw, currentY + floorH, hd), // Front
                Vector3(hw, currentY, -hd), Vector3(-hw, currentY, -hd), Vector3(-hw, currentY + floorH, -hd), Vector3(hw, currentY + floorH, -hd) // Back
            )

            val isEven = floor % 2 == 0
            val wallCol = if (isEven) config.wallColor else config.wallColor.lerp(Color4(0.05f, 0.05f, 0.08f, 1f), 0.3f)
            val glowCol = config.windowGlowColor

            for (p in positions) {
                mesh.vertices.add(Vertex(p, color = wallCol))
            }

            // Box faces
            addBoxIndices(mesh, baseIdx)

            // Add Window bands / Neon trims
            addWindowBands(mesh, currentY + floorH * 0.3f, floorH * 0.4f, hw * 1.02f, hd * 1.02f, glowCol)

            currentY += floorH
            currentW *= config.setbackRate
            currentD *= config.setbackRate
        }

        // Helipad or Antenna
        if (config.hasAntenna) {
            val spireH = 1.8f
            val spireBase = mesh.vertices.size
            mesh.vertices.add(Vertex(Vector3(0f, currentY + spireH, 0f), color = config.windowGlowColor)) // Top
            mesh.vertices.add(Vertex(Vector3(-0.1f, currentY, -0.1f), color = config.wallColor))
            mesh.vertices.add(Vertex(Vector3(0.1f, currentY, -0.1f), color = config.wallColor))
            mesh.vertices.add(Vertex(Vector3(0.1f, currentY, 0.1f), color = config.wallColor))
            mesh.vertices.add(Vertex(Vector3(-0.1f, currentY, 0.1f), color = config.wallColor))

            mesh.triangles.add(Triangle(spireBase, spireBase + 1, spireBase + 2))
            mesh.triangles.add(Triangle(spireBase, spireBase + 2, spireBase + 3))
            mesh.triangles.add(Triangle(spireBase, spireBase + 3, spireBase + 4))
            mesh.triangles.add(Triangle(spireBase, spireBase + 4, spireBase + 1))
        }

        mesh.recalculateNormals()
        mesh.recalculateAABB()
        return mesh
    }

    private fun addWindowBands(mesh: Mesh, y: Float, h: Float, hw: Float, hd: Float, color: Color4) {
        val base = mesh.vertices.size
        val corners = arrayOf(
            Vector3(-hw, y, hd), Vector3(hw, y, hd), Vector3(hw, y + h, hd), Vector3(-hw, y + h, hd),
            Vector3(hw, y, -hd), Vector3(-hw, y, -hd), Vector3(-hw, y + h, -hd), Vector3(hw, y + h, -hd)
        )
        for (c in corners) mesh.vertices.add(Vertex(c, color = color))
        addBoxIndices(mesh, base)
    }

    private fun addBoxIndices(mesh: Mesh, base: Int) {
        // Front
        mesh.triangles.add(Triangle(base + 0, base + 1, base + 2))
        mesh.triangles.add(Triangle(base + 0, base + 2, base + 3))
        // Right
        mesh.triangles.add(Triangle(base + 1, base + 4, base + 7))
        mesh.triangles.add(Triangle(base + 1, base + 7, base + 2))
        // Back
        mesh.triangles.add(Triangle(base + 4, base + 5, base + 6))
        mesh.triangles.add(Triangle(base + 4, base + 6, base + 7))
        // Left
        mesh.triangles.add(Triangle(base + 5, base + 0, base + 3))
        mesh.triangles.add(Triangle(base + 5, base + 3, base + 6))
        // Top
        mesh.triangles.add(Triangle(base + 3, base + 2, base + 7))
        mesh.triangles.add(Triangle(base + 3, base + 7, base + 6))
        // Bottom
        mesh.triangles.add(Triangle(base + 5, base + 4, base + 1))
        mesh.triangles.add(Triangle(base + 5, base + 1, base + 0))
    }

    /**
     * 3. Procedural Weapon & Prop Generator (Blaster, Energy Blade, Cyber Drone)
     */
    fun generateProceduralWeapon(config: ProceduralWeaponConfig = ProceduralWeaponConfig()): Mesh {
        val mesh = Mesh(config.weaponType.replace(" ", "_"))
        val pCol = config.primaryColor
        val aCol = config.accentColor

        when {
            config.weaponType.contains("Rifle", ignoreCase = true) || config.weaponType.contains("Blaster", ignoreCase = true) -> {
                // Stock & Body
                addBoxCustom(mesh, Vector3(-0.1f, -0.15f, -0.6f), Vector3(0.1f, 0.2f, 0.4f), pCol)
                // Barrel (Long cylinder along +Z)
                addBoxCustom(mesh, Vector3(-0.06f, 0.05f, 0.4f), Vector3(0.06f, 0.16f, 0.4f + config.barrelLength), aCol)
                // Grip
                addBoxCustom(mesh, Vector3(-0.08f, -0.55f, -0.15f), Vector3(0.08f, -0.15f, 0.0f), pCol)
                // Scope / Optic
                addBoxCustom(mesh, Vector3(-0.05f, 0.22f, -0.2f), Vector3(0.05f, 0.32f, 0.2f), aCol)
                // Power Core
                if (config.emissiveCore) {
                    addBoxCustom(mesh, Vector3(-0.08f, 0.0f, -0.1f), Vector3(0.08f, 0.14f, 0.15f), Color4.CYAN)
                }
            }
            config.weaponType.contains("Sword", ignoreCase = true) || config.weaponType.contains("Blade", ignoreCase = true) -> {
                // Hilt
                addBoxCustom(mesh, Vector3(-0.06f, -0.4f, -0.06f), Vector3(0.06f, 0.0f, 0.06f), Color4(0.2f, 0.2f, 0.2f, 1f))
                // Guard
                addBoxCustom(mesh, Vector3(-0.35f, 0.0f, -0.1f), Vector3(0.35f, 0.08f, 0.1f), pCol)
                // Plasma Blade
                addBoxCustom(mesh, Vector3(-0.1f, 0.08f, -0.02f), Vector3(0.1f, 1.8f, 0.02f), aCol)
                // Tip Apex
                val tipBase = mesh.vertices.size
                mesh.vertices.add(Vertex(Vector3(0f, 2.1f, 0f), color = aCol))
                mesh.vertices.add(Vertex(Vector3(-0.1f, 1.8f, 0f), color = aCol))
                mesh.vertices.add(Vertex(Vector3(0.1f, 1.8f, 0f), color = aCol))
                mesh.triangles.add(Triangle(tipBase, tipBase + 1, tipBase + 2))
            }
            else -> {
                // Cyber Drone Prop
                // Central Sphere Core
                addBoxCustom(mesh, Vector3(-0.3f, -0.2f, -0.3f), Vector3(0.3f, 0.2f, 0.3f), pCol)
                // 4 Rotor Arms
                addBoxCustom(mesh, Vector3(-0.8f, 0f, -0.8f), Vector3(-0.3f, 0.08f, -0.3f), aCol)
                addBoxCustom(mesh, Vector3(0.3f, 0f, -0.8f), Vector3(0.8f, 0.08f, -0.3f), aCol)
                addBoxCustom(mesh, Vector3(-0.8f, 0f, 0.3f), Vector3(-0.3f, 0.08f, 0.8f), aCol)
                addBoxCustom(mesh, Vector3(0.3f, 0f, 0.3f), Vector3(0.8f, 0.08f, 0.8f), aCol)
                // Neon Eye
                addBoxCustom(mesh, Vector3(-0.1f, -0.05f, 0.31f), Vector3(0.1f, 0.05f, 0.33f), Color4.RED)
            }
        }

        mesh.recalculateNormals()
        mesh.recalculateAABB()
        return mesh
    }

    private fun addBoxCustom(mesh: Mesh, min: Vector3, max: Vector3, color: Color4) {
        val base = mesh.vertices.size
        val positions = arrayOf(
            Vector3(min.x, min.y, max.z), Vector3(max.x, min.y, max.z), Vector3(max.x, max.y, max.z), Vector3(min.x, max.y, max.z), // Front
            Vector3(max.x, min.y, min.z), Vector3(min.x, min.y, min.z), Vector3(min.x, max.y, min.z), Vector3(max.x, max.y, min.z)  // Back
        )
        for (p in positions) mesh.vertices.add(Vertex(p, color = color))
        addBoxIndices(mesh, base)
    }

    /**
     * 4. Procedural Dungeon Room & Corridor Generator (BSP Maze)
     */
    fun generateProceduralDungeon(config: DungeonConfig = DungeonConfig(), seed: Int = 101): Mesh {
        val mesh = Mesh("Dungeon_BSP_Maze")
        val random = Random(seed)
        val rooms = mutableListOf<Pair<Vector3, Vector3>>() // min to max

        val step = 4.5f
        for (gx in 0 until config.roomGridSize) {
            for (gz in 0 until config.roomGridSize) {
                if (random.nextFloat() > 0.35f) { // 65% chance of room
                    val w = random.nextFloat() * (config.roomMaxSize - config.roomMinSize) + config.roomMinSize
                    val d = random.nextFloat() * (config.roomMaxSize - config.roomMinSize) + config.roomMinSize
                    val cx = (gx - config.roomGridSize / 2f) * step
                    val cz = (gz - config.roomGridSize / 2f) * step

                    val min = Vector3(cx - w * 0.5f, 0f, cz - d * 0.5f)
                    val max = Vector3(cx + w * 0.5f, config.wallHeight, cz + d * 0.5f)
                    rooms.add(Pair(min, max))

                    // Floor slab
                    addBoxCustom(mesh, Vector3(min.x, -0.1f, min.z), Vector3(max.x, 0.0f, max.z), config.floorColor)
                    // 4 Walls around room
                    val th = 0.2f
                    addBoxCustom(mesh, Vector3(min.x, 0f, max.z - th), Vector3(max.x, config.wallHeight, max.z), config.wallColor) // North
                    addBoxCustom(mesh, Vector3(min.x, 0f, min.z), Vector3(max.x, config.wallHeight, min.z + th), config.wallColor) // South
                    addBoxCustom(mesh, Vector3(min.x, 0f, min.z), Vector3(min.x + th, config.wallHeight, max.z), config.wallColor) // West
                    addBoxCustom(mesh, Vector3(max.x - th, 0f, min.z), Vector3(max.x, config.wallHeight, max.z), config.wallColor) // East
                }
            }
        }

        // Add connecting corridors between adjacent rooms
        for (i in 0 until rooms.size - 1) {
            val r1 = rooms[i]
            val r2 = rooms[i + 1]
            val c1 = (r1.first + r1.second) * 0.5f
            val c2 = (r2.first + r2.second) * 0.5f

            // Corridor floor
            val corW = 0.8f
            val minX = min(c1.x, c2.x) - corW * 0.5f
            val maxX = max(c1.x, c2.x) + corW * 0.5f
            val minZ = min(c1.z, c2.z) - corW * 0.5f
            val maxZ = max(c1.z, c2.z) + corW * 0.5f

            addBoxCustom(mesh, Vector3(minX, -0.08f, minZ), Vector3(maxX, 0.0f, maxZ), config.floorColor)
        }

        mesh.recalculateNormals()
        mesh.recalculateAABB()
        return mesh
    }

    /**
     * 5. Spline-based 3D Extrusion Generator (Pipes, Tunnels, Coils, Rollercoasters)
     */
    fun generateSplineExtrusion(config: SplineExtrusionConfig = SplineExtrusionConfig()): Mesh {
        val mesh = Mesh("Spline_${config.shapeProfile.replace(" ", "_")}")
        val points = mutableListOf<Vector3>()

        // Generate 3D parametric helical curve
        for (i in 0..config.pointsCount * 4) {
            val t = i.toFloat() / (config.pointsCount * 4)
            val angle = t * config.spiralCoils * 2.0 * PI
            val radius = 1.8f * (1.0f - t * 0.3f)
            val x = (cos(angle) * radius).toFloat()
            val y = t * config.height
            val z = (sin(angle) * radius).toFloat()
            points.add(Vector3(x, y, z))
        }

        val sides = when (config.shapeProfile) {
            "Square Pipe" -> 4
            "Hexagon Pipe" -> 6
            "Star Tube" -> 8
            else -> 10 // Smooth Circle
        }

        // Ring vertices along the spline path
        for (pIdx in points.indices) {
            val p = points[pIdx]
            val forward = if (pIdx < points.size - 1) (points[pIdx + 1] - p).normalized() else (p - points[pIdx - 1]).normalized()
            var arbitrary = if (abs(forward.y) < 0.9f) Vector3.UP else Vector3.FORWARD
            val right = forward.cross(arbitrary).normalized()
            val up = right.cross(forward).normalized()

            for (s in 0 until sides) {
                val theta = (s * 2.0 * PI / sides).toFloat()
                var r = config.radius
                if (config.shapeProfile == "Star Tube" && s % 2 == 1) {
                    r *= 0.5f
                }
                val offset = (right * cos(theta) + up * sin(theta)) * r
                mesh.vertices.add(Vertex(p + offset, color = config.color))
            }
        }

        // Bridge rings into quads/triangles
        for (pIdx in 0 until points.size - 1) {
            val ring0 = pIdx * sides
            val ring1 = (pIdx + 1) * sides
            for (s in 0 until sides) {
                val sNext = (s + 1) % sides
                val a = ring0 + s
                val b = ring1 + s
                val c = ring1 + sNext
                val d = ring0 + sNext

                mesh.triangles.add(Triangle(a, b, c))
                mesh.triangles.add(Triangle(a, c, d))
            }
        }

        mesh.recalculateNormals()
        mesh.recalculateAABB()
        return mesh
    }
}
