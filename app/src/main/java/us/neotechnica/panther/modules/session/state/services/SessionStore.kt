//
//  SessionStore.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import kotlinx.coroutines.runBlocking
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.session.entity.extensions.isEmpty
import us.neotechnica.panther.modules.session.entity.extensions.isMock
import us.neotechnica.panther.modules.session.entity.extensions.sessionStoreDidChange
import us.neotechnica.panther.modules.session.state.constants.SessionStoreFloats
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.networking.modules.common.extensions.SessionStoreStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.sessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// MARK: - Constants Accessors

private typealias Floats = SessionStoreFloats

/**
 * The in-memory store of the session's conversations, messages, and
 * users.
 *
 * [SessionStore] holds the session's resolved conversations,
 * messages, and users, persists them to disk, and publishes a
 * [SessionStoreChange] whenever its contents change.
 */
// The iOS SessionStore disables swiftlint file_length/type_body_length.
@Suppress("LargeClass")
object SessionStore {
    // MARK: - Types

    private data class ArchiveState(
        val isConversationArchiveDirty: Boolean = false,
        val isMessageArchiveDirty: Boolean = false,
        val isUserArchiveDirty: Boolean = false,
    )

    private data class StoreState(
        val conversations: Map<String, Conversation> = emptyMap(),
        val messages: Map<String, Message> = emptyMap(),
        val users: Map<String, User> = emptyMap(),
    )

    private enum class TaskID(val rawValue: String) {
        DEADLINE_FLUSH("deadlineFlush"),
        PERSIST_CONVERSATION_ARCHIVE("persistConversationArchive"),
        PERSIST_MESSAGE_ARCHIVE("persistMessageArchive"),
        PERSIST_USER_ARCHIVE("persistUserArchive"),
    }

    // MARK: - Properties

    private val archiveState = LockIsolated(ArchiveState())
    private val currentEpoch = LockIsolated(0uL)
    private val storeState = LockIsolated(StoreState())

    // MARK: - Computed Properties

    /** The conversations in the store, keyed by identifier key. */
    val conversations: Map<String, Conversation> get() = storeState.wrappedValue.conversations

    /** The messages in the store, keyed by identifier. */
    val messages: Map<String, Message> get() = storeState.wrappedValue.messages

    /** The users in the store, keyed by identifier. */
    val users: Map<String, User> get() = storeState.wrappedValue.users

    private var persistedConversationArchive: Set<Conversation>?
        get() =
            Persistent.archive(PersistentStorageKey.sessionStore(SessionStoreStorageKey.CONVERSATION_ARCHIVE)) { it }?.let { maps ->
                runBlocking { maps.mapNotNull { runCatching { Conversation.decode(it) }.getOrNull() } }.toSet()
            }
        set(value) {
            Persistent.setArchive(PersistentStorageKey.sessionStore(SessionStoreStorageKey.CONVERSATION_ARCHIVE), value?.map { it.encoded })
        }

    private var persistedMessageArchive: Set<Message>?
        get() =
            Persistent.archive(PersistentStorageKey.sessionStore(SessionStoreStorageKey.MESSAGE_ARCHIVE)) { it }?.let { maps ->
                runBlocking { maps.mapNotNull { runCatching { Message.decode(it) }.getOrNull() } }.toSet()
            }
        set(value) {
            Persistent.setArchive(PersistentStorageKey.sessionStore(SessionStoreStorageKey.MESSAGE_ARCHIVE), value?.map { it.encoded })
        }

    private var persistedUserArchive: Set<User>?
        get() =
            Persistent.archive(PersistentStorageKey.sessionStore(SessionStoreStorageKey.USER_ARCHIVE)) { it }?.let { maps ->
                runBlocking { maps.mapNotNull { runCatching { User.decode(it) }.getOrNull() } }.toSet()
            }
        set(value) {
            Persistent.setArchive(PersistentStorageKey.sessionStore(SessionStoreStorageKey.USER_ARCHIVE), value?.map { it.encoded })
        }

    // MARK: - Init

    init {
        loadArchives()
    }

    /** Clears state and reloads the archives; for tests only. */
    internal fun reloadForTesting() {
        storeState.wrappedValue = StoreState()
        archiveState.wrappedValue = ArchiveState()
        currentEpoch.wrappedValue = 0uL
        loadArchives()
    }

