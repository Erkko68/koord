package io.github.erkko68.koord.hit

import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane

/**
 * One intersection of a hit-test ray with the tracked world: ARCore's
 * `HitResult`, ARKit's `ARRaycastResult`. Returned by
 * [io.github.erkko68.koord.ArFrame.hitTest].
 */
expect class HitResult {

    /** Pose of the hit point in world space. */
    val transform: Mat4

    /** Distance from the ray origin to the hit point, in metres. */
    val distance: Float

    /** The plane that was hit. */
    val plane: Plane

    /** Creates an anchor at the hit point. */
    fun createAnchor(): Anchor
}
