//
//  MessageDeliveryService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction
import us.neotechnica.panther.modules.content.user.models.MessageDeliveryServiceEffectID
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.isMock
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.util.Date
import java.util.UUID
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.MessageSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.modules.session.clientSession

/**
 * Sends messages from the chat page, staging each in the outbox so
 * a failed send can be retried.
 *
 * Use [MessageDeliveryService] to send text and media messages to
 * the current conversation's participants. While a send is in
 * flight, [isSendingMessage] is `true`. Only this service updates
 * that value.
 */
object MessageDeliveryService {
    // MARK: - Properties

    private val internalIsSendingMessage = MutableStateFlow(false)

    private val uponIsSendingMessageChangedToFalse =
        LockIsolated(mapOf<MessageDeliveryServiceEffectID, () -> Unit>())
    private val uponIsSendingMessageChangedToTrue =
        LockIsolated(mapOf<MessageDeliveryServiceEffectID, () -> Unit>())

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether a message send is in
     * flight.
     */
    val isSendingMessage: StateFlow<Boolean> = internalIsSendingMessage.asStateFlow()

    private val conversation: Conversation?
        get() = ConversationSessionService.currentConversation

    private val isExistingConversation: Boolean
        get() = conversation != null && conversation?.isMock != true

    private val selectedContactPairUsers: List<User>
        get() =
            RecipientBarContactSelectionUIService
                .selectedContactPairs
                .value
                .flatMap { contactPair -> contactPair.numberPairs.flatMap { it.users } }

    private val users: List<User>
        get() = (conversation?.users?.takeIf { it.isNotEmpty() } ?: selectedContactPairUsers).distinctBy { it.id }

    // MARK: - Add Effect

    /**
     * Registers an effect to run once, the next time
     * [isSendingMessage] is set to the given value.
     *
     * The effect is cleared after it runs. Registering a new
     * effect with the same identifier and target value replaces
     * the existing one.
     *
     * @param state The value of [isSendingMessage] that triggers
     *   the effect.
     * @param id The identifier under which to register the effect.
     * @param effect The effect to run.
     */
    fun addEffectUponIsSendingMessage(
        state: Boolean,
        id: MessageDeliveryServiceEffectID,
        effect: () -> Unit,
    ) {
        val registry = if (state) uponIsSendingMessageChangedToTrue else uponIsSendingMessageChangedToFalse
        registry.withValue { it.value = it.value + (id to effect) }
    }

    // MARK: - Methods

    /**
     * Sends [text] to the current conversation's participants.
     *
     * When the current conversation already exists, the message is
     * staged in the outbox, then delivered; on success the staged
     * entry is removed, and on failure it is marked failed so it can
     * be retried. When the current conversation is a mock, the
     * message creates a new conversation, and a delivery failure is
     * thrown. Does nothing when the text is blank or no recipients
     * are resolved.
     *
     * @throws Exception if delivery fails for a message that was not
     *   staged in the outbox.
     */
    suspend fun sendTextMessage(text: String) {
        val recipients = users
        if (recipients.isEmpty() || text.isBlank()) return

        HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)

        val currentConversation = conversation
        val currentUser = UserSessionService.currentUser
        var outboxEntryID: String? = null
        if (isExistingConversation && currentConversation != null && currentUser != null) {
            val entry =
                OutboxEntry(
                    id = "${OutboxEntry.ID_PREFIX}${UUID.randomUUID()}",
                    conversationIDKey = currentConversation.id.key,
                    fromAccountID = currentUser.id,
                    recipientUserIDs = recipients.map { it.id },
                    payload = OutboxEntry.Payload.Text(text.trimEnd()),
                    isPenPalsConversation = currentConversation.metadata.isPenPalsConversation,
                    createdDate = Date(),
                    attemptCount = 1,
                    lastAttemptDate = Date(),
                    reservedRemoteID = null,
                    state = OutboxEntry.State.SENDING,
                    transcription = null,
                )
            outboxEntryID = entry.id
            MessageOutboxService.enqueue(entry)
        }

        beginSend()

