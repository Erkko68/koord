package io.github.erkko68.koord

/** How well the session currently knows where something is. */
enum class TrackingState {
    /** Pose is reliable. ARCore `TRACKING`, ARKit `normal`. */
    TRACKING,

    /** Tracking is lost for now but may recover. ARCore `PAUSED`, ARKit `limited`. */
    LIMITED,

    /** Tracking is gone and will not recover. ARCore `STOPPED`, ARKit `notAvailable`. */
    STOPPED,
}

/**
 * Why the camera's [TrackingState] is not [TrackingState.TRACKING]. Some
 * reasons are only ever reported by one platform.
 */
enum class TrackingFailureReason {
    /** Tracking is fine, or the platform gave no reason. */
    NONE,

    /** The session is still starting up. ARKit only. */
    INITIALIZING,

    /** The session is trying to resume after an interruption. ARKit only. */
    RELOCALIZING,

    /** The device is moving too fast. */
    EXCESSIVE_MOTION,

    /** The camera sees too little detail to track, e.g. a blank wall. */
    INSUFFICIENT_FEATURES,

    /** The scene is too dark. ARCore only. */
    INSUFFICIENT_LIGHT,

    /** Another app is using the camera. ARCore only. */
    CAMERA_UNAVAILABLE,

    /** Internal tracking error. ARCore only. */
    BAD_STATE,
}
