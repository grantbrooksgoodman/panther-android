//
//  ConversationSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.conversation.services.ConversationService
import us.neotechnica.panther.networking.modules.message.services.MessageService
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.networking.modules.user.services.UserService

/**
 * Whether the current user still has this conversation visible – i.e.
 * is a participant who has not deleted it.
 */
val Conversation.isVisibleForCurrentUser: Boolean
    get() {
        val currentUserID = User.currentUserID ?: return false
        val participant = participants.firstOrNull { it.userID == currentUserID } ?: return false
        return !participant.hasDeletedConversation
    }

/**
 * The conversation's messages resolved from the [SessionStore], or
 * `null` if they are not all loaded yet while the conversation is
 * visible.
 */
val Conversation.messages: List<Message>?
    get() {
        val resolved = messageIDs.mapNotNull { SessionStore.messages[it] }
        if (resolved.size != realMessageIDs.size && isVisibleForCurrentUser) return null
        return resolved.ifEmpty { null }
    }

/**
 * The conversation's other participants resolved from the
 * [SessionStore], or `null` if any are missing.
 */
val Conversation.users: List<User>?
    get() {
        val otherUserIDs = participants.map { it.userID }.filter { it != User.currentUserID }
        val resolved = otherUserIDs.mapNotNull { SessionStore.users[it] }
        if (resolved.size != otherUserIDs.size) return null
        return resolved.ifEmpty { null }
    }

/** The real (non-sentinel) message IDs, which begin with `-`. */
val Conversation.realMessageIDs: List<String>
    get() = messageIDs.filter { it.startsWith("-") }

/** Fetches any missing messages for [ids] (or all) into the store. */
suspend fun Conversation.resolveMessages(ids: Set<String>? = null) {
    val targetIDs = (ids ?: realMessageIDs.toSet()).filter { it.startsWith("-") }
    val missingIDs = targetIDs.filter { SessionStore.messages[it] == null }
    if (missingIDs.isNotEmpty()) MessageService.getMessages(missingIDs)
}

/** Fetches any missing participant users into the store. */
suspend fun Conversation.resolveUsers() {
    val otherUserIDs = participants.map { it.userID }.filter { it != User.currentUserID }
    val missingIDs = otherUserIDs.filter { SessionStore.users[it] == null }
    if (missingIDs.isNotEmpty()) UserService.getUsers(missingIDs)
}

/** Refetches the conversation's full record into the store. */
suspend fun Conversation.resolve() {
    ConversationService.getConversation(id.key)
}
