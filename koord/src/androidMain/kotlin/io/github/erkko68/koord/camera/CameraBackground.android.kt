package io.github.erkko68.koord.camera

import com.google.ar.core.Coordinates2d
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Texture
import io.github.erkko68.filament.TextureSampler
import io.github.erkko68.koord.ArFrame
import io.github.erkko68.koord.ArSession

// Three viewport corners: bottom-left, bottom-right, top-left.
private val VIEWPORT_CORNERS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f)

actual class CameraBackground actual constructor(private val engine: Engine, session: ArSession) {
    // Wraps the GL textures ARCore writes to, keyed by GL name. They are
    // visible to Filament because the engine shares the session's GL context.
    private val textures = session.cameraTextures.associateWith { name ->
        Texture.Builder()
            .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            .format(Texture.InternalFormat.RGB8)
            .import(name.toLong())
            .build(engine)
    }

    private val sampler = TextureSampler(TextureSampler.MinFilter.LINEAR, TextureSampler.MagFilter.LINEAR)

    private val quad = CameraQuad(engine, CAMERA_BACKGROUND_FILAMAT_BASE64).apply {
        materialInstance.setParameter("cameraTexture", textures.values.first(), sampler)
    }

    actual val entity: Int get() = quad.entity

    private val uv = FloatArray(6)

    actual fun update(frame: ArFrame) {
        // Draw the texture this frame was written to, so the image matches the
        // frame's camera pose and ARCore is not writing it while it is drawn.
        textures[frame.frame.cameraTextureName]?.let { quad.materialInstance.setParameter("cameraTexture", it, sampler) }

        // Where the three viewport corners fall in the camera texture, which
        // accounts for the display rotation and the crop to the viewport's shape.
        frame.frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, VIEWPORT_CORNERS,
            Coordinates2d.TEXTURE_NORMALIZED, uv,
        )
        quad.setUvTransform(uv)
    }

    actual fun destroy() {
        quad.destroy()
        textures.values.forEach { engine.destroy(it) }
    }
}
