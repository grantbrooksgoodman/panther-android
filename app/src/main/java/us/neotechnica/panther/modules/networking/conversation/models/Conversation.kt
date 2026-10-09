//
//  Conversation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.models

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.bundle.conversation
import us.neotechnica.panther.bundle.conversations
import us.neotechnica.panther.bundle.messages
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.content.user.extensions.firstWithCurrentUserID
import us.neotechnica.panther.modules.content.user.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.networking.common.messageService
import us.neotechnica.panther.modules.networking.common.userService
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.util.Date

// This model exceeds the file-length and type-body-length limits.

/**
 * A conversation between two or more users.
 *
 * A conversation carries its participants, message identifiers,
 * activities, reactions, and metadata. Values may be updated
 * individually and written to the remote database.
 */
@Suppress("LargeClass")
data class Conversation(
    /** The conversation's identifier. */
    val id: ConversationID,
    /** The conversation's activities, or `null` if it has none. */
    val activities: List<Activity>?,
    /** The identifiers of the conversation's messages. */
    val messageIDs: List<String>,
    /** The conversation's metadata. */
    val metadata: ConversationMetadata,
    /** The conversation's participants. */
    val participants: List<Participant>,
    /** The reactions applied to the conversation's messages, or `null` if it has none. */
    val reactionMetadata: List<ReactionMetadata>?,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Types

    /** The serializable keys for encoding and decoding a conversation. */
    enum class SerializableKey(
        val rawValue: String,
    ) {
        ID("id"),
        ACTIVITIES("activities"),
        ENCODED_HASH("hash"),
        MESSAGES("messages"),
        METADATA("metadata"),
        PARTICIPANTS("participants"),
        REACTION_METADATA("reactionMetadata"),
    }

    // MARK: - Computed Properties

    /** The serialized representation of the conversation. */
    override val encoded: Map<String, Any?>
        get() {
            val messagesMap =
                messageIDs
                    .filter { it.startsWith("-") }
                    .associateWith { true }

            val participantsMap =
                participants.associate { participant ->
                    participant.userID to
                        mapOf(
                            Participant.Keys.HAS_DELETED_CONVERSATION.rawValue to
                                participant.hasDeletedConversation,
                            Participant.Keys.IS_TYPING.rawValue to participant.isTyping,
                        )
                }

            return mapOf(
                SerializableKey.ID.rawValue to id.encoded,
                SerializableKey.ACTIVITIES.rawValue to
                    (activities?.map { it.encoded } ?: listOf(Activity.empty.encoded)),
                SerializableKey.ENCODED_HASH.rawValue to encodedHash,
                SerializableKey.MESSAGES.rawValue to messagesMap,
                SerializableKey.METADATA.rawValue to metadata.encoded,
                SerializableKey.PARTICIPANTS.rawValue to participantsMap,
                SerializableKey.REACTION_METADATA.rawValue to
                    (reactionMetadata?.map { it.encoded } ?: listOf(ReactionMetadata.empty.encoded)),
            )
        }

    /**
     * The strings that collectively define this instance's identity
     * for hashing purposes, sorted alphabetically.
     */
    override val hashFactors: List<String>
        get() {
            val formatter = DependencyValues.current.timestampDateFormatter
            return buildList {
                add(id.key)
                addAll(activities?.map { it.encodedHash } ?: emptyList())
                addAll(messageIDs.filter { it.startsWith("-") })
                add(metadata.name)
                add(metadata.imageHash ?: BANG_QUALIFIED_EMPTY)
                add(metadata.isPenPalsConversation.toString())
                add(formatter.format(metadata.lastModifiedDate))
                addAll(metadata.messageRecipientConsentAcknowledgementData.map { it.encoded })
                addAll(metadata.penPalsSharingData.map { it.encoded })
                add(metadata.requiresConsentFromInitiator ?: BANG_QUALIFIED_EMPTY)
                // Content-version only: userID + hasDeletedConversation.
                // isTyping is excluded so typing writes do not mint
                // version tokens; presence propagates via the observer.
                addAll(participants.map { "${it.userID} | ${it.hasDeletedConversation}" })
                addAll(reactionMetadata?.map { it.encodedHash } ?: emptyList())
            }.sorted()
        }

    /**
     * Resolves messages from the session store using this
     * conversation's `messageIDs`.
     *
     * Returns `null` if the conversation does not include the current
     * user or if no messages are in the store.
     */
    val messages: List<Message>?
        get() {
            val sessionStore = DependencyValues.current.clientSession.store
            val messages = messageIDs.mapNotNull { sessionStore.messages[it] }

            // Session store does not store system messages.
            if (messages.size != messageIDs.size && isVisibleForCurrentUser) return null

            return messages.ifEmpty { null }
        }

    /**
     * Resolves non-current-user participants from the session store.
     *
     * Returns `null` if no matching users are in the store.
     */
    val users: List<User>?
        get() {
            val sessionStore = DependencyValues.current.clientSession.store
            val userIDs = participants.map { it.userID }.filter { it != User.currentUserID }
            val users = userIDs.mapNotNull { sessionStore.users[it] }
            if (users.size != userIDs.size) return null
            return users.ifEmpty { null }
        }

    // MARK: - Resolve Messages

    /**
     * Fetches messages from the network and upserts them to the
     * session store.
     *
     * When [ids] is `null`, all non-system message identifiers on this
     * conversation are fetched. When [ids] is provided, only the
     * specified messages are fetched.
     *
     * Concurrent calls for the same conversation version and identifier
     * set coalesce onto a single in-flight fetch. If the calling
     * coroutine is cancelled before the shared fetch settles, this
     * method returns without effect for the caller; the fetch itself
     * always runs to completion, so the session store never observes a
     * partially applied resolution.
     *
     * After this method returns, the fetched messages are available
     * through the [messages] computed property.
     *
     * @param ids A set of message identifiers to fetch. Pass `null` to
     *   fetch all non-system messages.
     *
     * @throws Exception if a message cannot be fetched.
     */
    suspend fun resolveMessages(ids: Set<String>? = null) {
        val idsKeyComponent = ids?.sorted()?.joinToString(",") ?: "all"
        messageCoalescer.submitUnlessCancelled("${id.encoded}/$idsKeyComponent") {
            fetchAndCommitMessages(ids)
        }
    }

    // MARK: - Resolve Users

    /**
     * Fetches non-current-user participants from the network and
     * upserts them to the session store.
     *
     * By default, this method returns early when all participants are
     * already available through the [users] computed property. Pass
     * [forceUpdate] to bypass the cache and re-fetch regardless.
     *
     * Concurrent calls for the same conversation version coalesce onto
     * a single in-flight fetch; forced and unforced calls occupy
     * separate lanes so a force re-fetch is never absorbed by a cached
     * one. If the calling coroutine is cancelled before the shared
     * fetch settles, this method returns without effect for the
     * caller; the fetch itself always runs to completion.
     *
     * @param forceUpdate When `true`, disregards the cache and fetches
     *   all participants from the network.
     *
     * @throws Exception if a participant cannot be fetched.
     */
    suspend fun resolveUsers(forceUpdate: Boolean = false) {
        userCoalescer.submitUnlessCancelled("${id.encoded}/$forceUpdate") {
            fetchAndCommitUsers(forceUpdate)
        }
    }

    // MARK: - Update Last Modified Date

    /**
     * Writes a new last-modified date for the conversation using
     * narrow child paths, leaving sibling metadata untouched.
     *
     * The date, conversation hash, and participant token entries are
     * committed in a single atomic fan-out.
     *
     * @param to The new last-modified date.
     *
     * @throws Exception if the write fails.
     */
    suspend fun updateLastModifiedDate(to: Date = Date()) {
        val database = DependencyValues.current.networking.database
        val dateFormatter = DependencyValues.current.timestampDateFormatter

        val conversationPath = "${NetworkPath.conversations.rawValue}/${id.key}"
        val newHash = copy(metadata = metadata.copyWith(lastModifiedDate = to)).encodedHash

        val updates =
            mutableMapOf<String, Any?>(
                listOf(
                    conversationPath,
                    SerializableKey.METADATA.rawValue,
                    ConversationMetadata.SerializableKey.LAST_MODIFIED_DATE.rawValue,
                ).joinToString("/") to dateFormatter.format(to),
                "$conversationPath/${SerializableKey.ENCODED_HASH.rawValue}" to newHash,
            )

        for (participant in participants) {
            updates[participant.conversationTokenPath] = newHash
        }

        database.commit(updates)
    }

    // MARK: - Update Read Date

    /**
     * Marks the given messages as read by the current user and writes
     * the read receipts.
     *
     * This method appends the current user's read receipt to each
     * message that the user has not already read, and has no effect
     * for messages the user has already read. In a one-to-one
     * conversation, it also updates the conversation's last-modified
     * date and hash token, and the participants' hash tokens, in the
     * same atomic fan-out.
     *
     * @param messages The messages to mark as read.
     *
     * @throws Exception if no messages are provided, the current user
     *   identifier has not been set, or the write fails.
     */
    suspend fun updateReadDate(messages: List<Message>) {
        val database = DependencyValues.current.networking.database
        val dateFormatter = DependencyValues.current.timestampDateFormatter
        val sessionStore = DependencyValues.current.clientSession.store

        if (messages.isEmpty()) {
            throw Exception(
                "No messages provided.",
                metadata = ExceptionMetadata(this),
            )
        }

        val currentUserID =
            User.currentUserID
                ?: throw Exception(
                    "Current user ID has not been set.",
                    metadata = ExceptionMetadata(this),
                )

        val now = Date()
        val readReceipt = ReadReceipt(userID = currentUserID, readDate = now)

        val unreadMessages = messages.filter { it.currentUserReadReceipt == null }
        if (unreadMessages.isEmpty()) return

        val updates = mutableMapOf<String, Any?>()
        val updatedMessages = mutableListOf<Message>()

        // Per-message read receipt entries. Array retained:
        // a message's receipts are small and only ever
        // written by readers of that message. Group-chat
        // concurrent-readers remain last-writer-wins at
        // per-message granularity (accepted residual).
        for (message in unreadMessages) {
            val updatedReceipts =
                ((message.readReceipts?.filter { it.userID != currentUserID } ?: emptyList()) + readReceipt)
                    .distinct()

            val path =
                listOf(
                    NetworkPath.messages.rawValue,
                    message.id,
                    Message.SerializableKey.READ_RECEIPTS.rawValue,
                ).joinToString("/")

            updates[path] = updatedReceipts.map { it.encoded }
            updatedMessages.add(message.copy(readReceipts = updatedReceipts))
        }

        // For 1:1 conversations, add lastModifiedDate +
        // hash + participant token entries.
        var updatedConversation: Conversation? = null
        if (participants.size == ONE_TO_ONE_PARTICIPANT_COUNT) {
            val conversationPath = "${NetworkPath.conversations.rawValue}/${id.key}"
            val lastModifiedPath =
                listOf(
                    conversationPath,
                    SerializableKey.METADATA.rawValue,
                    ConversationMetadata.SerializableKey.LAST_MODIFIED_DATE.rawValue,
                ).joinToString("/")

            updates[lastModifiedPath] = dateFormatter.format(now)

            val withMetadata = copy(metadata = metadata.copyWith(lastModifiedDate = now))
            val newHash = withMetadata.encodedHash
            updates["$conversationPath/${SerializableKey.ENCODED_HASH.rawValue}"] = newHash

            for (participant in participants) {
                updates[participant.conversationTokenPath] = newHash
            }

            updatedConversation = withMetadata.copy(id = ConversationID(key = id.key, hash = newHash))
        }

        database.commit(updates)
        // Propagates locally-written read receipts to the session store.
        sessionStore.upsertMessages(updatedMessages.toSet())

        if (updatedConversation != null) {
            // Reflects the updated hash/metadata after read-date commit.
            sessionStore.upsertConversation(updatedConversation)
        }

        Logger.log(
            "Updated read date for ${unreadMessages.size} message${if (unreadMessages.size == 1) "" else "s"}.",
            domain = LoggerDomain.conversation,
        )
    }

    // MARK: - Auxiliary

    private val Participant.conversationTokenPath: String
        get() =
            listOf(
                NetworkPath.users.rawValue,
                userID,
                User.SerializableKey.CONVERSATION_IDS.rawValue,
                id.key,
            ).joinToString("/")

    private suspend fun fetchAndCommitMessages(ids: Set<String>?) {
        val messageService = DependencyValues.current.networking.messageService
        val sessionStore = DependencyValues.current.clientSession.store

        if (ids != null) {
            // Fetched from network; bypasses RemotelyUpdatable.update.
            val fetched =
                coroutineScope {
                    ids
                        .filter { it in messageIDs }
                        .map { async { messageService.getMessage(it) } }
                        .awaitAll()
                }

            sessionStore.upsertMessages(fetched.toSet())
            return
        }

        val filteredMessageIDs = filteringSystemMessages.messageIDs
        val fetchedMessages = messageService.getMessages(filteredMessageIDs)

        if (fetchedMessages.isNotEmpty()) {
            // Fetched from network; bypasses RemotelyUpdatable.update.
            sessionStore.upsertMessages(fetchedMessages.toSet())
        }

        // Reconcile: remove IDs that could not be fetched so
        // the messages computed property resolves fully.
        val fetchedIDs = fetchedMessages.map { it.id }.toSet()
        val missingIDs = filteredMessageIDs.toSet() - fetchedIDs

        if (missingIDs.isNotEmpty()) {
            sessionStore.removeMessages(missingIDs)
            // Strips unfetchable message IDs so the store stays consistent.
            sessionStore.upsertConversation(copy(messageIDs = messageIDs.filter { it !in missingIDs }))
        }

        Logger.log(
            Exception(
                "Resolved messages for conversation.",
                isReportable = false,
                userInfo = mapOf("ConversationID" to id.encoded),
                metadata = ExceptionMetadata(this),
            ),
            domain = LoggerDomain.conversation,
        )
    }

    private suspend fun fetchAndCommitUsers(forceUpdate: Boolean) {
        val networking = DependencyValues.current.networking

        val userInfo = mapOf<String, Any>("ConversationID" to id.encoded)
        if (!forceUpdate) {
            val users = users
            if (users != null && users.size == participants.size - 1) return
        }

        val userIDs = participants.map { it.userID }.filter { it != User.currentUserID }
        if (userIDs.isBangQualifiedEmpty) {
            throw Exception(
                "No participants for this conversation.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        val fetchedUsers =
            try {
                networking.userService.getUsers(
                    userIDs,
                    bypassSnapshotCache = forceUpdate,
                    cacheStrategy = if (forceUpdate) CacheStrategy.DISREGARD_CACHE else null,
                )
            } catch (exception: Exception) {
                throw exception.appending(userInfo)
            }

        if (fetchedUsers.isEmpty() || fetchedUsers.size != userIDs.size) {
            throw Exception(
                "Mismatched ratio returned.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        Logger.log(
            Exception(
                "Resolved users for conversation.",
                isReportable = false,
                userInfo = mapOf("ConversationID" to id.encoded),
                metadata = ExceptionMetadata(this),
            ),
            domain = LoggerDomain.conversation,
        )
    }

    // MARK: - Companion

    companion object : SerializableDecoder<Conversation, Map<String, Any?>> {
        private const val ONE_TO_ONE_PARTICIPANT_COUNT = 2

        private val messageCoalescer = Coalescer<String, Unit>()
        private val userCoalescer = Coalescer<String, Unit>()

        /** An empty conversation placeholder. */
        val empty: Conversation
            get() =
                Conversation(
                    id = ConversationID(key = "", hash = ""),
                    activities = null,
                    messageIDs = emptyList(),
                    metadata = ConversationMetadata.empty(userIDs = emptyList()),
                    participants = emptyList(),
                    reactionMetadata = null,
                )

        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[SerializableKey.ID.rawValue] !is String) return false
            val activities = mapList(data, SerializableKey.ACTIVITIES) ?: return false
            if (!activities.all { Activity.canDecode(it) }) return false
            val metadata = stringKeyMap(data[SerializableKey.METADATA.rawValue]) ?: return false
            if (!ConversationMetadata.canDecode(metadata)) return false
            val encodedConsentData =
                stringList(metadata[ConversationMetadata.SerializableKey.MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA.rawValue])
                    ?: return false
            val encodedPenPalsSharingData =
                stringList(metadata[ConversationMetadata.SerializableKey.PEN_PALS_SHARING_DATA.rawValue]) ?: return false

            val participantMap = data[SerializableKey.PARTICIPANTS.rawValue] as? Map<*, *> ?: return false
            if (participantMap.size <= 1) return false
            if (encodedConsentData.size != encodedPenPalsSharingData.size) return false
            if (encodedPenPalsSharingData.size != participantMap.size) return false
            val reactionMetadata = mapList(data, SerializableKey.REACTION_METADATA) ?: return false
            if (!reactionMetadata.all { ReactionMetadata.canDecode(it) }) return false
            val messages = data[SerializableKey.MESSAGES.rawValue]
            return messages == null || messages is Map<*, *>
        }

        override fun decode(data: Map<String, Any?>): Conversation {
            val idString = data[SerializableKey.ID.rawValue] as? String
            val encodedActivities = mapList(data, SerializableKey.ACTIVITIES)
            val encodedMetadata = stringKeyMap(data[SerializableKey.METADATA.rawValue])
            val encodedReactionMetadata = mapList(data, SerializableKey.REACTION_METADATA)
            val participantMap = participantMap(data)

            if (idString == null ||
                encodedActivities == null ||
                encodedMetadata == null ||
                encodedReactionMetadata == null ||
                participantMap == null
            ) {
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
            }

            val messageIDs = messageKeys(data)
            val conversationID = ConversationID.decode(idString)
            val participants =
                participantMap
                    .map { (userID, values) ->
                        val hasDeleted = values[Participant.Keys.HAS_DELETED_CONVERSATION.rawValue] as? Boolean
                        val isTyping = values[Participant.Keys.IS_TYPING.rawValue] as? Boolean
                        if (hasDeleted == null || isTyping == null) {
                            throw Exception.Networking.decodingFailed(
                                values,
                                ExceptionMetadata(this),
                            )
                        }
                        Participant(
                            userID = userID,
                            hasDeletedConversation = hasDeleted,
                            isTyping = isTyping,
                        )
                    }.sortedBy { it.userID }

            val reactionMetadata = encodedReactionMetadata.map { ReactionMetadata.decode(it) }

            // Message resolution is deferred to resolveMessages /
            // resolveMessagesOnCurrentUserConversations; decoding
            // only records the message IDs.

            val currentUserParticipant = participants.firstWithCurrentUserID
            if (currentUserParticipant == null || currentUserParticipant.hasDeletedConversation) {
                Logger.log(
                    Exception(
                        "Current user is not participating in or has deleted this conversation.",
                        isReportable = false,
                        userInfo =
                            mapOf(
                                "ConversationIDKey" to conversationID.key,
                                "ConversationIDHash" to conversationID.hash,
                            ),
                        metadata = ExceptionMetadata(this),
                    ),
                    domain = LoggerDomain.conversation,
                )
            }

            return Conversation(
                id = conversationID,
                activities = encodedActivities.map { Activity.decode(it) },
                messageIDs = if (messageIDs.isBangQualifiedEmpty) bangQualifiedEmptyList else messageIDs,
                metadata = ConversationMetadata.decode(encodedMetadata),
                participants = participants,
                reactionMetadata = if (reactionMetadata.all { it == ReactionMetadata.empty }) null else reactionMetadata,
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun mapList(
            data: Map<String, Any?>,
            key: SerializableKey,
        ): List<Map<String, Any?>>? =
            (data[key.rawValue] as? List<*>)
                ?.takeIf { list -> list.all { it is Map<*, *> } }
                ?.map { it as Map<String, Any?> }

        private fun messageKeys(data: Map<String, Any?>): List<String> =
            (data[SerializableKey.MESSAGES.rawValue] as? Map<*, *>)
                ?.keys
                ?.filterIsInstance<String>()
                ?.sorted()
                ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        private fun participantMap(data: Map<String, Any?>): Map<String, Map<String, Any?>>? =
            (data[SerializableKey.PARTICIPANTS.rawValue] as? Map<*, *>)
                ?.takeIf { map -> map.keys.all { it is String } && map.values.all { it is Map<*, *> } }
                ?.let { it as Map<String, Map<String, Any?>> }

        @Suppress("UNCHECKED_CAST")
        private fun stringKeyMap(value: Any?): Map<String, Any?>? =
            (value as? Map<*, *>)
                ?.takeIf { map -> map.keys.all { it is String } }
                ?.let { it as Map<String, Any?> }

        @Suppress("UNCHECKED_CAST")
        private fun stringList(value: Any?): List<String>? =
            (value as? List<*>)
                ?.takeIf { list -> list.all { it is String } }
                ?.map { it as String }
    }
}
