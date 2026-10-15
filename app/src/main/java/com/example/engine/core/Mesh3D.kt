package com.example.engine.core

import kotlin.math.*

data class Vertex(
    var position: Vector3,
    var normal: Vector3 = Vector3(0f, 1f, 0f),
    var uv: Vector2 = Vector2(0f, 0f),
    var color: Color4 = Color4.WHITE,
    var boneWeights: FloatArray = floatArrayOf(1f, 0f, 0f, 0f),
    var boneIndices: IntArray = intArrayOf(0, 0, 0, 0)
)

data class Triangle(
    val v0: Int,
    val v1: Int,
    val v2: Int,
    var normal: Vector3 = Vector3(0f, 1f, 0f)
)

data class Bone(
    val id: Int,
    val name: String,
    var parentId: Int = -1,
    var localPosition: Vector3 = Vector3.ZERO,
    var localRotation: Vector3 = Vector3.ZERO, // Euler angles
    var length: Float = 1.0f
)

data class Skeleton(
    val bones: MutableList<Bone> = mutableListOf()
) {
    fun addBone(name: String, parentId: Int = -1, pos: Vector3 = Vector3.ZERO, length: Float = 1.0f): Bone {
        val bone = Bone(bones.size, name, parentId, pos, Vector3.ZERO, length)
        bones.add(bone)
        return bone
    }

    companion object {
        fun createHumanoid(): Skeleton {
            val skel = Skeleton()
            skel.addBone("Root", -1, Vector3(0f, 0f, 0f), 0.5f) // 0
            skel.addBone("Pelvis", 0, Vector3(0f, 0.8f, 0f), 0.3f) // 1
            skel.addBone("Spine", 1, Vector3(0f, 1.2f, 0f), 0.4f) // 2
            skel.addBone("Chest", 2, Vector3(0f, 1.6f, 0f), 0.3f) // 3
            skel.addBone("Neck", 3, Vector3(0f, 1.9f, 0f), 0.2f) // 4
            skel.addBone("Head", 4, Vector3(0f, 2.2f, 0f), 0.3f) // 5
            skel.addBone("LeftShoulder", 3, Vector3(-0.4f, 1.7f, 0f), 0.2f) // 6
            skel.addBone("LeftArm", 6, Vector3(-0.8f, 1.7f, 0f), 0.4f) // 7
            skel.addBone("LeftHand", 7, Vector3(-1.2f, 1.7f, 0f), 0.2f) // 8
            skel.addBone("RightShoulder", 3, Vector3(0.4f, 1.7f, 0f), 0.2f) // 9
            skel.addBone("RightArm", 9, Vector3(0.8f, 1.7f, 0f), 0.4f) // 10
            skel.addBone("RightHand", 10, Vector3(1.2f, 1.7f, 0f), 0.2f) // 11
            skel.addBone("LeftHip", 1, Vector3(-0.3f, 0.7f, 0f), 0.2f) // 12
            skel.addBone("LeftLeg", 12, Vector3(-0.3f, 0.2f, 0f), 0.5f) // 13
            skel.addBone("LeftFoot", 13, Vector3(-0.3f, 0.0f, 0.2f), 0.2f) // 14
            skel.addBone("RightHip", 1, Vector3(0.3f, 0.7f, 0f), 0.2f) // 15
            skel.addBone("RightLeg", 15, Vector3(0.3f, 0.2f, 0f), 0.5f) // 16
            skel.addBone("RightFoot", 16, Vector3(0.3f, 0.0f, 0.2f), 0.2f) // 17
            return skel
        }
    }
}

enum class ShadingMode {
    LIT_PHONG,
    LIT_PBR,
    UNLIT,
    WIREFRAME,
    NORMALS_VISUALIZER,
    DEPTH_MAP
}

data class Material(
    var name: String = "Default Material",
    var albedoColor: Color4 = Color4(0.85f, 0.85f, 0.9f, 1f),
    var metallic: Float = 0.2f,
    var roughness: Float = 0.5f,
    var emissionColor: Color4 = Color4.BLACK,
    var emissionIntensity: Float = 0f,
    var wireframe: Boolean = false,
    var wireframeColor: Color4 = Color4.CYAN,
    var shadingMode: ShadingMode = ShadingMode.LIT_PHONG,
    var doubleSided: Boolean = false,
    var castShadows: Boolean = true,
    var receiveShadows: Boolean = true
) {
    val isEmissive: Boolean get() = emissionIntensity > 0f
}

