package io.github.erkko68.koord.filament

import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.utils.Float4
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.math.Mat4
import io.github.erkko68.filament.utils.Mat4 as FilamentMat4

/**
 * Creates the Filament engine to render this session with. Only an engine
 * created here can draw the camera image through [CameraBackground]: on
 * Android it uses the OpenGL backend and shares the GL context ARCore writes
 * the image in. On iOS it is a Metal engine with nothing special about it.
 *
 * Call it from the thread that calls [ArSession.update]. The caller owns the
 * engine and destroys it with `Engine.destroy`.
 */
expect fun ArSession.createEngine(): Engine

/** This transform as `filament-utils`' matrix, for the math Koord's own type leaves out. */
fun Mat4.toFilament(): FilamentMat4 {
    // Column-major, as FilamentMat4's columns are; FilamentMat4.of() reads row-major.
    val m = toFloatArray()
    return FilamentMat4(
        Float4(m[0], m[1], m[2], m[3]),
        Float4(m[4], m[5], m[6], m[7]),
        Float4(m[8], m[9], m[10], m[11]),
        Float4(m[12], m[13], m[14], m[15]),
    )
}
