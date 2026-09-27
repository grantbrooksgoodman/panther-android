//
//  ConversationSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.update
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.asDisplayMessage
import us.neotechnica.panther.modules.session.entity.extensions.currentConversationDidBecomeUnavailable
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.session.entity.extensions.hydrated
import us.neotechnica.panther.modules.session.entity.extensions.messageOutboxDidChange
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.offsetFromCurrentUserAdditionDate
import us.neotechnica.panther.modules.session.entity.extensions.sessionStoreDidChange
import us.neotechnica.panther.modules.session.entity.extensions.sortedByAscendingSentDate
import us.neotechnica.panther.modules.session.entity.extensions.uniquedByID
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.models.send
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.PendingTranslationArchive
import us.neotechnica.panther.modules.session.state.services.SelfWriteRegistry
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService

/**
 * Manages the current conversation and the messages displayed for it.
 *
 * The current conversation pointer references a stored conversation by
 * key; [displayedMessages] recomputes from the [SessionStore] (plus any
 * pending outbox entries) whenever the store or outbox changes.
 * [addMessages] commits new messages, their conversation index entries,
 * a participant un-delete, a typing reset, the conversation hash, and
 * the participants' hash tokens in a single atomic fan-out.
 */
object ConversationSessionService {
    // MARK: - Types

    private sealed interface CurrentConversationReference {
        data class Draft(
            val conversation: Conversation,
        ) : CurrentConversationReference

        data object None : CurrentConversationReference

        data class Stored(
            val idKey: String,
        ) : CurrentConversationReference
    }

    // MARK: - Properties

    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val reference = LockIsolated<CurrentConversationReference>(CurrentConversationReference.None)
    private val messageOffset = LockIsolated(DEFAULT_MESSAGE_OFFSET)
    private val observersStarted = LockIsolated(false)

    private val internalDisplayedMessages = MutableStateFlow(emptyList<Message>())

    private val database get() = Networking.config.databaseDelegate
    private val sharedEvents get() = DependencyValues.current.sharedEvents

    // MARK: - Computed Properties

    /** The messages currently displayed for the current conversation. */
    val displayedMessages: StateFlow<List<Message>> = internalDisplayedMessages.asStateFlow()

    /** The current conversation, or `null` if none is set. */
    val currentConversation: Conversation?
        get() =
            when (val current = reference.wrappedValue) {
                is CurrentConversationReference.Draft -> current.conversation
                is CurrentConversationReference.Stored -> SessionStore.getConversation(current.idKey)
                CurrentConversationReference.None -> null
            }

    // MARK: - Set Current Conversation

    /**
     * Sets the current conversation, or clears it when `null`.
     *
     * Stored conversations are upserted into the session store and
     * observed for real-time updates.
     */
    fun setCurrentConversation(conversation: Conversation?) {
        if (conversation == null) return clearPointer()

        if (conversation.isDraft) {
            reference.wrappedValue = CurrentConversationReference.Draft(conversation)
            persistOpenConversationIDKey(null)
        } else {
            SessionStore.upsertConversation(conversation)
            reference.wrappedValue = CurrentConversationReference.Stored(conversation.id.key)
            // Live-observe the open conversation (iOS starts this on view-appear).
            ConversationObserverService.startObserving(conversation.id.key)
            // Persist the open conversation so process death can restore it (R6.2).
            persistOpenConversationIDKey(conversation.id.key)
        }

        ensureObserving()
        updateDisplayedMessages()
    }

    // MARK: - Message Offset

    /** Increases the number of displayed messages by a fixed increment. */
    fun incrementMessageOffset() {
        if (currentConversation == null) return
        messageOffset.withValue { it.value += MESSAGE_OFFSET_INCREMENT }
        updateDisplayedMessages()
    }

