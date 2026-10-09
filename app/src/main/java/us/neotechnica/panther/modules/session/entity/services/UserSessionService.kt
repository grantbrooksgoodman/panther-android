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
import us.neotechnica.panther.bundle.currentUserID
import us.neotechnica.panther.bundle.userSession
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.content.user.extensions.ignoredConversationIDKeys
import us.neotechnica.panther.modules.networking.common.allDataTypes
import us.neotechnica.panther.modules.networking.common.merge
import us.neotechnica.panther.modules.networking.common.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.user.models.DeviceID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.modules.session.state.services.SelfWriteRegistry
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.models.SyncSession
import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.observe
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

// This service exceeds the file-length and type-body-length limits.

/**
 * The service that manages the current user and resolves their
 * session data.
 */
@Suppress("LargeClass")
object UserSessionService {
    // MARK: - Properties

    private val conversationCoalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.REPLACE)
    private val messageCoalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.REPLACE)
    private val updateCoalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.RERUN)
    private val userCoalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.REPLACE)

    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observationTask = LockIsolated<Job?>(null)

    private val currentUserID: String?
        get() = Persistent.string(PersistentStorageKey.currentUserID)

    // MARK: - Computed Properties

    /** The current user, or `null` if none is set. */
    val currentUser: User?
        get() {
            val currentUserID = currentUserID ?: return null
            return SessionStore.users[currentUserID]
        }

    // MARK: - Resolve Current User

    /**
     * Fetches the current user from the server and upserts it to the
     * session store, optionally resolving associated data.
     *
     * Pass one or more [User.DataType] values to indicate which
     * associated data to resolve after the user itself is fetched.
     * Each data type is coalesced independently, so concurrent calls
     * converge rather than duplicate work.
     *
     * ```kotlin
     * userSession.resolveCurrentUser(
     *     setOf(User.DataType.CONVERSATIONS, User.DataType.MESSAGES, User.DataType.USERS),
     * )
     * ```
     *
     * @param data The set of associated data types to resolve. Pass an
     *   empty set to resolve only the user.
     *
     * @throws Exception if the current user identifier has not been
     *   set or any resolution fails.
     */
    suspend fun resolveCurrentUser(data: Set<User.DataType> = emptySet()) {
        resolveCurrentUser()

        if (User.DataType.CONVERSATIONS in data) {
            conversationCoalescer { resolveCurrentUserConversations() }
        }

        if (User.DataType.MESSAGES in data) {
            messageCoalescer { resolveMessagesOnCurrentUserConversations() }
        }

        if (User.DataType.USERS in data) {
            userCoalescer { resolveUsersOnCurrentUserConversations() }
        }
    }

    // MARK: - Current User Observation

    /**
     * Starts observing the current user's record for real-time
     * changes.
     *
     * Relevant changes to the user's blocked users or conversations
     * trigger a refresh. A change to the user's device identifier
     * signs the user out to preserve a single active session.
     */
    fun startObservingCurrentUserChanges() {
        val currentUserID = currentUser?.id ?: return
        observationTask.withValue { task ->
            task.value?.cancel()
            task.value =
                observationScope.launch {
                    try {
                        Logger.log(
                            "Started observing current user changes.",
                            domain = LoggerDomain.userSession,
                        )

                        Networking.config.databaseDelegate
                            .observe<Map<String, Any?>>(
                                listOf(NetworkPath.users.rawValue, currentUserID).joinToString("/"),
                            ).collect { dictionary ->
                                if (blockedUserIDsDidChange(dictionary) || conversationsDidChange(dictionary)) {
                                    updateCurrentUser()
                                } else if (deviceIDDidChange(dictionary)) {
                                    signOutToPreserveSingleActiveUser()
                                } else {
                                    Logger.log(
                                        "Skipping current user update as relevant values do not appear to have changed.",
                                        domain = LoggerDomain.userSession,
                                    )
                                }
                            }
                    } catch (exception: Exception) {
                        Logger.log(exception, domain = LoggerDomain.userSession)
                    }
                }
        }
    }

    /** Stops observing the current user's record. */
    fun stopObservingCurrentUserChanges() {
        observationTask.withValue { task ->
            if (task.value != null) {
                Logger.log(
                    "Stopped observing current user changes.",
                    domain = LoggerDomain.userSession,
                )
            }

            task.value?.cancel()
            task.value = null
        }
    }

    // MARK: - Auxiliary

    private fun blockedUserIDsDidChange(dictionary: Map<String, Any?>): Boolean {
        val currentBlockedUserIDs = (currentUser?.blockedUserIDs ?: bangQualifiedEmptyList).sorted()

        val map = dictionary[User.SerializableKey.BLOCKED_USER_IDS.rawValue] as? Map<*, *> ?: return false
        val updatedBlockedUserIDs = map.keys.map { it.toString() }.sorted()

        return currentBlockedUserIDs != updatedBlockedUserIDs
    }

    private fun commitConversationsToMemory(conversations: Set<Conversation>) {
        // Resolved from archive or network; bypasses RemotelyUpdatable.update.
        SessionStore.upsertConversations(conversations)
    }

    private fun conversationsDidChange(dictionary: Map<String, Any?>): Boolean {
        val currentConversationIDStrings =
            currentUser?.conversationIDs?.map { it.encoded }?.sorted() ?: return true

        val map = dictionary[User.SerializableKey.CONVERSATION_IDS.rawValue] as? Map<*, *> ?: return false
        val updatedConversationIDStrings = map.map { "${it.key} | ${it.value}" }.sorted()

        // Remove deleted conversations.
        val currentIDKeys = currentConversationIDStrings.map { it.idKey }.toSet()
        val updatedIDKeys = updatedConversationIDStrings.map { it.idKey }.toSet()
        val removedIDKeys = currentIDKeys - updatedIDKeys

        if (removedIDKeys.isNotEmpty()) {
            Logger.log(
                Exception(
                    "Removing ${removedIDKeys.size} conversation(s) no longer present on user node.",
                    isReportable = false,
                    userInfo = mapOf("RemovedIDKeys" to removedIDKeys.sorted().joinToString(", ")),
                    metadata = ExceptionMetadata(this),
                ),
                domain = LoggerDomain.userSession,
            )
        }

        for (idKey in removedIDKeys) {
            SessionStore.removeConversation(idKey)
        }

        if (currentConversationIDStrings == updatedConversationIDStrings) return false

        // When there are no removals and every added/changed
        // entry is a version the store already holds or this
        // client just wrote, local state already reflects the
        // payload and no resync is needed.
        val currentSet = currentConversationIDStrings.toSet()
        val changedEntries = updatedConversationIDStrings.filter { it !in currentSet }

        if (removedIDKeys.isEmpty() &&
            changedEntries.isNotEmpty() &&
            changedEntries.all { entry -> ConversationID.from(entry)?.let { isKnownVersion(it) } == true }
        ) {
            Logger.log(
                Exception(
                    "Skipping update for already-known conversation versions.",
                    isReportable = false,
                    userInfo =
                        changedEntries
                            .mapNotNull { ConversationID.from(it) }
                            .associate { it.key to it.hash },
                    metadata = ExceptionMetadata(this),
                ),
                domain = LoggerDomain.userSession,
            )

            return false
        }

        Logger.log(
            Exception(
                "Detected ${changedEntries.size} unrecognized conversation version(s); triggering full resolve.",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            ),
            domain = LoggerDomain.userSession,
        )

        return true
    }

    private fun deviceIDDidChange(dictionary: Map<String, Any?>): Boolean {
        val currentDeviceID = DeviceID.current
        val updatedDeviceID = dictionary[User.SerializableKey.DEVICE_ID.rawValue] as? String ?: return false
        return currentDeviceID != updatedDeviceID
    }

    /**
     * A conversation version is "known" when the session store already
     * holds it, when this client wrote it and the store is still
     * settling, or when the conversation-node observer is actively
     * streaming it (the observer pipeline owns delivery of its
     * updates). Known versions never require ingestion.
     */
    private fun isKnownVersion(conversationID: ConversationID): Boolean =
        SelfWriteRegistry.contains(conversationID) ||
            SessionStore.getConversation(conversationID) != null ||
            ConversationObserverService.isActivelyObserving(conversationID.key)

    /** Fetches the current user from the server and upserts the result to the session store. */
    private suspend fun resolveCurrentUser() {
        val currentUserID =
            currentUserID
                ?: throw Exception(
                    "Current user ID has not been set.",
                    metadata = ExceptionMetadata(this),
                )

        // getUser(id:) upserts the fetched result to the session store.
        UserService.getUser(currentUserID)
    }

    /**
     * Populates conversations for the current user from the archive,
     * sync service, or network.
     *
     * Conversations already present in the archive are used directly.
     * Conversations whose identifier matches an archived entry but
     * whose hash has changed are synchronized. Remaining conversations
     * are fetched from the network. The resolved set is committed to
     * the session store.
     *
     * This method resolves conversation objects only; it does not
     * fetch their associated messages or users.
     */
    private suspend fun resolveCurrentUserConversations() {
        val user = currentUser ?: return
        if (user.id != User.currentUserID) return
        var conversationIDs = user.conversationIDs ?: return

        val conversationsNeedingFetch = mutableSetOf<ConversationID>()
        val conversationsNeedingUpdate = mutableSetOf<Conversation>()
        val decodedConversations = mutableSetOf<Conversation>()

        val ignoredConversationIDKeys = SessionStore.ignoredConversationIDKeys
        conversationIDs = conversationIDs.filter { it.key !in ignoredConversationIDKeys }

        for (conversationID in conversationIDs) {
            val exactMatch = SessionStore.getConversation(conversationID)
            if (exactMatch != null) {
                decodedConversations.merge(listOf(exactMatch))
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
                decodedConversations.merge(listOf(keyMatch))
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

            return commitConversationsToMemory(decodedConversations)
        }

        decodedConversations.merge(
            coroutineScope {
                conversationsNeedingUpdate
                    .map { conversation -> async { SyncSession.conversationSync.synchronizeConversation(conversation) } }
                    .awaitAll()
            },
        )

        if (conversationsNeedingFetch.isEmpty()) {
            return commitConversationsToMemory(decodedConversations)
        }

        val conversations = ConversationService.getConversations(conversationsNeedingFetch.map { it.key })

        // Stamp fetched conversations with the user-record
        // token when it differs from the node hash. Atomic
        // fan-out keeps the two equal at rest, but this read
        // path is not atomic: the user record is snapshotted
        // before the node fetch, and the fetch may be served
        // from cache, so the two can transiently disagree.
        // The user-record token is canonical – it is what
        // resolveCurrentUserConversations compares against to
        // decide staleness, and ConversationSyncService
        // .synchronizeHash applies the same policy on the
        // sync path.
        val reconciledConversations =
            conversations.map { conversation ->
                val userRecordID = conversationsNeedingFetch.firstOrNull { it.key == conversation.id.key }
                if (userRecordID == null || userRecordID.hash == conversation.id.hash) return@map conversation
                conversation.copy(id = ConversationID(key = conversation.id.key, hash = userRecordID.hash))
            }

        decodedConversations.merge(reconciledConversations)
        commitConversationsToMemory(decodedConversations)
    }

    /**
     * Fetches messages for visible conversations on the current user
     * whose messages are not yet in the session store.
     *
     * Conversation resolution populates conversation objects but does
     * not always fetch their messages. This method fills that gap for
     * conversations that were loaded from the archive or freshly
     * fetched from the network.
     */
    private suspend fun resolveMessagesOnCurrentUserConversations() {
        val conversations = currentUser?.conversations ?: return

        val conversationsNeedingMessages =
            conversations
                .visibleForCurrentUser
                .map { it.filteringSystemMessages }
                .filter {
                    val messages = it.messages
                    !it.messageIDs.isBangQualifiedEmpty &&
                        (messages == null || messages.isEmpty() || it.messageIDs.size != messages.size)
                }

        if (conversationsNeedingMessages.isEmpty()) return

        Logger.log(
            Exception(
                "Resolving messages for ${conversationsNeedingMessages.size} conversation(s).",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            ),
            domain = LoggerDomain.userSession,
        )

        coroutineScope {
            conversationsNeedingMessages.map { conversation -> async { conversation.resolveMessages() } }.awaitAll()
        }
    }

    /**
     * Fetches users for visible conversations on the current user
     * whose participants are not yet in the session store.
     */
    private suspend fun resolveUsersOnCurrentUserConversations() {
        val user = currentUser ?: return
        val conversations = user.conversations ?: return

        // Fetch each missing participant once across all conversations,
        // rather than re-fetching a conversation's full roster – shared,
        // already-resolved users included – whenever one participant is absent.
        val missingUserIDs =
            conversations
                .visibleForCurrentUser
                .flatMap { conversation -> conversation.participants.map { it.userID } }
                .toSet()
                .minus(setOfNotNull(User.currentUserID))
                .filter { SessionStore.users[it] == null }

        if (missingUserIDs.isEmpty()) return

        UserService.getUsers(ids = missingUserIDs)
    }

    private fun signOutToPreserveSingleActiveUser() {
        observationScope.launch(Dispatchers.Main) {
            Toast.show(
                Toast(
                    type = Toast.ToastType.Banner(style = ToastStyle.INFO),
                    title = "You have been signed out.",
                    message = "A sign-in was detected from another device.",
                ),
                translating = listOf(Toast.TranslationOptionKey.Message, Toast.TranslationOptionKey.Title),
            )

            Application.reset(onCompletion = Application.ResetCompletionProcedure.NAVIGATE_TO_SPLASH)
        }
    }

    /**
     * Resolves the current user and their conversations in response to
     * an observed change.
     *
     * Only one update runs at a time. A call that arrives while an
     * update is in progress waits for it to finish and then runs once
     * more, so every observed change is reflected by an update that
     * began after it. Any number of such calls collapse into a single
     * rerun.
     */
    private fun updateCurrentUser() {
        observationScope.launch {
            // Logging and follow-up work live inside the operation so
            // they happen once per run, not once per waiting caller.
            updateCoalescer {
                try {
                    resolveCurrentUser(allDataTypes)

                    Logger.log(
                        "Updated current user.",
                        domain = LoggerDomain.userSession,
                    )
                } catch (exception: Exception) {
                    Logger.log(exception, domain = LoggerDomain.userSession)
                }
            }
        }
    }

    private val String.idKey: String
        get() = split(" ").firstOrNull() ?: this
}
