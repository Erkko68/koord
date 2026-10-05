package io.github.erkko68.koord.sample

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.erkko68.koord.ArSession

fun MainViewController() = ComposeUIViewController {
    App(remember { ArSession() })
}
