package io.github.erkko68.koord.camera

import io.github.erkko68.filament.Engine
import io.github.erkko68.koord.ArFrame
import io.github.erkko68.koord.ArSession

/**
 * Draws nothing yet. ARKit delivers the camera image as a `CVPixelBuffer`,
 * which Filament takes through `Texture::setExternalImage`, and filament-kmp
 * does not bind that call. [entity] is an empty entity so that common code
 * runs unchanged.
 */
actual class CameraBackground actual constructor(private val engine: Engine, session: ArSession) {
    actual val entity: Int = engine.entityManager.create()

    actual fun update(frame: ArFrame) {}

    actual fun destroy() {
        engine.entityManager.destroy(entity)
    }
}
