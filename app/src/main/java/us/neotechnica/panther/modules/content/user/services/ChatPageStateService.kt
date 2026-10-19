//
//  ChatPageStateService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.bundle.chatPageState
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.modules.content.user.extensions.drainEffects
import us.neotechnica.panther.modules.content.user.models.ChatPageStateServiceEffectID
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Tracks whether the chat page is presented and schedules one-shot
 * effects on presentation changes.
 */
object ChatPageStateService {
    // MARK: - Dependencies

    private val navigation: Navigation by Dependency { it.navigation }

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

    // MARK: - Is Conversation Still Presented

    /**
     * Returns whether the given conversation's chat page is on
     * the navigation stack: either it is the top page, or a
     * sub-page of the same conversation – its chat info – was
     * pushed over it. A chat page being disposed while this holds
     * is not being left, as when the new chat page hands off to
     * the conversation after its first message is sent.
     *
     * @param conversationIDKey The conversation's identifier key.
     */
    fun isConversationStillPresented(conversationIDKey: String): Boolean {
        val topPath =
            navigation.state.value.userContent.stack
                .lastOrNull()
        return when (topPath) {
            is UserContentNavigatorState.SeguePaths.Chat -> topPath.conversationIDKey == conversationIDKey
            is UserContentNavigatorState.SeguePaths.ChatInfo -> topPath.conversationIDKey == conversationIDKey
            else -> false
        }
    }

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
        val effects = registry.drainEffects()
        if (effects.isEmpty()) return

        Logger.log(
            "Running effects for change of \"isPresented\" to ${if (isPresented) "TRUE" else "FALSE"}. " +
                "[EnqueuedEffectIDs: ${effects.keys.map { it.rawValue }}]",
            domain = LoggerDomain.chatPageState,
        )
        effects.values.forEach { it() }
    }
}
