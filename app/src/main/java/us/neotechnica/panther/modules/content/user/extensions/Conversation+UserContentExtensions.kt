//
//  Conversation+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import kotlin.random.Random

// MARK: - Properties

/**
 * The text shown in the chat page header, or `null` when the
 * conversation has a name or two or fewer participants.
 *
 * For an unnamed group conversation, this is the number of other
 * participants followed by a localized label.
 */
val Conversation.chatPageHeaderLabelText: String?
    get() {
        if (!metadata.name.isBangQualifiedEmpty || participants.size <= 2) return null
        return "${participants.size - 1} ${LocalizedStringKey.People.localized()}"
    }

/** The participant representing the current user, if any. */
val Conversation.currentUserParticipant: Participant?
    get() = participants.firstWithCurrentUserID

/** A copy of the conversation with its system messages removed. */
val Conversation.filteringSystemMessages: Conversation
    get() {
        val messageIDs = messageIDs.filter { it.startsWith("-") }
        return copy(messageIDs = messageIDs.ifEmpty { bangQualifiedEmptyList })
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

/**
 * A Boolean value that indicates whether the conversation is visible
 * to the current user.
 *
 * A conversation is hidden when the current user has deleted it, or
 * when any participant is blocked.
 */
val Conversation.isVisibleForCurrentUser: Boolean
    get() {
        val currentUser = DependencyValues.current.clientSession.entity.user.currentUser
        val blockedUserIDs = currentUser?.blockedUserIDs
        val currentUserParticipant = currentUserParticipant ?: return false
        if (currentUserParticipant.hasDeletedConversation) return false
        if (participants.any { it.userID in (blockedUserIDs ?: emptyList()) }) return false
        return true
    }

/**
 * A copy of the conversation whose messages are limited to those sent
 * after the current user joined it.
 */
val Conversation.withMessagesOffsetFromCurrentUserAdditionDate: Conversation
    get() {
        val sessionStore = DependencyValues.current.clientSession.store
        val currentUserAddedActivity = activities?.lastOrNull { it.action.isCurrentUserAdded } ?: return this
        val filteredIDs =
            messageIDs.filter { id ->
                val message = sessionStore.messages[id] ?: return@filter true
                message.sentDate >= currentUserAddedActivity.date
            }
        return copy(messageIDs = filteredIDs)
    }

/** A copy of the conversation with its messages sorted from oldest to newest. */
val Conversation.withMessagesSortedByAscendingSentDate: Conversation
    get() {
        val sessionStore = DependencyValues.current.clientSession.store
        val sortedIDs =
            messageIDs.sortedWith { first, second ->
                val firstMessage = sessionStore.messages[first]
                val secondMessage = sessionStore.messages[second]
                if (firstMessage == null || secondMessage == null) {
                    0
                } else {
                    firstMessage.sentDate.compareTo(secondMessage.sentDate)
                }
            }
        return copy(messageIDs = sortedIDs)
    }

// MARK: - Methods

/**
 * Creates an empty conversation with the given users as its
 * participants.
 *
 * @param withUsers The users to include as participants.
 *
 * @return An empty conversation containing the given users.
 */
fun Conversation.Companion.empty(withUsers: List<User>): Conversation {
    val sessionStore = DependencyValues.current.clientSession.store
    // Stores users so the conversation's computed properties can resolve them.
    sessionStore.upsertUsers(withUsers.toSet())
    return Conversation(
        id = ConversationID(key = "", hash = ""),
        activities = null,
        messageIDs = emptyList(),
        metadata = ConversationMetadata.empty(userIDs = withUsers.map { it.id }),
        participants =
            withUsers.map {
                Participant(
                    userID = it.id,
                    hasDeletedConversation = false,
                    isTyping = false,
                )
            },
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
    val sessionStore = DependencyValues.current.clientSession.store
    // Stores users so the conversation's computed properties can resolve them.
    sessionStore.upsertUsers(withUsers.toSet())
    return Conversation(
        id = ConversationID(key = CommonConstants.NEW_CONVERSATION_ID, hash = ""),
        activities = null,
        messageIDs = emptyList(),
        metadata = ConversationMetadata.empty(userIDs = withUsers.map { it.id }),
        participants = withUsers.map { Participant(userID = it.id) },
        reactionMetadata = null,
    )
}

/**
 * Invalidates the conversation's local hash to force a re-fetch on
 * the next sync cycle.
 *
 * This method replaces the conversation's hash with a randomly
 * generated value and upserts the modified copy into the session
 * store. Because the local hash no longer matches the server hash,
 * the sync system treats the conversation as out-of-date and resolves
 * it from the server on its next pass.
 *
 * No remote write is performed – the change is purely local.
 */
fun Conversation.markStaleLocally() {
    val sessionStore = DependencyValues.current.clientSession.store
    // Local hash/messageID modification to force re-fetch; no remote write.
    sessionStore.upsertConversation(
        copy(
            id =
                ConversationID(
                    key = id.key,
                    hash = encodedHashOf(listOf(Random.nextInt(1, STALE_HASH_UPPER_BOUND).toString())),
                ),
        ),
    )
}

private const val STALE_HASH_UPPER_BOUND = 1_000_001
