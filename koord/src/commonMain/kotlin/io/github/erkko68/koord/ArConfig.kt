package io.github.erkko68.koord

/**
 * What an [ArSession] should track. Pass it to [ArSession.configure].
 *
 * @property planeDetection which surfaces to detect
 * @property lightEstimation whether frames carry a
 *   [io.github.erkko68.koord.light.LightEstimate]
 * @property autoFocus `true` for continuous autofocus, `false` for fixed focus
 * @property depth whether frames carry a
 *   [io.github.erkko68.koord.depth.DepthImage]. Off by default: it costs
 *   processing on Android and runs the LiDAR scanner on iOS. Ignored on a
 *   device that cannot measure depth, which on iOS is every device without a
 *   LiDAR scanner: see [ArFrame.depthImage].
 */
data class ArConfig(
    val planeDetection: PlaneDetection = PlaneDetection.HORIZONTAL_AND_VERTICAL,
    val lightEstimation: Boolean = true,
    val autoFocus: Boolean = true,
    val depth: Boolean = false,
)

/** Which plane orientations a session looks for. */
enum class PlaneDetection { NONE, HORIZONTAL, VERTICAL, HORIZONTAL_AND_VERTICAL }
