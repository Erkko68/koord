package io.github.erkko68.koord.hit

import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane

actual class HitResult internal constructor() {
    actual val transform: Mat4 get() = TODO("Not yet implemented")
    actual val distance: Float get() = TODO("Not yet implemented")
    actual val plane: Plane get() = TODO("Not yet implemented")
    actual fun createAnchor(): Anchor = TODO("Not yet implemented")
}
