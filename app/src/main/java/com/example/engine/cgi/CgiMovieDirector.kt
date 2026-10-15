package com.example.engine.cgi

import com.example.engine.core.Color4
import com.example.engine.core.Vector3
import com.example.engine.ecs.Camera3D
import kotlin.math.*

enum class AspectRatioPreset(val displayName: String, val ratio: Float, val letterboxPercent: Float) {
    CINEMASCOPE_21_9("CinemaScope 2.39:1", 2.39f, 0.12f),
    IMAX_1_43("IMAX 1.43:1", 1.43f, 0.0f),
    STANDARD_16_9("Standard HD 16:9", 1.777f, 0.0f),
    ANAMORPHIC_2_0("Anamorphic 2:1", 2.0f, 0.08f),
    VINTAGE_ACADEMY_4_3("Academy 4:3", 1.333f, 0.0f)
}

enum class ColorGradingLut(
    val displayName: String,
    val tintR: Float,
    val tintG: Float,
    val tintB: Float,
    val contrast: Float,
    val saturation: Float,
    val vignetteIntensity: Float
) {
    NATURAL_REC709("Natural Rec.709", 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.15f),
    CYBERPUNK_NEON("Cyberpunk Neon", 0.85f, 1.15f, 1.35f, 1.25f, 1.35f, 0.45f),
    HOLLYWOOD_TEAL_ORANGE("Blockbuster Teal & Orange", 1.18f, 0.95f, 1.22f, 1.3f, 1.2f, 0.35f),
    FILM_NOIR_MONOCHROME("Film Noir Monochromatic", 0.95f, 0.95f, 0.95f, 1.45f, 0.05f, 0.6f),
    GOLDEN_HOUR_DREAM("Golden Hour Sunset", 1.3f, 1.05f, 0.75f, 1.15f, 1.25f, 0.25f),
    SCI_FI_MATRIX_CODE("Matrix Phosphor Green", 0.75f, 1.35f, 0.8f, 1.35f, 1.1f, 0.5f),
    ANALOG_VINTAGE_VHS("Retro VHS 80s Tape", 1.15f, 1.0f, 1.15f, 0.95f, 0.85f, 0.3f)
}

enum class CameraRigShot(
    val displayName: String,
    val description: String,
    val defaultFov: Float,
    val fStop: Float
) {
    ORBIT_CINEMATIC("Orbit 360° Hero Shot", "Smooth 360 rotation around selected character", 48f, 2.0f),
    DOLLY_ZOOM("Vertigo Dolly Zoom", "Compensating camera zoom while moving forward", 35f, 1.4f),
    LOW_ANGLE_HERO("Low-Angle Power Stance", "Dramatic upward angle with wide focal length", 28f, 2.8f),
    CRANE_SWEEP("Overhead Crane Sweep", "Descends from high vantage point down to eye level", 55f, 4.0f),
    ACTION_CHASE_CAM("Dynamic Action Chase", "Over-the-shoulder motion tracking with organic sway", 65f, 2.8f),
    STATIC_MASTER_SHOT("Cinematographer Master", "Rock-solid golden ratio composed master shot", 50f, 5.6f)
}

data class CinematicKeyframe(
    val timeSec: Float,
    val camPosition: Vector3,
    val camTarget: Vector3,
    val fov: Float,
    val rollDeg: Float = 0f
)

/**
 * Movie & CGI Director Subsystem.
 * Provides Hollywood-grade cinematic camera rigs, anamorphic aspect ratio matte bars,
 * lens focal lengths, color grading curves, and camera motion tracking.
 */
class CgiMovieDirector {
    var activeShot: CameraRigShot = CameraRigShot.ORBIT_CINEMATIC
    var activeAspect: AspectRatioPreset = AspectRatioPreset.CINEMASCOPE_21_9
    var activeLut: ColorGradingLut = ColorGradingLut.CYBERPUNK_NEON

    var focalLengthMm: Float = 50f // 24mm, 35mm, 50mm, 85mm
    var apertureFStop: Float = 2.0f // f/1.4, f/2.0, f/4.0, f/8.0
    var depthOfFieldEnabled: Boolean = true
    var focalDistance: Float = 6.0f // Distance in meters for DoF focus plane

    var isRecordingMovie: Boolean = false
    var shotPlaybackTime: Float = 0f
    var shotDuration: Float = 6.0f
    var isShotPlaying: Boolean = true

