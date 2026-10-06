package io.github.erkko68.koord.camera

import io.github.erkko68.filament.Engine
import io.github.erkko68.koord.ArFrame
import io.github.erkko68.koord.ArSession

/**
 * Draws the session's camera image behind everything else in a Filament
 * scene, filling the viewport. This is what makes rendered content look
 * placed in the real world.
 *
 * Add [entity] to the scene, call [update] with every new frame, and
 * [destroy] it before the engine. The view does not need to be transparent.
 *
 * On Android it draws the external texture ARCore writes. On iOS it does not
 * draw anything yet: [entity] is empty and the scene keeps its own background.
 *
 * @param engine must come from [ArSession.createEngine] of the same [session];
 *   an engine created any other way cannot see the camera image
 * @param session the session whose camera image is drawn
 */
expect class CameraBackground(engine: Engine, session: ArSession) {

    /** The renderable to add to the Filament scene. */
    val entity: Int

    /**
     * Fits the camera image of [frame] to the viewport set with
     * [ArSession.setDisplayGeometry]. Call it once for every frame returned by
     * [ArSession.update].
     */
    fun update(frame: ArFrame)

    /**
     * Releases the Filament resources. Remove [entity] from the scene first,
     * and call this before destroying the engine.
     */
    fun destroy()
}
