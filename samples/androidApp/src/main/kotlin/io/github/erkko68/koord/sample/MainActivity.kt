package io.github.erkko68.koord.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.erkko68.koord.ArSession

class MainActivity : ComponentActivity() {
    private lateinit var session: ArSession

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // The camera permission is not requested here yet; without it the
        // session reports ArException.CameraPermissionDenied.
        session = ArSession(this)

        setContent {
            App(session)
        }
    }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
    }
}
