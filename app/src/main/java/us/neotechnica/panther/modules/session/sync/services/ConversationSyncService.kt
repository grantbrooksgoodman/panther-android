//
//  ConversationSyncService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.sync.services

import us.neotechnica.panther.bundle.conversationSync
import us.neotechnica.panther.bundle.conversations
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.conversation.models.ReactionMetadata
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.modifyKey
import us.neotechnica.panther.modules.networking.message.services.MessageService
import us.neotechnica.panther.modules.session.entity.extensions.currentUserParticipant
import us.neotechnica.panther.modules.session.entity.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.resolveMessages
import us.neotechnica.panther.modules.session.entity.extensions.sortedByAscendingSentDate
import us.neotechnica.panther.modules.session.entity.extensions.uniquedByID
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.models.ConversationSyncData
import us.neotechnica.panther.modules.session.sync.models.SynchronizationRecord
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * The service that synchronizes a conversation with its server state.
 *
 * [ConversationSyncService] reconciles a local conversation against
 * the latest data on the server – fetching new messages and updating
 * activities, participants, metadata, and reactions – and commits the
 * reconciled conversation to the session store.
 */
@Suppress("LargeClass")
class ConversationSyncService {
    // MARK: - Properties

    private val syncDataValue = LockIsolated<ConversationSyncData?>(null)

    private var syncData: ConversationSyncData?
        get() = syncDataValue.wrappedValue
        set(value) {
            syncDataValue.wrappedValue = value
        }

    // MARK: - Synchronize Conversation

    /**
     * Synchronizes the given conversation with its server state and
     * returns the reconciled conversation.
     *
     * Concurrent calls for the same conversation coalesce onto a single
     * synchronization. A conversation whose recent synchronization
     * failed is temporarily ignored, on an exponential backoff, and
     * returned unchanged until its cooldown elapses.
     */
    suspend fun synchronizeConversation(conversation: Conversation): Conversation =
        coalescer(conversation.id.key) {
            val recentlyFailed =
                recentlyFailedSyncRecords.wrappedValue.any {
                    it.conversationIDKey == conversation.id.key && !it.isExpired
                }
            if (recentlyFailed) {
                Logger.log(
                    "Conversation recently failed sync; temporarily ignoring updates. " +
                        "(ConversationIDKey: ${conversation.id.key})",
                    domain = LoggerDomain.conversationSync,
                )
                conversation
            } else {
                try {
                    _synchronizeConversation(conversation)
                } catch (exception: Exception) {
                    recordFailure(conversation.id.key)
                    throw exception
                }
            }
        }

    // MARK: - Synchronization

    private suspend fun synchronizeActivities() {
        @Suppress("UNCHECKED_CAST")
        val newActivities =
            syncData?.newData?.get(ConversationUpdatableKey.ACTIVITIES.rawValue) as? List<Map<String, Any?>>
                ?: throw Exception.Networking.decodingFailed(
                    syncData?.newData ?: emptyMap<String, Any?>(),
                    ExceptionMetadata(this),
                )

        val updatedActivities = newActivities.map { Activity.decode(it) }

        val conversation =
            syncData?.conversation?.modifyKey(ConversationUpdatableKey.ACTIVITIES, updatedActivities)
                ?: throw Exception.Networking.typeMismatch(
                    ConversationUpdatableKey.ACTIVITIES.rawValue,
                    updatedActivities,
                    ExceptionMetadata(this),
                )

        syncData = ConversationSyncData(conversation, syncData?.messages ?: emptyList(), syncData?.newData ?: emptyMap())
    }

    private suspend fun synchronizeData() {
        synchronizeActivities()
        synchronizeParticipants()
        synchronizeMetadata()
        synchronizeReactionMetadata()
        synchronizeHash()
    }

    private fun synchronizeHash() {
        val data =
            syncData ?: throw Exception.Networking.decodingFailed(
                syncData?.newData ?: emptyMap<String, Any?>(),
                ExceptionMetadata(this),
            )

        // Use the server hash (from the user's conversationIDs) as the
        // conversation's id.hash so archive lookups match on the server
        // hash rather than the client-computed encodedHash.
        val serverHash =
            UserSessionService.currentUser
                ?.conversationIDs
                ?.firstOrNull { it.key == data.conversation.id.key }
                ?.hash ?: data.conversation.encodedHash

        syncData =
            ConversationSyncData(
                data.conversation.copy(id = ConversationID(key = data.conversation.id.key, hash = serverHash)),
                data.messages,
                data.newData,
            )
    }

