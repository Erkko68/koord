package io.github.erkko68.koord.camera

import com.google.ar.core.Coordinates2d
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.IndexBuffer
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.RenderableManager
import io.github.erkko68.filament.Texture
import io.github.erkko68.filament.TextureSampler
import io.github.erkko68.filament.VertexBuffer
import io.github.erkko68.koord.ArFrame
import io.github.erkko68.koord.ArSession
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.encoding.Base64

// Corners of a triangle covering the whole viewport, in normalised device coordinates.
private val TRIANGLE = floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)

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

    private val material = checkNotNull(
        Material.Builder().payload(Base64.decode(CAMERA_BACKGROUND_FILAMAT_BASE64)).build(engine),
    ) { "The camera background material failed to build" }

    private val materialInstance = material.createInstance().apply {
        setParameter("cameraTexture", textures.values.first(), sampler)
    }

    private val vertexBuffer = VertexBuffer.Builder()
        .bufferCount(1)
        .vertexCount(3)
        .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT2)
        .build(engine)
        .apply {
            val bytes = ByteBuffer.allocate(TRIANGLE.size * 4).order(ByteOrder.nativeOrder())
            bytes.asFloatBuffer().put(TRIANGLE)
            setBufferAt(engine, 0, bytes.array())
        }

    private val indexBuffer = IndexBuffer.Builder()
        .indexCount(3)
        .bufferType(IndexBuffer.IndexType.USHORT)
        .build(engine)
        .apply {
            val bytes = ByteBuffer.allocate(3 * 2).order(ByteOrder.nativeOrder())
            bytes.asShortBuffer().put(shortArrayOf(0, 1, 2))
            setBuffer(engine, bytes.array())
        }

    actual val entity: Int = engine.entityManager.create().also {
        RenderableManager.Builder(1)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer)
            .material(0, materialInstance)
            // Drawn first and never culled; the material ignores depth, so everything else lands on top.
            .priority(0)
            .culling(false)
            .castShadows(false)
            .receiveShadows(false)
            .build(engine, it)
    }

    private val uv = FloatArray(6)

    actual fun update(frame: ArFrame) {
        // Draw the texture this frame was written to, so the image matches the
        // frame's camera pose and ARCore is not writing it while it is drawn.
        textures[frame.frame.cameraTextureName]?.let { materialInstance.setParameter("cameraTexture", it, sampler) }

        // Where the three viewport corners fall in the camera texture, which
        // accounts for the display rotation and the crop to the viewport's shape.
        frame.frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, VIEWPORT_CORNERS,
            Coordinates2d.TEXTURE_NORMALIZED, uv,
        )
        materialInstance.setParameter("uvTransformU", uv[2] - uv[0], uv[4] - uv[0], uv[0])
        materialInstance.setParameter("uvTransformV", uv[3] - uv[1], uv[5] - uv[1], uv[1])
    }

    actual fun destroy() {
        engine.destroy(entity)
        engine.entityManager.destroy(entity)
        engine.destroy(materialInstance)
        engine.destroy(material)
        textures.values.forEach { engine.destroy(it) }
        engine.destroy(vertexBuffer)
        engine.destroy(indexBuffer)
    }
}
