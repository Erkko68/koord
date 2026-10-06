package io.github.erkko68.koord.filament

import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Filament
import io.github.erkko68.koord.ArSession

actual fun ArSession.createEngine(): Engine {
    Filament.init()
    return checkNotNull(Engine.create()) { "Failed to create the Filament engine" }
}
