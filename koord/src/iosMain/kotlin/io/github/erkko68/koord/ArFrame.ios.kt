@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord

import io.github.erkko68.koord.math.Float3
import io.github.erkko68.koord.math.Ray
import io.github.erkko68.koord.math.colorTemperatureToRgb
import io.github.erkko68.koord.math.length
import io.github.erkko68.koord.math.minus
import io.github.erkko68.koord.camera.ArCamera
import io.github.erkko68.koord.hit.HitResult
import io.github.erkko68.koord.hit.HitTarget
import io.github.erkko68.koord.interop.koord_frame_camera
import io.github.erkko68.koord.interop.koord_frame_camera_image
import io.github.erkko68.koord.interop.koord_frame_display_transform
import io.github.erkko68.koord.interop.koord_frame_light_estimate
import io.github.erkko68.koord.interop.koord_frame_raycast_query
import io.github.erkko68.koord.interop.koord_frame_timestamp
import io.github.erkko68.koord.light.LightEstimate
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.cinterop.vectorOf
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARRaycastQuery
import platform.ARKit.ARRaycastResult
import platform.ARKit.ARRaycastTarget
import platform.ARKit.ARRaycastTargetAlignment
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGPointApplyAffineTransform
import platform.CoreGraphics.CGPointMake
import platform.CoreVideo.CVPixelBufferRef

// ARKit's ambient intensity for a neutrally lit scene, in lumens.
private const val NEUTRAL_LUMENS = 1000.0

// Three viewport corners in ARKit's viewport space (origin top-left): bottom-left, bottom-right, top-left.
private val VIEWPORT_CORNERS = listOf(0.0 to 1.0, 1.0 to 1.0, 0.0 to 0.0)

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
            LightEstimate(
                intensity = (estimate.ambientIntensity / NEUTRAL_LUMENS).toFloat(),
                color = colorTemperatureToRgb(estimate.ambientColorTemperature.toFloat()),
            )
        }

    /**
     * The camera image: ARKit's `ARFrame.capturedImage`, a full-range BT.601
     * YCbCr pixel buffer with a luma plane and a chroma plane. The frame owns
     * it, so it is only valid until the next [ArSession.update]; retain it to
     * keep it longer, and not for long, since ARKit stops delivering images
     * while too many are held. iOS only; Android writes the image to
     * `ArFrame.cameraTextureName`.
     */
    val cameraImage: CVPixelBufferRef get() = koord_frame_camera_image(handle)!!

    actual val cameraImageUv: FloatArray
        get() {
            val viewportToImage = CGAffineTransformInvert(
                koord_frame_display_transform(handle, session.orientation, session.viewportSize()),
            )
            val uv = FloatArray(6)
            VIEWPORT_CORNERS.forEachIndexed { i, (x, y) ->
                CGPointApplyAffineTransform(CGPointMake(x, y), viewportToImage).useContents {
                    uv[i * 2] = this.x.toFloat()
                    uv[i * 2 + 1] = this.y.toFloat()
                }
            }
            return uv
        }

    actual fun hitTest(xPx: Float, yPx: Float, target: HitTarget): List<HitResult> {
        // ARKit wants the point in the camera image, normalised; the display transform maps the image to the viewport.
        val viewportToImage = CGAffineTransformInvert(
            koord_frame_display_transform(handle, session.orientation, session.viewportSize()),
        )
        val point = CGPointApplyAffineTransform(
            CGPointMake((xPx / session.widthPx).toDouble(), (yPx / session.heightPx).toDouble()),
            viewportToImage,
        )
        return raycast(koord_frame_raycast_query(handle, point, target.toArKit()) ?: return emptyList())
    }

    actual fun hitTest(ray: Ray, target: HitTarget): List<HitResult> {
        val direction = ray.direction
        val scale = 1f / direction.length
        return raycast(
            ARRaycastQuery(
                origin = vectorOf(ray.origin.x, ray.origin.y, ray.origin.z, 0f),
                direction = vectorOf(direction.x * scale, direction.y * scale, direction.z * scale, 0f),
                allowingTarget = target.toArKit(),
                alignment = ARRaycastTargetAlignment.ARRaycastTargetAlignmentAny,
            ),
        )
    }

    private fun HitTarget.toArKit() = when (this) {
        HitTarget.PLANE_POLYGON -> ARRaycastTarget.ARRaycastTargetExistingPlaneGeometry
        HitTarget.PLANE_INFINITE -> ARRaycastTarget.ARRaycastTargetExistingPlaneInfinite
    }

    /** ARKit already sorts nearest first, and both targets only report hits on a detected plane. */
    private fun raycast(query: ARRaycastQuery): List<HitResult> {
        val origin = Float3(query.origin.getFloatAt(0), query.origin.getFloatAt(1), query.origin.getFloatAt(2))
        return session.session.raycast(query).mapNotNull { result ->
            result as ARRaycastResult
            val plane = result.anchor as? ARPlaneAnchor ?: return@mapNotNull null
            val transform = result.worldTransform.toMat4()
            HitResult(session, transform, (transform.position - origin).length, plane)
        }
    }
}
