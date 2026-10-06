@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord.camera

import io.github.erkko68.filament.utils.Float2
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.filament.utils.inverse
import io.github.erkko68.koord.TrackingFailureReason
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.toKoord
import io.github.erkko68.koord.toMat4
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.get
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import platform.ARKit.ARCamera as ArKitCamera
import platform.CoreGraphics.CGSize
import platform.UIKit.UIInterfaceOrientation

actual class ArCamera internal constructor(
    private val camera: ArKitCamera,
    private val orientation: UIInterfaceOrientation,
    private val viewportSize: CValue<CGSize>,
) {
    actual val transform: Mat4 get() = inverse(viewMatrix)

    actual val viewMatrix: Mat4 get() = camera.viewMatrixForOrientation(orientation).toMat4()

    actual fun projectionMatrix(near: Float, far: Float): Mat4 =
        camera.projectionMatrixForOrientation(orientation, viewportSize, near.toDouble(), far.toDouble()).toMat4()

    actual val intrinsics: CameraIntrinsics
        get() {
            // Three columns of a simd_float3, each padded to four floats: [fx 0 0 _] [0 fy 0 _] [cx cy 1 _].
            val k = camera.intrinsics.useContents {
                val floats = ptr.reinterpret<FloatVar>()
                FloatArray(12) { floats[it] }
            }
            val (width, height) = camera.imageResolution.useContents { width.toInt() to height.toInt() }
            return CameraIntrinsics(
                focalLength = Float2(k[0], k[5]),
                principalPoint = Float2(k[8], k[9]),
                imageWidth = width,
                imageHeight = height,
            )
        }

    actual val trackingState: TrackingState get() = camera.trackingState.toKoord()

    actual val trackingFailureReason: TrackingFailureReason get() = camera.trackingStateReason.toKoord()
}
