@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord

import io.github.erkko68.filament.Color
import io.github.erkko68.filament.utils.Float3
import io.github.erkko68.filament.utils.Ray
import io.github.erkko68.filament.utils.length
import io.github.erkko68.filament.utils.normalize
import io.github.erkko68.koord.camera.ArCamera
import io.github.erkko68.koord.hit.HitResult
import io.github.erkko68.koord.interop.koord_frame_camera
import io.github.erkko68.koord.interop.koord_frame_display_transform
import io.github.erkko68.koord.interop.koord_frame_light_estimate
import io.github.erkko68.koord.interop.koord_frame_raycast_query
import io.github.erkko68.koord.interop.koord_frame_timestamp
import io.github.erkko68.koord.light.LightEstimate
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.vectorOf
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARRaycastQuery
import platform.ARKit.ARRaycastResult
import platform.ARKit.ARRaycastTarget
import platform.ARKit.ARRaycastTargetAlignment
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGPointApplyAffineTransform
import platform.CoreGraphics.CGPointMake

// ARKit's ambient intensity for a neutrally lit scene, in lumens.
private const val NEUTRAL_LUMENS = 1000.0

/**
 * Holds ARKit's `ARFrame` as a pointer the session retains until the next
 * [ArSession.update], not as a Kotlin reference: see `arkit.def`.
 */
actual class ArFrame internal constructor(private val session: ArSession, private val frame: COpaquePointer) {
    /** The `ARFrame`, valid until the session's next update. */
    internal val handle: COpaquePointer
        get() {
            check(session.isCurrent(frame)) { "This frame is no longer valid: ArSession.update() has been called since" }
            return frame
        }

    actual val timestampNanos: Long get() = (koord_frame_timestamp(handle) * 1e9).toLong()

    actual val camera: ArCamera
        get() = ArCamera(koord_frame_camera(handle)!!, session.orientation, session.viewportSize())

    actual val lightEstimate: LightEstimate?
        get() = koord_frame_light_estimate(handle)?.let { estimate ->
            val color = Color.cct(estimate.ambientColorTemperature.toFloat())
            LightEstimate(
                intensity = (estimate.ambientIntensity / NEUTRAL_LUMENS).toFloat(),
                color = Float3(color[0], color[1], color[2]),
            )
        }

    actual fun hitTest(xPx: Float, yPx: Float): List<HitResult> {
        // ARKit wants the point in the camera image, normalised; the display transform maps the image to the viewport.
        val viewportToImage = CGAffineTransformInvert(
            koord_frame_display_transform(handle, session.orientation, session.viewportSize()),
        )
        val point = CGPointApplyAffineTransform(
            CGPointMake((xPx / session.widthPx).toDouble(), (yPx / session.heightPx).toDouble()),
            viewportToImage,
        )
        return raycast(koord_frame_raycast_query(handle, point) ?: return emptyList())
    }

    actual fun hitTest(ray: Ray): List<HitResult> {
        val direction = normalize(ray.direction)
        return raycast(
            ARRaycastQuery(
                origin = vectorOf(ray.origin.x, ray.origin.y, ray.origin.z, 0f),
                direction = vectorOf(direction.x, direction.y, direction.z, 0f),
                allowingTarget = ARRaycastTarget.ARRaycastTargetExistingPlaneGeometry,
                alignment = ARRaycastTargetAlignment.ARRaycastTargetAlignmentAny,
            ),
        )
    }

    /** ARKit already sorts nearest first, and this target only reports hits inside a detected plane's geometry. */
    private fun raycast(query: ARRaycastQuery): List<HitResult> {
        val origin = Float3(query.origin.getFloatAt(0), query.origin.getFloatAt(1), query.origin.getFloatAt(2))
        return session.session.raycast(query).mapNotNull { result ->
            result as ARRaycastResult
            val plane = result.anchor as? ARPlaneAnchor ?: return@mapNotNull null
            val transform = result.worldTransform.toMat4()
            HitResult(session, transform, length(transform.position - origin), plane)
        }
    }
}
