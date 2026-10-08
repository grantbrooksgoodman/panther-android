//
//  UserSessionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.userSession
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.content.user.extensions.ignoredConversationIDKeys
import us.neotechnica.panther.modules.content.user.extensions.updateDeviceIDIfNeeded
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.user.models.DeviceID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.resolveMessages
import us.neotechnica.panther.modules.session.entity.extensions.visibleForCurrentUser
import us.neotechnica.panther.modules.session.state.services.SelfWriteRegistry
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.models.SyncSession
import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.database.interfaces.observe
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

// This service exceeds the file-length and type-body-length limits.

/**
 * Resolves and keeps live the signed-in user and their world.
 *
 * [resolveCurrentUser] fetches the current user and, on request, their
 * conversations, messages, and participant users into the
 * [SessionStore]. [startObservingCurrentUserChanges] watches the user's
 * database node: a change to their blocked users or conversations
 * re-resolves them, while a change to their device identifier signs the
 * device out to preserve a single active session.
 */
@Suppress("LargeClass")
object UserSessionService {
    // MARK: - Types

    private enum class UpdateState {
        IDLE,
        RUNNING,
        RUNNING_WITH_PENDING,
    }

    /** A category of data resolvable alongside the current user. */
    enum class DataType {
        CONVERSATIONS,
        MESSAGES,
        USERS,
    }

    // MARK: - Properties

