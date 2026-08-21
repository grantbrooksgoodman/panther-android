//
//  ConversationObserverService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.resolveMessages
import us.neotechnica.panther.networking.modules.session.extensions.resolveUsers
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.util.UUID

/**
 * Observes a single conversation for real-time updates, applying each
 * snapshot to the [SessionStore].
 *
 * At most one conversation is observed at a time. When the stream ends
 * it is retried once after a short delay; thereafter the user-node
 * pipeline remains the safety net.
 */
object ConversationObserverService {
    // MARK: - Types

    private data class ObservationState(
        val conversationIDKey: String,
        val generation: String,
        val job: Job,
    )

    // MARK: - Properties

    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val state = LockIsolated<ObservationState?>(null)

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Observation

    /** Whether the given conversation is actively being observed. */
    fun isActivelyObserving(conversationIDKey: String): Boolean = state.wrappedValue?.conversationIDKey == conversationIDKey

    /** Starts observing the given conversation, stopping any prior one. */
    fun startObserving(conversationIDKey: String) {
        state.withValue { reference ->
            reference.value?.job?.cancel()
            val generation = UUID.randomUUID().toString()
            val job =
                observationScope.launch {
                    observe(conversationIDKey, isRetry = false)
                    state.withValue { current ->
                        if (current.value?.generation == generation) current.value = null
                    }
                }
            reference.value = ObservationState(conversationIDKey, generation, job)
        }

        Logger.log("Started observing conversation $conversationIDKey.")
    }

    /** Stops observing the currently observed conversation, if any. */
    fun stopObserving() {
        state.withValue {
            if (it.value != null) Logger.log("Stopped observing conversation.")
            it.value?.job?.cancel()
            it.value = null
        }
    }

    // MARK: - Auxiliary

    private suspend fun observe(
        conversationIDKey: String,
        isRetry: Boolean,
    ) {
        try {
            database
                .observe<Map<String, Any?>>("$PATH_CONVERSATIONS/$conversationIDKey")
                .collect { snapshot -> handleSnapshot(snapshot, conversationIDKey) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Logger.log(Exception.from(throwable, ExceptionMetadata(this)))
        }

        // Stream terminated. Retry once after a short delay; the
        // user-node pipeline remains the safety net.
        if (isRetry) return
        delay(RETRY_DELAY_MILLIS)
        Logger.log("Retrying conversation observation after stream termination.")
        observe(conversationIDKey, isRetry = true)
    }

    private suspend fun handleSnapshot(
        data: Map<String, Any?>,
        conversationIDKey: String,
    ) {
        val hash = data[KEY_HASH] as? String ?: BANG_QUALIFIED_EMPTY
        val decodable = data.toMutableMap().apply { put(KEY_ID, "$conversationIDKey | $hash") }

        if (!Conversation.canDecode(decodable)) {
            Logger.log("Received non-decodable conversation snapshot for $conversationIDKey.")
            return
        }

        try {
            val conversation = Conversation.decode(decodable)
            val existingIDs = SessionStore.getConversation(conversationIDKey)?.messageIDs?.toSet() ?: emptySet()
            val newMessageIDs = conversation.messageIDs.toSet() - existingIDs
            if (newMessageIDs.isNotEmpty()) conversation.resolveMessages(newMessageIDs)

            // Received from real-time observer; bypasses the update path.
            SessionStore.upsertConversation(conversation)

            val participantUserIDs =
                conversation.participants.map { it.userID }.filter { it != User.currentUserID }
            if (participantUserIDs.any { SessionStore.users[it] == null }) conversation.resolveUsers()
        } catch (exception: Exception) {
            Logger.log(exception)
        }
    }

    // MARK: - Companion

    private const val PATH_CONVERSATIONS = "conversations"
    private const val KEY_HASH = "hash"
    private const val KEY_ID = "id"
    private const val RETRY_DELAY_MILLIS = 2_000L
}
