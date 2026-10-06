package io.github.erkko68.koord.math

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class MathTest {
    // A quarter turn about Y, then a translation.
    private val pose = Mat4(floatArrayOf(0f, 0f, -1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 2f, 3f, 1f))

    private fun assertClose(expected: FloatArray, actual: FloatArray) =
        assertTrue(
            expected.size == actual.size && expected.indices.all { abs(expected[it] - actual[it]) < 1e-5f },
            "expected ${expected.toList()}, got ${actual.toList()}",
        )

    @Test
    fun aRigidTransformTimesItsInverseIsTheIdentity() {
        val identity = FloatArray(16) { if (it % 5 == 0) 1f else 0f }
        assertClose(identity, (pose * pose.inverseRigid()).toFloatArray())
        assertClose(identity, (pose.inverseRigid() * pose).toFloatArray())
    }

    @Test
    fun aQuarterTurnAboutYAsAQuaternion() {
        val half = sqrt(0.5f)
        assertClose(floatArrayOf(0f, half, 0f, half), pose.rotationQuaternion())
        // A half turn about X has no trace to divide by, which takes another branch.
        val halfTurn = Mat4(floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 1f))
        assertClose(floatArrayOf(1f, 0f, 0f, 0f), halfTurn.rotationQuaternion())
    }

    @Test
    fun colourTemperature() {
        // A black body at daylight's temperature is a little short of green next to D65 white.
        val daylight = colorTemperatureToRgb(6504f)
        assertTrue(minOf(daylight.x, daylight.y, daylight.z) > 0.9f, "6504 K should be near white, got $daylight")
        val candle = colorTemperatureToRgb(2000f)
        assertTrue(candle.x == 1f && candle.y < 0.6f && candle.z < 0.1f, "2000 K should be orange, got $candle")
    }
}