    private fun loadArchives() {
        persistedConversationArchive?.let { archive ->
            storeState.withValue { state ->
                val loaded =
                    archive
                        .filter {
                            !it.isEmpty &&
                                !it.isMock &&
                                it.id.hash.isNotBlank() &&
                                it.id.key.isNotBlank()
                        }.associateBy { it.id.key }
                state.value = state.value.copy(conversations = state.value.conversations + loaded)
            }

            Logger.log("Loaded ${archive.size} conversations into memory.", domain = LoggerDomain.conversationStore)
        }

        persistedMessageArchive?.let { archive ->
            val messages = archive.toList().filteringSystemMessages
            storeState.withValue { state ->
                state.value = state.value.copy(messages = state.value.messages + messages.associateBy { it.id })
            }

            Logger.log("Loaded ${archive.size} messages into memory.", domain = LoggerDomain.messageStore)
        }

        persistedUserArchive?.let { archive ->
            storeState.withValue { state ->
                val loaded = archive.filter { it.id.isNotBlank() }.associateBy { it.id }
                state.value = state.value.copy(users = state.value.users + loaded)
            }

            Logger.log("Loaded ${archive.size} users into memory.", domain = LoggerDomain.userStore)
        }

        sweepOrphanedMessages()
    }

    // MARK: - Conversation Methods

    /** Removes every conversation from the store. */
    fun clearConversationArchive() {
        var removedIDKeys = emptySet<String>()
        storeState.withValue { state ->
            removedIDKeys = state.value.conversations.keys.toSet()
            state.value = state.value.copy(conversations = emptyMap())
        }

        persistConversationArchive()
        if (removedIDKeys.isNotEmpty()) {
            emitChange(SessionStoreChange.Conversations(upsertedIDKeys = emptySet(), removedIDKeys = removedIDKeys))
        }
    }

    /**
     * Returns the conversation with the given identifier, or `null`
     * if no conversation with a matching key and hash is in the
     * store.
     */
    fun getConversation(id: ConversationID): Conversation? {
        val conversation = storeState.wrappedValue.conversations[id.key] ?: return null
        return if (conversation.id == id) conversation else null
    }

    /**
     * Returns the conversation with the given identifier key, or
     * `null` if it is not in the store.
     */
    fun getConversation(idKey: String): Conversation? = storeState.wrappedValue.conversations[idKey]

    /**
     * Removes the conversation with the given identifier key from
     * the store.
     *
     * Any of the conversation's messages that no longer belong to
     * another conversation are removed as well.
     */
    fun removeConversation(idKey: String) {
        var didRemove = false
        var orphanedMessageIDs = emptySet<String>()

        storeState.withValue { state ->
            val conversation = state.value.conversations[idKey] ?: return@withValue
            didRemove = true

            val conversationMessageIDs = conversation.messageIDs.toSet()
            val conversationsAfter = state.value.conversations - idKey
            val allOtherMessageIDs = conversationsAfter.values.flatMap { it.messageIDs }.toSet()
            orphanedMessageIDs = conversationMessageIDs - allOtherMessageIDs

            state.value =
                state.value.copy(
                    conversations = conversationsAfter,
                    messages = state.value.messages - orphanedMessageIDs,
                )
        }

        if (!didRemove) return
        persistConversationArchive()

        if (orphanedMessageIDs.isNotEmpty()) {
            persistMessageArchive()
            emitChange(SessionStoreChange.Messages(upsertedIDs = emptySet(), removedIDs = orphanedMessageIDs))
        }

        Logger.log(
            "Removed conversation from persisted archive. (ConversationIDKey: $idKey)",
            domain = LoggerDomain.conversationStore,
        )

        emitChange(SessionStoreChange.Conversations(upsertedIDKeys = emptySet(), removedIDKeys = setOf(idKey)))
    }

