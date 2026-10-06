package io.github.erkko68.koord.filament

import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.IndexBuffer
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.MaterialInstance
import io.github.erkko68.filament.RenderableManager
import io.github.erkko68.filament.VertexBuffer
import io.github.erkko68.filament.toBytes
import kotlin.io.encoding.Base64

// Corners of a triangle covering the whole viewport, in normalised device coordinates.
private val TRIANGLE = floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)

/**
 * What [CameraBackground] draws on both platforms: one triangle over the
 * whole viewport, behind everything else, with the platform's camera
 * material. The platform sets the material's textures and calls
 * [setUvTransform].
 *
 * @param materialPackage the compiled camera background material
 */
internal class CameraQuad(private val engine: Engine, materialPackage: String) {
    private val material = checkNotNull(Material.Builder().payload(Base64.decode(materialPackage)).build(engine)) {
        "The camera background material failed to build"
    }

    val materialInstance: MaterialInstance = material.createInstance()

    private val vertexBuffer = VertexBuffer.Builder()
        .bufferCount(1)
        .vertexCount(3)
        .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT2)
        .build(engine)
        .apply { setBufferAt(engine, 0, TRIANGLE.toBytes()) }

    private val indexBuffer = IndexBuffer.Builder()
        .indexCount(3)
        .bufferType(IndexBuffer.IndexType.USHORT)
        .build(engine)
        .apply { setBuffer(engine, shortArrayOf(0, 1, 2).toBytes()) }

    val entity: Int = engine.entityManager.create().also {
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

    /**
     * Sets where the viewport falls in the camera image.
     *
     * @param uv camera texture coordinates of three viewport corners, as
     *   `[u, v]` pairs: bottom-left, bottom-right, top-left
     */
    fun setUvTransform(uv: FloatArray) {
        materialInstance.setParameter("uvTransformU", uv[2] - uv[0], uv[4] - uv[0], uv[0])
        materialInstance.setParameter("uvTransformV", uv[3] - uv[1], uv[5] - uv[1], uv[1])
    }

    fun destroy() {
        engine.destroy(entity)
        engine.entityManager.destroy(entity)
        engine.destroy(materialInstance)
        engine.destroy(material)
        engine.destroy(vertexBuffer)
        engine.destroy(indexBuffer)
    }
}
