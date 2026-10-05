package io.github.erkko68.koord

import android.content.Context
import com.google.ar.core.ArCoreApk

/**
 * Checks whether AR can run on this device. See [ArAvailability].
 *
 * ponytail: ARCore may still be querying Google Play when this is called;
 * that and its error states are reported as [ArAvailability.SUPPORTED], and
 * [ArSession.resume] throws [ArException.Unsupported] if that turns out wrong.
 * Switch to `ArCoreApk.checkAvailabilityAsync` if a definite answer is needed.
 */
fun checkArAvailability(context: Context): ArAvailability =
    when (ArCoreApk.getInstance().checkAvailability(context)) {
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArAvailability.UNSUPPORTED
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ArAvailability.NEEDS_INSTALL
        else -> ArAvailability.SUPPORTED
    }
