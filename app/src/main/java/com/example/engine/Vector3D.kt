package com.example.engine

import kotlin.math.*

/**
 * High-performance 3D Vector and Rotation math for Isometric BMX Game.
 */
data class Vector3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector3D(x / scalar, y / scalar, z / scalar) else Vector3D()

    fun plusAssign(other: Vector3D) {
        x += other.x
        y += other.y
        z += other.z
    }

    fun minusAssign(other: Vector3D) {
        x -= other.x
        y -= other.y
        z -= other.z
    }

    fun timesAssign(scalar: Float) {
        x *= scalar
        y *= scalar
        z *= scalar
    }

    fun set(newX: Float, newY: Float, newZ: Float) {
        x = newX
        y = newY
        z = newZ
    }

    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0.0001f) Vector3D(x / len, y / len, z / len) else Vector3D(0f, 0f, 0f)
    }

    fun distanceTo(other: Vector3D): Float = sqrt(
        (x - other.x) * (x - other.x) +
        (y - other.y) * (y - other.y) +
        (z - other.z) * (z - other.z)
    )

    /**
     * Rotates vector by Pitch (around Y axis), Roll (around X axis), and Yaw (around Z axis) in degrees.
     */
    fun rotateEuler(pitchDeg: Float, rollDeg: Float, yawDeg: Float): Vector3D {
        var vx = x
        var vy = y
        var vz = z

        // 1. Roll around X (lateral tilt)
        if (rollDeg != 0f) {
            val rRad = rollDeg * Math.PI.toFloat() / 180f
            val cosR = cos(rRad)
            val sinR = sin(rRad)
            val ny = vy * cosR - vz * sinR
            val nz = vy * sinR + vz * cosR
            vy = ny
            vz = nz
        }

        // 2. Pitch around Y (nose up / nose down / flips)
        if (pitchDeg != 0f) {
            val pRad = pitchDeg * Math.PI.toFloat() / 180f
            val cosP = cos(pRad)
            val sinP = sin(pRad)
            val nx = vx * cosP + vz * sinP
            val nz = -vx * sinP + vz * cosP
            vx = nx
            vz = nz
        }

        // 3. Yaw around Z (heading / tailwhip spins)
        if (yawDeg != 0f) {
            val yRad = yawDeg * Math.PI.toFloat() / 180f
            val cosY = cos(yRad)
            val sinY = sin(yRad)
            val nx = vx * cosY - vy * sinY
            val ny = vx * sinY + vy * cosY
            vx = nx
            vy = ny
        }

        return Vector3D(vx, vy, vz)
    }

    companion object {
        val ZERO = Vector3D(0f, 0f, 0f)
        val FORWARD = Vector3D(1f, 0f, 0f)
        val RIGHT = Vector3D(0f, 1f, 0f)
        val UP = Vector3D(0f, 0f, 1f)
    }
}