    /**
     * Increases the number of displayed messages until the message with
     * the given identifier is displayed.
     *
     * @param messageID The identifier of the message to reveal.
     */
    fun incrementMessageOffset(messageID: String) {
        val conversation = currentConversation ?: return
        if (messageID !in conversation.messageIDs) return
        if (messageID !in (conversation.messages ?: emptyList()).map { it.id }) return

        val offsetMessages = hydratedMessages.offsetFromCurrentUserAdditionDate(conversation.activities)
        if (messageID !in offsetMessages.map { it.id }) return

        while (messageID !in internalDisplayedMessages.value.map { it.id } &&
            messageOffset.wrappedValue < offsetMessages.size
        ) {
            messageOffset.withValue { it.value += 1 }
            internalDisplayedMessages.value = withMessagesOffset(offsetMessages)
        }
    }

    /** Resets the number of displayed messages to the default. */
    fun resetMessageOffset() {
        messageOffset.wrappedValue = DEFAULT_MESSAGE_OFFSET
    }

    // MARK: - Add Messages

    /**
     * Appends [messages] to [conversation] and commits the result in a
     * single atomic fan-out, upserting the updated conversation and
     * messages into the store.
     *
     * @return The updated conversation.
     *
     * @throws Exception if no messages are provided, the current user
     *   participant cannot be resolved, or the write fails.
     */
    suspend fun addMessages(
        messages: List<Message>,
        conversation: Conversation,
    ): Conversation {
        if (messages.isEmpty()) {
            throw Exception("No messages provided.", metadata = ExceptionMetadata(this))
        }

        val appendedMessages =
            ((conversation.messages ?: emptyList()) + messages)
                .filter { !it.isMockOrOutbox }
                .sortedByAscendingSentDate

        return conversation.update(ConversationUpdatableKey.MESSAGES, to = appendedMessages)
    }

    // MARK: - Deletion

    /**
     * Deletes or hides [conversation].
     *
     * When not [forced] and the other participants have not all deleted
     * the conversation, it is hidden for the current user; otherwise the
     * conversation, its messages, and every participant's reference to it
     * are removed in a single atomic fan-out.
     *
     * @throws Exception if the current user ID is unset or the write fails.
     */
    suspend fun deleteConversation(
        conversation: Conversation,
        forced: Boolean = false,
    ) {
        val currentUserID =
            User.currentUserID
                ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

        if (!forced) {
            val othersAllDeleted =
                conversation.participants
                    .filter { it.userID != currentUserID }
                    .all { it.hasDeletedConversation }
            if (!othersAllDeleted) return hideConversation(conversation, currentUserID)
        }

        val key = conversation.id.key
        val updates = mutableMapOf<String, Any?>()
        for (participant in conversation.participants) {
            updates["$PATH_USERS/${participant.userID}/$KEY_OPEN_CONVERSATIONS/$key"] = null
        }
        for (messageID in conversation.messageIDs) {
            updates["$PATH_MESSAGES/$messageID"] = null
        }
        updates["$PATH_CONVERSATIONS/$key"] = null

        SelfWriteRegistry.record(conversation.id)
        database.commit(updates)
        SessionStore.removeConversation(key)
        if (currentConversation?.id?.key == key) setCurrentConversation(null)
    }

    // MARK: - Update Displayed Messages

    /** Recomputes the displayed messages, including any outbox entries. */
    fun updateDisplayedMessages() {
        val conversation = currentConversation
        val hydrated =
            hydratedMessages
                .offsetFromCurrentUserAdditionDate(conversation?.activities)
                .sortedByAscendingSentDate

        val windowed = withMessagesOffset(hydrated).toMutableList()

        conversation?.id?.key?.let { key ->
            windowed += MessageOutboxService.entries(key).map { it.asDisplayMessage }
        }

        internalDisplayedMessages.value = windowed.uniquedByID
    }

    // MARK: - Auxiliary

    private val hydratedMessages: List<Message>
        get() {
            val conversation = currentConversation ?: return emptyList()
            return (conversation.messages ?: emptyList()).hydrated(conversation.activities)
        }

    private val Conversation.isDraft: Boolean
        get() = id.key == CommonConstants.NEW_CONVERSATION_ID || id.key.isBlank()

