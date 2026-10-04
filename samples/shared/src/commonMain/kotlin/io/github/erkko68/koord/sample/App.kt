package io.github.erkko68.koord.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.erkko68.koord.koordPlaceholder

// Placeholder until the passthrough spike lands — see docs/spike-camera-passthrough.md.
@Composable
fun App() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(koordPlaceholder())
    }
}