    private suspend fun synchronizeMessages(
        messageIDs: List<String>,
        lastTenOnly: Boolean = false,
    ) {
        val conversation =
            syncData?.conversation
                ?: throw Exception("Failed to resolve conversation in sync data.", metadata = ExceptionMetadata(this))

        // Firebase push IDs are chronologically ordered, so sorted map
        // keys give ascending sent-order – reversed[0..9] is the newest ten.
        var ids = messageIDs
        if (lastTenOnly && ids.size >= LAST_MESSAGES_RELOAD_COUNT) {
            ids = ids.reversed().subList(0, LAST_MESSAGES_RELOAD_COUNT)
        }

        val messages = MessageService.getMessages(ids)

        Logger.log(
            "Fetched ${messages.size} message(s) during conversation sync. " +
                "(ConversationIDKey: ${syncData?.conversation?.id?.key ?: "unknown"})",
            domain = LoggerDomain.conversationSync,
        )

        val updatedMessages =
            ((syncData?.messages ?: emptyList()) + messages)
                .filteringSystemMessages
                .uniquedByID
                .sortedByAscendingSentDate

        // The server map is the source of truth for which IDs exist.
        @Suppress("UNCHECKED_CAST")
        val serverMessageIDs =
            (syncData?.newData?.get(ConversationUpdatableKey.MESSAGES.rawValue) as? Map<String, Any?>)
                ?.keys
                ?.sorted() ?: conversation.messageIDs

        syncData =
            ConversationSyncData(
                conversation.copy(messageIDs = serverMessageIDs),
                updatedMessages,
                syncData?.newData ?: emptyMap(),
            )

        synchronizeHash()
    }

    private suspend fun synchronizeMetadata() {
        @Suppress("UNCHECKED_CAST")
        val newMetadata =
            syncData?.newData?.get(ConversationUpdatableKey.METADATA.rawValue) as? Map<String, Any?>
                ?: throw Exception.Networking.decodingFailed(
                    syncData?.newData ?: emptyMap<String, Any?>(),
                    ExceptionMetadata(this),
                )

        val decodedMetadata = ConversationMetadata.decode(newMetadata)

        val conversation =
            syncData?.conversation?.modifyKey(ConversationUpdatableKey.METADATA, decodedMetadata)
                ?: throw Exception.Networking.typeMismatch(
                    ConversationUpdatableKey.METADATA.rawValue,
                    decodedMetadata,
                    ExceptionMetadata(this),
                )

        syncData = ConversationSyncData(conversation, syncData?.messages ?: emptyList(), syncData?.newData ?: emptyMap())
    }

    private suspend fun synchronizeParticipants() {
        @Suppress("UNCHECKED_CAST")
        val participantMap =
            syncData?.newData?.get(ConversationUpdatableKey.PARTICIPANTS.rawValue) as? Map<String, Map<String, Any?>>
                ?: throw Exception.Networking.decodingFailed(
                    syncData?.newData ?: emptyMap<String, Any?>(),
                    ExceptionMetadata(this),
                )

        val updatedParticipants = mutableListOf<Participant>()
        for ((userID, values) in participantMap) {
            val hasDeletedConversation = values[KEY_HAS_DELETED] as? Boolean
            val isTyping = values[KEY_IS_TYPING] as? Boolean
            if (hasDeletedConversation == null || isTyping == null) {
                throw Exception.Networking.decodingFailed(
                    values,
                    ExceptionMetadata(this),
                )
            }

            updatedParticipants.add(
                Participant(userID = userID, hasDeletedConversation = hasDeletedConversation, isTyping = isTyping),
            )
        }

        val conversation =
            syncData?.conversation?.modifyKey(
                ConversationUpdatableKey.PARTICIPANTS,
                updatedParticipants.sortedBy { it.userID },
            ) ?: throw Exception.Networking.typeMismatch(
                ConversationUpdatableKey.PARTICIPANTS.rawValue,
                updatedParticipants,
                ExceptionMetadata(this),
            )

        syncData = ConversationSyncData(conversation, syncData?.messages ?: emptyList(), syncData?.newData ?: emptyMap())
    }

