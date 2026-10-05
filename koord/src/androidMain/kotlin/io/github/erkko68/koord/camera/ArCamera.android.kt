package io.github.erkko68.koord.camera

import com.google.ar.core.Camera
import io.github.erkko68.filament.utils.Float2
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingFailureReason
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.toKoord
import io.github.erkko68.koord.toMat4

actual class ArCamera internal constructor(private val camera: Camera) {
    actual val transform: Mat4 get() = camera.displayOrientedPose.toMat4()

    actual val viewMatrix: Mat4 get() = FloatArray(16).also { camera.getViewMatrix(it, 0) }.toMat4()

    actual fun projectionMatrix(near: Float, far: Float): Mat4 =
        FloatArray(16).also { camera.getProjectionMatrix(it, 0, near, far) }.toMat4()

    actual val intrinsics: CameraIntrinsics
        get() {
            val intrinsics = camera.imageIntrinsics
            val focalLength = intrinsics.focalLength
            val principalPoint = intrinsics.principalPoint
            val dimensions = intrinsics.imageDimensions
            return CameraIntrinsics(
                focalLength = Float2(focalLength[0], focalLength[1]),
                principalPoint = Float2(principalPoint[0], principalPoint[1]),
                imageWidth = dimensions[0],
                imageHeight = dimensions[1],
            )
        }

    actual val trackingState: TrackingState get() = camera.trackingState.toKoord()

    actual val trackingFailureReason: TrackingFailureReason get() = camera.trackingFailureReason.toKoord()
}
