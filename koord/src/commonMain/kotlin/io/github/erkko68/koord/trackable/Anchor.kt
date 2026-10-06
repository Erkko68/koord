package io.github.erkko68.koord.trackable

import io.github.erkko68.koord.math.Mat4
import io.github.erkko68.koord.TrackingState

/**
 * A fixed pose in the real world that the session keeps correcting as its
 * understanding of the world improves: ARCore's `Anchor`, ARKit's `ARAnchor`.
 *
 * Attach content to an anchor rather than to a raw transform, and re-read
 * [transform] every frame.
 *
 * Create one with [io.github.erkko68.koord.ArSession.createAnchor] or
 * [io.github.erkko68.koord.hit.HitResult.createAnchor].
 */
expect class Anchor {

    /** Current pose in world space. Changes over time. */
    val transform: Mat4

    /**
     * Whether [transform] can be trusted right now. ARKit does not track plain
     * anchors individually, so on iOS this follows the camera's state.
     */
    val trackingState: TrackingState

    /**
     * Removes the anchor from its session. It stops updating and must not be
     * used afterwards.
     */
    fun detach()
}