        try {
            val updated =
                MessageSessionService.sendTextMessage(
                    text = text.trimEnd(),
                    presetID = null,
                    users = recipients,
                    conversation = currentConversation?.takeUnless { it.isMock },
                )
            outboxEntryID?.let { MessageOutboxService.remove(it) }
            AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.SEND_TEXT_MESSAGE)
            setCurrentConversationIfApplicable(updated)
        } catch (exception: Exception) {
            val id = outboxEntryID
            if (id != null) {
                MessageOutboxService.markFailed(id)
                Logger.log(exception)
            } else {
                throw exception
            }
        } finally {
            cleanUpAfterSend()
        }
    }

    /**
     * Sends [mediaFile] to the current conversation's participants.
     *
     * The media is staged in the outbox, then delivered; on success
     * the staged entry is removed, and on failure it is marked
     * failed so it can be retried. Does nothing when no recipients
     * are resolved.
     */
    suspend fun sendMediaMessage(mediaFile: MediaFile) {
        val recipients = users
        if (recipients.isEmpty()) return

        HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)

        val currentConversation = conversation
        val currentUser = UserSessionService.currentUser
        var outboxEntryID: String? = null
        if (isExistingConversation && currentConversation != null && currentUser != null) {
            val stagedFileName =
                mediaFile.localPathFile?.let { MessageOutboxService.storePayloadFile(from = it) } ?: return
            val entry =
                OutboxEntry(
                    id = "${OutboxEntry.ID_PREFIX}${UUID.randomUUID()}",
                    conversationIDKey = currentConversation.id.key,
                    fromAccountID = currentUser.id,
                    recipientUserIDs = recipients.map { it.id },
                    payload = OutboxEntry.Payload.Media(stagedFileName, mediaFile.fileExtension),
                    isPenPalsConversation = currentConversation.metadata.isPenPalsConversation,
                    createdDate = Date(),
                    attemptCount = 1,
                    lastAttemptDate = Date(),
                    reservedRemoteID = null,
                    state = OutboxEntry.State.SENDING,
                    transcription = null,
                )
            outboxEntryID = entry.id
            MessageOutboxService.enqueue(entry)
        }

        beginSend()

        val targetConversation = currentConversation?.takeUnless { it.isMock }
        val isPenPalsConversation = currentConversation?.metadata?.isPenPalsConversation ?: false
        try {
            val updated =
                MessageSessionService.sendMediaMessage(
                    mediaFile = mediaFile,
                    users = recipients,
                    conversation = targetConversation,
                    isPenPalsConversation = isPenPalsConversation,
                )
            outboxEntryID?.let { MessageOutboxService.remove(it) }
            AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.SEND_MEDIA_MESSAGE)
            setCurrentConversationIfApplicable(updated)
        } catch (exception: Exception) {
            val id = outboxEntryID
            if (id != null) {
                MessageOutboxService.markFailed(id)
                Logger.log(exception)
            } else {
                throw exception
            }
        } finally {
            cleanUpAfterSend()
        }
    }

    // MARK: - Auxiliary

    private fun setCurrentConversationIfApplicable(conversation: Conversation) {
        val current = ConversationSessionService.currentConversation
        if (current != null && !current.isMock && current.id.key != conversation.id.key) return
        ConversationSessionService.setCurrentConversation(conversation)
    }

    private suspend fun beginSend() {
        setIsSendingMessage(true)
        withContext(Dispatchers.Main) {
            DependencyValues.current.clientSession
                .deliveryProgressIndicator
                ?.startAnimatingDeliveryProgress()
        }
    }

    private suspend fun cleanUpAfterSend() {
        setIsSendingMessage(false)
        withContext(Dispatchers.Main) {
            DependencyValues.current.clientSession
                .deliveryProgressIndicator
                ?.stopAnimatingDeliveryProgress()
        }
    }

    private suspend fun setIsSendingMessage(isSendingMessage: Boolean) {
        internalIsSendingMessage.value = isSendingMessage
        didSetIsSendingMessage(isSendingMessage)
    }

    private suspend fun didSetIsSendingMessage(isSendingMessage: Boolean) =
        withContext(Dispatchers.Main) {
            if (isSendingMessage) {
                ContextMenuInteraction.setCanBegin(false)
                val effects = drainEffects(uponIsSendingMessageChangedToTrue)
                if (effects.isEmpty()) return@withContext
                Logger.log(
                    Exception(
                        "Running effects for change of \"isSendingMessage\" to TRUE. " +
                            "[EnqueuedEffectIDs: ${effects.keys.map { it.rawValue }}]",
                        isReportable = false,
                        metadata = ExceptionMetadata(this@MessageDeliveryService),
                    ),
                )
                effects.values.forEach { it() }
            } else {
                ContextMenuInteraction.setCanBegin(true)
                val effects = drainEffects(uponIsSendingMessageChangedToFalse)
                if (effects.isEmpty()) return@withContext
                Logger.log(
                    Exception(
                        "Running effects for change of \"isSendingMessage\" to FALSE. " +
                            "[EnqueuedEffectIDs: ${effects.keys.map { it.rawValue }}]",
                        isReportable = false,
                        metadata = ExceptionMetadata(this@MessageDeliveryService),
                    ),
                )
                effects.values.forEach { it() }
            }
        }

    private fun drainEffects(
        effects: LockIsolated<Map<MessageDeliveryServiceEffectID, () -> Unit>>,
    ): Map<MessageDeliveryServiceEffectID, () -> Unit> =
        effects.withValue { ref ->
            val drained = ref.value
            if (drained.isNotEmpty()) ref.value = emptyMap()
            drained
        }
}
