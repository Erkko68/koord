package io.github.erkko68.koord.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.github.erkko68.koord.ArSession

class MainActivity : ComponentActivity() {
    private lateinit var session: ArSession

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showApp()
        } else {
            Toast.makeText(this, "Koord needs the camera for AR", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        session = ArSession(this)

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showApp()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showApp() = setContent { App(session) }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
    }
}
