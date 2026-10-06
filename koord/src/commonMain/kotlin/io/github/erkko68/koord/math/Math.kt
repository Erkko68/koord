package io.github.erkko68.koord.math

import kotlin.math.max
import kotlin.math.sqrt

/** Two values: a point in a plane's local space, or a pair of pixel measures. */
data class Float2(val x: Float, val y: Float)

/** Three values: a point or direction in world space, or a linear RGB colour. */
data class Float3(val x: Float, val y: Float, val z: Float)

/**
 * A half-line in world space, for [io.github.erkko68.koord.ArFrame.hitTest].
 *
 * @property origin where the ray starts
 * @property direction where it points; need not be normalised
 */
data class Ray(val origin: Float3, val direction: Float3)

/**
 * A 4×4 transform, the type of every pose and projection in the API.
 *
 * It only carries the values. They are in column-major order, the layout of
 * ARCore's matrices, ARKit's `simd_float4x4` and every renderer's, so
 * [toFloatArray] goes straight to one; do the math with the renderer's own
 * types.
 *
 * @param values 16 values in column-major order; copied
 */
class Mat4(values: FloatArray) {
    private val m = values.copyOf()

    init {
        require(m.size == 16) { "A Mat4 takes 16 values, got ${m.size}" }
    }

    /** The 16 values in column-major order, as a new array. */
    fun toFloatArray(): FloatArray = m.copyOf()

    /** The translation: where the transform puts its origin. */
    val position: Float3 get() = Float3(m[12], m[13], m[14])

    internal operator fun times(other: Mat4): Mat4 {
        val o = other.m
        return Mat4(
            FloatArray(16) {
                val row = it % 4
                val column = it - row
                m[row] * o[column] + m[4 + row] * o[column + 1] + m[8 + row] * o[column + 2] + m[12 + row] * o[column + 3]
            },
        )
    }

    /** The inverse, for a rigid transform (rotation and translation only): the rotation transposed, the translation undone. */
    internal fun inverseRigid(): Mat4 = Mat4(
        floatArrayOf(
            m[0], m[4], m[8], 0f,
            m[1], m[5], m[9], 0f,
            m[2], m[6], m[10], 0f,
            -(m[0] * m[12] + m[1] * m[13] + m[2] * m[14]),
            -(m[4] * m[12] + m[5] * m[13] + m[6] * m[14]),
            -(m[8] * m[12] + m[9] * m[13] + m[10] * m[14]),
            1f,
        ),
    )

    /** The rotation of a rigid transform as a unit quaternion, `[x, y, z, w]`. */
    internal fun rotationQuaternion(): FloatArray {
        // Divide by the largest of the four components, so the division is never by something near zero.
        val trace = m[0] + m[5] + m[10]
        return when {
            trace > 0f -> {
                val s = sqrt(trace + 1f) * 2f
                floatArrayOf((m[6] - m[9]) / s, (m[8] - m[2]) / s, (m[1] - m[4]) / s, s / 4f)
            }
            m[0] > m[5] && m[0] > m[10] -> {
                val s = sqrt(1f + m[0] - m[5] - m[10]) * 2f
                floatArrayOf(s / 4f, (m[4] + m[1]) / s, (m[8] + m[2]) / s, (m[6] - m[9]) / s)
            }
            m[5] > m[10] -> {
                val s = sqrt(1f + m[5] - m[0] - m[10]) * 2f
                floatArrayOf((m[4] + m[1]) / s, s / 4f, (m[9] + m[6]) / s, (m[8] - m[2]) / s)
            }
            else -> {
                val s = sqrt(1f + m[10] - m[0] - m[5]) * 2f
                floatArrayOf((m[8] + m[2]) / s, (m[9] + m[6]) / s, s / 4f, (m[1] - m[4]) / s)
            }
        }
    }
}

internal operator fun Float3.minus(other: Float3) = Float3(x - other.x, y - other.y, z - other.z)

internal val Float3.length: Float get() = sqrt(x * x + y * y + z * z)

/**
 * Linear sRGB of a black body at [kelvin], scaled so its largest channel is
 * 1, by Krystek's approximation of the Planckian locus. ARKit reports the
 * ambient light as a temperature; the API reports a colour.
 */
internal fun colorTemperatureToRgb(kelvin: Float): Float3 {
    // Temperature to CIE 1960 (u, v), then to CIE xy.
    val k2 = kelvin * kelvin
    val u = (0.860117757f + 1.54118254e-4f * kelvin + 1.28641212e-7f * k2) /
        (1f + 8.42420235e-4f * kelvin + 7.08145163e-7f * k2)
    val v = (0.317398726f + 4.22806245e-5f * kelvin + 4.20481691e-8f * k2) /
        (1f - 2.89741816e-5f * kelvin + 1.61456053e-7f * k2)
    val d = 1f / (2f * u - 8f * v + 4f)
    val x = 3f * u * d
    val y = 2f * v * d
    // xyY with Y = 1 to XYZ, then to linear sRGB.
    val cieX = x / y
    val cieZ = (1f - x - y) / y
    val r = 3.2404542f * cieX - 1.5371385f - 0.4985314f * cieZ
    val g = -0.9692660f * cieX + 1.8760108f + 0.0415560f * cieZ
    val b = 0.0556434f * cieX - 0.2040259f + 1.0572252f * cieZ
    val largest = max(1e-5f, max(r, max(g, b)))
    return Float3((r / largest).coerceIn(0f, 1f), (g / largest).coerceIn(0f, 1f), (b / largest).coerceIn(0f, 1f))
}
