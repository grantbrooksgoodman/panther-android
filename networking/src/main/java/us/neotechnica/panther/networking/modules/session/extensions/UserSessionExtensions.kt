//
//  UserSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.services.SessionStore
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
