//
//  User+SessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * The signed-in user's identifier, from persistent storage.
 */
val User.Companion.currentUserID: String?
    get() = Persistent.string(PersistentStorageKey.currentUserID)

/**
 * The user's conversations resolved from the [SessionStore], or `null`
 * if the user has none recorded.
 *
 * Each conversation ID is matched exactly, then by key alone.
 */
val User.conversations: List<Conversation>?
    get() {
        val conversationIDs = conversationIDs ?: return null
        return conversationIDs.mapNotNull { conversationID ->
            SessionStore.getConversation(conversationID) ?: SessionStore.getConversation(conversationID.key)
        }
    }

// MARK: - Methods

/**
 * Returns the number of unread incoming messages across the user's
 * visible conversations.
 *
 * @return The unread message count, or `0` for users other than the
 *   current user.
 */
fun User.calculateBadgeNumber(): Int {
    if (id != User.currentUserID) return 0
    val conversations = conversations ?: return 0
    return conversations
        .filter { it.isVisibleForCurrentUser }
        .flatMap { it.messages ?: emptyList() }
        .filter { !it.isFromCurrentUser && it.currentUserReadReceipt == null }
        .count()
}
