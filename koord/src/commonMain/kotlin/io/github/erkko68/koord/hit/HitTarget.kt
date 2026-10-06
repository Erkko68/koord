package io.github.erkko68.koord.hit

/**
 * How much of a detected plane a hit test can hit. Pass it to
 * [io.github.erkko68.koord.ArFrame.hitTest].
 */
enum class HitTarget {

    /**
     * Only inside the plane's [io.github.erkko68.koord.trackable.Plane.polygon]:
     * ARKit's `existingPlaneGeometry`, ARCore's hits filtered with
     * `Plane.isPoseInPolygon`.
     */
    PLANE_POLYGON,

    /**
     * Anywhere on the plane extended without limit: ARKit's
     * `existingPlaneInfinite`, ARCore's plane hits unfiltered. It reaches the
     * parts of a surface detection has not covered yet, such as a plain wall
     * of which only a patch was found. A plane extended this far also passes
     * behind and through real objects, so prefer a [PLANE_POLYGON] hit when
     * there is one and check [HitResult.distance].
     */
    PLANE_INFINITE,
}
