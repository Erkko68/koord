package io.github.erkko68.koord.trackable

import io.github.erkko68.filament.utils.Float2
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.toKoord
import io.github.erkko68.koord.toMat4
import com.google.ar.core.Pose
import com.google.ar.core.Session
import kotlin.math.abs
import com.google.ar.core.Plane as ArCorePlane
import com.google.ar.core.TrackingState as ArCoreTrackingState

// How far apart, in metres along the normal, two overlapping parallel planes
// can be and still count as one surface seen twice. Tune on a device: higher
// hides more of ARCore's stacked duplicates, but also low real surfaces.
private const val LAYER_DISTANCE = 0.1f

// Cosine of the largest angle between the normals of two "parallel" planes.
private const val LAYER_ALIGNMENT = 0.9f

/**
 * The planes Koord reports: tracked, not merged away, and not a layer of a
 * larger one. ARCore starts a new plane on almost any flat patch (the top of
 * an object, a rug, the same floor a few centimetres off) and only merges
 * them later, if ever; ARKit holds such patches back. The hidden planes are
 * still tracked by ARCore, so one that outgrows its neighbour comes back.
 */
internal fun Session.reportedPlanes(): List<ArCorePlane> {
    val kept = mutableListOf<ArCorePlane>()
    getAllTrackables(ArCorePlane::class.java)
        .filter { it.subsumedBy == null && it.trackingState != ArCoreTrackingState.STOPPED }
        .sortedByDescending { it.extentX * it.extentZ }
        .forEach { plane -> if (kept.none { plane.isLayerOf(it) }) kept += plane }
    return kept
}

// ponytail: only this plane's centre is tested against the larger polygon, so a layer that
// overlaps from the side survives. Intersect the polygons if those show up.
private fun ArCorePlane.isLayerOf(larger: ArCorePlane): Boolean {
    val local = larger.centerPose.inverse().compose(centerPose)
    return abs(local.ty()) < LAYER_DISTANCE &&
        local.yAxis[1] > LAYER_ALIGNMENT &&
        larger.isPoseInPolygon(larger.centerPose.compose(Pose.makeTranslation(local.tx(), 0f, local.tz())))
}

actual class Plane internal constructor(private val plane: ArCorePlane) {
    actual val transform: Mat4 get() = plane.centerPose.toMat4()

    actual val extentX: Float get() = plane.extentX

    actual val extentZ: Float get() = plane.extentZ

    actual val polygon: List<Float2>
        get() {
            // [x0, z0, x1, z1, …] in the plane's local space.
            val vertices = plane.polygon
            return List(vertices.remaining() / 2) { Float2(vertices.get(), vertices.get()) }
        }

    actual val alignment: Alignment
        get() = if (plane.type == ArCorePlane.Type.VERTICAL) Alignment.VERTICAL else Alignment.HORIZONTAL

    actual val trackingState: TrackingState get() = plane.trackingState.toKoord()

    // Wrappers are created on demand, so identity is the ARCore plane's.
    override fun equals(other: Any?) = other is Plane && other.plane == plane
    override fun hashCode() = plane.hashCode()

    actual enum class Alignment { HORIZONTAL, VERTICAL }
}