    private suspend fun synchronizeReactionMetadata() {
        @Suppress("UNCHECKED_CAST")
        val newReactionMetadata =
            syncData?.newData?.get(ConversationUpdatableKey.REACTION_METADATA.rawValue) as? List<Map<String, Any?>>
                ?: throw Exception.Networking.decodingFailed(
                    syncData?.newData ?: emptyMap<String, Any?>(),
                    ExceptionMetadata(this),
                )

        val updatedReactionMetadata = newReactionMetadata.map { ReactionMetadata.decode(it) }

        val conversation =
            syncData?.conversation?.modifyKey(ConversationUpdatableKey.REACTION_METADATA, updatedReactionMetadata)
                ?: throw Exception.Networking.typeMismatch(
                    ConversationUpdatableKey.REACTION_METADATA.rawValue,
                    updatedReactionMetadata,
                    ExceptionMetadata(this),
                )

        syncData = ConversationSyncData(conversation, syncData?.messages ?: emptyList(), syncData?.newData ?: emptyMap())
    }

    // MARK: - Auxiliary

    private suspend fun getConversationData(conversation: Conversation) {
        val userInfo = mapOf<String, Any>("ConversationIDHash" to conversation.id.hash, "ConversationIDKey" to conversation.id.key)
        try {
            val currentMessages = conversation.messages?.uniquedByID ?: emptyList()
            val newData: Map<String, Any?> =
                Networking.config.databaseDelegate.getValues<Map<String, Any?>>(
                    path = "${NetworkPath.conversations.rawValue}/${conversation.id.key}",
                    cacheStrategy = CacheStrategy.DISREGARD_CACHE,
                )
            syncData = ConversationSyncData(conversation, currentMessages, newData)
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }
    }

    private fun resolveConversation(userInfo: Map<String, Any>): Conversation {
        val data =
            syncData
                ?: throw Exception("Failed to resolve updated conversation.", metadata = ExceptionMetadata(this))
                    .appending(userInfo)

        Logger.log(
            "Committing synced conversation to store (${data.messages.size} message(s)). " +
                "(ConversationIDKey: ${data.conversation.id.key})",
            domain = LoggerDomain.conversationSync,
        )

        // Synced from network; bypasses RemotelyUpdatable.update.
        SessionStore.upsertConversation(data.conversation)
        if (data.messages.isNotEmpty()) {
            SessionStore.upsertMessages(data.messages.toSet())
        }

        syncData = null
        return data.conversation
    }

