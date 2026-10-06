package io.github.erkko68.koord.camera

import io.github.erkko68.filament.Engine
import io.github.erkko68.koord.ArFrame
import io.github.erkko68.koord.ArSession

actual class CameraBackground actual constructor(engine: Engine, session: ArSession) {
    actual val entity: Int get() = TODO("Not yet implemented")
    actual fun update(frame: ArFrame): Unit = TODO("Not yet implemented")
    actual fun destroy(): Unit = TODO("Not yet implemented")
}
