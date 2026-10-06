package io.github.erkko68.koord.trackable

import io.github.erkko68.filament.utils.Float2
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingState

/**
 * A flat real-world surface the session has detected: ARCore's `Plane`,
 * ARKit's `ARPlaneAnchor`. Planes grow and move as more of the surface is
 * seen, so re-read the properties every frame.
 *
 * Listed by [io.github.erkko68.koord.ArSession.planes].
 */
expect class Plane {

    /**
     * Pose of the plane's centre in world space. The local Y axis is the
     * plane's normal; X and Z lie in the plane.
     */
    val transform: Mat4

    /** Size along the plane's local X axis, in metres. */
    val extentX: Float

    /** Size along the plane's local Z axis, in metres. */
    val extentZ: Float

    /**
     * Boundary of the detected surface in metres, in the plane's local space
     * (see [transform]): each vertex holds local X in `x` and local Z in `y`.
     * The polygon is convex on Android and may be concave on iOS.
     */
    val polygon: List<Float2>

    /** Whether the surface is horizontal or vertical. */
    val alignment: Alignment

    /**
     * Whether the plane is currently being tracked. ARKit removes a plane it
     * stops tracking, so on iOS this is never [TrackingState.LIMITED].
     */
    val trackingState: TrackingState

    /** Orientation of a [Plane] relative to gravity. */
    enum class Alignment { HORIZONTAL, VERTICAL }
}
