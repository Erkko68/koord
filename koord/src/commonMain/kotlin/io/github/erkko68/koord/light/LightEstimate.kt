package io.github.erkko68.koord.light

import io.github.erkko68.filament.utils.Float3

/**
 * The session's estimate of the real scene's ambient light for one frame,
 * reduced to what both platforms can report during world tracking. Read it
 * from [io.github.erkko68.koord.ArFrame.lightEstimate].
 *
 * @property intensity ambient brightness, normalised so that `1.0` is a
 *   neutrally lit scene. Derived from ARCore's pixel intensity and from
 *   ARKit's ambient intensity in lumens (1000 lm is neutral).
 * @property color linear RGB tint of the ambient light, `(1, 1, 1)` for
 *   neutral white. Derived from ARCore's colour correction gains and from
 *   ARKit's colour temperature.
 */
data class LightEstimate(
    val intensity: Float,
    val color: Float3,
)
