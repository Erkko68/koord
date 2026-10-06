package io.github.erkko68.koord.sample

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.erkko68.koord.ArSession
import io.github.erkko68.koord.DisplayRotation
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIWindowScene

fun MainViewController() = ComposeUIViewController {
    App(remember { ArSession() }) { displayRotation() }
}

private fun displayRotation(): DisplayRotation {
    val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull() as? UIWindowScene
    return when (scene?.interfaceOrientation) {
        UIInterfaceOrientationLandscapeRight -> DisplayRotation.ROTATION_90
        UIInterfaceOrientationPortraitUpsideDown -> DisplayRotation.ROTATION_180
        UIInterfaceOrientationLandscapeLeft -> DisplayRotation.ROTATION_270
        else -> DisplayRotation.ROTATION_0
    }
}
