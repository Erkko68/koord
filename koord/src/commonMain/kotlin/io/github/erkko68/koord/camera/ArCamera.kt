package io.github.erkko68.koord.camera

import io.github.erkko68.koord.math.Float2
import io.github.erkko68.koord.math.Mat4
import io.github.erkko68.koord.TrackingFailureReason
import io.github.erkko68.koord.TrackingState

/**
 * The device camera at the time of one [io.github.erkko68.koord.ArFrame]:
 * ARCore's `Camera`, ARKit's `ARCamera`.
 *
 * Feeds a renderer's camera directly: [transform] is its model matrix,
 * [projectionMatrix] its projection.
 */
expect class ArCamera {

    /**
     * Camera pose in world space, oriented to the display rotation set with
     * [io.github.erkko68.koord.ArSession.setDisplayGeometry]. The inverse of
     * [viewMatrix].
     */
    val transform: Mat4

    /** World-to-camera matrix for the current display rotation. */
    val viewMatrix: Mat4

    /**
     * Projection matrix matching the camera image as shown in the current
     * viewport.
     *
     * @param near near clip plane distance in metres
     * @param far far clip plane distance in metres
     */
    fun projectionMatrix(near: Float, far: Float): Mat4

    /** Intrinsics of the camera image, unrotated. */
    val intrinsics: CameraIntrinsics

    /** Whether the camera pose can be trusted right now. */
    val trackingState: TrackingState

    /** Why [trackingState] is not [TrackingState.TRACKING]. */
    val trackingFailureReason: TrackingFailureReason
}

/**
 * Pinhole parameters of the camera image, all in pixels.
 *
 * @property focalLength focal length along the image's x and y axes
 * @property principalPoint optical centre, from the image's top-left
 * @property imageWidth width of the image these parameters describe
 * @property imageHeight height of the image these parameters describe
 */
data class CameraIntrinsics(
    val focalLength: Float2,
    val principalPoint: Float2,
    val imageWidth: Int,
    val imageHeight: Int,
)
