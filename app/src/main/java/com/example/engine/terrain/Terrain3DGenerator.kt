package com.example.engine.terrain

import com.example.engine.core.*
import kotlin.math.*
import kotlin.random.Random

enum class TerrainBiome(
    val displayName: String,
    val flatColor: Color4,
    val slopeColor: Color4,
    val peakColor: Color4,
    val foliageType: String
) {
    VERDANT_FOREST(
        displayName = "Verdant Forest",
        flatColor = Color4(0.2f, 0.6f, 0.25f, 1f),
        slopeColor = Color4(0.45f, 0.4f, 0.35f, 1f),
        peakColor = Color4(0.9f, 0.95f, 1.0f, 1f),
        foliageType = "Pine Trees & Grass"
    ),
    CYBER_GRID_PLAINS(
        displayName = "Cyber Neon Grid",
        flatColor = Color4(0.04f, 0.08f, 0.16f, 1f),
        slopeColor = Color4(0.08f, 0.18f, 0.35f, 1f),
        peakColor = Color4(0f, 0.95f, 1f, 1f),
        foliageType = "Neon Data Beacons"
    ),
    VOLCANIC_CALDERA(
        displayName = "Volcanic Caldera",
        flatColor = Color4(0.2f, 0.08f, 0.05f, 1f),
        slopeColor = Color4(0.35f, 0.15f, 0.08f, 1f),
        peakColor = Color4(1.0f, 0.35f, 0.05f, 1f),
        foliageType = "Magma Crystals"
    ),
    DESERT_DUNES(
        displayName = "Golden Desert Dunes",
        flatColor = Color4(0.85f, 0.7f, 0.4f, 1f),
        slopeColor = Color4(0.7f, 0.55f, 0.3f, 1f),
        peakColor = Color4(0.95f, 0.85f, 0.55f, 1f),
        foliageType = "Desert Palms & Rocks"
    ),
    ALIEN_BIOLUMINESCENT(
        displayName = "Alien Bioluminescent",
        flatColor = Color4(0.12f, 0.05f, 0.25f, 1f),
        slopeColor = Color4(0.35f, 0.08f, 0.45f, 1f),
        peakColor = Color4(0.9f, 0.2f, 1.0f, 1f),
        foliageType = "Alien Spire Spores"
    ),
    ALPINE_GLACIER(
        displayName = "Alpine Glacier",
        flatColor = Color4(0.6f, 0.75f, 0.85f, 1f),
        slopeColor = Color4(0.35f, 0.45f, 0.55f, 1f),
        peakColor = Color4(0.98f, 0.99f, 1.0f, 1f),
        foliageType = "Ice Shards & Pines"
    )
}

data class TerrainNoiseSettings(
    var seed: Int = 1337,
    var scale: Float = 0.12f,
    var octaves: Int = 4,
    var persistence: Float = 0.5f,
    var lacunarity: Float = 2.0f,
    var heightMultiplier: Float = 3.5f,
    var ridgedMountains: Boolean = false,
    var terraceSteps: Int = 0
)

data class TerrainScatterItem(
    val name: String,
    val position: Vector3,
    val scale: Vector3,
    val rotationY: Float,
    val meshGenerator: () -> Mesh
)

class Terrain3DGenerator {