    private val Message.isMockOrOutbox: Boolean
        get() = id == CommonConstants.NEW_MESSAGE_ID || id.startsWith("outbox-")

    private suspend fun hideConversation(
        conversation: Conversation,
        userID: String,
    ) {
        val updatedParticipants =
            conversation.participants.map {
                if (it.userID == userID) it.copy(hasDeletedConversation = true) else it
            }
        val updated = conversation.copy(participants = updatedParticipants)
        val newHash = updated.encodedHash
        val key = conversation.id.key

        val updates = mutableMapOf<String, Any?>()
        updates["$PATH_CONVERSATIONS/$key/$KEY_PARTICIPANTS/$userID/$KEY_HAS_DELETED"] = true
        updates["$PATH_CONVERSATIONS/$key/$KEY_HASH"] = newHash
        for (participant in conversation.participants) {
            updates["$PATH_USERS/${participant.userID}/$KEY_OPEN_CONVERSATIONS/$key"] = newHash
        }

        SelfWriteRegistry.record(conversation.id)
        database.commit(updates)
        SessionStore.upsertConversation(updated.copy(id = ConversationID(key = key, hash = newHash)))
        if (currentConversation?.id?.key == key) setCurrentConversation(null)
    }

    private fun clearPointer() {
        ConversationObserverService.stopObserving()
        reference.wrappedValue = CurrentConversationReference.None
        internalDisplayedMessages.value = emptyList()
        persistOpenConversationIDKey(null)
    }

    /** Records (or clears) the open conversation for process-death restoration, avoiding redundant writes. */
    private fun persistOpenConversationIDKey(conversationIDKey: String?) {
        if (Persistent.string(PersistentStorageKey.openConversationIDKey) == conversationIDKey) return
        Persistent.setString(PersistentStorageKey.openConversationIDKey, conversationIDKey)
    }

    private fun ensureObserving() {
        observersStarted.withValue { started ->
            if (started.value) return@withValue
            started.value = true

            observationScope.launch {
                sharedEvents.sessionStoreDidChange.events.collect { handleStoreChange(it) }
            }
            observationScope.launch {
                sharedEvents.messageOutboxDidChange.events.collect { updateDisplayedMessages() }
            }
        }
    }

    private fun handleStoreChange(change: SessionStoreChange) {
        val idKey = (reference.wrappedValue as? CurrentConversationReference.Stored)?.idKey ?: return

        when (change) {
            is SessionStoreChange.Conversations -> {
                if (idKey in change.removedIDKeys) {
                    clearPointer()
                    sharedEvents.currentConversationDidBecomeUnavailable.send()
                    return
                }
                if (idKey in change.upsertedIDKeys) updateDisplayedMessages()
            }

            is SessionStoreChange.Messages -> {
                val affected = change.upsertedIDs + change.removedIDs
                val conversationMessageIDs = currentConversation?.messageIDs?.toSet() ?: return
                if (conversationMessageIDs.intersect(affected).isNotEmpty()) updateDisplayedMessages()
            }

            is SessionStoreChange.Users -> Unit
        }
    }

    private fun withMessagesOffset(messages: List<Message>): List<Message> {
        val unique = messages.uniquedByID
        val amountToGet = messageOffset.wrappedValue
        if (unique.size <= amountToGet) return unique
        return unique.takeLast(amountToGet + 1)
    }

    // MARK: - Companion

    private const val DEFAULT_MESSAGE_OFFSET = 20
    private const val MESSAGE_OFFSET_INCREMENT = 10

    private const val PATH_CONVERSATIONS = "conversations"
    private const val PATH_MESSAGES = "messages"
    private const val PATH_USERS = "users"

    private const val KEY_PARTICIPANTS = "participants"
    private const val KEY_HASH = "hash"
    private const val KEY_HAS_DELETED = "hasDeletedConversation"
    private const val KEY_OPEN_CONVERSATIONS = "openConversations"
}