    // The leading underscore marks this as an internal synchronization helper.
    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _synchronizeConversation(
        conversation: Conversation,
        hasResolvedMessages: Boolean = false,
    ): Conversation {
        val userInfo = mapOf<String, Any>("ConversationIDHash" to conversation.id.hash, "ConversationIDKey" to conversation.id.key)

        Logger.log("Synchronizing conversation with ID ${conversation.id.key}.", domain = LoggerDomain.conversationSync)

        // Resolve sync data.
        try {
            getConversationData(conversation)
        } catch (exception: Exception) {
            syncData = null
            throw exception.appending(userInfo)
        }

        // Skip message updates if current user isn't participating.
        val currentUserParticipant = conversation.currentUserParticipant
        if (currentUserParticipant == null || currentUserParticipant.hasDeletedConversation) {
            Logger.log(
                "Skipping message retrieval for conversation in which current user is not participating or has deleted. " +
                    "(ConversationIDKey: ${conversation.id.key})",
                domain = LoggerDomain.conversationSync,
            )
            try {
                synchronizeData()
            } catch (exception: Exception) {
                syncData = null
                throw exception.appending(userInfo)
            }
            return resolveConversation(userInfo)
        }

        // Resolve values for message comparison.
        val currentMessages = conversation.messages?.uniquedByID
        if (currentMessages == null) {
            try {
                conversation.resolveMessages()
                val reconciled = SessionStore.conversations[conversation.id.key] ?: conversation
                syncData =
                    ConversationSyncData(reconciled, reconciled.messages?.uniquedByID ?: emptyList(), syncData?.newData ?: emptyMap())

                if (hasResolvedMessages) {
                    val exception =
                        Exception("Messages could not be fully resolved after reconciliation.", metadata = ExceptionMetadata(this))
                            .appending(userInfo)
                    Logger.log(exception)
                    throw exception
                }

                return _synchronizeConversation(reconciled, hasResolvedMessages = true)
            } catch (exception: Exception) {
                syncData = null
                throw exception.appending(userInfo)
            }
        }

        val data =
            syncData
                ?: throw Exception("Failed to resolve current sync data.", metadata = ExceptionMetadata(this)).appending(userInfo)

        @Suppress("UNCHECKED_CAST")
        val messageIDs =
            (data.newData[ConversationUpdatableKey.MESSAGES.rawValue] as? Map<String, Any?>)?.keys?.sorted() ?: emptyList()

        val currentMessageIDs = currentMessages.map { it.id }.toSet()
        var filteredMessageIDs = messageIDs.filter { it !in currentMessageIDs }
        if (filteredMessageIDs.isEmpty()) {
            val existingMessageIDs = conversation.messageIDs.toSet()
            filteredMessageIDs = messageIDs.filter { it !in existingMessageIDs }
        }

        // Update messages if necessary.
        filteredMessageIDs = filteredMessageIDs.distinct()
        if (filteredMessageIDs.isNotEmpty()) {
            try {
                synchronizeMessages(filteredMessageIDs)
                synchronizeData()
            } catch (exception: Exception) {
                syncData = null
                throw exception.appending(userInfo)
            }
            return resolveConversation(userInfo)
        }

        // No new messages: reconcile via metadata, then message reloads.
        return reconcileWithoutNewMessages(conversation, messageIDs, userInfo)
    }

    private suspend fun reconcileWithoutNewMessages(
        conversation: Conversation,
        messageIDs: List<String>,
        userInfo: Map<String, Any>,
    ): Conversation {
        // If no messages to update, synchronize metadata until hashes are sufficiently mismatched.
        try {
            synchronizeData()
        } catch (exception: Exception) {
            syncData = null
            throw exception.appending(userInfo)
        }

        if (syncData?.conversation?.encodedHash != conversation.encodedHash) {
            return resolveConversation(userInfo)
        }

        // If metadata ostensibly didn't need an update, reload select or all messages.
        try {
            synchronizeMessages(messageIDs, lastTenOnly = true)
        } catch (exception: Exception) {
            syncData = null
            throw exception.appending(userInfo)
        }

        // If reloading last 10 messages didn't help, reload all messages.
        if (syncData?.conversation?.encodedHash == conversation.encodedHash) {
            Logger.log(
                "Resolving all messages to fully synchronize conversation. (ConversationIDKey: ${conversation.id.key})",
                domain = LoggerDomain.conversationSync,
            )

            recordFailure(conversation.id.key)

            try {
                synchronizeMessages(messageIDs)
            } catch (exception: Exception) {
                syncData = null
                throw exception.appending(userInfo)
            }
        }

        return resolveConversation(userInfo)
    }

    // MARK: - Companion

    companion object {
        private val coalescer = Coalescer<String, Conversation>()
        private val recentlyFailedSyncRecords = LockIsolated(setOf<SynchronizationRecord>())

        private const val KEY_HAS_DELETED = "hasDeletedConversation"
        private const val KEY_IS_TYPING = "isTyping"
        private const val LAST_MESSAGES_RELOAD_COUNT = 10

        /**
         * Records a failed synchronization attempt for the given
         * conversation, incrementing its attempt count so its
         * backoff cooldown lengthens.
         */
        private fun recordFailure(conversationIDKey: String) {
            recentlyFailedSyncRecords.withValue { ref ->
                var records = ref.value.filter { !it.isExpired }.toSet()
                val previousAttempt = records.firstOrNull { it.conversationIDKey == conversationIDKey }?.attempt ?: 0
                records = records - SynchronizationRecord(conversationIDKey)
                records = records + SynchronizationRecord(conversationIDKey, attempt = previousAttempt + 1)
                ref.value = records
            }
        }
    }
}
