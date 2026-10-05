package io.github.erkko68.koord.camera

import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingFailureReason
import io.github.erkko68.koord.TrackingState

actual class ArCamera internal constructor() {
    actual val transform: Mat4 get() = TODO("Not yet implemented")
    actual val viewMatrix: Mat4 get() = TODO("Not yet implemented")
    actual fun projectionMatrix(near: Float, far: Float): Mat4 = TODO("Not yet implemented")
    actual val intrinsics: CameraIntrinsics get() = TODO("Not yet implemented")
    actual val trackingState: TrackingState get() = TODO("Not yet implemented")
    actual val trackingFailureReason: TrackingFailureReason get() = TODO("Not yet implemented")
}
