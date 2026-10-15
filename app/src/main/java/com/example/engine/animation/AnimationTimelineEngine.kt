package com.example.engine.animation

import com.example.engine.core.*
import kotlin.math.*

enum class EasingCurve(val displayName: String) {
    LINEAR("Linear"),
    EASE_IN_QUAD("Ease In Quad"),
    EASE_OUT_CUBIC("Ease Out Cubic"),
    EASE_IN_OUT_SINE("Ease In/Out Sine"),
    BOUNCE("Bounce"),
    ELASTIC("Elastic")
}

data class Keyframe3D(
    val timeSec: Float,
    val value: Vector3,
    val easing: EasingCurve = EasingCurve.EASE_IN_OUT_SINE
)

data class AnimationTrack(
    val property: String, // "position", "rotation", "scale"
    val keyframes: MutableList<Keyframe3D> = mutableListOf()
) {
    fun evaluate(time: Float): Vector3 {
        if (keyframes.isEmpty()) return Vector3.ZERO
        if (keyframes.size == 1) return keyframes.first().value

        if (time <= keyframes.first().timeSec) return keyframes.first().value
        if (time >= keyframes.last().timeSec) return keyframes.last().value

        for (i in 0 until keyframes.size - 1) {
            val k0 = keyframes[i]
            val k1 = keyframes[i + 1]
            if (time >= k0.timeSec && time <= k1.timeSec) {
                val span = k1.timeSec - k0.timeSec
                val rawT = if (span > 0.0001f) (time - k0.timeSec) / span else 0f
                val easedT = applyEasing(rawT, k0.easing)
                return k0.value.lerp(k1.value, easedT)
            }
        }
        return keyframes.last().value
    }

    private fun applyEasing(t: Float, easing: EasingCurve): Float {
        return when (easing) {
            EasingCurve.LINEAR -> t
            EasingCurve.EASE_IN_QUAD -> t * t
            EasingCurve.EASE_OUT_CUBIC -> {
                val f = t - 1f
                f * f * f + 1f
            }
            EasingCurve.EASE_IN_OUT_SINE -> -(cos(PI * t).toFloat() - 1f) / 2f
            EasingCurve.BOUNCE -> {
                var x = t
                val n1 = 7.5625f
                val d1 = 2.75f
                when {
                    x < 1f / d1 -> n1 * x * x
                    x < 2f / d1 -> {
                        x -= 1.5f / d1
                        n1 * x * x + 0.75f
                    }
                    x < 2.5f / d1 -> {
                        x -= 2.25f / d1
                        n1 * x * x + 0.9375f
                    }
                    else -> {
                        x -= 2.625f / d1
                        n1 * x * x + 0.984375f
                    }
                }
            }
            EasingCurve.ELASTIC -> {
                if (t == 0f) 0f
                else if (t == 1f) 1f
                else {
                    val c4 = (2f * PI.toFloat()) / 3f
                    -(2.0.pow(10.0 * (t - 1)).toFloat() * sin((t * 10f - 10.75f) * c4))
                }
            }
        }
    }
}

data class AnimationClip(
    val name: String,
    val durationSec: Float = 2.0f,
    val isLooping: Boolean = true,
    val tracks: MutableList<AnimationTrack> = mutableListOf()
)

class AnimationTimelineEngine {
    var currentTimeSec: Float = 0f
    var isPlaying: Boolean = true
    var playbackSpeed: Float = 1.0f
    var activeClip: AnimationClip = createWalkCycleClip()

    fun update(dt: Float, onApplyTransform: (posOffset: Vector3, rotOffset: Vector3, scaleOffset: Vector3) -> Unit) {
        if (!isPlaying) return

        currentTimeSec += dt * playbackSpeed
        if (currentTimeSec > activeClip.durationSec) {
            if (activeClip.isLooping) {
                currentTimeSec %= activeClip.durationSec
            } else {
                currentTimeSec = activeClip.durationSec
                isPlaying = false
            }
        }

        var posOffset = Vector3.ZERO
        var rotOffset = Vector3.ZERO
        var scaleOffset = Vector3(1f, 1f, 1f)

        for (track in activeClip.tracks) {
            when (track.property) {
                "position" -> posOffset = track.evaluate(currentTimeSec)
                "rotation" -> rotOffset = track.evaluate(currentTimeSec)
                "scale" -> scaleOffset = track.evaluate(currentTimeSec)
            }
        }

        onApplyTransform(posOffset, rotOffset, scaleOffset)
    }

