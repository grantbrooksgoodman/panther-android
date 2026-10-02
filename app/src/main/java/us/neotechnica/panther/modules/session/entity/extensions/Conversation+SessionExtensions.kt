//
//  Conversation+SessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
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
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.KeyedCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import java.util.Date

/**
 * Whether the conversation is visible to the current user.
 *
 * A conversation is hidden when the current user has deleted it, or
 * when any participant is blocked.
 */
val Conversation.isVisibleForCurrentUser: Boolean
    get() {
        val currentUserID = User.currentUserID ?: return false
        val participant = participants.firstOrNull { it.userID == currentUserID } ?: return false
        if (participant.hasDeletedConversation) return false
        val blockedUserIDs = UserSessionService.currentUser?.blockedUserIDs ?: emptyList()
        if (participants.any { it.userID in blockedUserIDs }) return false
        return true
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

/**
 * Fetches the messages for [ids] (or all non-system messages) from the
 * network and upserts them into the store.
 *
 * Concurrent calls for the same conversation version and identifier set
 * coalesce onto a single in-flight fetch. If the calling coroutine is
 * cancelled before the shared fetch settles, this returns without effect
 * for the caller; the fetch itself always runs to completion, so the
 * store never observes a partially applied resolution.
 *
 * @param ids The message identifiers to fetch, or `null` for all
 *   non-system messages.
 */
suspend fun Conversation.resolveMessages(ids: Set<String>? = null) {
    val idsKeyComponent = ids?.sorted()?.joinToString(",") ?: "all"
    messageCoalescer.submitUnlessCancelled("${id.encoded}/$idsKeyComponent") {
        fetchAndCommitMessages(ids)
    }
}

/**
 * Fetches the non-current-user participants from the network and upserts
 * them into the store.
 *
 * By default, returns early when all participants are already available
 * through [users]. Pass [forceUpdate] to re-fetch regardless. Concurrent
 * calls for the same conversation version coalesce onto a single
 * in-flight fetch; forced and unforced calls occupy separate lanes so a
 * force re-fetch is never absorbed by a cached one.
 *
 * @param forceUpdate When `true`, re-fetches all participants regardless
 *   of what is already in the store.
 */
suspend fun Conversation.resolveUsers(forceUpdate: Boolean = false) {
    userCoalescer.submitUnlessCancelled("${id.encoded}/$forceUpdate") {
        fetchAndCommitUsers(forceUpdate)
    }
}

/** Refetches the conversation's full record into the store. */
suspend fun Conversation.resolve() {
    ConversationService.getConversation(id.key)
}

/**
 * Writes a new last-modified date for the conversation using narrow
 * child paths, leaving sibling metadata untouched. The date, hash, and
 * participant token entries are committed in a single atomic fan-out.
 *
 * @param to The new last-modified date.
 */
suspend fun Conversation.updateLastModifiedDate(to: Date = Date()) {
    val database = Networking.config.databaseDelegate
    val formatter = DependencyValues.current.timestampDateFormatter
    val conversationPath = "${NetworkPath.conversations.rawValue}/${id.key}"
    val newHash = copy(metadata = metadata.copyWith(lastModifiedDate = to)).encodedHash

    val updates =
        mutableMapOf<String, Any?>(
            "$conversationPath/$KEY_METADATA/$KEY_LAST_MODIFIED" to formatter.format(to),
            "$conversationPath/$KEY_HASH" to newHash,
        )
    for (participant in participants) {
        updates["${NetworkPath.users.rawValue}/${participant.userID}/$KEY_OPEN_CONVERSATIONS/${id.key}"] = newHash
    }

    database.commit(updates)
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

/** The participant representing the current user, or `null` if the current user is not a participant. */
val Conversation.currentUserParticipant: Participant?
    get() = participants.firstOrNull { it.userID == User.currentUserID }

/** The conversation with any system-message identifiers removed from its message IDs. */
val Conversation.filteringSystemMessages: Conversation
    get() {
        val nonSystemIDs = messageIDs.filter { SessionStore.messages[it]?.isSystemMessage != true }
        return if (nonSystemIDs.size == messageIDs.size) this else copy(messageIDs = nonSystemIDs)
    }

// MARK: - Auxiliary

private suspend fun Conversation.fetchAndCommitMessages(ids: Set<String>?) {
    if (ids != null) {
        // Fetched from network; bypasses RemotelyUpdatable.update.
        val fetched = ids.filter { it in messageIDs }.map { MessageService.getMessage(it) }
        SessionStore.upsertMessages(fetched.toSet())
        warmTranslations(fetched)
        return
    }

    val filteredMessageIDs = filteringSystemMessages.messageIDs
    val fetchedMessages = MessageService.getMessages(filteredMessageIDs)
    if (fetchedMessages.isNotEmpty()) SessionStore.upsertMessages(fetchedMessages.toSet())

    // Reconcile: remove IDs that could not be fetched so the messages
    // computed property resolves fully.
    val missingIDs = filteredMessageIDs.toSet() - fetchedMessages.map { it.id }.toSet()
    if (missingIDs.isNotEmpty()) {
        SessionStore.removeMessages(missingIDs)
        // Strips unfetchable message IDs so the store stays consistent.
        SessionStore.upsertConversation(copy(messageIDs = messageIDs.filter { it !in missingIDs }))
    }

    warmTranslations(fetchedMessages)
    Logger.log("Resolved messages for conversation. (ConversationID: ${id.encoded})", domain = LoggerDomain.conversation)
}

/**
 * Resolves each message's translation into the persistent archive, so a
 * conversation presents from memory without visibly resolving on entry.
 *
 * Mirrors iOS, which resolves a message's translation as it decodes the
 * message; on Android the resolved translation lands in the archive that
 * the chat page seeds from synchronously. Failures are swallowed by
 * [resolvedTranslation], so warming never fails message resolution.
 */
private suspend fun warmTranslations(messages: List<Message>) {
    if (messages.isEmpty()) return
    val languageCode = RuntimeStorage.languageCode
    coroutineScope {
        messages.map { message -> async { message.resolvedTranslation(languageCode) } }.awaitAll()
    }
}

private suspend fun Conversation.fetchAndCommitUsers(forceUpdate: Boolean) {
    val userInfo = mapOf<String, Any>("ConversationID" to id.encoded)
    if (!forceUpdate && users != null && users?.size == participants.size - 1) return

    val userIDs = participants.map { it.userID }.filter { it != User.currentUserID }
    if (userIDs.isEmpty()) {
        throw Exception("No participants for this conversation.", metadata = ExceptionMetadata(this)).appending(userInfo)
    }

    val fetchedUsers =
        try {
            UserService.getUsers(
                userIDs,
                bypassSnapshotCache = forceUpdate,
                cacheStrategy = if (forceUpdate) CacheStrategy.DISREGARD_CACHE else null,
            )
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }

    if (fetchedUsers.isEmpty() || fetchedUsers.size != userIDs.size) {
        throw Exception("Mismatched ratio returned.", metadata = ExceptionMetadata(this)).appending(userInfo)
    }

    Logger.log("Resolved users for conversation. (ConversationID: ${id.encoded})", domain = LoggerDomain.conversation)
}

// MARK: - Coalescers

private val messageCoalescer = KeyedCoalescer<String, Unit>()
private val userCoalescer = KeyedCoalescer<String, Unit>()

// MARK: - Constants

private const val KEY_HASH = "hash"
private const val KEY_METADATA = "metadata"
private const val KEY_LAST_MODIFIED = "lastModified"
private const val KEY_OPEN_CONVERSATIONS = "openConversations"
