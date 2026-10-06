package io.github.erkko68.koord

import io.github.erkko68.koord.math.Mat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane

/**
 * A world-tracking (6DoF) AR session: ARCore's `Session` on Android, ARKit's
 * `ARSession` on iOS.
 *
 * Construction is platform-specific (Android needs a `Context`, iOS needs
 * nothing), so there is no common constructor; create the session in platform
 * code and hand it to common code.
 *
 * World space is right-handed, Y-up and in metres on both platforms. A [Mat4]
 * goes to a renderer through [Mat4.toFloatArray].
 *
 * Koord does not draw. The camera image is handed out per platform, as the
 * platform delivers it: see the platform members of this class and of
 * [ArFrame]. `koord-filament` draws it with Filament.
 */
expect class ArSession {

    /**
     * Applies [config]. May be called before the first [resume] or while the
     * session is running.
     */
    fun configure(config: ArConfig)

    /**
     * Starts or resumes tracking and the camera.
     *
     * @throws ArException if the camera or the AR runtime cannot be started
     */
    fun resume()

    /** Stops tracking and releases the camera. The session can be [resume]d later. */
    fun pause()

    /**
     * Releases the session for good. It must not be used afterwards.
     *
     * Anchors and planes obtained from it become invalid.
     */
    fun close()

    /**
     * Tells the session how the camera image is shown on screen. Call it when
     * the surface is created and every time its size or rotation changes;
     * [io.github.erkko68.koord.camera.ArCamera.viewMatrix],
     * [io.github.erkko68.koord.camera.ArCamera.projectionMatrix] and
     * screen-point [ArFrame.hitTest] depend on it.
     *
     * @param widthPx viewport width in pixels
     * @param heightPx viewport height in pixels
     */
    fun setDisplayGeometry(rotation: DisplayRotation, widthPx: Int, heightPx: Int)

    /**
     * Returns the most recent frame, or `null` if the session has not produced
     * one yet.
     *
     * The model is pull on both platforms: call this once per rendered frame,
     * always from the same thread, which must also be the one that calls
     * [close]. On Android it advances the session
     * (`Session.update()`); on iOS it returns the frame ARKit pushed last.
     *
     * @throws ArException if the session failed since the last call
     */
    fun update(): ArFrame?

    /**
     * Creates an anchor at a fixed pose in world space.
     *
     * @param transform world transform; must be rigid (rotation and
     *   translation only)
     */
    fun createAnchor(transform: Mat4): Anchor

    /** Every anchor created through this session and not yet detached. */
    val anchors: List<Anchor>

    /**
     * Every plane currently detected. Empty unless [ArConfig.planeDetection]
     * is on.
     *
     * When two planes turn out to be the same surface, the platform merges
     * them and only the surviving plane stays in this list (ARCore's subsumed
     * planes are filtered out, matching ARKit, which removes them).
     *
     * ARCore also reports one surface as several stacked planes, and the tops
     * of low objects as planes over the floor. A plane lying within 10 cm of a
     * larger parallel one is left out on Android, as ARKit holds those back.
     */
    val planes: List<Plane>
}

/**
 * Rotation of the display from the device's natural orientation, with the same
 * meaning as Android's `Surface.ROTATION_*` constants.
 */
enum class DisplayRotation { ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270 }
