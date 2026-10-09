//
//  ConversationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.bundle.conversations
import us.neotechnica.panther.bundle.messages
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.modules.networking.common.BatchFailureStrategy
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.PendingTranslationArchive
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * The service that creates, retrieves, and removes conversations.
 */
object ConversationService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    private val coalescer = Coalescer<String, Conversation>()

    // MARK: - Conversation Creation

    /**
     * Creates a conversation with the given first message and
     * participants, and writes it to the database.
     *
     * The conversation node, the participants' conversation tokens, the
     * first message node, and any pending hosted translation archive
     * entries are written in a single atomic fan-out.
     *
     * @param firstMessage The conversation's first message.
     * @param isPenPalsConversation A Boolean value that indicates
     *   whether the conversation is a PenPals conversation.
     * @param participants The conversation's participants.
     *
     * @return The created conversation.
     *
     * @throws Exception if the participants fail validation, a key
     *   cannot be generated, or the write fails.
     */
    suspend fun createConversation(
        firstMessage: Message,
        isPenPalsConversation: Boolean,
        participants: List<Participant>,
    ): Conversation {
        if (!participants.all { it.isWellFormed }) {
            throw Exception(
                "Passed arguments fail validation.",
                metadata = ExceptionMetadata(this),
            )
        }

        val path = NetworkPath.conversations.rawValue
        val id =
            database.generateKey(path)
                ?: throw Exception(
                    "Failed to generate key for new conversation.",
                    metadata = ExceptionMetadata(this),
                )

        // Optimistic insert before remote write; didWrite does not apply.
        SessionStore.upsertMessages(setOf(firstMessage))
        val mockConversation =
            Conversation(
                id = ConversationID(key = id, hash = ""),
                activities = null,
                messageIDs = listOf(firstMessage.id),
                metadata =
                    ConversationMetadata.empty(
                        userIDs = participants.map { it.userID },
                        isPenPalsConversation = isPenPalsConversation,
                    ),
                participants = participants,
                reactionMetadata = null,
            )

        val data = mockConversation.encoded.filterKeys { it != Conversation.SerializableKey.ID.rawValue }

        val conversationID =
            ConversationID(
                key = mockConversation.id.key,
                hash = mockConversation.encodedHash,
            )

        val updates = mutableMapOf<String, Any?>()
        for ((key, value) in data) {
            updates[listOf(path, id, key).joinToString("/")] = value
        }

        for (participant in participants) {
            updates[
                listOf(
                    NetworkPath.users.rawValue,
                    participant.userID,
                    User.SerializableKey.CONVERSATION_IDS.rawValue,
                    conversationID.key,
                ).joinToString("/"),
            ] = conversationID.hash
        }

        // The message node joins the same atomic fan-out;
        // buildMessage leaves it unwritten on the send path.
        updates[listOf(NetworkPath.messages.rawValue, firstMessage.id).joinToString("/")] =
            firstMessage.encoded.filterKeys { it != Message.SerializableKey.ID.rawValue }

        // Merge pending hosted-archive entries into the same payload;
        // a message node must never commit without its translations
        // being resolvable from the hosted archive.
        for (reference in firstMessage.translationReferences ?: emptyList()) {
            val entry = PendingTranslationArchive.drain(reference.hostingKey) ?: continue
            updates[entry.first] = entry.second
        }

        database.commit(updates)
        return mockConversation.copy(id = conversationID)
    }

    // MARK: - Retrieval by ID

    /**
     * Returns the conversations with the given identifier keys, fetched
     * concurrently.
     *
     * @param idKeys The identifier keys of the conversations to fetch.
     *
     * @return The conversations.
     *
     * @throws Exception if no identifier keys are provided or any
     *   conversation cannot be fetched.
     */
    suspend fun getConversations(idKeys: List<String>): List<Conversation> {
        val userInfo = mapOf<String, Any>("ConversationIDs" to idKeys)

        if (idKeys.isBangQualifiedEmpty) {
            throw Exception(
                "No IDs provided.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        try {
            return coroutineScope {
                idKeys.map { idKey -> async { getConversation(idKey) } }.awaitAll()
            }
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }
    }

    private suspend fun getConversation(idKey: String): Conversation {
        val userInfo = mapOf<String, Any>("ConversationIDKey" to idKey)

        if (idKey.isBangQualifiedEmpty) {
            throw Exception(
                "No ID provided.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        // Coalesce concurrent fetches of the same conversation so it is
        // fetched and decoded only once.
        return coalescer(idKey) { fetchConversation(idKey) }
    }

    private suspend fun fetchConversation(idKey: String): Conversation {
        val userInfo = mapOf<String, Any>("ConversationIDKey" to idKey)

        val data: Map<String, Any?> =
            try {
                database.getValues<Map<String, Any?>>(
                    listOf(NetworkPath.conversations.rawValue, idKey).joinToString("/"),
                    cacheStrategy = CacheStrategy.DISREGARD_CACHE,
                )
            } catch (exception: Exception) {
                throw exception.appending(userInfo)
            }

        val conversationIDHash =
            data[Conversation.SerializableKey.ENCODED_HASH.rawValue] as? String
                ?: throw Exception(
                    "Failed to decode conversation ID.",
                    metadata = ExceptionMetadata(this),
                ).appending(userInfo)

        val childData =
            data.toMutableMap().apply {
                put(
                    Conversation.SerializableKey.ID.rawValue,
                    ConversationID(key = idKey, hash = conversationIDHash).encoded,
                )
            }

        try {
            return Conversation.decode(childData)
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }
    }

    // MARK: - Deletion

    /**
     * Removes the conversation with the given identifier key from the
     * given users.
     *
     * This method deletes each user's token for the conversation in a
     * single atomic fan-out.
     *
     * @param userIDs The identifiers of the users to remove the
     *   conversation from.
     * @param conversationIDKey The identifier key of the conversation
     *   to remove.
     * @param failureStrategy The strategy that determines how a
     *   failure is handled.
     *
     * @throws Exception if the conversation identifier key is empty or
     *   the write fails.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun removeConversationFromUsers(
        userIDs: List<String>,
        conversationIDKey: String,
        failureStrategy: BatchFailureStrategy = BatchFailureStrategy.RETURN_ON_FAILURE,
    ) {
        if (conversationIDKey.isBangQualifiedEmpty) {
            throw Exception(
                "Passed arguments fail validation.",
                metadata = ExceptionMetadata(this),
            )
        }

        val updates = mutableMapOf<String, Any?>()
        for (userID in userIDs.filter { !it.isBangQualifiedEmpty }) {
            val path =
                listOf(
                    NetworkPath.users.rawValue,
                    userID,
                    User.SerializableKey.CONVERSATION_IDS.rawValue,
                    conversationIDKey,
                ).joinToString("/")

            updates[path] = null
        }

        database.commit(updates)
    }
}
