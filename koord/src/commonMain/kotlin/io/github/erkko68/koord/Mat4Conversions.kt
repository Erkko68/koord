package io.github.erkko68.koord

import io.github.erkko68.filament.utils.Float4
import io.github.erkko68.filament.utils.Mat4

/** Reads a 16-element column-major array, the layout both ARCore and ARKit use. */
internal fun FloatArray.toMat4() = Mat4(
    Float4(this[0], this[1], this[2], this[3]),
    Float4(this[4], this[5], this[6], this[7]),
    Float4(this[8], this[9], this[10], this[11]),
    Float4(this[12], this[13], this[14], this[15]),
)
