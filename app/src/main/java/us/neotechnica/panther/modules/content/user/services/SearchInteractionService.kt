//
//  SearchInteractionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.delay
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction

/**
 * Manages focusing a message from search.
 *
 * Opens the context menu for a message the user navigated to from
 * search, so the focused message is emphasized when the chat page
 * appears. The interaction is triggered at most once per
 * presentation.
 *
 * @property focusedMessageID The identifier of the message to
 *   focus, or `null` if no message should be focused.
 * @property present Presents the context menu registered under the
 *   given key, returning whether one was presented.
 */
class SearchInteractionService(
    private val focusedMessageID: String?,
    private val present: (String) -> Boolean,
) {
    // MARK: - Properties

    private var hasTriggeredInteractionOnce = false

    // MARK: - Trigger Focused Message Cell Interaction

    /**
     * Opens the context menu for the focused message, if one is
     * pending.
     *
     * This method has no effect when the chat page is not
     * presented, or when it has already triggered the interaction
     * during the current presentation. When the focused message's
     * cell is not yet available, the interaction is retried once
     * after a brief delay.
     */
    suspend fun triggerFocusedMessageCellInteractionIfNeeded() {
        if (!ChatPageStateService.isPresented || hasTriggeredInteractionOnce) return
        triggerCellInteraction()
    }

    private suspend fun triggerCellInteraction(retryOnFailure: Boolean = true) {
        val key = focusedMessageID
        if (!ChatPageStateService.isPresented ||
            key == null ||
            !ContextMenuInteraction.canBegin ||
            !present(key)
        ) {
            if (!retryOnFailure) return
            delay(TRIGGER_FOCUSED_MESSAGE_CELL_INTERACTION_DELAY_MILLISECONDS)
            triggerCellInteraction(retryOnFailure = false)
            return
        }

        hasTriggeredInteractionOnce = true
    }

    // MARK: - Companion

    companion object {
        private const val TRIGGER_FOCUSED_MESSAGE_CELL_INTERACTION_DELAY_MILLISECONDS = 500L
    }
}
