@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord

import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Filament
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane
import io.github.erkko68.koord.interop.koord_frame_anchors
import io.github.erkko68.koord.interop.koord_frame_camera
import io.github.erkko68.koord.interop.koord_frame_retain_current
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGSizeMake
import platform.ARKit.ARAnchor
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARPlaneDetectionHorizontal
import platform.ARKit.ARPlaneDetectionNone
import platform.ARKit.ARPlaneDetectionVertical
import platform.ARKit.ARSession
import platform.ARKit.ARSessionDelegateProtocol
import platform.ARKit.ARWorldTrackingConfiguration
import platform.Foundation.NSError
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.darwin.NSObject

/**
 * Backed by an ARKit `ARSession` running an `ARWorldTrackingConfiguration`.
 *
 * ARKit pushes frames and reports failures through a delegate; [update]
 * takes the session's current frame and throws the failure it was told
 * about since the last call.
 *
 * Exactly one `ARFrame` is held at a time, the one of the last [update], as a
 * manually retained pointer: see `arkit.def` for why it is not a Kotlin
 * reference.
 */
actual class ArSession {
    internal val session = ARSession()

    // ARSession holds its delegate weakly.
    private val delegate = FailureDelegate()

    private var running = false
    private var config = ArConfig()

    // ARKit takes the display geometry with each call that depends on it.
    internal var orientation: UIInterfaceOrientation = UIInterfaceOrientationPortrait
        private set
    internal var widthPx = 1
        private set
    internal var heightPx = 1
        private set

    private val createdAnchors = mutableListOf<ARAnchor>()

    // The ARFrame of the last update(), retained.
    private var frame: COpaquePointer? = null

    init {
        session.delegate = delegate
    }

    actual fun configure(config: ArConfig) {
        this.config = config
        if (running) run()
    }

    actual fun resume() {
        if (!ARWorldTrackingConfiguration.isSupported) {
            throw ArException.Unsupported("This device does not support ARKit world tracking")
        }
        run()
        running = true
    }

    actual fun pause() {
        running = false
        session.pause()
        releaseFrame()
    }

    actual fun close() {
        pause()
        createdAnchors.clear()
        session.delegate = null
    }

    actual fun setDisplayGeometry(rotation: DisplayRotation, widthPx: Int, heightPx: Int) {
        orientation = rotation.toOrientation()
        this.widthPx = widthPx
        this.heightPx = heightPx
    }

    actual fun update(): ArFrame? {
        delegate.error?.let {
            delegate.error = null
            running = false
            throw it.toArException()
        }
        if (!running) return null
        releaseFrame()
        return koord_frame_retain_current(session)?.let {
            frame = it
            ArFrame(this, it)
        }
    }

    actual fun createEngine(): Engine {
        Filament.init()
        return checkNotNull(Engine.create()) { "Failed to create the Filament engine" }
    }

    actual fun createAnchor(transform: Mat4): Anchor {
        val anchor = ARAnchor(transform = transform.toSimd())
        session.addAnchor(anchor)
        createdAnchors += anchor
        return Anchor(this, anchor)
    }

    actual val anchors: List<Anchor>
        get() = createdAnchors.map { Anchor(this, it) }

    actual val planes: List<Plane>
        get() = frameAnchors().filterIsInstance<ARPlaneAnchor>().map { Plane(this, it) }

    /**
     * The session's current copy of [anchor], or `null` once it is gone.
     * ARKit hands out a new object each time an anchor changes, so the one a
     * wrapper was created with goes stale.
     *
     * ponytail: a linear search per call; keep a map by identifier, updated
     * from the delegate's anchor callbacks, if scenes get many anchors.
     */
    internal fun current(anchor: ARAnchor): ARAnchor? =
        frameAnchors().firstOrNull { (it as ARAnchor).identifier == anchor.identifier } as ARAnchor?

    /** The camera's tracking state as of the last [update]. */
    internal fun cameraTrackingState(): TrackingState =
        frame?.let { koord_frame_camera(it) }?.trackingState?.toKoord() ?: TrackingState.STOPPED

    internal fun isCurrent(frame: COpaquePointer) = frame == this.frame

    // Only the aspect ratio matters to ARKit, so pixels do as well as points.
    internal fun viewportSize() = CGSizeMake(widthPx.toDouble(), heightPx.toDouble())

    private fun frameAnchors(): List<*> = frame?.let { koord_frame_anchors(it) }.orEmpty()

    private fun releaseFrame() {
        frame?.let { CFRelease(it) }
        frame = null
    }

    internal fun detach(anchor: ARAnchor) {
        session.removeAnchor(anchor)
        createdAnchors.remove(anchor)
    }

    private fun run() {
        session.runWithConfiguration(
            ARWorldTrackingConfiguration().apply {
                planeDetection = when (config.planeDetection) {
                    PlaneDetection.NONE -> ARPlaneDetectionNone
                    PlaneDetection.HORIZONTAL -> ARPlaneDetectionHorizontal
                    PlaneDetection.VERTICAL -> ARPlaneDetectionVertical
                    PlaneDetection.HORIZONTAL_AND_VERTICAL -> ARPlaneDetectionHorizontal or ARPlaneDetectionVertical
                }
                lightEstimationEnabled = config.lightEstimation
                autoFocusEnabled = config.autoFocus
            },
        )
    }

    private class FailureDelegate : NSObject(), ARSessionDelegateProtocol {
        var error: NSError? = null

        override fun session(session: ARSession, didFailWithError: NSError) {
            error = didFailWithError
        }
    }
}