    private val conversationCoalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.REPLACE)
    private val messageCoalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.REPLACE)
    private val observationJob = LockIsolated<Job?>(null)
    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val updateState = LockIsolated(UpdateState.IDLE)
    private val userCoalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.REPLACE)

    // MARK: - Computed Properties

    /** The signed-in user, from the store, or `null`. */
    val currentUser: User?
        get() = User.currentUserID?.let { SessionStore.users[it] }

    // MARK: - Resolve

    /**
     * Resolves the current user, plus any requested [dataTypes], into
     * the store. Each data type is coalesced independently, so
     * concurrent calls converge rather than duplicate work.
     */
    suspend fun resolveCurrentUser(dataTypes: Set<DataType> = emptySet()) {
        resolveCurrentUserRecord()

        if (DataType.CONVERSATIONS in dataTypes) {
            conversationCoalescer { resolveCurrentUserConversations() }
        }

        if (DataType.MESSAGES in dataTypes) {
            messageCoalescer { resolveMessagesOnCurrentUserConversations() }
        }

        if (DataType.USERS in dataTypes) {
            userCoalescer { resolveUsersOnCurrentUserConversations() }
        }
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
                        // Claim the session for this device before observing, so the
                        // deviceID watcher does not sign this device out against a node
                        // deviceID last written by another device or a prior install.
                        // (The device ID is claimed here on first run.)
                        runCatching { currentUser?.updateDeviceIDIfNeeded() }
                            .onFailure { Logger.log("Failed to claim device ID. ${it.message}") }

                        Logger.log("Started observing current user changes.", domain = LoggerDomain.userSession)
                        Networking.config.databaseDelegate
                            .observe<Map<String, Any?>>("$NETWORK_PATH_USERS/$currentUserID")
                            .collect { snapshot ->
                                if (blockedUserIDsDidChange(snapshot) || conversationsDidChange(snapshot)) {
                                    updateCurrentUser()
                                } else if (deviceIDDidChange(snapshot)) {
                                    signOutToPreserveSingleActiveUser()
                                } else {
                                    Logger.log(
                                        "Skipping current user update as relevant values do not appear to have changed.",
                                        domain = LoggerDomain.userSession,
                                    )
                                }
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
            if (it.value != null) {
                Logger.log("Stopped observing current user changes.", domain = LoggerDomain.userSession)
            }
            it.value?.cancel()
            it.value = null
        }
    }

    // MARK: - Auxiliary

    private fun blockedUserIDsDidChange(snapshot: Map<String, Any?>): Boolean {
        val currentBlockedUserIDs = (currentUser?.blockedUserIDs ?: bangQualifiedEmptyList).sorted()

        @Suppress("UNCHECKED_CAST")
        val map = snapshot[BLOCKED_USER_IDS_KEY] as? Map<String, Any?> ?: return false
        val updatedBlockedUserIDs = map.keys.sorted()

        return currentBlockedUserIDs != updatedBlockedUserIDs
    }

    private fun conversationsDidChange(snapshot: Map<String, Any?>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val updatedMap = snapshot[CONVERSATION_IDS_KEY] as? Map<String, Any?> ?: return false
        val updated = updatedMap.mapValues { it.value.toString() }
        val current =
            currentUser?.conversationIDs?.associate { it.key to it.hash }
                ?: return updated.isNotEmpty()

        for (removedKey in current.keys - updated.keys) SessionStore.removeConversation(removedKey)

        // A changed entry is a new key or a differing hash token.
        val changedKeys = updated.filterKeys { current[it] != updated[it] }.keys
        if (changedKeys.isEmpty()) return false

        // Skip re-resolving changes that are the app's own writes, are
        // already applied to the store, or are being live-observed.
        val allKnown =
            changedKeys.all { key ->
                val id = ConversationID(key = key, hash = updated.getValue(key))
                SelfWriteRegistry.contains(id) ||
                    SessionStore.getConversation(id) != null ||
                    ConversationObserverService.isActivelyObserving(key)
            }

        return !allKnown
    }

    private fun deviceIDDidChange(snapshot: Map<String, Any?>): Boolean {
        val updatedDeviceID = snapshot[DEVICE_ID_KEY] as? String ?: return false
        return DeviceID.current != updatedDeviceID
    }

    /** Fetches the current user from the server and upserts it to the store. */
    private suspend fun resolveCurrentUserRecord() {
        val currentUserID =
            User.currentUserID
                ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

        UserService.getUser(currentUserID)

        // Apply the user's stored language before resolving messages, so
        // translation warming targets the language the chat will display.
        currentUser?.languageCode?.let { CoreUtilities.setLanguageCode(it) }
    }

    private suspend fun resolveCurrentUserConversations() {
        val user = currentUser ?: return
        if (user.id != User.currentUserID) return

        val ignoredConversationIDKeys = SessionStore.ignoredConversationIDKeys
        val conversationIDs = (user.conversationIDs ?: return).filter { it.key !in ignoredConversationIDKeys }

        val conversationsNeedingFetch = mutableSetOf<ConversationID>()
        val conversationsNeedingUpdate = mutableSetOf<Conversation>()
        val decodedConversations = mutableSetOf<Conversation>()

        for (conversationID in conversationIDs) {
            val exactMatch = SessionStore.getConversation(conversationID)
            if (exactMatch != null) {
                decodedConversations.add(exactMatch)
                continue
            }

            val keyMatch = SessionStore.getConversation(conversationID.key)
            if (keyMatch == null) {
                conversationsNeedingFetch.add(conversationID)
            } else if (SelfWriteRegistry.contains(conversationID) ||
                ConversationObserverService.isActivelyObserving(conversationID.key)
            ) {
                // Self-written or actively observed: the owning
                // pipeline settles the store; no network sync needed.
                decodedConversations.add(keyMatch)
            } else {
                conversationsNeedingUpdate.add(keyMatch)
            }
        }

        Logger.log(
            "Conversations needing update: ${conversationsNeedingUpdate.size}\n" +
                "Conversations needing fetch: ${conversationsNeedingFetch.size}\n" +
                "Ignored conversations: ${ignoredConversationIDKeys.size}\n" +
                "Decoded conversations: ${decodedConversations.size}",
            domain = LoggerDomain.userSession,
        )

        if (conversationsNeedingFetch.isEmpty() && conversationsNeedingUpdate.isEmpty()) {
            val existingConversations = user.conversations
            if (!existingConversations.isNullOrEmpty() && existingConversations.toSet() == decodedConversations) {
                return
            }
            SessionStore.upsertConversations(decodedConversations)
            return
        }

        if (conversationsNeedingUpdate.isNotEmpty()) {
            val synchronized =
                coroutineScope {
                    conversationsNeedingUpdate
                        .map { conversation -> async { SyncSession.conversationSync.synchronizeConversation(conversation) } }
                        .awaitAll()
                }
            decodedConversations.addAll(synchronized)
        }

        if (conversationsNeedingFetch.isEmpty()) {
            SessionStore.upsertConversations(decodedConversations)
            return
        }

        val conversations = ConversationService.getConversations(conversationsNeedingFetch.map { it.key })

        // Stamp fetched conversations with the user-record token when it
        // differs from the node hash. Atomic fan-out keeps the two equal
        // at rest, but this read path is not atomic: the user record is
        // snapshotted before the node fetch, and the fetch may be served
        // from cache, so the two can transiently disagree. The
        // user-record token is canonical.
        val reconciledConversations =
            conversations.map { conversation ->
                val userRecordID = conversationsNeedingFetch.firstOrNull { it.key == conversation.id.key }
                if (userRecordID != null && userRecordID.hash != conversation.id.hash) {
                    conversation.copy(id = conversation.id.copy(hash = userRecordID.hash))
                } else {
                    conversation
                }
            }

        decodedConversations.addAll(reconciledConversations)
        SessionStore.upsertConversations(decodedConversations)
    }

    private suspend fun resolveMessagesOnCurrentUserConversations() {
        val conversations = currentUser?.conversations ?: return

        val conversationsNeedingMessages =
            conversations
                .visibleForCurrentUser
                .map { it.filteringSystemMessages }
                .filter {
                    it.messageIDs.isNotEmpty() &&
                        (it.messages == null || it.messages?.isEmpty() == true || it.messageIDs.size != it.messages?.size)
                }

        if (conversationsNeedingMessages.isEmpty()) return

        Logger.log(
            "Resolving messages for ${conversationsNeedingMessages.size} conversation(s).",
            domain = LoggerDomain.userSession,
        )

        coroutineScope {
            conversationsNeedingMessages.map { conversation -> async { conversation.resolveMessages() } }.awaitAll()
        }
    }

    private suspend fun resolveUsersOnCurrentUserConversations() {
        val conversations = currentUser?.conversations?.visibleForCurrentUser ?: return
        val missingUserIDs =
            conversations
                .flatMap { conversation -> conversation.participants.map { it.userID } }
                .toSet()
                .minus(setOfNotNull(User.currentUserID))
                .filter { SessionStore.users[it] == null }
        if (missingUserIDs.isNotEmpty()) UserService.getUsers(missingUserIDs)
    }

    private fun signOutToPreserveSingleActiveUser() {
        observationScope.launch(Dispatchers.Main) {
            Toast.show(
                Toast(
                    type = Toast.ToastType.Banner(style = ToastStyle.INFO),
                    title = "You have been signed out.",
                    message = "A sign-in was detected from another device.",
                ),
            )

            Application.reset(onCompletion = Application.ResetCompletionProcedure.NAVIGATE_TO_SPLASH)
        }
    }

    private fun updateCurrentUser() {
        observationScope.launch {
            val didStart =
                updateState.withValue { ref ->
                    when (ref.value) {
                        UpdateState.IDLE -> {
                            ref.value = UpdateState.RUNNING
                            true
                        }

                        UpdateState.RUNNING -> {
                            ref.value = UpdateState.RUNNING_WITH_PENDING
                            false
                        }

                        UpdateState.RUNNING_WITH_PENDING -> false
                    }
                }

            if (!didStart) {
                Logger.log(
                    "Queuing pending current user update because an update is already occurring.",
                    domain = LoggerDomain.userSession,
                )
                return@launch
            }

            while (true) {
                try {
                    resolveCurrentUser(DataType.entries.toSet())
                    Logger.log("Updated current user.", domain = LoggerDomain.userSession)
                } catch (exception: Exception) {
                    Logger.log(exception)
                }

                val shouldContinue =
                    updateState.withValue { ref ->
                        when (ref.value) {
                            UpdateState.IDLE -> false

                            UpdateState.RUNNING -> {
                                ref.value = UpdateState.IDLE
                                false
                            }

                            UpdateState.RUNNING_WITH_PENDING -> {
                                ref.value = UpdateState.RUNNING
                                true
                            }
                        }
                    }

                if (!shouldContinue) break
                Logger.log("Retrying current user update from pending request.", domain = LoggerDomain.userSession)
            }
        }
    }

    // MARK: - Companion

    private const val BLOCKED_USER_IDS_KEY = "blockedUserIDs"
    private const val CONVERSATION_IDS_KEY = "openConversations"
    private const val DEVICE_ID_KEY = "deviceID"
    private const val NETWORK_PATH_USERS = "users"
}