    // Organic handheld camera sway
    private var swayTime: Float = 0f

    fun update(dt: Float, camera: Camera3D, targetCenter: Vector3) {
        if (!isShotPlaying) return

        shotPlaybackTime += dt
        if (shotPlaybackTime > shotDuration) {
            shotPlaybackTime %= shotDuration
        }
        swayTime += dt

        val t = (shotPlaybackTime / shotDuration).coerceIn(0f, 1f)

        when (activeShot) {
            CameraRigShot.ORBIT_CINEMATIC -> {
                val angle = t * 2f * PI.toFloat()
                val dist = 8.0f
                val height = 2.5f + sin(t * PI.toFloat() * 2f) * 0.8f
                camera.position = Vector3(
                    targetCenter.x + cos(angle) * dist,
                    targetCenter.y + height,
                    targetCenter.z + sin(angle) * dist
                )
                camera.target = targetCenter + Vector3(0f, 1.0f, 0f)
                camera.fov = 45f
            }
            CameraRigShot.DOLLY_ZOOM -> {
                // Move camera from 12m to 4m, while adjusting FOV from 30 to 80 deg (Vertigo Hitchcock effect)
                val dist = 12.0f - (t * 7.5f)
                camera.position = Vector3(targetCenter.x, targetCenter.y + 1.2f, targetCenter.z - dist)
                camera.target = targetCenter + Vector3(0f, 1.0f, 0f)
                camera.fov = 30f + (t * 50f)
            }
            CameraRigShot.LOW_ANGLE_HERO -> {
                val subtleDrift = sin(swayTime * 0.8f) * 0.4f
                camera.position = Vector3(targetCenter.x + 2.5f + subtleDrift, targetCenter.y + 0.35f, targetCenter.z - 4.5f)
                camera.target = targetCenter + Vector3(0f, 1.8f, 0f)
                camera.fov = 32f
            }
            CameraRigShot.CRANE_SWEEP -> {
                val startPos = Vector3(targetCenter.x - 6f, targetCenter.y + 9f, targetCenter.z - 8f)
                val endPos = Vector3(targetCenter.x + 2f, targetCenter.y + 1.2f, targetCenter.z - 3.5f)
                val easedT = -(cos(PI * t).toFloat() - 1f) / 2f
                camera.position = startPos.lerp(endPos, easedT)
                camera.target = targetCenter + Vector3(0f, 1.0f, 0f)
                camera.fov = 50f
            }
            CameraRigShot.ACTION_CHASE_CAM -> {
                // Organic handheld camera noise
                val shakeX = sin(swayTime * 6.5f) * 0.08f
                val shakeY = cos(swayTime * 8.2f) * 0.06f
                camera.position = Vector3(
                    targetCenter.x - 1.2f + shakeX,
                    targetCenter.y + 2.2f + shakeY,
                    targetCenter.z - 5.5f
                )
                camera.target = targetCenter + Vector3(0f, 1.2f, 0f)
                camera.fov = 65f
            }
            CameraRigShot.STATIC_MASTER_SHOT -> {
                camera.position = Vector3(targetCenter.x + 5.0f, targetCenter.y + 2.0f, targetCenter.z - 7.5f)
                camera.target = targetCenter + Vector3(0f, 1.0f, 0f)
                camera.fov = 50f
            }
        }
    }

    fun applyColorGrading(baseColor: Color4): Color4 {
        val lut = activeLut
        var r = (baseColor.r * lut.tintR)
        var g = (baseColor.g * lut.tintG)
        var b = (baseColor.b * lut.tintB)

        // Apply Contrast
        r = ((r - 0.5f) * lut.contrast + 0.5f).coerceIn(0f, 1f)
        g = ((g - 0.5f) * lut.contrast + 0.5f).coerceIn(0f, 1f)
        b = ((b - 0.5f) * lut.contrast + 0.5f).coerceIn(0f, 1f)

        // Apply Saturation (Luminance preserving)
        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
        r = (luma + (r - luma) * lut.saturation).coerceIn(0f, 1f)
        g = (luma + (g - luma) * lut.saturation).coerceIn(0f, 1f)
        b = (luma + (b - luma) * lut.saturation).coerceIn(0f, 1f)

        return Color4(r, g, b, baseColor.a)
    }
}
