//
//  ChatPageReducer+SessionStoreChange.kt
//  Panther
//
//  Created by Grant Brooks Goodman.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange

/**
 * Determines whether the given store change affects the displayed
 * conversation and should reload the message list.
 *
 * A change is relevant when it upserts the displayed conversation, or
 * when it upserts or removes any of the conversation's messages. User
 * changes never trigger a reload.
 *
 * @param change The session store change to evaluate.
 *
 * @return `true` when the change should reload the message list;
 *   otherwise, `false`.
 */
internal fun ChatPageReducer.shouldReload(change: SessionStoreChange): Boolean {
    val currentConversation = ConversationSessionService.currentConversation ?: return false
    return when (change) {
        is SessionStoreChange.Conversations ->
            currentConversation.id.key in change.upsertedIDKeys

        is SessionStoreChange.Messages ->
            currentConversation.messageIDs
                .toSet()
                .intersect(change.upsertedIDs + change.removedIDs)
                .isNotEmpty()

        is SessionStoreChange.Users -> false
    }
}
