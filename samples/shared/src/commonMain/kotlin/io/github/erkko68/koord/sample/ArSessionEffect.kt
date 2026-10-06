package io.github.erkko68.koord.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.erkko68.filament.compose.FilamentEffect
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.FilamentViewState
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.ArException
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.DisplayRotation
import io.github.erkko68.koord.TrackingState
import io.github.erkko68.koord.camera.ArCamera
import io.github.erkko68.koord.filament.CameraBackground
import io.github.erkko68.koord.filament.toFilament
import io.github.erkko68.koord.hit.HitTarget
import io.github.erkko68.koord.light.LightEstimate

private const val NEAR = 0.05f
private const val FAR = 100f

/**
 * What the screen and the session tell each other: [ArSessionEffect] fills in
 * the first group every frame, the screen sets the second.
 */
class ArSceneState {
    var status by mutableStateOf("Starting…")
    var light by mutableStateOf<LightEstimate?>(null)
    var planes by mutableStateOf(emptyList<PlaneMesh>())

    /** World transforms of the anchors, as Filament's matrices. */
    var anchors by mutableStateOf(emptyList<Mat4>())

    /** Size of the view, in pixels. */
    var viewport by mutableStateOf(IntSize.Zero)

    /** A tap to place an anchor at, taken by the next frame. */
    var tap: Offset? = null

    /** Asks the next frame to detach every anchor. */
    var clear = false
}

/**
 * Runs [session] from Filament's render loop. Each rendered frame it takes
 * the session's frame and hands Filament what it needs from it: the camera
 * image, the camera's lens and pose, and through [state] the light, planes
 * and anchors for the scene to draw.
 *
 * @param viewState of the view that shows the scene, for its camera
 * @param displayRotation the current rotation of the screen, read every frame
 */
@Composable
fun FilamentSceneScope.ArSessionEffect(
    session: ArSession,
    viewState: FilamentViewState,
    state: ArSceneState,
    displayRotation: () -> DisplayRotation,
) {
    FilamentEffect(session) {
        val background = CameraBackground(engine, session)
        scene.addEntity(background.entity)

        onFrame {
            // Every frame: turning the device upside down changes the rotation but not the size.
            val viewport = state.viewport
            if (viewport != IntSize.Zero) session.setDisplayGeometry(displayRotation(), viewport.width, viewport.height)

            val frame = try {
                session.update()
            } catch (e: ArException) {
                state.status = "AR failed: ${e.message}"
                null
            } ?: return@onFrame

            background.update(frame)

            // Look through the device's camera: its lens and its pose.
            viewState.view?.camera?.let { camera ->
                val projection = frame.camera.projectionMatrix(NEAR, FAR).toFloatArray()
                camera.setCustomProjection(DoubleArray(16) { projection[it].toDouble() }, NEAR.toDouble(), FAR.toDouble())
                camera.setModelMatrix(frame.camera.transform.toFloatArray())
            }

            if (state.clear) {
                session.anchors.forEach { it.detach() }
                state.clear = false
            }
            state.tap?.let {
                // Inside a detected surface if there is one, otherwise on its extension: a
                // plain wall is rarely covered beyond a patch.
                (frame.hitTest(it.x, it.y).firstOrNull()
                    ?: frame.hitTest(it.x, it.y, HitTarget.PLANE_INFINITE).firstOrNull())?.createAnchor()
                state.tap = null
            }

            state.light = frame.lightEstimate
            // Anchors move as tracking improves, and planes grow as more of the surface is
            // seen, so both are read again every frame.
            state.anchors = session.anchors.map { it.transform.toFilament() }
            state.planes = session.planes.mapNotNull { it.toMesh() }
            state.status = frame.camera.status(state.planes.size, state.anchors.size)
        }

        onDispose {
            scene.remove(background.entity)
            background.destroy()
        }
    }
}

private fun ArCamera.status(planes: Int, anchors: Int): String = when {
    trackingState != TrackingState.TRACKING ->
        "Tracking ${trackingState.name.lowercase()}: ${trackingFailureReason.name.lowercase().replace('_', ' ')}"
    planes == 0 -> "Move the device around to find a surface"
    else -> "$planes surfaces · $anchors cubes · tap a surface to place a cube"
}
