package io.github.erkko68.koord

/**
 * What an [ArSession] should track. Pass it to [ArSession.configure].
 *
 * @property planeDetection which surfaces to detect
 * @property lightEstimation whether frames carry a
 *   [io.github.erkko68.koord.light.LightEstimate]
 * @property autoFocus `true` for continuous autofocus, `false` for fixed focus
 */
data class ArConfig(
    val planeDetection: PlaneDetection = PlaneDetection.HORIZONTAL_AND_VERTICAL,
    val lightEstimation: Boolean = true,
    val autoFocus: Boolean = true,
)

/** Which plane orientations a session looks for. */
enum class PlaneDetection { NONE, HORIZONTAL, VERTICAL, HORIZONTAL_AND_VERTICAL }