class Mesh(
    var name: String = "Mesh",
    val vertices: MutableList<Vertex> = mutableListOf(),
    val triangles: MutableList<Triangle> = mutableListOf()
) {
    var aabb: AABB = AABB()
    var skeleton: Skeleton? = null
    val hasNormals: Boolean get() = vertices.isNotEmpty() && vertices.any { it.normal.lengthSquared() > 0.1f }

    init {
        recalculateNormals()
        recalculateAABB()
    }

    fun recalculateAABB() {
        if (vertices.isEmpty()) {
            aabb = AABB(Vector3(-0.5f, -0.5f, -0.5f), Vector3(0.5f, 0.5f, 0.5f))
            return
        }
        var min = Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var max = Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (v in vertices) {
            min = Vector3(min(min.x, v.position.x), min(min.y, v.position.y), min(min.z, v.position.z))
            max = Vector3(max(max.x, v.position.x), max(max.y, v.position.y), max(max.z, v.position.z))
        }
        aabb = AABB(min, max)
    }

    fun recalculateNormals() {
        for (i in 0 until triangles.size) {
            val tri = triangles[i]
            if (tri.v0 < vertices.size && tri.v1 < vertices.size && tri.v2 < vertices.size) {
                val p0 = vertices[tri.v0].position
                val p1 = vertices[tri.v1].position
                val p2 = vertices[tri.v2].position
                val normal = (p1 - p0).cross(p2 - p0).normalized()
                tri.normal = normal
            }
        }
        // Vertex normals average
        val vertexNormals = Array(vertices.size) { Vector3.ZERO }
        for (tri in triangles) {
            if (tri.v0 < vertices.size) vertexNormals[tri.v0] += tri.normal
            if (tri.v1 < vertices.size) vertexNormals[tri.v1] += tri.normal
            if (tri.v2 < vertices.size) vertexNormals[tri.v2] += tri.normal
        }
        for (i in vertices.indices) {
            vertices[i].normal = vertexNormals[i].normalized()
        }
    }

    fun clone(): Mesh {
        val newMesh = Mesh(name)
        for (v in vertices) {
            newMesh.vertices.add(v.copy(position = v.position.copy(), normal = v.normal.copy(), uv = v.uv.copy(), color = v.color.copy()))
        }
        for (t in triangles) {
            newMesh.triangles.add(t.copy(normal = t.normal.copy()))
        }
        newMesh.recalculateAABB()
        return newMesh
    }

    companion object {
        fun createPlane(width: Float = 10.0f, depth: Float = 10.0f, color: Color4 = Color4(0.3f, 0.35f, 0.4f, 1f)): Mesh {
            val hw = width * 0.5f
            val hd = depth * 0.5f
            val mesh = Mesh("Plane")
            mesh.vertices.add(Vertex(position = Vector3(-hw, 0f,  hd), normal = Vector3.UP, uv = Vector2(0f, 1f), color = color))
            mesh.vertices.add(Vertex(position = Vector3( hw, 0f,  hd), normal = Vector3.UP, uv = Vector2(1f, 1f), color = color))
            mesh.vertices.add(Vertex(position = Vector3( hw, 0f, -hd), normal = Vector3.UP, uv = Vector2(1f, 0f), color = color))
            mesh.vertices.add(Vertex(position = Vector3(-hw, 0f, -hd), normal = Vector3.UP, uv = Vector2(0f, 0f), color = color))
            mesh.triangles.add(Triangle(0, 1, 2, Vector3.UP))
            mesh.triangles.add(Triangle(0, 2, 3, Vector3.UP))
            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createCube(size: Float = 1.0f, color: Color4 = Color4(0.2f, 0.7f, 0.9f, 1f)): Mesh {
            val h = size * 0.5f
            val mesh = Mesh("Cube")
            val positions = arrayOf(
                // Front
                Vector3(-h, -h,  h), Vector3( h, -h,  h), Vector3( h,  h,  h), Vector3(-h,  h,  h),
                // Back
                Vector3( h, -h, -h), Vector3(-h, -h, -h), Vector3(-h,  h, -h), Vector3( h,  h, -h),
                // Top
                Vector3(-h,  h,  h), Vector3( h,  h,  h), Vector3( h,  h, -h), Vector3(-h,  h, -h),
                // Bottom
                Vector3(-h, -h, -h), Vector3( h, -h, -h), Vector3( h, -h,  h), Vector3(-h, -h,  h),
                // Left
                Vector3(-h, -h, -h), Vector3(-h, -h,  h), Vector3(-h,  h,  h), Vector3(-h,  h, -h),
                // Right
                Vector3( h, -h,  h), Vector3( h, -h, -h), Vector3( h,  h, -h), Vector3( h,  h,  h)
            )

            for (p in positions) {
                mesh.vertices.add(Vertex(position = p, color = color))
            }

            for (f in 0 until 6) {
                val base = f * 4
                mesh.triangles.add(Triangle(base, base + 1, base + 2))
                mesh.triangles.add(Triangle(base, base + 2, base + 3))
            }
            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createSphere(radius: Float = 0.5f, rings: Int = 12, sectors: Int = 16, color: Color4 = Color4(0.9f, 0.4f, 0.3f, 1f)): Mesh {
            val mesh = Mesh("Sphere")
            val rInv = 1.0f / (rings - 1).toFloat()
            val sInv = 1.0f / (sectors - 1).toFloat()

            for (r in 0 until rings) {
                for (s in 0 until sectors) {
                    val y = sin(-Math.PI / 2 + Math.PI * r * rInv).toFloat()
                    val x = (cos(2 * Math.PI * s * sInv) * sin(Math.PI * r * rInv)).toFloat()
                    val z = (sin(2 * Math.PI * s * sInv) * sin(Math.PI * r * rInv)).toFloat()
                    val pos = Vector3(x * radius, y * radius, z * radius)
                    mesh.vertices.add(Vertex(position = pos, normal = pos.normalized(), uv = Vector2(s * sInv, r * rInv), color = color))
                }
            }

            for (r in 0 until rings - 1) {
                for (s in 0 until sectors - 1) {
                    val cur = r * sectors + s
                    val next = (r + 1) * sectors + s
                    mesh.triangles.add(Triangle(cur, next, cur + 1))
                    mesh.triangles.add(Triangle(cur + 1, next, next + 1))
                }
            }
            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createCylinder(radius: Float = 0.5f, height: Float = 1.0f, segments: Int = 16, color: Color4 = Color4(0.4f, 0.8f, 0.4f, 1f)): Mesh {
            val mesh = Mesh("Cylinder")
            val hh = height * 0.5f
            val topCenter = mesh.vertices.size
            mesh.vertices.add(Vertex(Vector3(0f, hh, 0f), Vector3.UP, color = color))
            val botCenter = mesh.vertices.size
            mesh.vertices.add(Vertex(Vector3(0f, -hh, 0f), Vector3.DOWN, color = color))

            for (i in 0..segments) {
                val theta = (2 * Math.PI * i / segments).toFloat()
                val x = cos(theta) * radius
                val z = sin(theta) * radius
                mesh.vertices.add(Vertex(Vector3(x, hh, z), Vector3(x, 0f, z).normalized(), color = color))
                mesh.vertices.add(Vertex(Vector3(x, -hh, z), Vector3(x, 0f, z).normalized(), color = color))
            }

            val base = 2
            for (i in 0 until segments) {
                val t0 = base + i * 2
                val b0 = base + i * 2 + 1
                val t1 = base + (i + 1) * 2
                val b1 = base + (i + 1) * 2 + 1

                mesh.triangles.add(Triangle(t0, t1, b0))
                mesh.triangles.add(Triangle(t1, b1, b0))
                mesh.triangles.add(Triangle(topCenter, t0, t1))
                mesh.triangles.add(Triangle(botCenter, b1, b0))
            }
            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createTorus(r1: Float = 0.6f, r2: Float = 0.2f, rings: Int = 16, sides: Int = 12, color: Color4 = Color4(0.95f, 0.7f, 0.1f, 1f)): Mesh {
            val mesh = Mesh("Torus")
            for (i in 0 until rings) {
                val u = i * 2.0f * Math.PI.toFloat() / rings
                for (j in 0 until sides) {
                    val v = j * 2.0f * Math.PI.toFloat() / sides
                    val x = (r1 + r2 * cos(v)) * cos(u)
                    val y = r2 * sin(v)
                    val z = (r1 + r2 * cos(v)) * sin(u)
                    mesh.vertices.add(Vertex(Vector3(x, y, z), color = color))
                }
            }
            for (i in 0 until rings) {
                val iNext = (i + 1) % rings
                for (j in 0 until sides) {
                    val jNext = (j + 1) % sides
                    val a = i * sides + j
                    val b = iNext * sides + j
                    val c = iNext * sides + jNext
                    val d = i * sides + jNext
                    mesh.triangles.add(Triangle(a, b, c))
                    mesh.triangles.add(Triangle(a, c, d))
                }
            }
            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createTerrain(width: Float = 10f, depth: Float = 10f, gridX: Int = 12, gridZ: Int = 12, color: Color4 = Color4(0.35f, 0.65f, 0.35f, 1f)): Mesh {
            val mesh = Mesh("Terrain")
            val hW = width * 0.5f
            val hD = depth * 0.5f
            val stepX = width / gridX
            val stepZ = depth / gridZ

            for (z in 0..gridZ) {
                for (x in 0..gridX) {
                    val px = -hW + x * stepX
                    val pz = -hD + z * stepZ
                    // Subtle procedural terrain height hills
                    val py = (sin(px * 0.8) * cos(pz * 0.8) * 0.5 + sin(px * 1.5 + pz * 1.2) * 0.2).toFloat()
                    mesh.vertices.add(Vertex(Vector3(px, py, pz), Vector3.UP, Vector2(x.toFloat() / gridX, z.toFloat() / gridZ), color))
                }
            }

            for (z in 0 until gridZ) {
                for (x in 0 until gridX) {
                    val row1 = z * (gridX + 1)
                    val row2 = (z + 1) * (gridX + 1)
                    mesh.triangles.add(Triangle(row1 + x, row2 + x, row1 + x + 1))
                    mesh.triangles.add(Triangle(row1 + x + 1, row2 + x, row2 + x + 1))
                }
            }
            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createCharacterModel(): Mesh {
            val mesh = Mesh("CyberDroid")
            // Create low-poly character model
            val bodyColor = Color4(0.15f, 0.55f, 0.9f, 1f)
            val jointColor = Color4(0.1f, 0.1f, 0.15f, 1f)
            val visorColor = Color4(0f, 1f, 0.9f, 1f)

            // Torso (0..7)
            val torso = arrayOf(
                Vector3(-0.35f, 0.7f,  0.2f), Vector3( 0.35f, 0.7f,  0.2f), Vector3( 0.3f, 1.4f,  0.2f), Vector3(-0.3f, 1.4f,  0.2f),
                Vector3( 0.35f, 0.7f, -0.2f), Vector3(-0.35f, 0.7f, -0.2f), Vector3(-0.3f, 1.4f, -0.2f), Vector3( 0.3f, 1.4f, -0.2f)
            )
            for (p in torso) mesh.vertices.add(Vertex(p, color = bodyColor))
            addBoxTriangles(mesh, 0)

            // Head (8..15)
            val headBase = mesh.vertices.size
            val head = arrayOf(
                Vector3(-0.22f, 1.5f,  0.2f), Vector3( 0.22f, 1.5f,  0.2f), Vector3( 0.22f, 1.9f,  0.2f), Vector3(-0.22f, 1.9f,  0.2f),
                Vector3( 0.22f, 1.5f, -0.2f), Vector3(-0.22f, 1.5f, -0.2f), Vector3(-0.22f, 1.9f, -0.2f), Vector3( 0.22f, 1.9f, -0.2f)
            )
            for (p in head) mesh.vertices.add(Vertex(p, color = visorColor))
            addBoxTriangles(mesh, headBase)

            // Left Arm
            val laBase = mesh.vertices.size
            for (p in arrayOf(
                Vector3(-0.55f, 0.6f, 0.1f), Vector3(-0.38f, 0.6f, 0.1f), Vector3(-0.38f, 1.35f, 0.1f), Vector3(-0.55f, 1.35f, 0.1f),
                Vector3(-0.38f, 0.6f, -0.1f), Vector3(-0.55f, 0.6f, -0.1f), Vector3(-0.55f, 1.35f, -0.1f), Vector3(-0.38f, 1.35f, -0.1f)
            )) mesh.vertices.add(Vertex(p, color = jointColor))
            addBoxTriangles(mesh, laBase)

            // Right Arm
            val raBase = mesh.vertices.size
            for (p in arrayOf(
                Vector3(0.38f, 0.6f, 0.1f), Vector3(0.55f, 0.6f, 0.1f), Vector3(0.55f, 1.35f, 0.1f), Vector3(0.38f, 1.35f, 0.1f),
                Vector3(0.55f, 0.6f, -0.1f), Vector3(0.38f, 0.6f, -0.1f), Vector3(0.38f, 1.35f, -0.1f), Vector3(0.55f, 1.35f, -0.1f)
            )) mesh.vertices.add(Vertex(p, color = jointColor))
            addBoxTriangles(mesh, raBase)

            // Left Leg
            val llBase = mesh.vertices.size
            for (p in arrayOf(
                Vector3(-0.3f, 0.0f, 0.15f), Vector3(-0.08f, 0.0f, 0.15f), Vector3(-0.08f, 0.7f, 0.15f), Vector3(-0.3f, 0.7f, 0.15f),
                Vector3(-0.08f, 0.0f, -0.15f), Vector3(-0.3f, 0.0f, -0.15f), Vector3(-0.3f, 0.7f, -0.15f), Vector3(-0.08f, 0.7f, -0.15f)
            )) mesh.vertices.add(Vertex(p, color = bodyColor))
            addBoxTriangles(mesh, llBase)

            // Right Leg
            val rlBase = mesh.vertices.size
            for (p in arrayOf(
                Vector3(0.08f, 0.0f, 0.15f), Vector3(0.3f, 0.0f, 0.15f), Vector3(0.3f, 0.7f, 0.15f), Vector3(0.08f, 0.7f, 0.15f),
                Vector3(0.3f, 0.0f, -0.15f), Vector3(0.08f, 0.0f, -0.15f), Vector3(0.08f, 0.7f, -0.15f), Vector3(0.3f, 0.7f, -0.15f)
            )) mesh.vertices.add(Vertex(p, color = bodyColor))
            addBoxTriangles(mesh, rlBase)

            mesh.skeleton = Skeleton.createHumanoid()
            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        private fun addBoxTriangles(mesh: Mesh, base: Int) {
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

        fun parseOBJ(objText: String, meshName: String = "ImportedOBJ"): Mesh {
            val mesh = Mesh(meshName)
            val positions = mutableListOf<Vector3>()
            val uvs = mutableListOf<Vector2>()
            val normals = mutableListOf<Vector3>()

            val lines = objText.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val parts = trimmed.split("\\s+".toRegex())
                when (parts[0]) {
                    "v" -> {
                        if (parts.size >= 4) {
                            positions.add(Vector3(parts[1].toFloatOrNull() ?: 0f, parts[2].toFloatOrNull() ?: 0f, parts[3].toFloatOrNull() ?: 0f))
                        }
                    }
                    "vt" -> {
                        if (parts.size >= 3) {
                            uvs.add(Vector2(parts[1].toFloatOrNull() ?: 0f, parts[2].toFloatOrNull() ?: 0f))
                        }
                    }
                    "vn" -> {
                        if (parts.size >= 4) {
                            normals.add(Vector3(parts[1].toFloatOrNull() ?: 0f, parts[2].toFloatOrNull() ?: 0f, parts[3].toFloatOrNull() ?: 0f))
                        }
                    }
                    "f" -> {
                        if (parts.size >= 4) {
                            val faceIndices = mutableListOf<Int>()
                            for (p in 1 until parts.size) {
                                val vertexData = parts[p].split("/")
                                val posIdx = (vertexData[0].toIntOrNull() ?: 1) - 1
                                if (posIdx in positions.indices) {
                                    faceIndices.add(posIdx)
                                }
                            }
                            // Triangulate polygon fan
                            for (i in 1 until faceIndices.size - 1) {
                                val v0 = faceIndices[0]
                                val v1 = faceIndices[i]
                                val v2 = faceIndices[i + 1]
                                mesh.triangles.add(Triangle(v0, v1, v2))
                            }
                        }
                    }
                }
            }

            for (p in positions) {
                mesh.vertices.add(Vertex(position = p, color = Color4(0.8f, 0.85f, 0.9f, 1f)))
            }

            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createHoverVehicle(): Mesh {
            val mesh = Mesh("HoverCar")
            val c = Color4(0f, 0.9f, 1f, 1f)
            // Body cockpit & hull
            mesh.vertices.add(Vertex(Vector3(0f, 0.4f, 1.2f), color = c)) // 0 Nose
            mesh.vertices.add(Vertex(Vector3(-0.8f, 0.3f, 0.0f), color = c)) // 1 Left wing
            mesh.vertices.add(Vertex(Vector3(0.8f, 0.3f, 0.0f), color = c)) // 2 Right wing
            mesh.vertices.add(Vertex(Vector3(-0.5f, 0.5f, -1.0f), color = c)) // 3 Left thruster
            mesh.vertices.add(Vertex(Vector3(0.5f, 0.5f, -1.0f), color = c)) // 4 Right thruster
            mesh.vertices.add(Vertex(Vector3(0f, 0.8f, -0.2f), color = Color4.WHITE)) // 5 Canopy top
            mesh.vertices.add(Vertex(Vector3(0f, 0.0f, 0.0f), color = Color4(0.1f, 0.2f, 0.3f, 1f))) // 6 Bottom keel

            // Top hood & wings
            mesh.triangles.add(Triangle(0, 1, 5))
            mesh.triangles.add(Triangle(0, 5, 2))
            mesh.triangles.add(Triangle(1, 3, 5))
            mesh.triangles.add(Triangle(2, 5, 4))
            mesh.triangles.add(Triangle(3, 4, 5))
            // Bottom hull
            mesh.triangles.add(Triangle(0, 6, 1))
            mesh.triangles.add(Triangle(0, 2, 6))
            mesh.triangles.add(Triangle(1, 6, 3))
            mesh.triangles.add(Triangle(2, 4, 6))
            mesh.triangles.add(Triangle(3, 6, 4))

            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createCrystalCluster(): Mesh {
            val mesh = Mesh("CrystalCluster")
            val c = Color4(0.85f, 0.25f, 0.95f, 1f)
            val angles = listOf(0.0, 60.0, 120.0, 180.0, 240.0, 300.0)

            mesh.vertices.add(Vertex(Vector3(0f, 2.0f, 0f), color = Color4.WHITE)) // 0 Apex
            mesh.vertices.add(Vertex(Vector3(0f, -0.2f, 0f), color = Color4(0.2f, 0.05f, 0.3f, 1f))) // 1 Base

            for (a in angles) {
                val rad = Math.toRadians(a).toFloat()
                val r = 0.5f
                mesh.vertices.add(Vertex(Vector3(cos(rad) * r, 0.8f, sin(rad) * r), color = c))
            }

            for (i in 0 until 6) {
                val v1 = 2 + i
                val v2 = 2 + ((i + 1) % 6)
                mesh.triangles.add(Triangle(0, v1, v2)) // Top cone
                mesh.triangles.add(Triangle(1, v2, v1)) // Bottom cone
            }

            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }

        fun createSciFiTower(): Mesh {
            val mesh = Mesh("SciFiTower")
            val c = Color4(0.15f, 0.8f, 0.9f, 1f)
            // Base base octagonal
            val baseR = 1.0f
            val topR = 0.4f
            val height = 4.0f
            val sides = 8

            mesh.vertices.add(Vertex(Vector3(0f, height + 0.8f, 0f), color = Color4.CYAN)) // 0 Spire tip

            for (i in 0 until sides) {
                val rad = (i * 2.0 * PI / sides).toFloat()
                // Bottom
                mesh.vertices.add(Vertex(Vector3(cos(rad) * baseR, 0f, sin(rad) * baseR), color = c))
                // Top
                mesh.vertices.add(Vertex(Vector3(cos(rad) * topR, height, sin(rad) * topR), color = Color4.WHITE))
            }

            for (i in 0 until sides) {
                val b1 = 1 + i * 2
                val t1 = b1 + 1
                val b2 = 1 + ((i + 1) % sides) * 2
                val t2 = b2 + 1

                mesh.triangles.add(Triangle(b1, t1, b2))
                mesh.triangles.add(Triangle(b2, t1, t2))
                mesh.triangles.add(Triangle(0, t1, t2)) // Spire cap
            }

            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }
    }
}
