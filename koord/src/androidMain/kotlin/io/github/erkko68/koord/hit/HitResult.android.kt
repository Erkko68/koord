package io.github.erkko68.koord.hit

import io.github.erkko68.koord.math.Mat4
import io.github.erkko68.koord.toMat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane
import com.google.ar.core.HitResult as ArCoreHitResult
import com.google.ar.core.Plane as ArCorePlane

actual class HitResult internal constructor(
    private val hit: ArCoreHitResult,
    private val arCorePlane: ArCorePlane,
) {
    actual val transform: Mat4 get() = hit.hitPose.toMat4()

    actual val distance: Float get() = hit.distance

    actual val plane: Plane get() = Plane(arCorePlane)

    actual fun createAnchor(): Anchor = Anchor(hit.createAnchor())
}
