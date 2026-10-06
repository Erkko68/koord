package io.github.erkko68.koord

import platform.ARKit.ARWorldTrackingConfiguration

/** Checks whether AR can run on this device. See [ArAvailability]. */
fun checkArAvailability(): ArAvailability =
    if (ARWorldTrackingConfiguration.isSupported) ArAvailability.SUPPORTED else ArAvailability.UNSUPPORTED
