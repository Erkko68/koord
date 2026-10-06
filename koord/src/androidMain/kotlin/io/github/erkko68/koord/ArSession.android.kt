package io.github.erkko68.koord

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Config
import com.google.ar.core.Session
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Filament
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.trackable.Anchor
import io.github.erkko68.koord.trackable.Plane
import com.google.ar.core.Plane as ArCorePlane
import com.google.ar.core.TrackingState as ArCoreTrackingState

// One being written by ARCore, plus the frames Filament's render thread may still have queued.
private const val CAMERA_TEXTURE_COUNT = 4

/**
 * Backed by an ARCore `Session`, which is created on the first [resume] so
 * that the constructor never throws.
 *
 * [update], [createEngine] and [close] must always be called from the same
 * thread: ARCore writes the camera image to a GL texture, and the GL context
 * that owns it is bound to whichever of the first two is called first.
 */
actual class ArSession(context: Context) {
    private val context = context.applicationContext

    private var session: Session? = null
    private var running = false
    private var config = ArConfig()
    private var displayGeometry: Triple<Int, Int, Int>? = null

    // The GL context that owns cameraTextures; eglSurface is only set when
    // ensureGl() had to create that context itself.
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    /**
     * GL names of the external textures ARCore writes the camera image to,
     * empty before [ensureGl]. ARCore writes each frame to the next one in
     * turn, so the texture of a frame Filament is still drawing on its own
     * thread is not overwritten under it.
     */
    internal var cameraTextures = IntArray(0)
        private set

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
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, eglSurface)
            EGL14.eglDestroyContext(display, eglContext)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        eglContext = EGL14.EGL_NO_CONTEXT
        cameraTextures = IntArray(0)
    }

    actual fun setDisplayGeometry(rotation: DisplayRotation, widthPx: Int, heightPx: Int) {
        // DisplayRotation is declared in the order of Surface.ROTATION_0..270.
        displayGeometry = Triple(rotation.ordinal, widthPx, heightPx)
        session?.setDisplayGeometry(rotation.ordinal, widthPx, heightPx)
    }

    actual fun update(): ArFrame? {
        val session = session?.takeIf { running } ?: return null
        try {
            ensureGl()
            session.setCameraTextureNames(cameraTextures)
            val frame = session.update()
            // Filament samples the texture from its own context on another thread.
            GLES20.glFlush()
            // Timestamp 0 means the camera has not delivered an image yet.
            return if (frame.timestamp == 0L) null else ArFrame(frame)
        } catch (e: Exception) {
            throw e.toArException()
        }
    }

    actual fun createEngine(): Engine {
        ensureGl()
        Filament.init()
        // ARCore's texture is an OpenGL one, so the engine cannot use Vulkan.
        return checkNotNull(Engine.create(Engine.Backend.OPENGL, sharedContext = eglContext)) {
            "Failed to create the Filament engine"
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
     * ARCore writes the camera image to GL textures, and refuses to update
     * without one even when nothing draws it. This creates those textures, in
     * the GL context current on this thread or, when there is none, in a
     * private 1×1 pbuffer context that is left current. [createEngine] shares
     * the context with Filament so the textures can be drawn.
     */
    private fun ensureGl() {
        if (cameraTextures.isNotEmpty()) return
        eglContext = EGL14.eglGetCurrentContext()
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            EGL14.eglChooseConfig(
                display,
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE,
                ),
                0, configs, 0, 1, count, 0,
            )
            check(count[0] > 0) { "No EGL config for an offscreen GLES3 context" }
            eglContext = EGL14.eglCreateContext(
                display, configs[0], EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0,
            )
            eglSurface = EGL14.eglCreatePbufferSurface(
                display, configs[0],
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0,
            )
            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }
        }
        val ids = IntArray(CAMERA_TEXTURE_COUNT)
        GLES20.glGenTextures(ids.size, ids, 0)
        ids.forEach { GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, it) }
        cameraTextures = ids
    }
}
