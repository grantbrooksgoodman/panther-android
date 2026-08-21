//
//  ConversationService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.conversation.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationMetadata
import us.neotechnica.panther.networking.modules.schema.conversation.models.Participant
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.services.PendingTranslationArchive
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Reads [Conversation] records from the database, upserting each into
 * the [SessionStore]. Ported from the iOS `ConversationService` read
 * path.
 */
object ConversationService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Conversation Creation

    /**
     * Creates a conversation with the given first message and
     * participants, writing the conversation node, the participants'
     * conversation tokens, the first message node, and any pending
     * hosted-translation archive entries in a single atomic fan-out.
     *
     * @param firstMessage The conversation's first message.
     * @param isPenPalsConversation Whether the conversation is a PenPals conversation.
     * @param participants The conversation's participants.
     *
     * @return The created conversation.
     *
     * @throws Exception if participants fail validation, a key cannot be
     *   generated, or the write fails.
     */
    suspend fun createConversation(
        firstMessage: Message,
        isPenPalsConversation: Boolean,
        participants: List<Participant>,
    ): Conversation {
        if (!participants.all { it.isWellFormed }) {
            throw Exception("Passed arguments fail validation.", metadata = ExceptionMetadata(this))
        }

        val path = NetworkPath.conversations.rawValue
        val id =
            database.generateKey(path)
                ?: throw Exception("Failed to generate key for new conversation.", metadata = ExceptionMetadata(this))

        // Optimistic insert before remote write.
        SessionStore.upsertMessages(setOf(firstMessage))

        val consentRequired = UserSessionService.currentUser?.messageRecipientConsentRequired == true
        val metadata =
            ConversationMetadata.empty(
                userIDs = participants.map { it.userID },
                isPenPalsConversation = isPenPalsConversation,
                consentAcknowledged = !consentRequired,
                requiresConsentFromInitiator = if (consentRequired) User.currentUserID else null,
            )

        val mockConversation =
            Conversation(
                id = ConversationID(key = id, hash = BANG_QUALIFIED_EMPTY),
                activities = null,
                messageIDs = listOf(firstMessage.id),
                metadata = metadata,
                participants = participants,
                reactionMetadata = null,
            )

        val conversationID = ConversationID(key = id, hash = mockConversation.encodedHash)
        val updates = buildCreateFanOut(path, id, mockConversation, conversationID, firstMessage)

        database.commit(updates)
        return mockConversation.copy(id = conversationID)
    }

    // MARK: - Read Methods

    /** Returns the conversations with the given keys, upserting them into the store. */
    suspend fun getConversations(idKeys: List<String>): List<Conversation> =
        coroutineScope {
            idKeys
                .map { idKey -> async { runCatching { getConversation(idKey) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

    /** Returns the conversation with the given key, upserting it into the store. */
    suspend fun getConversation(idKey: String): Conversation {
        val data: Map<String, Any?> = database.getValues("${NetworkPath.conversations.rawValue}/$idKey")
        val hash = data[ENCODED_HASH_KEY] as? String ?: BANG_QUALIFIED_EMPTY
        val childData = data.toMutableMap().apply { put(ID_KEY, "$idKey | $hash") }

        if (!Conversation.canDecode(childData)) {
            throw us.neotechnica.panther.subsystem.modules.foundation.models.Exception(
                "Failed to decode conversation.",
                userInfo = mapOf("ConversationIDKey" to idKey),
                metadata =
                    us.neotechnica.panther.subsystem.modules.foundation.models
                        .ExceptionMetadata(this),
            )
        }

        return Conversation.decode(childData).also { SessionStore.upsertConversation(it) }
    }

    // MARK: - Auxiliary

    private fun buildCreateFanOut(
        path: String,
        id: String,
        conversation: Conversation,
        conversationID: ConversationID,
        firstMessage: Message,
    ): Map<String, Any?> {
        val updates = mutableMapOf<String, Any?>()

        for ((key, value) in conversation.encoded.filterKeys { it != ID_KEY }) {
            updates["$path/$id/$key"] = value
        }

        for (participant in conversation.participants) {
            updates[
                "${NetworkPath.users.rawValue}/${participant.userID}/$OPEN_CONVERSATIONS_KEY/${conversationID.key}",
            ] = conversationID.hash
        }

        updates["${NetworkPath.messages.rawValue}/${firstMessage.id}"] =
            firstMessage.encoded.filterKeys { it != ID_KEY }

        for (reference in firstMessage.translationReferences ?: emptyList()) {
            val entry = PendingTranslationArchive.drain(reference.hostingKey) ?: continue
            updates[entry.first] = entry.second
        }

        return updates
    }

    // MARK: - Companion

    private const val ID_KEY = "id"
    private const val ENCODED_HASH_KEY = "hash"
    private const val OPEN_CONVERSATIONS_KEY = "openConversations"
}
