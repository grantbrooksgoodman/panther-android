//
//  ChatPageReducer+SessionStoreChange.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

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
    val currentConversation = DependencyValues.current.clientSession.entity.conversation.currentConversation ?: return false
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
