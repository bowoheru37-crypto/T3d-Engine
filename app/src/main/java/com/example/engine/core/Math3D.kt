package com.example.engine.core

import kotlin.math.*

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun unaryMinus() = Vector3(-x, -y, -z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun times(other: Vector3) = Vector3(x * other.x, y * other.y, z * other.z)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else Vector3()

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z
    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.00001f) this / len else Vector3(0f, 1f, 0f)
    }

    fun distanceTo(other: Vector3): Float = (this - other).length()

    fun lerp(target: Vector3, t: Float): Vector3 {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector3(
            x + (target.x - x) * clampedT,
            y + (target.y - y) * clampedT,
            z + (target.z - z) * clampedT
        )
    }

    fun copy() = Vector3(x, y, z)

    companion object {
        val ZERO get() = Vector3(0f, 0f, 0f)
        val ONE get() = Vector3(1f, 1f, 1f)
        val UP get() = Vector3(0f, 1f, 0f)
        val DOWN get() = Vector3(0f, -1f, 0f)
        val FORWARD get() = Vector3(0f, 0f, 1f)
        val BACK get() = Vector3(0f, 0f, -1f)
        val RIGHT get() = Vector3(1f, 0f, 0f)
        val LEFT get() = Vector3(-1f, 0f, 0f)

        fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 {
            val clampedT = t.coerceIn(0f, 1f)
            return Vector3(
                a.x + (b.x - a.x) * clampedT,
                a.y + (b.y - a.y) * clampedT,
                a.z + (b.z - a.z) * clampedT
            )
        }
    }
}

data class Vector2(var x: Float = 0f, var y: Float = 0f) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)
    fun length(): Float = sqrt(x * x + y * y)
}

data class Color4(var r: Float = 1f, var g: Float = 1f, var b: Float = 1f, var a: Float = 1f) {
    fun toComposeColor(): androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(r, g, b, a)

    fun lerp(target: Color4, t: Float): Color4 {
        val clampedT = t.coerceIn(0f, 1f)
        return Color4(
            r + (target.r - r) * clampedT,
            g + (target.g - g) * clampedT,
            b + (target.b - b) * clampedT,
            a + (target.a - a) * clampedT
        )
    }

    operator fun times(scalar: Float) = Color4(
        (r * scalar).coerceIn(0f, 1f),
        (g * scalar).coerceIn(0f, 1f),
        (b * scalar).coerceIn(0f, 1f),
        a
    )

    operator fun times(other: Color4) = Color4(
        (r * other.r).coerceIn(0f, 1f),
        (g * other.g).coerceIn(0f, 1f),
        (b * other.b).coerceIn(0f, 1f),
        (a * other.a).coerceIn(0f, 1f)
    )

    companion object {
        val WHITE = Color4(1f, 1f, 1f, 1f)
        val BLACK = Color4(0f, 0f, 0f, 1f)
        val RED = Color4(0.95f, 0.26f, 0.21f, 1f)
        val GREEN = Color4(0.29f, 0.69f, 0.31f, 1f)
        val BLUE = Color4(0.13f, 0.59f, 0.95f, 1f)
        val CYAN = Color4(0f, 0.94f, 1f, 1f)
        val ORANGE = Color4(1f, 0.6f, 0f, 1f)
        val PURPLE = Color4(0.61f, 0.35f, 0.71f, 1f)
        val GRAY = Color4(0.5f, 0.5f, 0.5f, 1f)
        val DARK_GRAY = Color4(0.2f, 0.2f, 0.25f, 1f)
    }
}

data class Matrix4x4(val m: FloatArray = FloatArray(16)) {
    init {
        if (m.all { it == 0f }) {
            m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
        }
    }