    companion object {
        // Fast 2D Perlin-style gradient noise implementation
        private fun hash2D(x: Int, y: Int, seed: Int): Float {
            var n = x * 374761393 + y * 668265263 + seed * 9624587
            n = (n xor (n shr 13)) * 1274126177
            return ((n xor (n shr 16)) and 0x7FFFFFFF) / 2147483647.0f
        }

        private fun smoothNoise2D(x: Float, y: Float, seed: Int): Float {
            val iX = floor(x).toInt()
            val iY = floor(y).toInt()
            val fX = x - iX
            val fY = y - iY

            val u = fX * fX * (3.0f - 2.0f * fX)
            val v = fY * fY * (3.0f - 2.0f * fY)

            val n00 = hash2D(iX, iY, seed)
            val n10 = hash2D(iX + 1, iY, seed)
            val n01 = hash2D(iX, iY + 1, seed)
            val n11 = hash2D(iX + 1, iY + 1, seed)

            val nx0 = n00 * (1f - u) + n10 * u
            val nx1 = n01 * (1f - u) + n11 * u

            return nx0 * (1f - v) + nx1 * v
        }

        fun evaluateFBM(x: Float, y: Float, settings: TerrainNoiseSettings): Float {
            var total = 0f
            var frequency = settings.scale
            var amplitude = 1f
            var maxValue = 0f

            for (o in 0 until settings.octaves) {
                var sample = smoothNoise2D(x * frequency, y * frequency, settings.seed + o * 101)
                if (settings.ridgedMountains) {
                    sample = 1.0f - abs(sample * 2.0f - 1.0f)
                    sample = sample * sample
                }
                total += sample * amplitude
                maxValue += amplitude
                amplitude *= settings.persistence
                frequency *= settings.lacunarity
            }

            var normalized = total / maxValue
            if (settings.terraceSteps > 1) {
                normalized = round(normalized * settings.terraceSteps) / settings.terraceSteps
            }
            return normalized * settings.heightMultiplier
        }

        /**
         * Simulates hydraulic erosion droplets carving channels into the heightmap
         */
        fun applyHydraulicErosion(
            heightmap: Array<FloatArray>,
            gridWidth: Int,
            gridDepth: Int,
            dropletsCount: Int = 120,
            inertia: Float = 0.05f,
            sedimentCapacityFactor: Float = 4.0f,
            evaporationRate: Float = 0.05f,
            erosionRate: Float = 0.3f,
            depositionRate: Float = 0.3f
        ) {
            val random = Random(42)
            for (d in 0 until dropletsCount) {
                var posX = random.nextFloat() * (gridWidth - 2) + 1
                var posZ = random.nextFloat() * (gridDepth - 2) + 1
                var dirX = 0f
                var dirZ = 0f
                var speed = 1.0f
                var water = 1.0f
                var sediment = 0.0f

                for (step in 0 until 24) {
                    val nodeX = posX.toInt().coerceIn(0, gridWidth - 2)
                    val nodeZ = posZ.toInt().coerceIn(0, gridDepth - 2)
                    val u = posX - nodeX
                    val v = posZ - nodeZ

                    // Calculate surface normal gradient
                    val h00 = heightmap[nodeZ][nodeX]
                    val h10 = heightmap[nodeZ][nodeX + 1]
                    val h01 = heightmap[nodeZ + 1][nodeX]
                    val h11 = heightmap[nodeZ + 1][nodeX + 1]

                    val gradX = (h10 - h00) * (1f - v) + (h11 - h01) * v
                    val gradZ = (h01 - h00) * (1f - u) + (h11 - h10) * u

                    // Update direction with inertia
                    dirX = dirX * inertia - gradX * (1f - inertia)
                    dirZ = dirZ * inertia - gradZ * (1f - inertia)

                    val len = sqrt(dirX * dirX + dirZ * dirZ)
                    if (len == 0f) break
                    dirX /= len
                    dirZ /= len

                    val nextPosX = posX + dirX
                    val nextPosZ = posZ + dirZ

                    if (nextPosX < 0 || nextPosX >= gridWidth - 1 || nextPosZ < 0 || nextPosZ >= gridDepth - 1) break

                    val nextNodeX = nextPosX.toInt().coerceIn(0, gridWidth - 2)
                    val nextNodeZ = nextPosZ.toInt().coerceIn(0, gridDepth - 2)
                    val nextHeight = heightmap[nextNodeZ][nextNodeX]
                    val curHeight = heightmap[nodeZ][nodeX]
                    val deltaH = nextHeight - curHeight

                    val capacity = max(-deltaH * speed * water * sedimentCapacityFactor, 0.01f)

                    if (sediment > capacity || deltaH > 0) {
                        // Deposit sediment
                        val depositAmount = if (deltaH > 0) min(deltaH, sediment) else (sediment - capacity) * depositionRate
                        sediment -= depositAmount
                        heightmap[nodeZ][nodeX] += depositAmount
                    } else {
                        // Erode terrain
                        val erodeAmount = min((capacity - sediment) * erosionRate, -deltaH)
                        sediment += erodeAmount
                        heightmap[nodeZ][nodeX] -= erodeAmount
                    }

                    speed = sqrt(max(0f, speed * speed + deltaH * 9.8f))
                    water *= (1f - evaporationRate)
                    posX = nextPosX
                    posZ = nextPosZ
                }
            }
        }

        /**
         * Generates a 3D terrain mesh with height-based and slope-based biome vertex shading
         */
        fun generateTerrainMesh(
            width: Float = 24.0f,
            depth: Float = 24.0f,
            gridX: Int = 20,
            gridZ: Int = 20,
            biome: TerrainBiome = TerrainBiome.VERDANT_FOREST,
            noiseSettings: TerrainNoiseSettings = TerrainNoiseSettings(),
            enableErosion: Boolean = true,
            erosionDroplets: Int = 80
        ): Pair<Mesh, List<TerrainScatterItem>> {
            val mesh = Mesh("Procedural_${biome.name}")
            val heightmap = Array(gridZ + 1) { FloatArray(gridX + 1) }

            val stepX = width / gridX
            val stepZ = depth / gridZ
            val halfW = width * 0.5f
            val halfD = depth * 0.5f

            // 1. Generate Heightmap using Multi-Octave FBM
            for (z in 0..gridZ) {
                for (x in 0..gridX) {
                    val worldX = -halfW + x * stepX
                    val worldZ = -halfD + z * stepZ
                    heightmap[z][x] = evaluateFBM(worldX, worldZ, noiseSettings)
                }
            }

            // 2. Apply Hydraulic Erosion pass if enabled
            if (enableErosion) {
                applyHydraulicErosion(heightmap, gridX + 1, gridZ + 1, dropletsCount = erosionDroplets)
            }

            // 3. Create Vertices & Compute slope/altitude-based biome colors
            for (z in 0..gridZ) {
                for (x in 0..gridX) {
                    val px = -halfW + x * stepX
                    val pz = -halfD + z * stepZ
                    val py = heightmap[z][x]

                    val u = x.toFloat() / gridX
                    val v = z.toFloat() / gridZ

                    mesh.vertices.add(Vertex(
                        position = Vector3(px, py, pz),
                        normal = Vector3.UP,
                        uv = Vector2(u, v),
                        color = biome.flatColor
                    ))
                }
            }

            // 4. Construct Triangles
            for (z in 0 until gridZ) {
                for (x in 0 until gridX) {
                    val row1 = z * (gridX + 1)
                    val row2 = (z + 1) * (gridX + 1)
                    val idx0 = row1 + x
                    val idx1 = row2 + x
                    val idx2 = row1 + x + 1
                    val idx3 = row2 + x + 1

                    mesh.triangles.add(Triangle(idx0, idx1, idx2))
                    mesh.triangles.add(Triangle(idx2, idx1, idx3))
                }
            }

            mesh.recalculateNormals()

            // 5. Apply Biome Color Gradients based on Normal Slope (steep vs flat) & Altitude (peaks vs valleys)
            val maxH = noiseSettings.heightMultiplier
            for (i in mesh.vertices.indices) {
                val v = mesh.vertices[i]
                val slope = 1.0f - v.normal.y.coerceIn(0f, 1f) // 0 = flat, 1 = vertical cliff
                val altitudeNorm = (v.position.y / max(0.1f, maxH)).coerceIn(0f, 1f)

                // Blend Flat -> Slope -> Peak
                val baseColor = if (slope > 0.45f) {
                    val t = ((slope - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    biome.flatColor.lerp(biome.slopeColor, t)
                } else {
                    biome.flatColor
                }

                val finalColor = if (altitudeNorm > 0.7f) {
                    val t = ((altitudeNorm - 0.7f) / 0.3f).coerceIn(0f, 1f)
                    baseColor.lerp(biome.peakColor, t)
                } else {
                    baseColor
                }

                v.color = finalColor
            }

            mesh.recalculateAABB()

            // 6. Generate Procedural Scatter Props (Trees, Rocks, Crystals, Beacons)
            val scatterList = mutableListOf<TerrainScatterItem>()
            val random = Random(noiseSettings.seed + 777)
            val scatterCount = when (biome) {
                TerrainBiome.VERDANT_FOREST -> 16
                TerrainBiome.CYBER_GRID_PLAINS -> 10
                TerrainBiome.VOLCANIC_CALDERA -> 12
                TerrainBiome.DESERT_DUNES -> 8
                TerrainBiome.ALIEN_BIOLUMINESCENT -> 14
                TerrainBiome.ALPINE_GLACIER -> 10
            }

            for (i in 0 until scatterCount) {
                val sx = random.nextInt(1, gridX - 1)
                val sz = random.nextInt(1, gridZ - 1)
                val vIdx = sz * (gridX + 1) + sx
                val vert = mesh.vertices[vIdx]

                // Only scatter on relatively flat surfaces (< 35 deg slope)
                if (vert.normal.y > 0.75f) {
                    val itemPos = vert.position.copy()
                    val rotY = random.nextFloat() * 360f
                    val uniformScale = 0.6f + random.nextFloat() * 0.8f

                    val scatter = when (biome) {
                        TerrainBiome.VERDANT_FOREST -> TerrainScatterItem(
                            name = "Pine Tree #${i + 1}",
                            position = itemPos,
                            scale = Vector3(uniformScale, uniformScale, uniformScale),
                            rotationY = rotY,
                            meshGenerator = { createLowPolyPineTree() }
                        )
                        TerrainBiome.CYBER_GRID_PLAINS -> TerrainScatterItem(
                            name = "Neon Beacon #${i + 1}",
                            position = itemPos,
                            scale = Vector3(uniformScale * 0.7f, uniformScale * 1.4f, uniformScale * 0.7f),
                            rotationY = rotY,
                            meshGenerator = { Mesh.createCylinder(radius = 0.3f, height = 2.0f, color = Color4.CYAN) }
                        )
                        TerrainBiome.VOLCANIC_CALDERA -> TerrainScatterItem(
                            name = "Magma Crystal #${i + 1}",
                            position = itemPos,
                            scale = Vector3(uniformScale * 0.8f, uniformScale, uniformScale * 0.8f),
                            rotationY = rotY,
                            meshGenerator = { Mesh.createCrystalCluster() }
                        )
                        TerrainBiome.DESERT_DUNES -> TerrainScatterItem(
                            name = "Desert Monolith #${i + 1}",
                            position = itemPos,
                            scale = Vector3(uniformScale * 0.6f, uniformScale * 1.5f, uniformScale * 0.6f),
                            rotationY = rotY,
                            meshGenerator = { Mesh.createCube(size = 1.0f, color = Color4(0.7f, 0.5f, 0.3f, 1f)) }
                        )
                        TerrainBiome.ALIEN_BIOLUMINESCENT -> TerrainScatterItem(
                            name = "Alien Spire #${i + 1}",
                            position = itemPos,
                            scale = Vector3(uniformScale, uniformScale * 1.2f, uniformScale),
                            rotationY = rotY,
                            meshGenerator = { Mesh.createCrystalCluster() }
                        )
                        TerrainBiome.ALPINE_GLACIER -> TerrainScatterItem(
                            name = "Glacier Monolith #${i + 1}",
                            position = itemPos,
                            scale = Vector3(uniformScale, uniformScale * 1.3f, uniformScale),
                            rotationY = rotY,
                            meshGenerator = { createLowPolyPineTree() }
                        )
                    }
                    scatterList.add(scatter)
                }
            }

            return Pair(mesh, scatterList)
        }

        private fun createLowPolyPineTree(): Mesh {
            val mesh = Mesh("PineTree")
            val trunkColor = Color4(0.35f, 0.2f, 0.1f, 1f)
            val leavesColor = Color4(0.12f, 0.45f, 0.2f, 1f)

            // Trunk (Cylinder)
            val trunkHeight = 1.2f
            val trunkRadius = 0.2f
            val trunkBase = mesh.vertices.size

            for (i in 0 until 5) {
                val theta = (i * 2.0 * PI / 5).toFloat()
                val x = cos(theta) * trunkRadius
                val z = sin(theta) * trunkRadius
                mesh.vertices.add(Vertex(Vector3(x, 0f, z), color = trunkColor))
                mesh.vertices.add(Vertex(Vector3(x, trunkHeight, z), color = trunkColor))
            }

            for (i in 0 until 5) {
                val b0 = trunkBase + i * 2
                val t0 = b0 + 1
                val b1 = trunkBase + ((i + 1) % 5) * 2
                val t1 = b1 + 1
                mesh.triangles.add(Triangle(b0, t0, b1))
                mesh.triangles.add(Triangle(b1, t0, t1))
            }

            // Canopy Cones (3 layers)
            val layers = listOf(
                Triple(0.8f, 2.0f, 1.2f),
                Triple(1.6f, 2.6f, 0.9f),
                Triple(2.3f, 3.2f, 0.6f)
            )

            for ((yBottom, yTop, radius) in layers) {
                val baseIdx = mesh.vertices.size
                mesh.vertices.add(Vertex(Vector3(0f, yTop, 0f), color = leavesColor)) // Apex

                for (i in 0 until 6) {
                    val theta = (i * 2.0 * PI / 6).toFloat()
                    val x = cos(theta) * radius
                    val z = sin(theta) * radius
                    mesh.vertices.add(Vertex(Vector3(x, yBottom, z), color = leavesColor))
                }

                for (i in 0 until 6) {
                    val v1 = baseIdx + 1 + i
                    val v2 = baseIdx + 1 + ((i + 1) % 6)
                    mesh.triangles.add(Triangle(baseIdx, v1, v2))
                }
            }

            mesh.recalculateNormals()
            mesh.recalculateAABB()
            return mesh
        }
    }
}
