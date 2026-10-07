package io.github.erkko68.koord

import io.github.erkko68.koord.math.Ray
import io.github.erkko68.koord.camera.ArCamera
import io.github.erkko68.koord.depth.DepthImage
import io.github.erkko68.koord.hit.HitResult
import io.github.erkko68.koord.hit.HitTarget
import io.github.erkko68.koord.light.LightEstimate
import io.github.erkko68.koord.trackable.Plane

/**
 * A snapshot of the session at one camera image: ARCore's `Frame`, ARKit's
 * `ARFrame`. Obtained from [ArSession.update].
 *
 * A frame is only meant to be used until the next [ArSession.update]; don't
 * hold on to it.
 *
 * Hit tests only report hits on detected planes: inside their [Plane.polygon]
 * boundary, or anywhere on them with [HitTarget.PLANE_INFINITE]. ARKit's
 * estimated planes and ARCore's instant placement, feature point and depth
 * hits are left out so both platforms answer the same way; nothing can be hit
 * until plane detection has found a surface.
 */
expect class ArFrame {

    /** Capture time of the camera image, in nanoseconds on a monotonic clock. */
    val timestampNanos: Long

    /** The camera as it was when this frame was captured. */
    val camera: ArCamera

    /**
     * Ambient light of the real scene, or `null` if the session has no valid
     * estimate for this frame or [ArConfig.lightEstimation] is off.
     */
    val lightEstimate: LightEstimate?

    /**
     * Where the viewport set with [ArSession.setDisplayGeometry] falls in the
     * camera image, which accounts for the display rotation and for the crop
     * to the viewport's shape: the image's texture coordinates at three
     * viewport corners, as `[u, v]` pairs, bottom-left, bottom-right, then
     * top-left. ARCore's `Frame.transformCoordinates2d`, ARKit's
     * `ARFrame.displayTransform`.
     *
     * The image itself has no common shape and is read per platform:
     * `cameraTextureName` on Android, `cameraImage` on iOS.
     */
    val cameraImageUv: FloatArray

    /**
     * The distance from the camera to the real scene at each pixel, or `null`
     * if [ArConfig.depth] is off, the device cannot measure depth, or the
     * session has no estimate for this frame yet. ARCore's
     * `Frame.acquireDepthImage16Bits`, ARKit's `ARFrame.smoothedSceneDepth`;
     * both are smoothed over the last frames.
     *
     * Reading it copies the image, so read it once per frame.
     *
     * **On iOS this needs a LiDAR scanner**, which only the Pro devices have:
     * iPhone 12 Pro and later Pro models, and iPad Pro from 2020 on. On every
     * other iPhone and iPad it is always `null`, since ARKit has no depth
     * without the scanner. The scanner measures up to about 5 m.
     *
     * On Android, ARCore estimates depth from the camera's motion on most
     * devices it supports, with no depth sensor needed. The first image only
     * comes after the device has moved a little, it is most accurate between
     * 0.5 m and 5 m, and it is wrong on moving objects.
     */
    val depthImage: DepthImage?

    /**
     * Casts a ray from a point on screen into the tracked world.
     *
     * @param xPx horizontal position in viewport pixels, origin top-left
     * @param yPx vertical position in viewport pixels, origin top-left
     * @param target how much of each plane can be hit
     * @return hits sorted nearest first; empty if nothing was hit
     */
    fun hitTest(xPx: Float, yPx: Float, target: HitTarget = HitTarget.PLANE_POLYGON): List<HitResult>

    /**
     * Casts an arbitrary world-space ray into the tracked world.
     *
     * @param ray origin and direction in world space; the direction need not
     *   be normalised
     * @param target how much of each plane can be hit
     * @return hits sorted nearest first; empty if nothing was hit
     */
    fun hitTest(ray: Ray, target: HitTarget = HitTarget.PLANE_POLYGON): List<HitResult>
}
