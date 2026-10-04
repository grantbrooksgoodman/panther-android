//
//  ChatPageStateService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.modules.content.user.models.ChatPageStateServiceEffectID
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Tracks whether the chat page is presented and schedules one-shot
 * effects on presentation changes.
 */
object ChatPageStateService {
    // MARK: - Properties

    /**
     * A Boolean value that indicates whether the chat page is
     * presented.
     */
    var isPresented: Boolean = false
        private set

    private val uponIsPresentedChangedToFalse =
        LockIsolated(mapOf<ChatPageStateServiceEffectID, () -> Unit>())
    private val uponIsPresentedChangedToTrue =
        LockIsolated(mapOf<ChatPageStateServiceEffectID, () -> Unit>())

    // MARK: - Setters

    /**
     * Sets whether the chat page is presented.
     *
     * Each assignment runs – and clears – the effects registered
     * for the assigned value, whether or not the value changed.
     *
     * @param isPresented A Boolean value that indicates whether the
     *   chat page is presented.
     */
    fun setIsPresented(isPresented: Boolean) {
        this.isPresented = isPresented
        didSetIsPresented()
    }

    // MARK: - Add Effect

    /**
     * Registers an effect to run once, the next time [isPresented]
     * is set to the given value.
     *
     * The effect is cleared after it runs. Registering a new effect
     * with the same identifier and target value replaces the
     * existing one.
     *
     * @param state The value of [isPresented] that triggers the
     *   effect.
     * @param id The identifier under which to register the effect.
     * @param effect The effect to run.
     */
    fun addEffectUponIsPresented(
        state: Boolean,
        id: ChatPageStateServiceEffectID,
        effect: () -> Unit,
    ) {
        val registry = if (state) uponIsPresentedChangedToTrue else uponIsPresentedChangedToFalse
        registry.withValue { it.value = it.value + (id to effect) }
    }

    // MARK: - Auxiliary

    private fun didSetIsPresented() {
        val registry = if (isPresented) uponIsPresentedChangedToTrue else uponIsPresentedChangedToFalse
        val effects = drainEffects(registry)
        if (effects.isEmpty()) return

        Logger.log(
            "Running effects for change of \"isPresented\" to ${if (isPresented) "TRUE" else "FALSE"}. " +
                "[EnqueuedEffectIDs: ${effects.keys.map { it.rawValue }}]",
            domain = LoggerDomain.chatPageState,
        )
        effects.values.forEach { it() }
    }

    private fun drainEffects(
        effects: LockIsolated<Map<ChatPageStateServiceEffectID, () -> Unit>>,
    ): Map<ChatPageStateServiceEffectID, () -> Unit> =
        effects.withValue { ref ->
            val drained = ref.value
            if (drained.isNotEmpty()) ref.value = emptyMap()
            drained
        }
}
