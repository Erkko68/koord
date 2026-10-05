package io.github.erkko68.koord

import com.google.ar.core.Frame
import io.github.erkko68.filament.utils.Float3
import io.github.erkko68.filament.utils.Ray
import io.github.erkko68.koord.camera.ArCamera
import io.github.erkko68.koord.hit.HitResult
import io.github.erkko68.koord.light.LightEstimate
import com.google.ar.core.HitResult as ArCoreHitResult
import com.google.ar.core.LightEstimate as ArCoreLightEstimate
import com.google.ar.core.Plane as ArCorePlane

// Pixel intensity of a neutrally lit scene, in gamma space; the constant
// ARCore's own samples divide by.
private const val MIDDLE_GRAY_GAMMA = 0.466f

actual class ArFrame internal constructor(private val frame: Frame) {
    actual val timestampNanos: Long get() = frame.timestamp

    actual val camera: ArCamera get() = ArCamera(frame.camera)

    actual val lightEstimate: LightEstimate?
        get() {
            val estimate = frame.lightEstimate
            if (estimate.state != ArCoreLightEstimate.State.VALID) return null
            // [r, g, b, pixel intensity]
            val correction = FloatArray(4).also { estimate.getColorCorrection(it, 0) }
            return LightEstimate(
                intensity = estimate.pixelIntensity / MIDDLE_GRAY_GAMMA,
                color = Float3(correction[0], correction[1], correction[2]),
            )
        }

    actual fun hitTest(xPx: Float, yPx: Float): List<HitResult> = frame.hitTest(xPx, yPx).onPlanes()

    actual fun hitTest(ray: Ray): List<HitResult> = frame.hitTest(
        floatArrayOf(ray.origin.x, ray.origin.y, ray.origin.z), 0,
        floatArrayOf(ray.direction.x, ray.direction.y, ray.direction.z), 0,
    ).onPlanes()

    /** ARCore already sorts nearest first; keep only hits inside a detected plane's polygon. */
    private fun List<ArCoreHitResult>.onPlanes(): List<HitResult> = mapNotNull { hit ->
        val plane = hit.trackable as? ArCorePlane
        if (plane != null && plane.isPoseInPolygon(hit.hitPose)) HitResult(hit, plane) else null
    }
}
