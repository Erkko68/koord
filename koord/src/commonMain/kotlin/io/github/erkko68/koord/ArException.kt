package io.github.erkko68.koord

/**
 * A failure reported by the AR platform. Thrown by [ArSession.resume] and
 * [ArSession.update].
 *
 * ARCore throws its errors where they happen. ARKit reports them later through
 * a delegate callback, so on iOS a failure surfaces on the next
 * [ArSession.update] instead of at the call that caused it.
 */
sealed class ArException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /** The camera is in use by another app or otherwise could not be opened. */
    class CameraUnavailable(message: String, cause: Throwable? = null) : ArException(message, cause)

    /** The app does not hold the camera permission. Requesting it is the app's job. */
    class CameraPermissionDenied(message: String, cause: Throwable? = null) : ArException(message, cause)

    /**
     * AR cannot run on this device as it is: unsupported hardware, or on
     * Android an ARCore runtime that is missing or too old. See
     * [ArAvailability].
     */
    class Unsupported(message: String, cause: Throwable? = null) : ArException(message, cause)

    /** Any other platform error; [cause] or [message] carries the platform's own detail. */
    class Unknown(message: String, cause: Throwable? = null) : ArException(message, cause)
}
