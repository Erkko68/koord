package io.github.erkko68.koord.trackable

import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingState

actual class Anchor internal constructor() {
    actual val transform: Mat4 get() = TODO("Not yet implemented")
    actual val trackingState: TrackingState get() = TODO("Not yet implemented")
    actual fun detach(): Unit = TODO("Not yet implemented")
}
