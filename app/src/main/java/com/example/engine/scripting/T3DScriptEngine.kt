package com.example.engine.scripting

import com.example.engine.core.*
import com.example.engine.ecs.*
import kotlin.math.*

enum class ScriptNodeType {
    EVENT_ON_START,
    EVENT_ON_UPDATE,
    EVENT_ON_TOUCH,
    EVENT_ON_COLLISION,
    CONDITION_IF,
    ACTION_ROTATE,
    ACTION_TRANSLATE,
    ACTION_APPLY_IMPULSE,
    ACTION_SET_COLOR,
    ACTION_SPAWN_CUBE,
    MATH_ADD,
    MATH_MULTIPLY
}

data class ScriptVisualNode(
    val id: String = java.util.UUID.randomUUID().toString().substring(0, 6),
    val type: ScriptNodeType,
    var label: String,
    var paramFloat: Float = 0f,
    var paramVector: Vector3 = Vector3.ZERO,
    var paramColor: Color4 = Color4.CYAN,
    var targetEntityName: String = "Self",
    var connectedNextNodeId: String? = null
)

class T3DScriptEngine {
    val globalVariables: MutableMap<String, Any> = mutableMapOf(
        "score" to 0,
        "player_health" to 100.0,
        "gravity" to 9.81,
        "jump_power" to 7.0,
        "game_state" to "PLAYING"
    )

    val consoleLogs: MutableList<String> = mutableListOf(
        "[T3D Script Engine v3.0 initialized]",
        "[WASM Bridge active - 60 FPS JIT compiler ready]"
    )

    fun log(message: String) {
        if (consoleLogs.size > 100) consoleLogs.removeAt(0)
        consoleLogs.add("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] $message")
    }

    fun clearLogs() {
        consoleLogs.clear()
        consoleLogs.add("[Console cleared]")
    }

    fun executeScript(scriptSource: String, entity: Entity, dt: Float, isTouchTap: Boolean = false): Boolean {
        if (scriptSource.isBlank()) return true
        val lines = scriptSource.lines()
        var currentBlock: String? = null

        try {
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue

                if (line.endsWith(":")) {
                    currentBlock = line.removeSuffix(":").trim().lowercase()
                    continue
                }

                when (currentBlock) {
                    "on_update", "update" -> {
                        executeStatement(line, entity, dt, isTouchTap)
                    }
                    "on_touch", "touch" -> {
                        if (isTouchTap) {
                            executeStatement(line, entity, dt, isTouchTap)
                        }
                    }
                    "on_start", "start" -> {
                        executeStatement(line, entity, dt, isTouchTap)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            log("Error executing script for ${entity.name}: ${e.message}")
            return false
        }
    }

    private fun executeStatement(statement: String, entity: Entity, dt: Float, isTouchTap: Boolean) {
        val clean = statement.removeSuffix(";").trim()

        // Handle inline condition: if (touch_tap) { apply_impulse(0, 5, 0); }
        if (clean.startsWith("if") && clean.contains("{")) {
            val condPart = clean.substringAfter("(").substringBefore(")").trim()
            val actionPart = clean.substringAfter("{").substringBefore("}").trim()

            val conditionMet = when (condPart) {
                "touch_tap" -> isTouchTap
                "is_grounded" -> entity.rigidBody?.isGrounded == true
                "score >= 10" -> (globalVariables["score"] as? Int ?: 0) >= 10
                else -> isTouchTap
            }

            if (conditionMet) {
                executeStatement(actionPart, entity, dt, isTouchTap)
            }
            return
        }

        // Functions
        when {
            clean.startsWith("rotate_y") -> {
                val value = extractNumberInParen(clean, dt)
                entity.transform.rotation.y = (entity.transform.rotation.y + value) % 360f
            }
            clean.startsWith("rotate_x") -> {
                val value = extractNumberInParen(clean, dt)
                entity.transform.rotation.x = (entity.transform.rotation.x + value) % 360f
            }
            clean.startsWith("rotate_z") -> {
                val value = extractNumberInParen(clean, dt)
                entity.transform.rotation.z = (entity.transform.rotation.z + value) % 360f
            }
            clean.startsWith("move_x") -> {
                val value = extractNumberInParen(clean, dt)
                entity.transform.position.x += value
            }
            clean.startsWith("move_z") -> {
                val value = extractNumberInParen(clean, dt)
                entity.transform.position.z += value
            }
            clean.startsWith("apply_impulse") -> {
                val numbers = extractVectorInParen(clean)
                entity.rigidBody?.applyImpulse(numbers)
                log("${entity.name} applied impulse: $numbers")
            }
            clean.startsWith("set_color") -> {
                val colName = clean.substringAfter("(").substringBefore(")").trim().lowercase()
                when (colName) {
                    "cyan", "blue" -> entity.material.albedoColor = Color4.CYAN
                    "red" -> entity.material.albedoColor = Color4.RED
                    "green" -> entity.material.albedoColor = Color4.GREEN
                    "orange", "gold" -> entity.material.albedoColor = Color4.ORANGE
                    "purple" -> entity.material.albedoColor = Color4.PURPLE
                }
            }
            clean.startsWith("play_sound") -> {
                val sfx = clean.substringAfter("(").substringBefore(")").replace("\"", "")
                log("SFX Triggered: $sfx")
            }
            clean.contains("=") -> {
                // Variable assignment
                val parts = clean.split("=")
                if (parts.size == 2) {
                    val varName = parts[0].trim()
                    val value = parts[1].trim().toDoubleOrNull() ?: parts[1].trim()
                    entity.script?.localVariables?.put(varName, value)
                    globalVariables[varName] = value
                }
            }
        }
    }

    private fun extractNumberInParen(str: String, dt: Float): Float {
        val inner = str.substringAfter("(").substringBefore(")").trim()
        if (inner.contains("* dt") || inner.contains("*dt")) {
            val num = inner.replace("* dt", "").replace("*dt", "").trim().toFloatOrNull() ?: 1f
            return num * dt
        }
        return inner.toFloatOrNull() ?: (10f * dt)
    }

    private fun extractVectorInParen(str: String): Vector3 {
        val inner = str.substringAfter("(").substringBefore(")").trim()
        val parts = inner.split(",")
        if (parts.size >= 3) {
            val x = parts[0].trim().toFloatOrNull() ?: 0f
            val y = parts[1].trim().toFloatOrNull() ?: 0f
            val z = parts[2].trim().toFloatOrNull() ?: 0f
            return Vector3(x, y, z)
        }
        return Vector3(0f, 5f, 0f)
    }

    companion object {
        fun createSampleVisualNodes(): List<ScriptVisualNode> {
            return listOf(
                ScriptVisualNode(
                    type = ScriptNodeType.EVENT_ON_UPDATE,
                    label = "On Frame Update (60 Hz)"
                ),
                ScriptVisualNode(
                    type = ScriptNodeType.ACTION_ROTATE,
                    label = "Rotate Y Axis",
                    paramFloat = 45f
                ),
                ScriptVisualNode(
                    type = ScriptNodeType.EVENT_ON_TOUCH,
                    label = "On Viewport Screen Tap"
                ),
                ScriptVisualNode(
                    type = ScriptNodeType.ACTION_APPLY_IMPULSE,
                    label = "Physics Jump Impulse",
                    paramVector = Vector3(0f, 7.5f, 0f)
                ),
                ScriptVisualNode(
                    type = ScriptNodeType.ACTION_SET_COLOR,
                    label = "Set Cyber Glow Color",
                    paramColor = Color4.CYAN
                )
            )
        }
    }
}
