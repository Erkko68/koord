@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord.trackable

import io.github.erkko68.koord.math.Mat4
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.toMat4
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ARKit.ARAnchor

actual class Anchor internal constructor(private val session: ArSession, private val anchor: ARAnchor) {
    // Not in the session's frame until the frame after it was added.
    actual val transform: Mat4 get() = (session.current(anchor) ?: anchor).transform.toMat4()

    actual val trackingState: TrackingState get() = session.cameraTrackingState()

    actual fun detach() = session.detach(anchor)

    // Wrappers are created on demand, so identity is the ARKit anchor's.
    override fun equals(other: Any?) = other is Anchor && other.anchor.identifier == anchor.identifier
    override fun hashCode() = anchor.identifier.hashCode()
}