    /**
     * Inserts or updates the given conversation in the store.
     *
     * Empty, mock, or unidentified conversations are ignored.
     * Observers are notified only when the conversation represents a
     * change.
     */
    fun upsertConversation(conversation: Conversation) {
        if (conversation.isEmpty ||
            conversation.isMock ||
            conversation.id.hash.isBlank() ||
            conversation.id.key.isBlank()
        ) {
            return
        }

        var didChange = false
        var shouldPersist = true
        storeState.withValue { state ->
            val existingConversation = state.value.conversations[conversation.id.key]
            if (existingConversation != null) {
                didChange =
                    !existingConversation.isTypingStatusEqual(conversation) ||
                    existingConversation.encodedHash != conversation.encodedHash
                shouldPersist = existingConversation != conversation
            } else {
                didChange = true
            }

            state.value =
                state.value.copy(conversations = state.value.conversations + (conversation.id.key to conversation))
        }

        // Gates on full equality rather than didChange – encodedHash
        // may under-report differences, and persistence must never
        // skip one.
        if (shouldPersist) {
            persistConversationArchive()
        }

        if (didChange) {
            Logger.log(
                "Added conversation to persisted archive. " +
                    "(ConversationIDKey: ${conversation.id.key}, ConversationIDHash: ${conversation.id.hash})",
                domain = LoggerDomain.conversationStore,
            )

            emitChange(
                SessionStoreChange.Conversations(upsertedIDKeys = setOf(conversation.id.key), removedIDKeys = emptySet()),
            )
        }
    }

    /**
     * Inserts or updates the given conversations in the store.
     *
     * Empty, mock, or unidentified conversations are ignored.
     */
    fun upsertConversations(newConversations: Set<Conversation>) {
        val filtered =
            newConversations.filter {
                !it.isEmpty &&
                    !it.isMock &&
                    it.id.hash.isNotBlank() &&
                    it.id.key.isNotBlank()
            }

        val changedIDKeys = mutableSetOf<String>()
        storeState.withValue { state ->
            var conversations = state.value.conversations
            for (conversation in filtered) {
                if (conversations[conversation.id.key] != conversation) {
                    changedIDKeys.add(conversation.id.key)
                }
                conversations = conversations + (conversation.id.key to conversation)
            }
            state.value = state.value.copy(conversations = conversations)
        }

        if (changedIDKeys.isEmpty()) return
        persistConversationArchive()

        Logger.log("Added ${changedIDKeys.size} conversations to persisted archive.", domain = LoggerDomain.conversationStore)

        emitChange(SessionStoreChange.Conversations(upsertedIDKeys = changedIDKeys, removedIDKeys = emptySet()))
    }

    // MARK: - Message Methods

    /** Removes every message from the store. */
    fun clearMessageArchive() {
        var clearedIDs = emptySet<String>()
        storeState.withValue { state ->
            clearedIDs = state.value.messages.keys.toSet()
            state.value = state.value.copy(messages = emptyMap())
        }

        persistMessageArchive()
        if (clearedIDs.isNotEmpty()) {
            emitChange(SessionStoreChange.Messages(upsertedIDs = emptySet(), removedIDs = clearedIDs))
        }
    }

    /** Removes the messages with the given identifiers from the store. */
    fun removeMessages(ids: Set<String>) {
        var removedIDs = emptySet<String>()
        storeState.withValue { state ->
            val present = ids.filter { state.value.messages.containsKey(it) }.toSet()
            removedIDs = present
            state.value = state.value.copy(messages = state.value.messages - present)
        }

        if (removedIDs.isEmpty()) return
        persistMessageArchive()

        Logger.log("Removed ${removedIDs.size} message(s) from persisted archive.", domain = LoggerDomain.messageStore)

        emitChange(SessionStoreChange.Messages(upsertedIDs = emptySet(), removedIDs = removedIDs))
    }

    /**
     * Inserts or updates the given messages in the store.
     *
     * System messages are ignored.
     */
    fun upsertMessages(newMessages: Set<Message>) {
        val messages = newMessages.toList().filteringSystemMessages

        val changedIDs = mutableSetOf<String>()
        storeState.withValue { state ->
            var stored = state.value.messages
            for (message in messages) {
                if (stored[message.id] != message) changedIDs.add(message.id)
                stored = stored + (message.id to message)
            }
            state.value = state.value.copy(messages = stored)
        }

        if (changedIDs.isEmpty()) return
        persistMessageArchive()

        Logger.log("Added ${changedIDs.size} messages to persisted archive.", domain = LoggerDomain.messageStore)

        emitChange(SessionStoreChange.Messages(upsertedIDs = changedIDs, removedIDs = emptySet()))
    }

    // MARK: - User Methods

