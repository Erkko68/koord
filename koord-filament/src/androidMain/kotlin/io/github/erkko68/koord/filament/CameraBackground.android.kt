package io.github.erkko68.koord.filament

import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Texture
import io.github.erkko68.filament.TextureSampler
import io.github.erkko68.koord.ArFrame
import io.github.erkko68.koord.ArSession

actual class CameraBackground actual constructor(private val engine: Engine, session: ArSession) {
    // Wraps the GL textures ARCore writes to, keyed by GL name. They are
    // visible to Filament because the engine shares the session's GL context.
    private val textures = session.cameraTextureNames.associateWith { name ->
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

    actual fun update(frame: ArFrame) {
        // Draw the texture this frame was written to, so the image matches the
        // frame's camera pose and ARCore is not writing it while it is drawn.
        textures[frame.cameraTextureName]?.let { quad.materialInstance.setParameter("cameraTexture", it, sampler) }
        quad.setUvTransform(frame.cameraImageUv)
    }

    actual fun destroy() {
        quad.destroy()
        textures.values.forEach { engine.destroy(it) }
    }
}