    companion object {
        fun createWalkCycleClip(): AnimationClip {
            val clip = AnimationClip("Humanoid Walk Cycle", durationSec = 1.2f, isLooping = true)
            // Position Bobbing
            val posTrack = AnimationTrack("position", mutableListOf(
                Keyframe3D(0.0f, Vector3(0f, 0.0f, 0f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(0.3f, Vector3(0f, 0.12f, 0f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(0.6f, Vector3(0f, 0.0f, 0f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(0.9f, Vector3(0f, 0.12f, 0f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(1.2f, Vector3(0f, 0.0f, 0f), EasingCurve.EASE_IN_OUT_SINE)
            ))
            // Hip Yaw & Roll Sway
            val rotTrack = AnimationTrack("rotation", mutableListOf(
                Keyframe3D(0.0f, Vector3(0f, -8f, -4f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(0.6f, Vector3(0f, 8f, 4f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(1.2f, Vector3(0f, -8f, -4f), EasingCurve.EASE_IN_OUT_SINE)
            ))
            clip.tracks.add(posTrack)
            clip.tracks.add(rotTrack)
            return clip
        }

        fun createHoverPulseClip(): AnimationClip {
            val clip = AnimationClip("Hover Floating & Pulse", durationSec = 2.0f, isLooping = true)
            val posTrack = AnimationTrack("position", mutableListOf(
                Keyframe3D(0.0f, Vector3(0f, 0.0f, 0f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(1.0f, Vector3(0f, 0.45f, 0f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(2.0f, Vector3(0f, 0.0f, 0f), EasingCurve.EASE_IN_OUT_SINE)
            ))
            val rotTrack = AnimationTrack("rotation", mutableListOf(
                Keyframe3D(0.0f, Vector3(0f, 0f, 0f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(1.0f, Vector3(5f, 180f, -5f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(2.0f, Vector3(0f, 360f, 0f), EasingCurve.EASE_IN_OUT_SINE)
            ))
            val scaleTrack = AnimationTrack("scale", mutableListOf(
                Keyframe3D(0.0f, Vector3(1f, 1f, 1f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(1.0f, Vector3(1.08f, 1.08f, 1.08f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(2.0f, Vector3(1f, 1f, 1f), EasingCurve.EASE_IN_OUT_SINE)
            ))
            clip.tracks.add(posTrack)
            clip.tracks.add(rotTrack)
            clip.tracks.add(scaleTrack)
            return clip
        }

        fun createBounceImpactClip(): AnimationClip {
            val clip = AnimationClip("Jump & Bounce Impact", durationSec = 1.5f, isLooping = true)
            val posTrack = AnimationTrack("position", mutableListOf(
                Keyframe3D(0.0f, Vector3(0f, 2.0f, 0f), EasingCurve.LINEAR),
                Keyframe3D(0.5f, Vector3(0f, 0.0f, 0f), EasingCurve.BOUNCE),
                Keyframe3D(1.5f, Vector3(0f, 2.0f, 0f), EasingCurve.EASE_OUT_CUBIC)
            ))
            val scaleTrack = AnimationTrack("scale", mutableListOf(
                Keyframe3D(0.0f, Vector3(0.9f, 1.2f, 0.9f), EasingCurve.EASE_IN_OUT_SINE),
                Keyframe3D(0.5f, Vector3(1.3f, 0.6f, 1.3f), EasingCurve.BOUNCE),
                Keyframe3D(0.8f, Vector3(1f, 1f, 1f), EasingCurve.ELASTIC),
                Keyframe3D(1.5f, Vector3(0.9f, 1.2f, 0.9f), EasingCurve.EASE_IN_OUT_SINE)
            ))
            clip.tracks.add(posTrack)
            clip.tracks.add(scaleTrack)
            return clip
        }
    }
}
