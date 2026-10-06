package io.github.erkko68.koord.sample

import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.Rotation
import io.github.erkko68.koord.filament.toFilament
import io.github.erkko68.koord.trackable.Plane

/**
 * A detected plane as triangles in the plane's own space, with where that
 * space sits in the world. Keeping the two apart means a plane that only moves
 * (ARCore nudges them almost every frame) updates a transform, and its
 * geometry is only rebuilt when the boundary itself changes.
 */
class PlaneMesh(
    val positions: FloatArray,
    val normals: FloatArray,
    val uvs: FloatArray,
    val indices: IntArray,
    val position: Position,
    val rotation: Rotation,
    val vertical: Boolean,
)

/** `null` while the plane's boundary is not a polygon yet. */
fun Plane.toMesh(): PlaneMesh? {
    val polygon = polygon
    if (polygon.size < 3) return null
    // The plane's centre, then the boundary. The polygon lies in the local XZ plane, with Y as the normal.
    val vertexCount = polygon.size + 1
    val positions = FloatArray(vertexCount * 3)
    val normals = FloatArray(vertexCount * 3)
    for (i in 0 until vertexCount) normals[i * 3 + 1] = 1f
    polygon.forEachIndexed { i, vertex ->
        positions[(i + 1) * 3] = vertex.x
        positions[(i + 1) * 3 + 2] = vertex.y
    }
    // A fan around the centre, which covers any boundary the centre can see all of. A fan from
    // a boundary vertex only fits convex ones, and neither platform promises those.
    val indices = IntArray(polygon.size * 3)
    for (i in polygon.indices) {
        indices[i * 3 + 1] = i + 1
        indices[i * 3 + 2] = (i + 1) % polygon.size + 1
    }
    // Filament's matrix, for the position and rotation Koord's own type leaves to the renderer.
    val transform = transform.toFilament()
    return PlaneMesh(
        positions, normals, FloatArray(vertexCount * 2), indices,
        Position(transform.position), Rotation(transform.toQuaternion()),
        vertical = alignment == Plane.Alignment.VERTICAL,
    )
}
