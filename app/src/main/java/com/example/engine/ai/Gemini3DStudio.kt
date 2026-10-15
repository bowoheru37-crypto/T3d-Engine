package com.example.engine.ai

import com.example.BuildConfig
import com.example.engine.core.*
import com.example.engine.ecs.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

enum class AIAssetType {
    MODEL_3D,
    ENVIRONMENT_TERRAIN,
    TEXTURE_PROCEDURAL,
    GAMEPLAY_SCRIPT,
    SHADER_MATERIAL,
    NPC_DIALOGUE_QUEST
}

data class AIGeneratedAssetResult(
    val assetType: AIAssetType,
    val title: String,
    val description: String,
    val rawContent: String,
    val generatedMesh: Mesh? = null,
    val generatedScript: String? = null,
    val generatedTexture: ProceduralTexture? = null,
    val tags: List<String> = emptyList()
)

object Gemini3DStudio {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun generate3DAsset(
        prompt: String,
        assetType: AIAssetType,
        complexity: String = "Medium"
    ): Result<AIGeneratedAssetResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Return offline procedural generator
                return@withContext Result.success(generateOfflineFallbackAsset(prompt, assetType))
            }

            val systemInstruction = when (assetType) {
                AIAssetType.MODEL_3D -> """
                    You are a 3D Game Engine Geometry Generator.
                    Output a low-to-medium poly 3D object in Wavefront .OBJ format.
                    Include:
                    - 'v x y z' for vertex positions (bounded between -2.0 and 2.0).
                    - 'f v1 v2 v3' (triangles) or 'f v1 v2 v3 v4' (quads).
                    Provide clean watertight topology suitable for mobile games.
                    Also provide a short description line starting with '# Description: '.
                """.trimIndent()

                AIAssetType.ENVIRONMENT_TERRAIN -> """
                    You are a 3D World Building Generator.
                    Generate a procedural environment description with Wavefront .OBJ terrain/structure definitions.
                    Generate coordinates centered around origin.
                """.trimIndent()

                AIAssetType.TEXTURE_PROCEDURAL -> """
                    You are a Procedural Texture and PBR Map Designer.
                    Describe texture parameters including pattern (Grid, Noise, Voronoi, Lava, Brick), primary/secondary colors, metallic, roughness, and emission.
                """.trimIndent()

                AIAssetType.GAMEPLAY_SCRIPT -> """
                    You are a Game Engine Gameplay Script Programmer for T3D engine.
                    Output a clean script with on_start, on_update, on_collision, on_touch blocks.
                    Supported functions: rotate_x, rotate_y, rotate_z, move, apply_impulse, set_color, spawn, play_sound.
                """.trimIndent()

                AIAssetType.SHADER_MATERIAL -> """
                    You are a 3D Shader and PBR Material Designer.
                    Output a PBR material profile with albedo, metallic, roughness, emission, wireframe mode, and GLSL shading snippet.
                """.trimIndent()

                AIAssetType.NPC_DIALOGUE_QUEST -> """
                    You are an RPG Game Dialogue and Quest Designer.
                    Generate an interactive branching dialogue tree and quest objective for mobile RPGs.
                """.trimIndent()
            }

            val promptWithDetails = "$prompt (Target: Mobile 3D Game Engine, Complexity: $complexity)"

            val requestJson = """
                {
                    "contents": [{
                        "parts": [{"text": "$promptWithDetails"}]
                    }],
                    "systemInstruction": {
                        "parts": [{"text": "${systemInstruction.replace("\n", "\\n").replace("\"", "\\\"")}"}]
                    },
                    "generationConfig": {
                        "temperature": 0.7,
                        "topP": 0.95
                    }
                }
            """.trimIndent()

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(generateOfflineFallbackAsset(prompt, assetType))
            }

            val textContent = extractTextFromGeminiResponse(responseBody)
            if (textContent.isBlank()) {
                return@withContext Result.success(generateOfflineFallbackAsset(prompt, assetType))
            }

            val result = processGeminiOutput(textContent, prompt, assetType)
            Result.success(result)
        } catch (e: Exception) {
            Result.success(generateOfflineFallbackAsset(prompt, assetType))
        }
    }

    private fun extractTextFromGeminiResponse(json: String): String {
        return try {
            val startIndex = json.indexOf("\"text\":")
            if (startIndex == -1) return ""
            val firstQuote = json.indexOf("\"", startIndex + 7)
            if (firstQuote == -1) return ""

            var i = firstQuote + 1
            val sb = java.lang.StringBuilder()
            while (i < json.length) {
                val c = json[i]
                if (c == '\\' && i + 1 < json.length) {
                    when (json[i + 1]) {
                        '"' -> { sb.append('"'); i += 2 }
                        '\\' -> { sb.append('\\'); i += 2 }
                        'n' -> { sb.append('\n'); i += 2 }
                        'r' -> { sb.append('\r'); i += 2 }
                        't' -> { sb.append('\t'); i += 2 }
                        else -> { sb.append(json[i + 1]); i += 2 }
                    }
                } else if (c == '"') {
                    break
                } else {
                    sb.append(c)
                    i++
                }
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun processGeminiOutput(rawText: String, prompt: String, assetType: AIAssetType): AIGeneratedAssetResult {
        return when (assetType) {
            AIAssetType.MODEL_3D, AIAssetType.ENVIRONMENT_TERRAIN -> {
                var objText = rawText
                if (rawText.contains("```")) {
                    val start = rawText.indexOf("```")
                    val lineBreak = rawText.indexOf("\n", start)
                    val end = rawText.indexOf("```", lineBreak)
                    if (lineBreak != -1 && end != -1) {
                        objText = rawText.substring(lineBreak + 1, end).trim()
                    }
                }

                val mesh = try {
                    if (objText.contains("v ") && objText.contains("f ")) {
                        Mesh.parseOBJ(objText, prompt.take(20))
                    } else {
                        generateProceduralShapeForPrompt(prompt)
                    }
                } catch (e: Exception) {
                    generateProceduralShapeForPrompt(prompt)
                }

                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = prompt.capitalize(),
                    description = "AI 3D Asset: ${mesh.vertices.size} vertices, ${mesh.triangles.size} polygons",
                    rawContent = objText,
                    generatedMesh = mesh,
                    tags = listOf("AI 3D", "OBJ", "Mobile Optimized")
                )
            }
            AIAssetType.TEXTURE_PROCEDURAL -> {
                val tex = generateTextureForPrompt(prompt)
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = "${prompt.capitalize()} Texture",
                    description = "Procedural PBR Texture Map (${tex.type.name})",
                    rawContent = rawText,
                    generatedTexture = tex,
                    tags = listOf("Texture", "PBR", "Procedural")
                )
            }
            AIAssetType.GAMEPLAY_SCRIPT -> {
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = "$prompt Script",
                    description = "T3D High Performance Game Logic Script",
                    rawContent = rawText,
                    generatedScript = rawText,
                    tags = listOf("T3D Script", "Game Logic", "AI Generated")
                )
            }
            AIAssetType.SHADER_MATERIAL -> {
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = "$prompt Material",
                    description = "PBR Material & Custom GLSL ES Shader",
                    rawContent = rawText,
                    tags = listOf("PBR", "Shader", "Material", "GLSL")
                )
            }
            AIAssetType.NPC_DIALOGUE_QUEST -> {
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = "$prompt Quest & Dialogue",
                    description = "Branching Narrative & NPC Interactive State",
                    rawContent = rawText,
                    tags = listOf("Dialogue", "Quest", "NPC", "RPG")
                )
            }
        }
    }

    private fun generateOfflineFallbackAsset(prompt: String, assetType: AIAssetType): AIGeneratedAssetResult {
        val lowPrompt = prompt.lowercase()
        return when (assetType) {
            AIAssetType.MODEL_3D -> {
                val mesh = generateProceduralShapeForPrompt(lowPrompt)
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = prompt.ifBlank { "Procedural Asset" },
                    description = "Optimized 3D Model: ${mesh.vertices.size} vertices, ${mesh.triangles.size} polygons",
                    rawContent = "# T3D Generated Mesh\n# Vertices: ${mesh.vertices.size}\n# Polygons: ${mesh.triangles.size}",
                    generatedMesh = mesh,
                    tags = listOf("Offline AI", "Low Poly", "3D Mesh")
                )
            }
            AIAssetType.ENVIRONMENT_TERRAIN -> {
                val terrain = if (lowPrompt.contains("island")) {
                    Mesh.createTerrain(width = 18f, depth = 18f, gridX = 14, gridZ = 14)
                } else {
                    Mesh.createTerrain(width = 24f, depth = 24f, gridX = 16, gridZ = 16)
                }
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = prompt.ifBlank { "Biome Terrain" },
                    description = "Procedural Biome & Heightmap Grid (${terrain.vertices.size} vertices)",
                    rawContent = "# Procedural Terrain Grid 16x16",
                    generatedMesh = terrain,
                    tags = listOf("Terrain", "World Building", "Biome")
                )
            }
            AIAssetType.TEXTURE_PROCEDURAL -> {
                val tex = generateTextureForPrompt(lowPrompt)
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = prompt.ifBlank { "Cyber Texture" },
                    description = "Procedural PBR Texture Map (${tex.type.name})",
                    rawContent = "# Procedural Texture: ${tex.name}",
                    generatedTexture = tex,
                    tags = listOf("Texture", "PBR", "Procedural")
                )
            }
            AIAssetType.GAMEPLAY_SCRIPT -> {
                val script = """
                    // T3D Auto-Generated Gameplay Script for: $prompt
                    on_start:
                      speed = 5.0;
                      health = 100.0;
                      target_dist = 12.0;

                    on_update:
                      rotate_y(25.0 * dt);
                      if (player_in_range(target_dist)) {
                        chase_player(speed * dt);
                      }
                      if (touch_tap) {
                        apply_impulse(0.0, 7.5, 0.0);
                        play_sound("jump_sfx");
                      }

                    on_collision:
                      spawn_particles("spark_emitter", 10);
                      apply_damage(15.0);
                """.trimIndent()
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = "$prompt Script",
                    description = "Interactive T3D Engine Script",
                    rawContent = script,
                    generatedScript = script,
                    tags = listOf("Script", "T3D Script", "Physics")
                )
            }
            AIAssetType.SHADER_MATERIAL -> {
                val shader = """
                    // T3D Mobile PBR Shader
                    precision mediump float;
                    uniform vec3 u_Albedo;
                    uniform float u_Metallic;
                    uniform float u_Roughness;
                    uniform vec3 u_LightDir;

                    void main() {
                        vec3 N = normalize(v_Normal);
                        vec3 L = normalize(u_LightDir);
                        float diff = max(dot(N, L), 0.15);
                        vec3 color = u_Albedo * diff + vec3(u_Metallic * 0.3);
                        gl_FragColor = vec4(color, 1.0);
                    }
                """.trimIndent()
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = "$prompt Shader",
                    description = "Mobile Optimized GLSL ES 3.0 Shader",
                    rawContent = shader,
                    tags = listOf("GLSL", "Shader", "PBR")
                )
            }
            AIAssetType.NPC_DIALOGUE_QUEST -> {
                val dialogue = """
                    {
                      "npc": "${prompt.ifBlank { "Cyber Merchant" }}",
                      "greeting": "Greetings traveler! Welcome to the neon district.",
                      "nodes": [
                        { "id": 1, "text": "Do you need cybernetic engine parts?", "options": [
                          { "text": "Yes, show me the inventory", "goto": 2 },
                          { "text": "I'm looking for a quest", "goto": 3 }
                        ]},
                        { "id": 2, "text": "Here are my rare 3D physics modules and shaders!", "goto": 0 },
                        { "id": 3, "text": "Defeat the rogue AI bot in the outer sector for 250 credits.", "goto": 0 }
                      ]
                    }
                """.trimIndent()
                AIGeneratedAssetResult(
                    assetType = assetType,
                    title = "$prompt Narrative",
                    description = "Branching Dialogue & Quest System",
                    rawContent = dialogue,
                    tags = listOf("Quest", "Dialogue", "RPG")
                )
            }
        }
    }

    private fun generateProceduralShapeForPrompt(prompt: String): Mesh {
        val low = prompt.lowercase()
        return when {
            low.contains("car") || low.contains("vehicle") || low.contains("ship") || low.contains("hover") -> {
                Mesh.createHoverVehicle()
            }
            low.contains("tower") || low.contains("monolith") || low.contains("building") || low.contains("spire") -> {
                Mesh.createSciFiTower()
            }
            low.contains("crystal") || low.contains("gem") || low.contains("ore") || low.contains("shard") -> {
                Mesh.createCrystalCluster()
            }
            low.contains("sphere") || low.contains("planet") || low.contains("orb") || low.contains("ball") -> {
                Mesh.createSphere(radius = 0.8f, rings = 14, sectors = 18, color = Color4(0.2f, 0.7f, 1.0f, 1f))
            }
            low.contains("cylinder") || low.contains("pillar") || low.contains("barrel") -> {
                Mesh.createCylinder(radius = 0.6f, height = 1.8f, color = Color4(0.9f, 0.4f, 0.2f, 1f))
            }
            low.contains("ring") || low.contains("torus") || low.contains("portal") -> {
                Mesh.createTorus(r1 = 0.8f, r2 = 0.25f, rings = 16, sides = 12, color = Color4(0.8f, 0.2f, 0.9f, 1f))
            }
            low.contains("character") || low.contains("robot") || low.contains("droid") || low.contains("human") -> {
                Mesh.createCharacterModel()
            }
            low.contains("terrain") || low.contains("mountain") || low.contains("island") || low.contains("landscape") -> {
                Mesh.createTerrain(width = 16f, depth = 16f, gridX = 14, gridZ = 14)
            }
            else -> {
                Mesh.createCube(size = 1.2f, color = Color4(0.15f, 0.85f, 0.75f, 1f))
            }
        }
    }

    private fun generateTextureForPrompt(prompt: String): ProceduralTexture {
        val low = prompt.lowercase()
        return when {
            low.contains("lava") || low.contains("magma") || low.contains("fire") -> {
                ProceduralTexture("Magma Glow", ProceduralTextureType.LAVA_GLOW, primaryColor = Color4(1f, 0.4f, 0.1f, 1f), secondaryColor = Color4(0.3f, 0.05f, 0f, 1f), scale = 4f, isEmissive = true)
            }
            low.contains("hex") || low.contains("armor") || low.contains("tech") -> {
                ProceduralTexture("Hex Shield", ProceduralTextureType.HEXAGON_TECH, primaryColor = Color4(0f, 0.9f, 1f, 1f), secondaryColor = Color4(0.05f, 0.1f, 0.2f, 1f), scale = 5f, metallic = 0.8f)
            }
            low.contains("brick") || low.contains("stone") || low.contains("dungeon") -> {
                ProceduralTexture("Dungeon Stone", ProceduralTextureType.BRICK_TILES, primaryColor = Color4(0.5f, 0.45f, 0.4f, 1f), secondaryColor = Color4(0.2f, 0.18f, 0.16f, 1f), scale = 3f)
            }
            low.contains("crystal") || low.contains("alien") || low.contains("voronoi") -> {
                ProceduralTexture("Crystal Cells", ProceduralTextureType.VORONOI_CELLS, primaryColor = Color4(0.9f, 0.2f, 1f, 1f), secondaryColor = Color4(0.1f, 0.05f, 0.2f, 1f), scale = 4f)
            }
            low.contains("checker") -> {
                ProceduralTexture("Checker Pattern", ProceduralTextureType.CHECKERBOARD, primaryColor = Color4.WHITE, secondaryColor = Color4.BLACK, scale = 4f)
            }
            else -> {
                ProceduralTexture("Cyber Grid", ProceduralTextureType.CYBER_GRID, primaryColor = Color4.CYAN, secondaryColor = Color4(0.05f, 0.08f, 0.15f, 1f), scale = 6f, isEmissive = true)
            }
        }
    }

    private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
