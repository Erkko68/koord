package io.github.erkko68.koord.sample

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.compose.FilamentEffect
import io.github.erkko68.filament.compose.FilamentView
import io.github.erkko68.filament.compose.rememberFilamentScene
import io.github.erkko68.filament.compose.rememberFilamentViewState
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.DirectionalLight
import io.github.erkko68.filament.compose.scene.LightIntensity
import io.github.erkko68.filament.compose.scene.LinearColor
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.SunLight
import io.github.erkko68.filament.compose.scene.primitives.Cube
import io.github.erkko68.filament.compose.scene.primitives.Mesh
import io.github.erkko68.filament.compose.scene.rememberColorMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberTransparentColorMaterialInstance
import io.github.erkko68.filament.utils.Float4
import io.github.erkko68.koord.ArConfig
import io.github.erkko68.koord.ArException
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.DisplayRotation
import io.github.erkko68.koord.camera.CameraBackground
import io.github.erkko68.koord.trackable.Plane

private const val NEAR = 0.05f
private const val FAR = 100f
private const val CUBE_SIZE = 0.1f

/**
 * First use of Koord with Filament: the camera image fills the screen,
 * detected surfaces are tinted, and tapping one places a cube there.
 *
 * The session is created by the platform entry point, since its constructor is
 * platform-specific. The camera permission must be granted before this is
 * shown.
 *
 * @param displayRotation the current rotation of the screen, read every frame
 */
@Composable
fun App(session: ArSession, displayRotation: () -> DisplayRotation = { DisplayRotation.ROTATION_0 }) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var status by remember { mutableStateOf("Starting…") }
    var tap by remember { mutableStateOf<Offset?>(null) }
    var cubes by remember { mutableStateOf(emptyList<Position>()) }
    var planes by remember { mutableStateOf(emptyList<PlaneMesh>()) }

    // The session holds the camera, so it only runs while the app is in front.
    LifecycleResumeEffect(session) {
        try {
            session.configure(ArConfig())
            session.resume()
        } catch (e: ArException) {
            status = "AR failed to start: ${e.message}"
        }
        onPauseOrDispose { session.pause() }
    }

    // Only an engine from the session can draw its camera image. Declared
    // first so that it is destroyed last, after everything created from it.
    val engine = remember(session) { session.createEngine() }
    DisposableEffect(engine) {
        onDispose { Engine.destroy(engine) }
    }

    val viewState = rememberFilamentViewState()

    val scene = rememberFilamentScene(engine) {
        SunLight()
        // A weaker light from the other side, so faces turned away from the sun are not black.
        DirectionalLight(direction = Direction(-0.3f, -0.4f, 0.5f), intensity = LightIntensity.LuminousPower(30_000f))

        FilamentEffect(session) {
            val background = CameraBackground(engine, session)
            scene.addEntity(background.entity)

            onFrame {
                // Every frame: turning the device upside down changes the rotation but not the size.
                if (size != IntSize.Zero) session.setDisplayGeometry(displayRotation(), size.width, size.height)

                val frame = try {
                    session.update()
                } catch (e: ArException) {
                    status = "AR failed: ${e.message}"
                    null
                } ?: return@onFrame

                background.update(frame)

                // Look through the device's camera: its lens and its pose.
                viewState.view?.camera?.let { camera ->
                    val projection = frame.camera.projectionMatrix(NEAR, FAR).toFloatArrayColumn()
                    camera.setCustomProjection(DoubleArray(16) { projection[it].toDouble() }, NEAR.toDouble(), FAR.toDouble())
                    camera.setModelMatrix(frame.camera.transform.toFloatArrayColumn())
                }

                tap?.let {
                    frame.hitTest(it.x, it.y).firstOrNull()?.createAnchor()
                    tap = null
                }

                // Anchors move as tracking improves, so the cubes follow them every frame.
                cubes = session.anchors.map { anchor ->
                    val p = anchor.transform.position
                    Position(p.x, p.y + CUBE_SIZE / 2, p.z)
                }
                // Planes grow and move as more of the surface is seen.
                planes = session.planes.mapNotNull { it.toMesh() }
                status = "${frame.camera.trackingState} · " +
                    "${planes.size} planes · ${cubes.size} anchors"
            }

            onDispose {
                scene.remove(background.entity)
                background.destroy()
            }
        }

        val tint = rememberTransparentColorMaterialInstance(LinearColor(0.2f, 0.6f, 1f), alpha = 0.3f)
        planes.forEachIndexed { index, plane ->
            key(index) {
                Mesh(
                    material = tint,
                    positions = plane.positions,
                    normals = plane.normals,
                    uvs = plane.uvs,
                    indices = plane.indices,
                    castShadows = false,
                    receiveShadows = false,
                )
            }
        }

        val red = rememberColorMaterialInstance(LinearColor(0.9f, 0.25f, 0.3f))
        cubes.forEachIndexed { index, position ->
            key(index) { Cube(material = red, size = CUBE_SIZE, position = position) }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) { detectTapGestures { tap = it } },
    ) {
        FilamentView(scene, Modifier.fillMaxSize(), viewState = viewState)
        BasicText(
            status,
            Modifier.align(Alignment.TopCenter).safeDrawingPadding(),
            style = TextStyle(color = Color.White),
        )
    }
}

/** The boundary of a detected plane as triangles in world space. */
private class PlaneMesh(val positions: FloatArray, val normals: FloatArray, val uvs: FloatArray, val indices: IntArray)

private fun Plane.toMesh(): PlaneMesh? {
    val polygon = polygon
    if (polygon.size < 3) return null
    val transform = transform
    val normal = transform.y
    val positions = FloatArray(polygon.size * 3)
    val normals = FloatArray(polygon.size * 3)
    polygon.forEachIndexed { i, vertex ->
        // The polygon lies in the plane's local XZ plane.
        val p = transform * Float4(vertex.x, 0f, vertex.y, 1f)
        positions[i * 3] = p.x
        positions[i * 3 + 1] = p.y
        positions[i * 3 + 2] = p.z
        normals[i * 3] = normal.x
        normals[i * 3 + 1] = normal.y
        normals[i * 3 + 2] = normal.z
    }
    // ponytail: a fan from the first vertex, exact only for convex polygons. ARKit's can be
    // concave; triangulate by ear clipping if the tint spills outside a boundary.
    val indices = IntArray((polygon.size - 2) * 3) { if (it % 3 == 0) 0 else it / 3 + it % 3 }
    return PlaneMesh(positions, normals, FloatArray(polygon.size * 2), indices)
}
