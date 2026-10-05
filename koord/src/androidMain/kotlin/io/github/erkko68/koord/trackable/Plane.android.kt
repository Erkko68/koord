package io.github.erkko68.koord.trackable

import io.github.erkko68.filament.utils.Float2
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.toKoord
import io.github.erkko68.koord.toMat4
import com.google.ar.core.Plane as ArCorePlane

actual class Plane internal constructor(private val plane: ArCorePlane) {
    actual val transform: Mat4 get() = plane.centerPose.toMat4()

    actual val extentX: Float get() = plane.extentX

    actual val extentZ: Float get() = plane.extentZ

    actual val polygon: List<Float2>
        get() {
            // [x0, z0, x1, z1, …] in the plane's local space.
            val vertices = plane.polygon
            return List(vertices.remaining() / 2) { Float2(vertices.get(), vertices.get()) }
        }

    actual val alignment: Alignment
        get() = if (plane.type == ArCorePlane.Type.VERTICAL) Alignment.VERTICAL else Alignment.HORIZONTAL

    actual val trackingState: TrackingState get() = plane.trackingState.toKoord()

    // Wrappers are created on demand, so identity is the ARCore plane's.
    override fun equals(other: Any?) = other is Plane && other.plane == plane
    override fun hashCode() = plane.hashCode()

    actual enum class Alignment { HORIZONTAL, VERTICAL }
}
