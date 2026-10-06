@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord

import io.github.erkko68.filament.utils.Mat4
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.cValue
import kotlinx.cinterop.get
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.useContents
import platform.ARKit.ARErrorCodeCameraUnauthorized
import platform.ARKit.ARErrorCodeSensorFailed
import platform.ARKit.ARErrorCodeSensorUnavailable
import platform.ARKit.ARErrorCodeUnsupportedConfiguration
import platform.ARKit.ARErrorDomain
import platform.ARKit.ARTrackingState
import platform.ARKit.ARTrackingStateReason
import platform.Foundation.NSError
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.darwin.simd_float4x4

// A simd_float4x4 is 16 floats in column-major order, the layout of Mat4.toFloatArrayColumn().

internal fun CValue<simd_float4x4>.toMat4(): Mat4 = useContents {
    val floats = ptr.reinterpret<FloatVar>()
    FloatArray(16) { floats[it] }
}.toMat4()

internal fun Mat4.toSimd(): CValue<simd_float4x4> {
    val values = toFloatArrayColumn()
    return cValue {
        val floats = ptr.reinterpret<FloatVar>()
        for (i in 0 until 16) floats[i] = values[i]
    }
}

/** Android's `Surface.ROTATION_90` turns the device counter-clockwise, which puts the home button on the right. */
internal fun DisplayRotation.toOrientation(): UIInterfaceOrientation = when (this) {
    DisplayRotation.ROTATION_0 -> UIInterfaceOrientationPortrait
    DisplayRotation.ROTATION_90 -> UIInterfaceOrientationLandscapeRight
    DisplayRotation.ROTATION_180 -> UIInterfaceOrientationPortraitUpsideDown
    DisplayRotation.ROTATION_270 -> UIInterfaceOrientationLandscapeLeft
}

internal fun ARTrackingState.toKoord() = when (this) {
    ARTrackingState.ARTrackingStateNormal -> TrackingState.TRACKING
    ARTrackingState.ARTrackingStateLimited -> TrackingState.LIMITED
    else -> TrackingState.STOPPED
}

internal fun ARTrackingStateReason.toKoord() = when (this) {
    ARTrackingStateReason.ARTrackingStateReasonInitializing -> TrackingFailureReason.INITIALIZING
    ARTrackingStateReason.ARTrackingStateReasonRelocalizing -> TrackingFailureReason.RELOCALIZING
    ARTrackingStateReason.ARTrackingStateReasonExcessiveMotion -> TrackingFailureReason.EXCESSIVE_MOTION
    ARTrackingStateReason.ARTrackingStateReasonInsufficientFeatures -> TrackingFailureReason.INSUFFICIENT_FEATURES
    else -> TrackingFailureReason.NONE
}

/** Maps what ARKit reports through `session(_:didFailWithError:)`. */
internal fun NSError.toArException(): ArException {
    val text = localizedDescription
    if (domain != ARErrorDomain) return ArException.Unknown(text)
    return when (code) {
        ARErrorCodeUnsupportedConfiguration -> ArException.Unsupported(text)
        ARErrorCodeSensorUnavailable, ARErrorCodeSensorFailed -> ArException.CameraUnavailable(text)
        ARErrorCodeCameraUnauthorized -> ArException.CameraPermissionDenied(text)
        else -> ArException.Unknown(text)
    }
}