    operator fun times(other: Matrix4x4): Matrix4x4 {
        val result = FloatArray(16)
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += this.m[row * 4 + k] * other.m[k * 4 + col]
                }
                result[row * 4 + col] = sum
            }
        }
        return Matrix4x4(result)
    }

    fun transformPoint(v: Vector3): Vector3 {
        val x = m[0] * v.x + m[1] * v.y + m[2] * v.z + m[3]
        val y = m[4] * v.x + m[5] * v.y + m[6] * v.z + m[7]
        val z = m[8] * v.x + m[9] * v.y + m[10] * v.z + m[11]
        val w = m[12] * v.x + m[13] * v.y + m[14] * v.z + m[15]
        return if (w != 0f && w != 1f) Vector3(x / w, y / w, z / w) else Vector3(x, y, z)
    }

    fun transformVector(v: Vector3): Vector3 {
        val x = m[0] * v.x + m[1] * v.y + m[2] * v.z
        val y = m[4] * v.x + m[5] * v.y + m[6] * v.z
        val z = m[8] * v.x + m[9] * v.y + m[10] * v.z
        return Vector3(x, y, z)
    }

    companion object {
        fun identity(): Matrix4x4 = Matrix4x4(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        ))

        fun translation(t: Vector3): Matrix4x4 = Matrix4x4(floatArrayOf(
            1f, 0f, 0f, t.x,
            0f, 1f, 0f, t.y,
            0f, 0f, 1f, t.z,
            0f, 0f, 0f, 1f
        ))

        fun scale(s: Vector3): Matrix4x4 = Matrix4x4(floatArrayOf(
            s.x, 0f, 0f, 0f,
            0f, s.y, 0f, 0f,
            0f, 0f, s.z, 0f,
            0f, 0f, 0f, 1f
        ))

        fun rotationEuler(yawDeg: Float, pitchDeg: Float, rollDeg: Float): Matrix4x4 {
            val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
            val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()
            val roll = Math.toRadians(rollDeg.toDouble()).toFloat()

            val cy = cos(yaw); val sy = sin(yaw)
            val cp = cos(pitch); val sp = sin(pitch)
            val cr = cos(roll); val sr = sin(roll)

            return Matrix4x4(floatArrayOf(
                cy * cr + sy * sp * sr, -cy * sr + sy * sp * cr, sy * cp, 0f,
                cp * sr, cp * cr, -sp, 0f,
                -sy * cr + cy * sp * sr, sy * sr + cy * sp * cr, cy * cp, 0f,
                0f, 0f, 0f, 1f
            ))
        }

        fun lookAt(eye: Vector3, target: Vector3, up: Vector3 = Vector3.UP): Matrix4x4 {
            var zAxis = (eye - target).normalized()
            if (zAxis.lengthSquared() < 0.00001f) {
                zAxis = Vector3(0f, 0f, 1f)
            }
            var safeUp = up
            if (abs(safeUp.dot(zAxis)) > 0.999f) {
                safeUp = if (abs(zAxis.y) > 0.999f) Vector3(0f, 0f, 1f) else Vector3(0f, 1f, 0f)
            }
            val xAxis = safeUp.cross(zAxis).normalized()
            val yAxis = zAxis.cross(xAxis).normalized()

            return Matrix4x4(floatArrayOf(
                xAxis.x, xAxis.y, xAxis.z, -xAxis.dot(eye),
                yAxis.x, yAxis.y, yAxis.z, -yAxis.dot(eye),
                zAxis.x, zAxis.y, zAxis.z, -zAxis.dot(eye),
                0f, 0f, 0f, 1f
            ))
        }

        fun perspective(fovYDeg: Float, aspect: Float, near: Float, far: Float): Matrix4x4 {
            val safeAspect = if (aspect <= 0.001f) 1.0f else aspect
            val safeNear = if (near <= 0.001f) 0.1f else near
            val safeFar = if (far <= safeNear) safeNear + 100f else far
            val tanHalfFov = tan(Math.toRadians((fovYDeg.coerceIn(10f, 170f) / 2.0))).toFloat()
            val f = 1f / max(0.0001f, tanHalfFov)
            val rangeInv = 1f / (safeNear - safeFar)

            return Matrix4x4(floatArrayOf(
                f / safeAspect, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, (safeFar + safeNear) * rangeInv, 2f * safeFar * safeNear * rangeInv,
                0f, 0f, -1f, 0f
            ))
        }

        fun ortho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4x4 {
            val rL = 1f / (right - left)
            val tB = 1f / (top - bottom)
            val fN = 1f / (far - near)
            return Matrix4x4(floatArrayOf(
                2f * rL, 0f, 0f, -(right + left) * rL,
                0f, 2f * tB, 0f, -(top + bottom) * tB,
                0f, 0f, -2f * fN, -(far + near) * fN,
                0f, 0f, 0f, 1f
            ))
        }
    }
}

