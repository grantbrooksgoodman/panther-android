//
//  ConversationSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CommonConstants
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import us.neotechnica.panther.networking.modules.schema.conversation.models.Participant
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.asDisplayMessage
import us.neotechnica.panther.networking.modules.session.extensions.currentConversationDidBecomeUnavailable
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.filteringSystemMessages
import us.neotechnica.panther.networking.modules.session.extensions.hydrated
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.messageOutboxDidChange
import us.neotechnica.panther.networking.modules.session.extensions.messages
import us.neotechnica.panther.networking.modules.session.extensions.offsetFromCurrentUserAdditionDate
import us.neotechnica.panther.networking.modules.session.extensions.sessionStoreDidChange
import us.neotechnica.panther.networking.modules.session.extensions.sortedByAscendingSentDate
import us.neotechnica.panther.networking.modules.session.extensions.uniquedByID
import us.neotechnica.panther.networking.modules.session.extensions.updateReadDate
import us.neotechnica.panther.networking.modules.session.models.SessionStoreChange
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.models.send

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

        val previous = reference.wrappedValue
        if (conversation.isDraft) {
            reference.wrappedValue = CurrentConversationReference.Draft(conversation)
        } else {
            SessionStore.upsertConversation(conversation)
            reference.wrappedValue = CurrentConversationReference.Stored(conversation.id.key)
            if (previous is CurrentConversationReference.Draft) {
                ConversationObserverService.startObserving(conversation.id.key)
            }
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

        val existingIDs = conversation.messageIDs.toSet()
        val newMessages =
            messages
                .filteringSystemMessages
                .filter { !it.isMockOrOutbox && it.id !in existingIDs }
        if (newMessages.isEmpty()) return conversation

        val currentUserParticipant =
            conversation.participants.firstOrNull { it.userID == User.currentUserID }
                ?: throw Exception(
                    "Failed to resolve current user participant.",
                    metadata = ExceptionMetadata(this),
                )

        // Reset typing for current user + un-delete all participants
        // (sending revives the conversation).
        val revivedParticipants =
            conversation.participants.map { participant ->
                participant.copy(
                    hasDeletedConversation = false,
                    isTyping = if (participant.userID == currentUserParticipant.userID) false else participant.isTyping,
                )
            }

        val updatedContent =
            conversation.copy(
                messageIDs = (conversation.messageIDs + newMessages.map { it.id }).distinct(),
                participants = revivedParticipants,
            )
        val newHash = updatedContent.encodedHash
        val updated = updatedContent.copy(id = ConversationID(key = conversation.id.key, hash = newHash))

        val updates = buildMessageFanOut(updated, newMessages, currentUserParticipant, newHash)

        SelfWriteRegistry.record(updated.id)
        database.commit(updates)

        SessionStore.upsertMessages(newMessages.toSet())
        SessionStore.upsertConversation(updated)
        return updated
    }

    // MARK: - Read Receipts

    /**
     * Marks the current conversation's unread incoming messages as read.
     *
     * Has no effect when the most recent incoming message has already
     * been read, or when there are no unread incoming messages.
     */
    suspend fun markCurrentConversationAsRead() {
        val conversation = currentConversation ?: return
        val currentUserID = User.currentUserID ?: return
        val incoming = conversation.messages?.filter { !it.isFromCurrentUser } ?: return

        val last = incoming.lastOrNull() ?: return
        if (last.readReceipts?.any { it.userID == currentUserID } == true) return

        val unread = incoming.filter { message -> message.readReceipts?.any { it.userID == currentUserID } != true }
        if (unread.isEmpty()) return

        conversation.updateReadDate(unread)
    }

    // MARK: - Update Displayed Messages

    /** Recomputes the displayed messages, including any outbox entries. */
    fun updateDisplayedMessages() {
        val conversation = currentConversation
        val hydrated =
            (conversation?.messages ?: emptyList())
                .hydrated(conversation?.activities)
                .offsetFromCurrentUserAdditionDate(conversation?.activities)
                .sortedByAscendingSentDate

        val windowed = withMessagesOffset(hydrated).toMutableList()

        conversation?.id?.key?.let { key ->
            windowed += MessageOutboxService.entries(key).map { it.asDisplayMessage }
        }

        internalDisplayedMessages.value = windowed.uniquedByID
    }

    // MARK: - Auxiliary

    private val Conversation.isDraft: Boolean
        get() = id.key == CommonConstants.NEW_CONVERSATION_ID || id.key.isBlank()

    private val Message.isMockOrOutbox: Boolean
        get() = id == CommonConstants.NEW_MESSAGE_ID || id.startsWith("outbox-")

    private fun buildMessageFanOut(
        conversation: Conversation,
        newMessages: List<Message>,
        currentUserParticipant: Participant,
        newHash: String,
    ): Map<String, Any?> {
        val conversationPath = "${PATH_CONVERSATIONS}/${conversation.id.key}"
        val updates = mutableMapOf<String, Any?>()

        // Message node data + conversation index entries.
        for (message in newMessages) {
            updates["${PATH_MESSAGES}/${message.id}"] = message.encoded.filterKeys { it != KEY_ID }
            updates["$conversationPath/$KEY_MESSAGES/${message.id}"] = true
        }

        // Un-delete participants who had deleted the conversation.
        for (participant in conversation.participants.filter { it.hasDeletedConversation }) {
            updates["$conversationPath/$KEY_PARTICIPANTS/${participant.userID}/$KEY_HAS_DELETED"] = false
        }

        // Reset typing status for the current user.
        updates["$conversationPath/$KEY_PARTICIPANTS/${currentUserParticipant.userID}/$KEY_IS_TYPING"] = false

        // Conversation hash + last-modified date.
        updates["$conversationPath/$KEY_HASH"] = newHash
        updates["$conversationPath/$KEY_METADATA/$KEY_LAST_MODIFIED"] =
            DependencyValues.current.timestampDateFormatter.format(java.util.Date())

        // Participant hash tokens.
        for (participant in conversation.participants) {
            updates["${PATH_USERS}/${participant.userID}/$KEY_OPEN_CONVERSATIONS/${conversation.id.key}"] = newHash
        }

        // Drain pending hosted-archive entries so a message node never
        // commits without its translations being resolvable.
        for (message in newMessages) {
            for (translationReference in message.translationReferences ?: emptyList()) {
                val entry = PendingTranslationArchive.drain(translationReference.hostingKey) ?: continue
                updates[entry.first] = entry.second
            }
        }

        return updates
    }

    private fun clearPointer() {
        ConversationObserverService.stopObserving()
        reference.wrappedValue = CurrentConversationReference.None
        internalDisplayedMessages.value = emptyList()
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

    private const val KEY_ID = "id"
    private const val KEY_MESSAGES = "messages"
    private const val KEY_METADATA = "metadata"
    private const val KEY_PARTICIPANTS = "participants"
    private const val KEY_HASH = "hash"
    private const val KEY_LAST_MODIFIED = "lastModified"
    private const val KEY_HAS_DELETED = "hasDeletedConversation"
    private const val KEY_IS_TYPING = "isTyping"
    private const val KEY_OPEN_CONVERSATIONS = "openConversations"
}
