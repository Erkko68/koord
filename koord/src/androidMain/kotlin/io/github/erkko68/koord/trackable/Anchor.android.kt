package io.github.erkko68.koord.trackable

import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.toKoord
import io.github.erkko68.koord.toMat4
import com.google.ar.core.Anchor as ArCoreAnchor

actual class Anchor internal constructor(private val anchor: ArCoreAnchor) {
    actual val transform: Mat4 get() = anchor.pose.toMat4()

    actual val trackingState: TrackingState get() = anchor.trackingState.toKoord()

    actual fun detach() = anchor.detach()

    // Wrappers are created on demand, so identity is the ARCore anchor's.
    override fun equals(other: Any?) = other is Anchor && other.anchor == anchor
    override fun hashCode() = anchor.hashCode()
}
