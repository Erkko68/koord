package io.github.erkko68.koord

/**
 * Whether AR can run on this device.
 *
 * Like the [ArSession] constructor, the check itself is platform-specific and
 * has no common declaration: `checkArAvailability(context)` on Android,
 * `checkArAvailability()` on iOS.
 */
enum class ArAvailability {
    /** A session can be created. */
    SUPPORTED,

    /**
     * The device is capable, but the ARCore runtime has to be installed or
     * updated first. Android only; triggering the install is left to the app
     * (`ArCoreApk.requestInstall`).
     */
    NEEDS_INSTALL,

    /** This device cannot run AR. */
    UNSUPPORTED,
}