    /** Removes every user from the store. */
    fun clearUserArchive() {
        var clearedIDs = emptySet<String>()
        storeState.withValue { state ->
            clearedIDs = state.value.users.keys.toSet()
            state.value = state.value.copy(users = emptyMap())
        }

        persistUserArchive()
        if (clearedIDs.isNotEmpty()) {
            emitChange(SessionStoreChange.Users(upsertedIDs = emptySet(), removedIDs = clearedIDs))
        }
    }

    /** Removes the user with the given identifier from the store. */
    fun removeUser(id: String) {
        var didRemove = false
        storeState.withValue { state ->
            didRemove = state.value.users.containsKey(id)
            state.value = state.value.copy(users = state.value.users - id)
        }

        if (!didRemove) return
        persistUserArchive()

        Logger.log("Removed user from persisted archive. (UserID: $id)", domain = LoggerDomain.userStore)

        emitChange(SessionStoreChange.Users(upsertedIDs = emptySet(), removedIDs = setOf(id)))
    }

    /** Inserts or updates the given user in the store. */
    fun upsertUser(user: User) {
        var didChange = false
        storeState.withValue { state ->
            didChange = state.value.users[user.id] != user
            state.value = state.value.copy(users = state.value.users + (user.id to user))
        }

        if (!didChange) return
        persistUserArchive()

        Logger.log("Added user to persisted archive. (UserID: ${user.id})", domain = LoggerDomain.userStore)

        emitChange(SessionStoreChange.Users(upsertedIDs = setOf(user.id), removedIDs = emptySet()))
    }

    /** Inserts or updates the given users in the store. */
    fun upsertUsers(newUsers: Set<User>) {
        val changedIDs = mutableSetOf<String>()
        storeState.withValue { state ->
            var users = state.value.users
            for (user in newUsers) {
                if (users[user.id] != user) changedIDs.add(user.id)
                users = users + (user.id to user)
            }
            state.value = state.value.copy(users = users)
        }

        if (changedIDs.isEmpty()) return
        persistUserArchive()

        Logger.log("Added ${changedIDs.size} users to persisted archive.", domain = LoggerDomain.userStore)

        emitChange(SessionStoreChange.Users(upsertedIDs = changedIDs, removedIDs = emptySet()))
    }

    // MARK: - Epoch

    /**
     * Advances the epoch counter so that any in-flight debounced
     * persist tasks scheduled under the previous epoch will no-op
     * when they fire.
     */
    fun advanceEpoch() {
        currentEpoch.withValue { it.value = it.value + 1uL }
    }

    // MARK: - Flush

    /**
     * Synchronously persists all dirty archives, bypassing the
     * debounced schedule. Call from background-entry and termination
     * handlers to avoid data loss.
     */
    fun flushNow() {
        val drained =
            archiveState.withValue {
                val current = it.value
                it.value = ArchiveState()
                current
            }

        if (drained.isConversationArchiveDirty) {
            val conversationsSnapshot = storeState.wrappedValue.conversations.values.toSet()
            persistedConversationArchive = conversationsSnapshot.ifEmpty { null }
        }

        if (drained.isMessageArchiveDirty) {
            val messagesSnapshot = cappedMessageSnapshot()
            persistedMessageArchive = messagesSnapshot.ifEmpty { null }
        }

        if (drained.isUserArchiveDirty) {
            val usersSnapshot = storeState.wrappedValue.users.values.toSet()
            persistedUserArchive = usersSnapshot.ifEmpty { null }
        }

        if (drained.isConversationArchiveDirty ||
            drained.isMessageArchiveDirty ||
            drained.isUserArchiveDirty
        ) {
            Logger.log("Flushed dirty archives synchronously.", domain = LoggerDomain.sessionStore)
        }
    }

    // MARK: - Auxiliary

