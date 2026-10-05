package io.github.erkko68.koord.trackable

import io.github.erkko68.filament.utils.Float2
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingState

actual class Plane internal constructor() {
    actual val transform: Mat4 get() = TODO("Not yet implemented")
    actual val extentX: Float get() = TODO("Not yet implemented")
    actual val extentZ: Float get() = TODO("Not yet implemented")
    actual val polygon: List<Float2> get() = TODO("Not yet implemented")
    actual val alignment: Alignment get() = TODO("Not yet implemented")
    actual val trackingState: TrackingState get() = TODO("Not yet implemented")

    actual enum class Alignment { HORIZONTAL, VERTICAL }
}
