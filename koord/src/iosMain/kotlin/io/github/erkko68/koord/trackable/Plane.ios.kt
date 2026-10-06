@file:OptIn(ExperimentalForeignApi::class)

package io.github.erkko68.koord.trackable

import io.github.erkko68.filament.utils.Float2
import io.github.erkko68.filament.utils.Float4
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.toMat4
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.value
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARPlaneAnchorAlignment
import kotlin.math.cos
import kotlin.math.sin

/**
 * ARKit describes a plane relative to its anchor: a centre, and an extent
 * turned about the anchor's Y axis. Koord's plane space is centred on the
 * plane and aligned with its extent, as ARCore's is, so [transform] and
 * [polygon] are converted.
 */
actual class Plane internal constructor(private val session: ArSession, private val anchor: ARPlaneAnchor) {
    private val current: ARPlaneAnchor? get() = session.current(anchor) as? ARPlaneAnchor

    actual val transform: Mat4
        get() {
            val plane = current ?: anchor
            val angle = plane.planeExtent.rotationOnYAxis
            val c = cos(angle)
            val s = sin(angle)
            val center = plane.center
            // Rotation about Y by the extent's angle, then translation to the centre.
            val centerFromAnchor = Mat4(
                Float4(c, 0f, -s, 0f),
                Float4(0f, 1f, 0f, 0f),
                Float4(s, 0f, c, 0f),
                Float4(center.getFloatAt(0), center.getFloatAt(1), center.getFloatAt(2), 1f),
            )
            return plane.transform.toMat4() * centerFromAnchor
        }

    actual val extentX: Float get() = (current ?: anchor).planeExtent.width

    actual val extentZ: Float get() = (current ?: anchor).planeExtent.height

    actual val polygon: List<Float2>
        get() {
            val plane = current ?: anchor
            val vertices = plane.geometry.boundaryVertices ?: return emptyList()
            val angle = plane.planeExtent.rotationOnYAxis
            val c = cos(angle)
            val s = sin(angle)
            val centerX = plane.center.getFloatAt(0)
            val centerZ = plane.center.getFloatAt(2)
            // From the anchor's space to the plane's: the inverse of the matrix in [transform].
            return List(plane.geometry.boundaryVertexCount.toInt()) {
                val x = vertices[it].value.getFloatAt(0) - centerX
                val z = vertices[it].value.getFloatAt(2) - centerZ
                Float2(x * c - z * s, x * s + z * c)
            }
        }

    actual val alignment: Alignment
        get() = if (anchor.alignment == ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical) Alignment.VERTICAL
        else Alignment.HORIZONTAL

    // ARKit drops a plane it no longer tracks from the frame's anchors.
    actual val trackingState: TrackingState
        get() = if (current != null) TrackingState.TRACKING else TrackingState.STOPPED

    // Wrappers are created on demand, so identity is the ARKit anchor's.
    override fun equals(other: Any?) = other is Plane && other.anchor.identifier == anchor.identifier
    override fun hashCode() = anchor.identifier.hashCode()

    actual enum class Alignment { HORIZONTAL, VERTICAL }
}
