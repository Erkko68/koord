package io.github.erkko68.koord.depth

/**
 * How far the real scene is from the camera, per pixel, for one frame. Read it
 * from [io.github.erkko68.koord.ArFrame.depthImage].
 *
 * The image is far smaller than the camera image (around 160×90 on ARCore and
 * 256×192 on ARKit) but covers the same view, in the same orientation: a
 * texture coordinate of the camera image, as in
 * [io.github.erkko68.koord.ArFrame.cameraImageUv], addresses the same point in
 * both.
 *
 * @property width number of columns
 * @property height number of rows
 * @property metres `width * height` distances in metres, row by row from the
 *   image's top-left, each measured along the camera's viewing direction, not
 *   along the ray through the pixel. `0` where the platform has no estimate,
 *   which only ARCore reports.
 */
class DepthImage(
    val width: Int,
    val height: Int,
    val metres: FloatArray,
)
