//
//  SessionStoreChange+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange

/**
 * Returns a Boolean value that indicates whether the change touches
 * the given conversation: the conversation itself, one of its
 * messages, or one of its participants.
 *
 * @param conversation The conversation to test the change against.
 *
 * @return `true` if the change is relevant to the conversation;
 *   otherwise, `false`.
 */
fun SessionStoreChange.isRelevant(conversation: Conversation): Boolean =
    when (this) {
        is SessionStoreChange.Conversations ->
            conversation.id.key in upsertedIDKeys || conversation.id.key in removedIDKeys

        is SessionStoreChange.Messages ->
            conversation.messageIDs
                .toSet()
                .intersect(upsertedIDs + removedIDs)
                .isNotEmpty()

        is SessionStoreChange.Users ->
            conversation.participants
                .map { it.userID }
                .toSet()
                .intersect(upsertedIDs + removedIDs)
                .isNotEmpty()
    }
