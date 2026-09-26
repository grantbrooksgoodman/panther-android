//
//  ConversationSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.message.services.MessageService
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.networking.user.services.UserService

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

/**
 * A Boolean value that indicates whether the conversation is empty –
 * having neither a key nor a hash.
 */
val Conversation.isEmpty: Boolean
    get() = id.key.isBlank() && id.hash.isBlank()

/**
 * A Boolean value that indicates whether the conversation is a mock,
 * representing a new conversation not yet created on the server.
 */
val Conversation.isMock: Boolean
    get() = id.key == CommonConstants.NEW_CONVERSATION_ID

/** An empty conversation with no participants. */
val Conversation.Companion.empty: Conversation
    get() = empty(withUsers = emptyList())

/**
 * Creates an empty conversation with the given users as its
 * participants.
 *
 * @param withUsers The users to include as participants.
 *
 * @return An empty conversation containing the given users.
 */
fun Conversation.Companion.empty(withUsers: List<User>): Conversation {
    SessionStore.upsertUsers(withUsers.toSet())
    return Conversation(
        id = ConversationID(key = "", hash = ""),
        activities = null,
        messageIDs = emptyList(),
        metadata = emptyConversationMetadata(withUsers.map { it.id }),
        participants = withUsers.map { Participant(userID = it.id) },
        reactionMetadata = null,
    )
}

/**
 * Creates a mock conversation with the given users as its
 * participants.
 *
 * Use a mock conversation to represent a new conversation before it
 * is created on the server.
 *
 * @param withUsers The users to include as participants.
 *
 * @return A mock conversation containing the given users.
 */
fun Conversation.Companion.mock(withUsers: List<User>): Conversation {
    SessionStore.upsertUsers(withUsers.toSet())
    return Conversation(
        id = ConversationID(key = CommonConstants.NEW_CONVERSATION_ID, hash = ""),
        activities = null,
        messageIDs = emptyList(),
        metadata = emptyConversationMetadata(withUsers.map { it.id }),
        participants = withUsers.map { Participant(userID = it.id) },
        reactionMetadata = null,
    )
}

private fun emptyConversationMetadata(userIDs: List<String>): ConversationMetadata {
    val consentRequired = UserSessionService.currentUser?.messageRecipientConsentRequired == true
    return ConversationMetadata.empty(
        userIDs = userIDs,
        isPenPalsConversation = false,
        consentAcknowledged = !consentRequired,
        requiresConsentFromInitiator = if (consentRequired) User.currentUserID else null,
    )
}
