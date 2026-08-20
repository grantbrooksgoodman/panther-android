//
//  UserSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.conversation.services.ConversationService
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.conversations
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.resolveMessages
import us.neotechnica.panther.networking.modules.session.extensions.visibleForCurrentUser
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Resolves and keeps live the signed-in user and their world.
 *
 * [resolveCurrentUser] fetches the current user and, on request, their
 * conversations, messages, and participant users into the
 * [SessionStore]. [startObservingCurrentUserChanges] watches the
 * user's database node and re-resolves whenever their conversation set
 * changes, so the conversations list stays live.
 *
 * **Note:** the iOS service also enforces single-active-device sign-out
 * on a `deviceID` change and skips already-known conversation
 * versions; those refinements are deferred.
 */
object UserSessionService {
    // MARK: - Types

    /** A category of data resolvable alongside the current user. */
    enum class DataType {
        CONVERSATIONS,
        MESSAGES,
        USERS,
    }

    // MARK: - Properties

    private val resolveMutex = Mutex()
    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observationJob = LockIsolated<Job?>(null)

    // MARK: - Computed Properties

    /** The signed-in user, from the store, or `null`. */
    val currentUser: User?
        get() = User.currentUserID?.let { SessionStore.users[it] }

    // MARK: - Resolve

    /**
     * Resolves the current user, plus any requested [dataTypes], into
     * the store. Serialized so concurrent calls do not overlap.
     */
    suspend fun resolveCurrentUser(dataTypes: Set<DataType> = emptySet()): Unit =
        resolveMutex.withLock {
            val currentUserID =
                User.currentUserID
                    ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

            UserService.getUser(currentUserID)

            if (DataType.CONVERSATIONS in dataTypes) resolveConversations()
            if (DataType.MESSAGES in dataTypes) resolveMessagesOnConversations()
            if (DataType.USERS in dataTypes) resolveParticipantUsers()
        }

    // MARK: - Observation

    /** Starts observing the current user's node for live updates. */
    fun startObservingCurrentUserChanges() {
        val currentUserID = currentUser?.id ?: return
        observationJob.withValue { job ->
            job.value?.cancel()
            job.value =
                observationScope.launch {
                    try {
                        Networking.config.databaseDelegate
                            .observe<Map<String, Any?>>("${NETWORK_PATH_USERS}/$currentUserID")
                            .collect { snapshot ->
                                if (conversationsDidChange(snapshot)) updateCurrentUser()
                            }
                    } catch (exception: Exception) {
                        Logger.log(exception)
                    }
                }
        }
    }

    /** Stops observing the current user's node. */
    fun stopObservingCurrentUserChanges() {
        observationJob.withValue {
            it.value?.cancel()
            it.value = null
        }
    }

    // MARK: - Auxiliary

    private suspend fun resolveConversations() {
        val conversationIDs = currentUser?.conversationIDs ?: return
        val conversations = ConversationService.getConversations(conversationIDs.map { it.key })
        val reconciled =
            conversations.map { conversation ->
                val recordID = conversationIDs.firstOrNull { it.key == conversation.id.key }
                if (recordID != null && recordID.hash != conversation.id.hash) {
                    conversation.copy(id = conversation.id.copy(hash = recordID.hash))
                } else {
                    conversation
                }
            }
        SessionStore.upsertConversations(reconciled.toSet())
    }

    private suspend fun resolveMessagesOnConversations() {
        val conversations = currentUser?.conversations?.visibleForCurrentUser ?: return
        coroutineScope {
            conversations.map { conversation -> async { conversation.resolveMessages() } }.awaitAll()
        }
    }

    private suspend fun resolveParticipantUsers() {
        val conversations = currentUser?.conversations?.visibleForCurrentUser ?: return
        val missingUserIDs =
            conversations
                .flatMap { conversation -> conversation.participants.map { it.userID } }
                .toSet()
                .minus(setOfNotNull(User.currentUserID))
                .filter { SessionStore.users[it] == null }
        if (missingUserIDs.isNotEmpty()) UserService.getUsers(missingUserIDs)
    }

    private fun conversationsDidChange(snapshot: Map<String, Any?>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val updatedMap = snapshot[CONVERSATION_IDS_KEY] as? Map<String, Any?> ?: return false
        val updated = updatedMap.map { "${it.key} | ${it.value}" }.sorted()
        val current = currentUser?.conversationIDs?.map { it.encoded }?.sorted() ?: return true

        val currentKeys = current.map { it.substringBefore(" | ") }.toSet()
        val updatedKeys = updated.map { it.substringBefore(" | ") }.toSet()
        for (removedKey in currentKeys - updatedKeys) SessionStore.removeConversation(removedKey)

        return current != updated
    }

    private fun updateCurrentUser() {
        observationScope.launch {
            try {
                resolveCurrentUser(DataType.entries.toSet())
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }
    }

    // MARK: - Companion

    private const val NETWORK_PATH_USERS = "users"
    private const val CONVERSATION_IDS_KEY = "openConversations"
}
