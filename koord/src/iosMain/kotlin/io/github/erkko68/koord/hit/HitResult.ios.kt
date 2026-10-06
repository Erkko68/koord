package io.github.erkko68.koord.hit

import io.github.erkko68.koord.math.Mat4
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane
import platform.ARKit.ARPlaneAnchor

actual class HitResult internal constructor(
    private val session: ArSession,
    actual val transform: Mat4,
    actual val distance: Float,
    private val planeAnchor: ARPlaneAnchor,
) {
    actual val plane: Plane get() = Plane(session, planeAnchor)

    actual fun createAnchor(): Anchor = session.createAnchor(transform)
}
