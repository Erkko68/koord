package io.github.erkko68.koord.sample

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.erkko68.koord.ArConfig
import io.github.erkko68.koord.ArException
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.DisplayRotation

/**
 * First use of the common API: run a session, read each frame, and drop an
 * anchor where the user taps a detected plane. There is no rendering yet, so
 * the screen only shows what the session reports.
 *
 * The session is created by the platform entry point, since its constructor is
 * platform-specific. The camera permission must be granted before this is
 * shown.
 */
@Composable
fun App(session: ArSession) {
    var status by remember { mutableStateOf("Starting…") }
    var tap by remember { mutableStateOf<Offset?>(null) }

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

    LaunchedEffect(session) {
        while (true) {
            withFrameNanos { }
            val frame = try {
                session.update() ?: continue
            } catch (e: ArException) {
                status = "AR failed: ${e.message}"
                break
            }
            tap?.let {
                frame.hitTest(it.x, it.y).firstOrNull()?.createAnchor()
                tap = null
            }
            status = "${frame.camera.trackingState} · " +
                "${session.planes.size} planes · ${session.anchors.size} anchors"
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // ponytail: assumes portrait; pass the real display rotation once the sample renders.
            .onSizeChanged { session.setDisplayGeometry(DisplayRotation.ROTATION_0, it.width, it.height) }
            .pointerInput(Unit) { detectTapGestures { tap = it } },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(status)
    }
}
