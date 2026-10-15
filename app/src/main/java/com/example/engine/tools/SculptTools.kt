package com.example.engine.tools

import com.example.engine.core.*
import kotlin.math.*

enum class SculptBrushType {
    RAISE,
    LOWER,
    SMOOTH,
    FLATTEN,
    DISPLACE_NOISE
}

class SculptToolController {
    var brushType: SculptBrushType = SculptBrushType.RAISE
    var brushRadius: Float = 1.5f
    var brushStrength: Float = 0.5f
    var brushPosition: Vector3 = Vector3.ZERO
    var isSculpting: Boolean = false

    fun applySculpt(mesh: Mesh, worldMatrix: Matrix4x4, sculptCenter: Vector3, dt: Float): Boolean {
        if (mesh.vertices.isEmpty()) return false

        // Inverse transform sculpt center to mesh local space
        // For simplicity assuming uniform scale or computing local distance
        var modified = false
        val r2 = brushRadius * brushRadius
        val strength = brushStrength * (dt * 60f) * 0.1f

        var avgY = 0f
        var count = 0
        if (brushType == SculptBrushType.SMOOTH) {
            for (v in mesh.vertices) {
                val worldPos = worldMatrix.transformPoint(v.position)
                if (worldPos.distanceTo(sculptCenter) <= brushRadius) {
                    avgY += v.position.y
                    count++
                }
            }
            if (count > 0) avgY /= count
        }

        for (v in mesh.vertices) {
            val worldPos = worldMatrix.transformPoint(v.position)
            val dist = worldPos.distanceTo(sculptCenter)

            if (dist <= brushRadius) {
                modified = true
                // Gaussian falloff
                val falloff = (1.0f - (dist / brushRadius).pow(2)).coerceIn(0f, 1f)

                when (brushType) {
                    SculptBrushType.RAISE -> {
                        v.position.y += strength * falloff
                    }
                    SculptBrushType.LOWER -> {
                        v.position.y -= strength * falloff
                    }
                    SculptBrushType.SMOOTH -> {
                        v.position.y = v.position.y + (avgY - v.position.y) * 0.3f * falloff
                    }
                    SculptBrushType.FLATTEN -> {
                        val targetLocalY = sculptCenter.y
                        v.position.y = v.position.y + (targetLocalY - v.position.y) * 0.4f * falloff
                    }
                    SculptBrushType.DISPLACE_NOISE -> {
                        val noise = (sin(v.position.x * 5.0 + v.position.z * 5.0) * 0.15f).toFloat()
                        v.position.y += noise * strength * falloff
                    }
                }
            }
        }

        if (modified) {
            mesh.recalculateNormals()
            mesh.recalculateAABB()
        }
        return modified
    }
}

typealias SculptTools = SculptToolController

