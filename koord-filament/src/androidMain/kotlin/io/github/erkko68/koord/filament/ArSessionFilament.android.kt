package io.github.erkko68.koord.filament

import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Filament
import io.github.erkko68.koord.ArSession

actual fun ArSession.createEngine(): Engine {
    Filament.init()
    // ARCore's texture is an OpenGL one, so the engine cannot use Vulkan.
    return checkNotNull(Engine.create(Engine.Backend.OPENGL, sharedContext = eglContext)) {
        "Failed to create the Filament engine"
    }
}
