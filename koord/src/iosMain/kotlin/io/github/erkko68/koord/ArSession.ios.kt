package io.github.erkko68.koord

import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane

actual class ArSession() {
    actual fun configure(config: ArConfig): Unit = TODO("Not yet implemented")
    actual fun resume(): Unit = TODO("Not yet implemented")
    actual fun pause(): Unit = TODO("Not yet implemented")
    actual fun close(): Unit = TODO("Not yet implemented")
    actual fun setDisplayGeometry(rotation: DisplayRotation, widthPx: Int, heightPx: Int): Unit = TODO("Not yet implemented")
    actual fun update(): ArFrame? = TODO("Not yet implemented")
    actual fun createAnchor(transform: Mat4): Anchor = TODO("Not yet implemented")
    actual val anchors: List<Anchor> get() = TODO("Not yet implemented")
    actual val planes: List<Plane> get() = TODO("Not yet implemented")
}