object FastTrig {
    private const val TABLE_SIZE = 3600
    private val sinTable = FloatArray(TABLE_SIZE) { i ->
        sin(Math.toRadians(i * 360.0 / TABLE_SIZE)).toFloat()
    }

    fun sinDeg(deg: Float): Float {
        var d = deg % 360f
        if (d < 0) d += 360f
        val idx = ((d / 360f) * TABLE_SIZE).toInt().coerceIn(0, TABLE_SIZE - 1)
        return sinTable[idx]
    }

    fun cosDeg(deg: Float): Float = sinDeg(deg + 90f)
}

data class AABB(
    var min: Vector3 = Vector3(-0.5f, -0.5f, -0.5f),
    var max: Vector3 = Vector3(0.5f, 0.5f, 0.5f)
) {
    val center: Vector3 get() = (min + max) * 0.5f
    val size: Vector3 get() = max - min
    val halfExtents: Vector3 get() = size * 0.5f

    fun contains(point: Vector3): Boolean {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z
    }

    fun intersects(other: AABB): Boolean {
        return (min.x <= other.max.x && max.x >= other.min.x) &&
               (min.y <= other.max.y && max.y >= other.min.y) &&
               (min.z <= other.max.z && max.z >= other.min.z)
    }

    fun transform(matrix: Matrix4x4): AABB {
        val corners = arrayOf(
            Vector3(min.x, min.y, min.z),
            Vector3(max.x, min.y, min.z),
            Vector3(min.x, max.y, min.z),
            Vector3(max.x, max.y, min.z),
            Vector3(min.x, min.y, max.z),
            Vector3(max.x, min.y, max.z),
            Vector3(min.x, max.y, max.z),
            Vector3(max.x, max.y, max.z)
        )
        var newMin = Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var newMax = Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (c in corners) {
            val tc = matrix.transformPoint(c)
            newMin = Vector3(min(newMin.x, tc.x), min(newMin.y, tc.y), min(newMin.z, tc.z))
            newMax = Vector3(max(newMax.x, tc.x), max(newMax.y, tc.y), max(newMax.z, tc.z))
        }
        return AABB(newMin, newMax)
    }
}

data class Ray(val origin: Vector3, val direction: Vector3) {
    fun pointAt(t: Float): Vector3 = origin + direction * t
    fun getPoint(t: Float): Vector3 = pointAt(t)

    fun intersectsAABB(box: AABB): Float? {
        var tmin = (box.min.x - origin.x) / (if (direction.x != 0f) direction.x else 0.00001f)
        var tmax = (box.max.x - origin.x) / (if (direction.x != 0f) direction.x else 0.00001f)
        if (tmin > tmax) { val tmp = tmin; tmin = tmax; tmax = tmp }

        var tymin = (box.min.y - origin.y) / (if (direction.y != 0f) direction.y else 0.00001f)
        var tymax = (box.max.y - origin.y) / (if (direction.y != 0f) direction.y else 0.00001f)
        if (tymin > tymax) { val tmp = tymin; tymin = tymax; tymax = tmp }

        if (tmin > tymax || tymin > tmax) return null
        if (tymin > tmin) tmin = tymin
        if (tymax < tmax) tmax = tymax

        var tzmin = (box.min.z - origin.z) / (if (direction.z != 0f) direction.z else 0.00001f)
        var tzmax = (box.max.z - origin.z) / (if (direction.z != 0f) direction.z else 0.00001f)
        if (tzmin > tzmax) { val tmp = tzmin; tzmin = tzmax; tzmax = tmp }

        if (tmin > tzmax || tzmin > tmax) return null
        if (tzmin > tmin) tmin = tzmin
        if (tzmax < tmax) tmax = tzmax

        return if (tmin >= 0f) tmin else if (tmax >= 0f) tmax else null
    }
}
