//
//  ConversationSessionService.kt
//  Panther Android
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
import us.neotechnica.panther.bundle.conversation
import us.neotechnica.panther.bundle.conversations
import us.neotechnica.panther.bundle.messageOutboxDidChange
import us.neotechnica.panther.bundle.openConversationIDKey
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.bundle.shouldNotifyOfConversationAvailability
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.content.user.extensions.asDisplayMessage
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.hydrated
import us.neotechnica.panther.modules.content.user.extensions.isEmpty
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.isOutboxMessage
import us.neotechnica.panther.modules.content.user.extensions.offsetFromCurrentUserAdditionDate
import us.neotechnica.panther.modules.content.user.extensions.sortedByAscendingSentDate
import us.neotechnica.panther.modules.networking.common.uniquedByID
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.ConversationUpdatableKey
import us.neotechnica.panther.modules.networking.conversation.remotelyupdatable.update
import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.services.MessageService
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.constants.ConversationSessionServiceFloats
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// This service exceeds the file-length and type-body-length limits.

/**
 * The service that manages the current conversation and its displayed
 * messages.
 */
@Suppress("LargeClass")
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

    private val internalDisplayedMessages = MutableStateFlow(emptyList<Message>())

    private val currentConversationReference =
        LockIsolated<CurrentConversationReference>(CurrentConversationReference.None)
    private val messageOffset = LockIsolated(ConversationSessionServiceFloats.DEFAULT_MESSAGE_OFFSET)
    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observersStarted = LockIsolated(false)

    private val database get() = Networking.config.databaseDelegate
    private val sharedEvents get() = DependencyValues.current.sharedEvents

    // MARK: - Computed Properties

    /**
     * The messages currently displayed for the current conversation,
     * including any outbox entries.
     */
    val displayedMessages: StateFlow<List<Message>> = internalDisplayedMessages.asStateFlow()

    /** The current conversation, or `null` if none is set. */
    val currentConversation: Conversation?
        get() =
            when (val reference = currentConversationReference.wrappedValue) {
                is CurrentConversationReference.Draft -> reference.conversation
                is CurrentConversationReference.Stored -> SessionStore.getConversation(reference.idKey)
                CurrentConversationReference.None -> null
            }

    private val hydratedMessages: List<Message>
        get() {
            val currentConversation = currentConversation ?: return emptyList()
            return (currentConversation.messages ?: emptyList()).hydrated(currentConversation.activities)
        }

    // MARK: - Add Messages

    /**
     * Appends the given messages to the given conversation and writes
     * the result.
     *
     * @param messages The messages to append.
     * @param conversation The conversation to append the messages to.
     *
     * @return The updated conversation.
     *
     * @throws Exception if no messages are provided or the write fails.
     */
    suspend fun addMessages(
        messages: List<Message>,
        conversation: Conversation,
    ): Conversation {
        if (messages.isEmpty()) {
            throw Exception(
                "No messages provided.",
                metadata = ExceptionMetadata(this),
            )
        }

        val appendedMessages =
            ((conversation.messages ?: emptyList()) + messages)
                .filter { !it.isMock && !it.isOutboxMessage }
                .sortedByAscendingSentDate

        return conversation.update(ConversationUpdatableKey.MESSAGES, to = appendedMessages)
    }

    // MARK: - Set Current Conversation

    /**
     * Sets the current conversation, or clears it when `null`.
     *
     * Draft conversations, which are empty or mock, are held locally.
     * Stored conversations are upserted into the session store and
     * observed for real-time updates.
     *
     * @param conversation The conversation to set as current, or `null`
     *   to clear it.
     */
    fun setCurrentConversation(conversation: Conversation?) {
        if (conversation == null) return clearPointer()
        val previousReference = currentConversationReference.wrappedValue

        if (conversation.isEmpty || conversation.isMock) {
            currentConversationReference.wrappedValue = CurrentConversationReference.Draft(conversation)
            persistOpenConversationIDKey(null)
        } else {
            // Ensures the store contains the conversation before setting the pointer.
            SessionStore.upsertConversation(conversation)
            currentConversationReference.wrappedValue = CurrentConversationReference.Stored(conversation.id.key)

            // Live-observe the open conversation; the observer coalesces
            // repeated starts for the same key.
            ConversationObserverService.startObserving(conversation.id.key)

            // Persist the open conversation so process death can restore it.
            persistOpenConversationIDKey(conversation.id.key)

            if (previousReference is CurrentConversationReference.Draft) {
                // First send in a new chat: observation begins now that the
                // conversation is stored.
                Logger.log(
                    "Started observing newly stored conversation.",
                    domain = LoggerDomain.conversation,
                )
            }
        }

        ensureObserving()
        updateDisplayedMessages()
    }

    // MARK: - Message Offset

    /** Increases the number of displayed messages by a fixed increment. */
    fun incrementMessageOffset() {
        if (currentConversation == null) return
        messageOffset.withValue { it.value += ConversationSessionServiceFloats.MESSAGE_OFFSET_INCREMENT }
        updateDisplayedMessages()
    }

    /**
     * Increases the number of displayed messages until the message with
     * the given identifier is displayed.
     *
     * @param messageID The identifier of the message to reveal.
     */
    fun incrementMessageOffset(messageID: String) {
        val currentConversation = currentConversation ?: return
        if (messageID !in currentConversation.messageIDs) return
        if (messageID !in (currentConversation.messages ?: emptyList()).map { it.id }) return

        val offsetMessages = hydratedMessages.offsetFromCurrentUserAdditionDate(currentConversation.activities)

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
        messageOffset.wrappedValue = ConversationSessionServiceFloats.DEFAULT_MESSAGE_OFFSET
    }

    // MARK: - Update Displayed Messages

    /**
     * Recomputes the current conversation's displayed messages,
     * including any outbox entries.
     *
     * Store-change events are delivered on a later dispatch; a caller
     * reloading UI in the same job as an upsert can call this first to
     * refresh immediately.
     */
    fun updateDisplayedMessages() {
        val messages =
            withMessagesOffset(
                hydratedMessages
                    .offsetFromCurrentUserAdditionDate(currentConversation?.activities)
                    .sortedByAscendingSentDate,
            ).toMutableList()

        currentConversation?.id?.key?.let { conversationIDKey ->
            messages += MessageOutboxService.entries(conversationIDKey).map { it.asDisplayMessage }
        }

        internalDisplayedMessages.value = messages.uniquedByID
    }

    // MARK: - Deletion

    /**
     * Deletes or hides the given conversation.
     *
     * When the deletion is not forced and the other participants have
     * not all deleted the conversation, it is hidden for the current
     * user instead of deleted for everyone. Otherwise, the
     * conversation, its messages, and its participants' references to
     * it are removed.
     *
     * @param conversation The conversation to delete.
     * @param forced A Boolean value that determines whether to delete
     *   the conversation outright rather than hide it.
     *
     * @throws Exception if the current user identifier has not been set
     *   or the write fails.
     */
    suspend fun deleteConversation(
        conversation: Conversation,
        forced: Boolean = false,
    ) {
        if (!forced) {
            val othersHaveAllDeleted =
                conversation.participants
                    .filter { it.userID != User.currentUserID }
                    .all { it.hasDeletedConversation }

            if (!othersHaveAllDeleted) {
                val currentUserID =
                    User.currentUserID
                        ?: throw Exception(
                            "Current user ID has not been set.",
                            metadata = ExceptionMetadata(this),
                        )

                return hideConversation(
                    conversation,
                    forUser = currentUserID,
                )
            }
        }

        ConversationService.removeConversationFromUsers(
            userIDs = conversation.participants.map { it.userID },
            conversationIDKey = conversation.id.key,
        )

        MessageService.deleteMessages(
            ids = conversation.messageIDs,
            inConversation = conversation,
            updateConversationHash = false,
        )

        database.setValue(
            value = null,
            key = listOf(NetworkPath.conversations.rawValue, conversation.id.key).joinToString("/"),
        )

        if (currentConversation?.id?.key == conversation.id.key) {
            setCurrentConversation(null)
        }
    }

    // MARK: - Auxiliary

    private fun clearPointer() {
        ConversationObserverService.stopObserving()
        currentConversationReference.wrappedValue = CurrentConversationReference.None
        internalDisplayedMessages.value = emptyList()
        persistOpenConversationIDKey(null)
    }

    private fun ensureObserving() {
        observersStarted.withValue { started ->
            if (started.value) return@withValue
            started.value = true

            observationScope.launch {
                sharedEvents.messageOutboxDidChange.events.collect { updateDisplayedMessages() }
            }

            observationScope.launch {
                sharedEvents.sessionStoreDidChange.events.collect { change ->
                    if (change is SessionStoreChange.Users) return@collect
                    handleStoreChange(change)
                }
            }
        }
    }

    private fun handleStoreChange(change: SessionStoreChange) {
        val idKey = (currentConversationReference.wrappedValue as? CurrentConversationReference.Stored)?.idKey ?: return

        when (change) {
            is SessionStoreChange.Conversations -> {
                if (idKey in change.removedIDKeys) {
                    Logger.log(
                        Exception(
                            "Current conversation was removed from the store.",
                            isReportable = false,
                            userInfo = mapOf("ConversationIDKey" to idKey),
                            metadata = ExceptionMetadata(this),
                        ),
                        domain = LoggerDomain.conversation,
                    )

                    // Dismiss the chat page when the current conversation
                    // is removed (e.g., deleted remotely by another
                    // participant).
                    observationScope.launch(Dispatchers.Main) {
                        DependencyValues.current.navigation.navigate(
                            Route.UserContent(UserContentRoute.Stack(emptyList())),
                        )

                        if (RuntimeStorage.shouldNotifyOfConversationAvailability) {
                            Toast.show(
                                Toast(
                                    Toast.ToastType.Banner(ToastStyle.INFO),
                                    message = "This conversation is no longer available.",
                                ),
                                translating = listOf(Toast.TranslationOptionKey.Message, Toast.TranslationOptionKey.Title),
                            )
                        } else {
                            RuntimeStorage.remove(StoredItemKey.shouldNotifyOfConversationAvailability)
                        }
                    }

                    return clearPointer()
                }

                if (idKey !in change.upsertedIDKeys) return
                updateDisplayedMessages()
            }

            is SessionStoreChange.Messages -> {
                val affectedIDs = change.upsertedIDs + change.removedIDs
                val conversation = currentConversation ?: return
                val messageIDs = conversation.messageIDs.toSet()
                if (messageIDs.intersect(affectedIDs).isEmpty()) return
                updateDisplayedMessages()
            }

            is SessionStoreChange.Users -> Unit
        }
    }

    private suspend fun hideConversation(
        conversation: Conversation,
        forUser: String,
    ) {
        if (conversation.participants.none { it.userID == forUser }) {
            throw Exception(
                "This conversation does not contain the specified participant.",
                userInfo = mapOf("UserID" to forUser),
                metadata = ExceptionMetadata(this),
            )
        }

        // Single-field fan-out write instead of replacing
        // the entire participants array.
        val conversationPath = listOf(NetworkPath.conversations.rawValue, conversation.id.key).joinToString("/")

        val participantPath =
            listOf(
                conversationPath,
                Conversation.SerializableKey.PARTICIPANTS.rawValue,
                forUser,
                Participant.Keys.HAS_DELETED_CONVERSATION.rawValue,
            ).joinToString("/")

        // Compute updated hash with the deletion applied.
        val updatedConversation =
            conversation.copy(
                participants =
                    conversation.participants.map { participant ->
                        if (participant.userID != forUser) return@map participant
                        Participant(
                            userID = participant.userID,
                            hasDeletedConversation = true,
                            isTyping = participant.isTyping,
                        )
                    },
            )

        val newHash = updatedConversation.encodedHash
        val updates = mutableMapOf<String, Any?>(participantPath to true)
        updates["$conversationPath/${Conversation.SerializableKey.ENCODED_HASH.rawValue}"] = newHash

        for (participant in conversation.participants) {
            val tokenPath =
                listOf(
                    NetworkPath.users.rawValue,
                    participant.userID,
                    User.SerializableKey.CONVERSATION_IDS.rawValue,
                    conversation.id.key,
                ).joinToString("/")

            updates[tokenPath] = newHash
        }

        database.commit(updates)

        // Upsert the updated conversation to the session store.
        SessionStore.upsertConversation(
            updatedConversation.copy(
                id =
                    ConversationID(
                        key = conversation.id.key,
                        hash = newHash,
                    ),
            ),
        )

        if (currentConversation?.id?.key == conversation.id.key) {
            setCurrentConversation(null)
        }
    }

    /** Records (or clears) the open conversation for process-death restoration, avoiding redundant writes. */
    private fun persistOpenConversationIDKey(conversationIDKey: String?) {
        if (Persistent.string(PersistentStorageKey.openConversationIDKey) == conversationIDKey) return
        Persistent.setString(PersistentStorageKey.openConversationIDKey, conversationIDKey)
    }

    private fun withMessagesOffset(messages: List<Message>): List<Message> {
        val amountToGet = messageOffset.wrappedValue
        val unique = messages.distinct()
        if (unique.size <= amountToGet) return messages
        return unique.takeLast(amountToGet + 1)
    }
}
