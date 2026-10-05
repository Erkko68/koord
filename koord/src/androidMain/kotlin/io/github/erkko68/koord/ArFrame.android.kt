package io.github.erkko68.koord

import io.github.erkko68.filament.utils.Ray
import io.github.erkko68.koord.camera.ArCamera
import io.github.erkko68.koord.hit.HitResult
import io.github.erkko68.koord.light.LightEstimate

actual class ArFrame internal constructor() {
    actual val timestampNanos: Long get() = TODO("Not yet implemented")
    actual val camera: ArCamera get() = TODO("Not yet implemented")
    actual val lightEstimate: LightEstimate? get() = TODO("Not yet implemented")
    actual fun hitTest(xPx: Float, yPx: Float): List<HitResult> = TODO("Not yet implemented")
    actual fun hitTest(ray: Ray): List<HitResult> = TODO("Not yet implemented")
}
