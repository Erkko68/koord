package io.github.erkko68.koord.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.DirectionalLight
import io.github.erkko68.filament.compose.scene.LightIntensity
import io.github.erkko68.filament.compose.scene.LinearColor
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.Rotation
import io.github.erkko68.filament.compose.scene.SunLight
import io.github.erkko68.filament.compose.scene.primitives.Cube
import io.github.erkko68.filament.compose.scene.primitives.Mesh
import io.github.erkko68.filament.compose.scene.rememberColorMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberTransparentColorMaterialInstance
import io.github.erkko68.filament.utils.Mat4
import io.github.erkko68.koord.light.LightEstimate

private const val CUBE_SIZE = 0.1f

// The lights of a neutrally lit room, which the estimate scales. Tune on a device.
private const val SUN_LUMENS = 100_000f
private const val FILL_LUMENS = 30_000f

/**
 * Lights the scene like the room: Koord's [LightEstimate] scales and tints
 * the lights, so a cube dims when the room does. Without an estimate the
 * lights are neutral.
 */
@Composable
fun FilamentSceneScope.EstimatedLight(estimate: LightEstimate?) {
    val intensity = estimate?.intensity ?: 1f
    val color = estimate?.color?.let { LinearColor(it.x, it.y, it.z) } ?: LinearColor(1f)
    SunLight(color = color, intensity = LightIntensity.LuminousPower(SUN_LUMENS * intensity))
    // A weaker light from the other side, so faces turned away from the sun are not black.
    DirectionalLight(
        direction = Direction(-0.3f, -0.4f, 0.5f),
        color = color,
        intensity = LightIntensity.LuminousPower(FILL_LUMENS * intensity),
    )
}

/** Tints every detected surface: floors and tables blue, walls orange. */
@Composable
fun FilamentSceneScope.DetectedPlanes(planes: List<PlaneMesh>) {
    val horizontal = rememberTransparentColorMaterialInstance(LinearColor(0.2f, 0.6f, 1f), alpha = 0.3f)
    val vertical = rememberTransparentColorMaterialInstance(LinearColor(1f, 0.6f, 0.2f), alpha = 0.3f)
    planes.forEachIndexed { index, plane ->
        key(index) {
            Mesh(
                material = if (plane.vertical) vertical else horizontal,
                positions = plane.positions,
                normals = plane.normals,
                uvs = plane.uvs,
                indices = plane.indices,
                position = plane.position,
                rotation = plane.rotation,
                castShadows = false,
                receiveShadows = false,
            )
        }
    }
}

/** A cube standing on each anchor, turned the way the anchor is. */
@Composable
fun FilamentSceneScope.AnchorCubes(anchors: List<Mat4>) {
    val red = rememberColorMaterialInstance(LinearColor(0.9f, 0.25f, 0.3f))
    anchors.forEachIndexed { index, transform ->
        key(index) {
            Cube(
                material = red,
                size = CUBE_SIZE,
                position = Position(transform.position),
                rotation = Rotation(transform.toQuaternion()),
                // The middle of the bottom face goes on the anchor. An anchor made from a hit
                // has the surface's normal as its Y axis, so the cube stands on walls too.
                pivot = Position(0f, -CUBE_SIZE / 2, 0f),
            )
        }
    }
}
