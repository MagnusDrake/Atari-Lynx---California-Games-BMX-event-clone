package com.example.engine

import kotlin.math.*

/**
 * Lightweight, allocation-friendly 2D vector for BMX physics calculations.
 */
data class Vector2D(
    var x: Float = 0f,
    var y: Float = 0f
) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector2D(x / scalar, y / scalar) else Vector2D()

    fun plusAssign(other: Vector2D) {
        x += other.x
        y += other.y
    }

    fun minusAssign(other: Vector2D) {
        x -= other.x
        y -= other.y
    }

    fun timesAssign(scalar: Float) {
        x *= scalar
        y *= scalar
    }

    fun set(newX: Float, newY: Float) {
        x = newX
        y = newY
    }

    fun dot(other: Vector2D): Float = x * other.x + y * other.y

    fun length(): Float = hypot(x, y)
    fun lengthSquared(): Float = x * x + y * y

    fun normalized(): Vector2D {
        val len = length()
        return if (len > 0.0001f) Vector2D(x / len, y / len) else Vector2D(0f, 0f)
    }

    fun distanceTo(other: Vector2D): Float = hypot(x - other.x, y - other.y)

    fun angleDegrees(): Float = atan2(y, x) * 180f / Math.PI.toFloat()

    fun rotate(degrees: Float): Vector2D {
        val rad = degrees * Math.PI.toFloat() / 180f
        val cosA = cos(rad)
        val sinA = sin(rad)
        return Vector2D(
            x * cosA - y * sinA,
            x * sinA + y * cosA
        )
    }

    companion object {
        val ZERO = Vector2D(0f, 0f)
        val UP = Vector2D(0f, -1f)
        val DOWN = Vector2D(0f, 1f)
        val LEFT = Vector2D(-1f, 0f)
        val RIGHT = Vector2D(1f, 0f)

        fun fromAngle(degrees: Float, magnitude: Float = 1f): Vector2D {
            val rad = degrees * Math.PI.toFloat() / 180f
            return Vector2D(cos(rad) * magnitude, sin(rad) * magnitude)
        }
    }
}
