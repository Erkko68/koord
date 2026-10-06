package io.github.erkko68.koord

import com.google.ar.core.Frame
import com.google.ar.core.Session
import io.github.erkko68.filament.utils.Float3
import io.github.erkko68.filament.utils.Ray
import io.github.erkko68.koord.camera.ArCamera
import io.github.erkko68.koord.hit.HitResult
import io.github.erkko68.koord.hit.HitTarget
import io.github.erkko68.koord.light.LightEstimate
import io.github.erkko68.koord.trackable.reportedPlanes
import com.google.ar.core.HitResult as ArCoreHitResult
import com.google.ar.core.LightEstimate as ArCoreLightEstimate
import com.google.ar.core.Plane as ArCorePlane

// Pixel intensity of a neutrally lit scene, in gamma space; the constant
// ARCore's own samples divide by.
private const val MIDDLE_GRAY_GAMMA = 0.466f

actual class ArFrame internal constructor(private val session: Session, internal val frame: Frame) {
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

    actual fun hitTest(xPx: Float, yPx: Float, target: HitTarget): List<HitResult> =
        frame.hitTest(xPx, yPx).onPlanes(target, frame.camera.pose.translation)

    actual fun hitTest(ray: Ray, target: HitTarget): List<HitResult> {
        val origin = floatArrayOf(ray.origin.x, ray.origin.y, ray.origin.z)
        return frame.hitTest(origin, 0, floatArrayOf(ray.direction.x, ray.direction.y, ray.direction.z), 0)
            .onPlanes(target, origin)
    }

    /**
     * ARCore already sorts nearest first and hits each plane wherever the ray
     * crosses it, inside its polygon or not. Keep the hits [target] allows on
     * a plane [ArSession.planes] lists, when the ray starts at [origin] on the
     * side its normal points to: a hit on the back of a plane went through the
     * real surface to get there.
     */
    private fun List<ArCoreHitResult>.onPlanes(target: HitTarget, origin: FloatArray): List<HitResult> {
        if (isEmpty()) return emptyList()
        val reported = session.reportedPlanes()
        return mapNotNull { hit ->
            val plane = hit.trackable as? ArCorePlane ?: return@mapNotNull null
            val inFront = plane.centerPose.inverse().transformPoint(origin)[1] > 0f
            val onTarget = target == HitTarget.PLANE_INFINITE || plane.isPoseInPolygon(hit.hitPose)
            if (inFront && onTarget && plane in reported) HitResult(hit, plane) else null
        }
    }
}