    /**
     * Returns the in-memory messages capped to the newest messages
     * per conversation for persistence.
     */
    private fun cappedMessageSnapshot(): Set<Message> {
        val currentState = storeState.wrappedValue
        val messages = currentState.messages
        if (messages.isEmpty()) return emptySet()

        // Group referenced message IDs by conversation, keeping only
        // the newest per conversation.
        val retainedIDs = mutableSetOf<String>()
        for (conversation in currentState.conversations.values) {
            val messageIDs = conversation.messageIDs.filter { messages[it] != null }

            if (messageIDs.size <= Floats.MESSAGE_ARCHIVE_CAP_PER_CONVERSATION) {
                retainedIDs.addAll(messageIDs)
            } else {
                retainedIDs.addAll(
                    messageIDs
                        .mapNotNull { messages[it] }
                        .sortedBy { it.sentDate }
                        .takeLast(Floats.MESSAGE_ARCHIVE_CAP_PER_CONVERSATION)
                        .map { it.id },
                )
            }
        }

        return retainedIDs.mapNotNull { messages[it] }.toSet()
    }

    private fun emitChange(change: SessionStoreChange) {
        DependencyValues.current.sharedEvents.sessionStoreDidChange.send(change)
    }

    private fun persistConversationArchive() {
        archiveState.withValue { it.value = it.value.copy(isConversationArchiveDirty = true) }
        val capturedEpoch = currentEpoch.wrappedValue

        Task.debounced("SessionStore/${TaskID.PERSIST_CONVERSATION_ARCHIVE.rawValue}", 250.milliseconds) {
            if (currentEpoch.wrappedValue != capturedEpoch) return@debounced
            archiveState.withValue { it.value = it.value.copy(isConversationArchiveDirty = false) }

            val conversationsSnapshot = storeState.wrappedValue.conversations.values.toSet()
            persistedConversationArchive = conversationsSnapshot.ifEmpty { null }
        }

        scheduleDeadlineFlush()
    }

    private fun persistMessageArchive() {
        archiveState.withValue { it.value = it.value.copy(isMessageArchiveDirty = true) }
        val capturedEpoch = currentEpoch.wrappedValue

        Task.debounced("SessionStore/${TaskID.PERSIST_MESSAGE_ARCHIVE.rawValue}", 250.milliseconds) {
            if (currentEpoch.wrappedValue != capturedEpoch) return@debounced
            archiveState.withValue { it.value = it.value.copy(isMessageArchiveDirty = false) }

            val messagesSnapshot = cappedMessageSnapshot()
            persistedMessageArchive = messagesSnapshot.ifEmpty { null }
        }

        scheduleDeadlineFlush()
    }

    private fun persistUserArchive() {
        archiveState.withValue { it.value = it.value.copy(isUserArchiveDirty = true) }
        val capturedEpoch = currentEpoch.wrappedValue

        Task.debounced("SessionStore/${TaskID.PERSIST_USER_ARCHIVE.rawValue}", 250.milliseconds) {
            if (currentEpoch.wrappedValue != capturedEpoch) return@debounced
            archiveState.withValue { it.value = it.value.copy(isUserArchiveDirty = false) }

            val usersSnapshot = storeState.wrappedValue.users.values.toSet()
            persistedUserArchive = usersSnapshot.ifEmpty { null }
        }

        scheduleDeadlineFlush()
    }

    /**
     * Schedules a forced flush after 1 second under sustained
     * mutation, guaranteeing that dirty state reaches disk even when
     * rapid writes keep resetting the 250 ms debounce.
     */
    private fun scheduleDeadlineFlush() {
        val capturedEpoch = currentEpoch.wrappedValue

        Task.debounced("SessionStore/${TaskID.DEADLINE_FLUSH.rawValue}", 1.seconds) {
            if (currentEpoch.wrappedValue != capturedEpoch) return@debounced
            flushNow()
        }
    }

    /**
     * Removes messages from memory whose ID does not appear in any
     * stored conversation's message IDs.
     */
    private fun sweepOrphanedMessages() {
        var orphanCount = 0
        storeState.withValue { state ->
            val referencedIDs = state.value.conversations.values.flatMap { it.messageIDs }.toSet()
            val orphanedIDs = state.value.messages.keys.toSet() - referencedIDs
            orphanCount = orphanedIDs.size
            state.value = state.value.copy(messages = state.value.messages - orphanedIDs)
        }

        if (orphanCount == 0) return
        persistMessageArchive()

        Logger.log("Swept $orphanCount orphaned message(s) at startup.", domain = LoggerDomain.messageStore)
    }

    private fun Conversation.isTypingStatusEqual(conversation: Conversation): Boolean =
        participants.map { it.isTyping }.toSet() == conversation.participants.map { it.isTyping }.toSet()
}
