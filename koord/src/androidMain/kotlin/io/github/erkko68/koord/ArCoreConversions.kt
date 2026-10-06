package io.github.erkko68.koord

import com.google.ar.core.Pose
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import io.github.erkko68.koord.math.Mat4
import com.google.ar.core.TrackingFailureReason as ArCoreFailureReason
import com.google.ar.core.TrackingState as ArCoreTrackingState

internal fun Pose.toMat4() = Mat4(FloatArray(16).also { toMatrix(it, 0) })

/** [this] must be rigid; any scale would end up in the rotation. */
internal fun Mat4.toPose(): Pose {
    val t = position
    return Pose(floatArrayOf(t.x, t.y, t.z), rotationQuaternion())
}

internal fun ArCoreTrackingState.toKoord() = when (this) {
    ArCoreTrackingState.TRACKING -> TrackingState.TRACKING
    ArCoreTrackingState.PAUSED -> TrackingState.LIMITED
    ArCoreTrackingState.STOPPED -> TrackingState.STOPPED
}

internal fun ArCoreFailureReason.toKoord() = when (this) {
    ArCoreFailureReason.NONE -> TrackingFailureReason.NONE
    ArCoreFailureReason.BAD_STATE -> TrackingFailureReason.BAD_STATE
    ArCoreFailureReason.INSUFFICIENT_LIGHT -> TrackingFailureReason.INSUFFICIENT_LIGHT
    ArCoreFailureReason.EXCESSIVE_MOTION -> TrackingFailureReason.EXCESSIVE_MOTION
    ArCoreFailureReason.INSUFFICIENT_FEATURES -> TrackingFailureReason.INSUFFICIENT_FEATURES
    ArCoreFailureReason.CAMERA_UNAVAILABLE -> TrackingFailureReason.CAMERA_UNAVAILABLE
}

/** Maps what ARCore throws while starting or updating a session. */
internal fun Exception.toArException(): ArException {
    val text = message ?: toString()
    return when (this) {
        is ArException -> this
        is CameraNotAvailableException -> ArException.CameraUnavailable(text, this)
        is SecurityException -> ArException.CameraPermissionDenied(text, this)
        is UnavailableException -> ArException.Unsupported(text, this)
        else -> ArException.Unknown(text, this)
    }
}
