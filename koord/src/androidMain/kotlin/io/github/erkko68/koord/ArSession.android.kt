package io.github.erkko68.koord

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Config
import com.google.ar.core.Session
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane
import com.google.ar.core.Plane as ArCorePlane
import com.google.ar.core.TrackingState as ArCoreTrackingState

/**
 * Backed by an ARCore `Session`, which is created on the first [resume] so
 * that the constructor never throws.
 *
 * [update] and [close] must always be called from the same thread: ARCore
 * writes the camera image to a GL texture, and the GL context that owns it is
 * bound to the thread that first calls [update].
 */
actual class ArSession(context: Context) {
    private val context = context.applicationContext

    private var session: Session? = null
    private var running = false
    private var config = ArConfig()
    private var displayGeometry: Triple<Int, Int, Int>? = null

    // Only set when update() had to create its own GL context.
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var cameraTexture = 0

    actual fun configure(config: ArConfig) {
        this.config = config
        session?.let { applyConfig(it) }
    }

    actual fun resume() {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw ArException.CameraPermissionDenied("The CAMERA permission has not been granted")
        }
        try {
            val session = session ?: Session(context).also {
                session = it
                applyConfig(it)
                displayGeometry?.let { (rotation, width, height) -> it.setDisplayGeometry(rotation, width, height) }
            }
            session.resume()
            running = true
        } catch (e: Exception) {
            throw e.toArException()
        }
    }

    actual fun pause() {
        running = false
        session?.pause()
    }

    actual fun close() {
        running = false
        session?.close()
        session = null
        if (eglContext != EGL14.EGL_NO_CONTEXT) {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, eglSurface)
            EGL14.eglDestroyContext(display, eglContext)
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        cameraTexture = 0
    }

    actual fun setDisplayGeometry(rotation: DisplayRotation, widthPx: Int, heightPx: Int) {
        // DisplayRotation is declared in the order of Surface.ROTATION_0..270.
        displayGeometry = Triple(rotation.ordinal, widthPx, heightPx)
        session?.setDisplayGeometry(rotation.ordinal, widthPx, heightPx)
    }

    actual fun update(): ArFrame? {
        val session = session?.takeIf { running } ?: return null
        try {
            ensureCameraTexture(session)
            val frame = session.update()
            // Timestamp 0 means the camera has not delivered an image yet.
            return if (frame.timestamp == 0L) null else ArFrame(frame)
        } catch (e: Exception) {
            throw e.toArException()
        }
    }

    actual fun createAnchor(transform: Mat4): Anchor =
        Anchor(checkNotNull(session) { "The session has not been resumed yet" }.createAnchor(transform.toPose()))

    actual val anchors: List<Anchor>
        get() = session?.allAnchors?.map(::Anchor).orEmpty()

    actual val planes: List<Plane>
        get() = session?.getAllTrackables(ArCorePlane::class.java)
            ?.filter { it.subsumedBy == null && it.trackingState != ArCoreTrackingState.STOPPED }
            ?.map(::Plane)
            .orEmpty()

    private fun applyConfig(session: Session) {
        session.configure(
            Config(session).apply {
                planeFindingMode = when (config.planeDetection) {
                    PlaneDetection.NONE -> Config.PlaneFindingMode.DISABLED
                    PlaneDetection.HORIZONTAL -> Config.PlaneFindingMode.HORIZONTAL
                    PlaneDetection.VERTICAL -> Config.PlaneFindingMode.VERTICAL
                    PlaneDetection.HORIZONTAL_AND_VERTICAL -> Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                }
                lightEstimationMode =
                    if (config.lightEstimation) Config.LightEstimationMode.AMBIENT_INTENSITY
                    else Config.LightEstimationMode.DISABLED
                focusMode = if (config.autoFocus) Config.FocusMode.AUTO else Config.FocusMode.FIXED
                // update() is a pull from the render loop and must not wait for the camera.
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            },
        )
    }

    /**
     * ARCore refuses to update without a GL context and a texture to write the
     * camera image to, even when nothing draws it.
     *
     * ponytail: with no GL context current on this thread, a private 1×1
     * pbuffer context is created and left current. Passthrough rendering will
     * need this texture in a context shared with Filament instead.
     */
    private fun ensureCameraTexture(session: Session) {
        if (cameraTexture != 0) return
        if (EGL14.eglGetCurrentContext() == EGL14.EGL_NO_CONTEXT) {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            EGL14.eglChooseConfig(
                display,
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE,
                ),
                0, configs, 0, 1, count, 0,
            )
            check(count[0] > 0) { "No EGL config for an offscreen GLES2 context" }
            eglContext = EGL14.eglCreateContext(
                display, configs[0], EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
            )
            eglSurface = EGL14.eglCreatePbufferSurface(
                display, configs[0],
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0,
            )
            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }
        }
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        cameraTexture = ids[0]
        session.setCameraTextureName(cameraTexture)
    }
}
