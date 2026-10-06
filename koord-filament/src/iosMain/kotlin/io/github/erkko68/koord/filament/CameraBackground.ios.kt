@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord.filament

import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Texture
import io.github.erkko68.filament.TextureSampler
import io.github.erkko68.koord.ArFrame
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.filament.interop.koord_image_plane_texture
import io.github.erkko68.koord.filament.interop.koord_texture_cache_create
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toLong
import kotlinx.cinterop.value
import platform.CoreFoundation.CFRelease
import platform.CoreVideo.CVMetalTextureCacheFlush
import platform.CoreVideo.CVMetalTextureRef
import platform.CoreVideo.CVMetalTextureRefVar
import platform.CoreVideo.CVPixelBufferGetHeightOfPlane
import platform.CoreVideo.CVPixelBufferGetWidthOfPlane

// How many of CoreVideo's texture handles to keep after their frame: two
// planes for each frame Filament's render thread may still be drawing.
private const val HELD_PLANES = 8

/**
 * ARKit delivers the camera image as a pixel buffer with a luma and a chroma
 * plane. Each plane becomes a Metal texture that shares the buffer's memory,
 * imported into Filament for the one frame it is drawn in; the material
 * converts to RGB.
 */
actual class CameraBackground actual constructor(private val engine: Engine, session: ArSession) {
    private val quad = CameraQuad(engine, CAMERA_BACKGROUND_FILAMAT_BASE64)
    private val sampler = TextureSampler(TextureSampler.MinFilter.LINEAR, TextureSampler.MagFilter.LINEAR)
    private val cache = checkNotNull(koord_texture_cache_create()) { "Failed to create the Metal texture cache" }

    private var textures = emptyList<Texture>()
    private val holders = ArrayDeque<CVMetalTextureRef>()

    actual val entity: Int get() = quad.entity

    actual fun update(frame: ArFrame) {
        val luma = planeTexture(frame, 0, Texture.InternalFormat.R8) ?: return
        val chroma = planeTexture(frame, 1, Texture.InternalFormat.RG8)
        if (chroma == null) {
            engine.destroy(luma)
            return
        }
        quad.materialInstance.setParameter("cameraTextureY", luma, sampler)
        quad.materialInstance.setParameter("cameraTextureCbCr", chroma, sampler)
        textures.forEach { engine.destroy(it) }
        textures = listOf(luma, chroma)
        while (holders.size > HELD_PLANES) CFRelease(holders.removeFirst())
        CVMetalTextureCacheFlush(cache, 0u)
        quad.setUvTransform(frame.cameraImageUv)
    }

    actual fun destroy() {
        quad.destroy()
        textures.forEach { engine.destroy(it) }
        holders.forEach { CFRelease(it) }
        holders.clear()
        CFRelease(cache)
    }

    private fun planeTexture(frame: ArFrame, plane: Int, format: Texture.InternalFormat): Texture? = memScoped {
        val image = frame.cameraImage
        val holder = alloc<CVMetalTextureRefVar>()
        val metalTexture = koord_image_plane_texture(image, cache, plane.toULong(), holder.ptr)
            ?: return null
        holder.value?.let { holders.addLast(it) }
        Texture.Builder()
            .width(CVPixelBufferGetWidthOfPlane(image, plane.toULong()).toInt())
            .height(CVPixelBufferGetHeightOfPlane(image, plane.toULong()).toInt())
            .levels(1)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(format)
            // Filament takes over the reference the helper retained.
            .import(metalTexture.toLong())
            .build(engine)
    }
}
