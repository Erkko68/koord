package io.github.erkko68.koord.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.compose.FilamentView
import io.github.erkko68.filament.compose.rememberFilamentScene
import io.github.erkko68.filament.compose.rememberFilamentViewState
import io.github.erkko68.koord.ArConfig
import io.github.erkko68.koord.ArException
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.DisplayRotation
import io.github.erkko68.koord.filament.createEngine

/**
 * Koord with Filament, as a Compose screen: the camera image fills the
 * screen, detected surfaces are tinted, tapping one places a cube on it, and
 * the cubes are lit like the room.
 *
 * This file is the screen. [ArSessionEffect] is where Koord meets Filament
 * each frame, and `ArContent.kt` is what gets drawn from the result.
 *
 * The session is created by the platform entry point, since its constructor is
 * platform-specific. The camera permission must be granted before this is
 * shown.
 *
 * @param displayRotation the current rotation of the screen, read every frame
 */
@Composable
fun App(session: ArSession, displayRotation: () -> DisplayRotation = { DisplayRotation.ROTATION_0 }) {
    val state = remember { ArSceneState() }

    // The session holds the camera, so it only runs while the app is in front.
    LifecycleResumeEffect(session) {
        try {
            session.configure(ArConfig(depth = true))
            session.resume()
        } catch (e: ArException) {
            state.status = "AR failed to start: ${e.message}"
        }
        onPauseOrDispose { session.pause() }
    }

    // Only an engine made for the session can draw its camera image. Declared
    // first so that it is destroyed last, after everything created from it.
    val engine = remember(session) { session.createEngine() }
    DisposableEffect(engine) {
        onDispose { Engine.destroy(engine) }
    }

    val viewState = rememberFilamentViewState()

    val scene = rememberFilamentScene(engine) {
        ArSessionEffect(session, viewState, state, displayRotation)
        EstimatedLight(state.light)
        DetectedPlanes(state.planes)
        AnchorCubes(state.anchors)
    }

    val text = TextStyle(color = Color.White)
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { state.viewport = it }
            .pointerInput(Unit) { detectTapGestures { state.tap = it } },
    ) {
        FilamentView(scene, Modifier.fillMaxSize(), viewState = viewState)
        BasicText(state.status, Modifier.align(Alignment.TopCenter).safeDrawingPadding(), style = text)
        if (state.anchors.isNotEmpty()) {
            BasicText(
                "Remove the cubes",
                Modifier.align(Alignment.BottomCenter).safeDrawingPadding().clickable { state.clear = true }.padding(16.dp),
                style = text,
            )
        }
    }
}
