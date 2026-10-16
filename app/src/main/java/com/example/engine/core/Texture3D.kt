package com.example.engine.core

import kotlin.math.*

enum class ProceduralTextureType {
    CYBER_GRID,
    CHECKERBOARD,
    PERLIN_NOISE,
    VORONOI_CELLS,
    LAVA_GLOW,
    BRICK_TILES,
    HEXAGON_TECH
}

data class ProceduralTexture(
    val name: String,
    val type: ProceduralTextureType,
    val width: Int = 64,
    val height: Int = 64,
    val primaryColor: Color4 = Color4.CYAN,
    val secondaryColor: Color4 = Color4(0.05f, 0.08f, 0.15f, 1f),
    val scale: Float = 4.0f,
    val roughness: Float = 0.4f,
    val metallic: Float = 0.6f,
    val isEmissive: Boolean = false
) {
    // Generate sample color at UV coordinate (0..1)
    fun sample(u: Float, v: Float, time: Float = 0f): Color4 {
        val su = (u * scale) % 1.0f
        val sv = (v * scale) % 1.0f

        return when (type) {
            ProceduralTextureType.CYBER_GRID -> {
                val lineWidth = 0.08f
                val isLine = (su < lineWidth || su > 1f - lineWidth) || (sv < lineWidth || sv > 1f - lineWidth)
                if (isLine) primaryColor else secondaryColor
            }
            ProceduralTextureType.CHECKERBOARD -> {
                val cx = (su * 2f).toInt() % 2
                val cy = (sv * 2f).toInt() % 2
                if (cx == cy) primaryColor else secondaryColor
            }
            ProceduralTextureType.HEXAGON_TECH -> {
                val qx = su * 1.732f
                val qy = sv + (if (qx.toInt() % 2 != 0) 0.5f else 0f)
                val dist = abs(sin(qx * 3.1415f) * cos(qy * 3.1415f))
                if (dist > 0.6f) primaryColor else secondaryColor
            }
            ProceduralTextureType.LAVA_GLOW -> {
                val wave = (sin(u * 10.0 + time) * cos(v * 10.0 + time)).toFloat()
                val pulse = (wave * 0.5f + 0.5f).coerceIn(0f, 1f)
                Color4(
                    primaryColor.r * pulse + secondaryColor.r * (1f - pulse),
                    primaryColor.g * pulse + secondaryColor.g * (1f - pulse),
                    primaryColor.b * pulse + secondaryColor.b * (1f - pulse),
                    1f
                )
            }
            ProceduralTextureType.VORONOI_CELLS -> {
                val cell = (sin(u * 8f) * sin(v * 8f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                Color4(
                    primaryColor.r * cell + secondaryColor.r * (1f - cell),
                    primaryColor.g * cell + secondaryColor.g * (1f - cell),
                    primaryColor.b * cell + secondaryColor.b * (1f - cell),
                    1f
                )
            }
            ProceduralTextureType.BRICK_TILES -> {
                val row = (v * scale * 2f).toInt()
                val shiftU = if (row % 2 == 0) u else u + 0.25f
                val bu = (shiftU * scale * 2f) % 1.0f
                val bv = (v * scale * 4f) % 1.0f
                val isMortar = bu < 0.06f || bv < 0.08f
                if (isMortar) secondaryColor else primaryColor
            }
            ProceduralTextureType.PERLIN_NOISE -> {
                val n = (sin(u * 12.0) * cos(v * 12.0) * 0.5 + 0.5).toFloat()
                Color4(
                    primaryColor.r * n + secondaryColor.r * (1f - n),
                    primaryColor.g * n + secondaryColor.g * (1f - n),
                    primaryColor.b * n + secondaryColor.b * (1f - n),
                    1f
                )
            }
        }
    }

    fun samplePS2(u: Float, v: Float, time: Float = 0f, ditherLevels: Int = 16): Color4 {
        val col = sample(u, v, time)
        val q = (ditherLevels.coerceIn(4, 64) - 1).toFloat()
        return Color4(
            (round(col.r * q) / q).coerceIn(0f, 1f),
            (round(col.g * q) / q).coerceIn(0f, 1f),
            (round(col.b * q) / q).coerceIn(0f, 1f),
            col.a
        )
    }

    companion object {
        fun presets(): List<ProceduralTexture> = listOf(
            ProceduralTexture("Neon Cyber Grid", ProceduralTextureType.CYBER_GRID, primaryColor = Color4(0f, 0.95f, 1f, 1f), secondaryColor = Color4(0.04f, 0.07f, 0.12f, 1f), scale = 6f, isEmissive = true),
            ProceduralTexture("Magma Core Lava", ProceduralTextureType.LAVA_GLOW, primaryColor = Color4(1f, 0.35f, 0.05f, 1f), secondaryColor = Color4(0.3f, 0.02f, 0.01f, 1f), scale = 4f, isEmissive = true),
            ProceduralTexture("Sci-Fi Hex Armor", ProceduralTextureType.HEXAGON_TECH, primaryColor = Color4(0.15f, 0.8f, 0.9f, 1f), secondaryColor = Color4(0.08f, 0.12f, 0.18f, 1f), scale = 5f, metallic = 0.8f, roughness = 0.2f),
            ProceduralTexture("Dungeon Brick", ProceduralTextureType.BRICK_TILES, primaryColor = Color4(0.45f, 0.4f, 0.38f, 1f), secondaryColor = Color4(0.18f, 0.16f, 0.15f, 1f), scale = 3f, roughness = 0.8f),
            ProceduralTexture("Alien Crystal Biome", ProceduralTextureType.VORONOI_CELLS, primaryColor = Color4(0.85f, 0.2f, 0.95f, 1f), secondaryColor = Color4(0.12f, 0.05f, 0.2f, 1f), scale = 4f, metallic = 0.6f)
        )
    }
}
