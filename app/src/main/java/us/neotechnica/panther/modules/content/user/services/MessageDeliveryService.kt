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
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.AnalyticsService
import us.neotechnica.panther.modules.common.services.sources.HapticsService
import us.neotechnica.panther.modules.content.user.extensions.drainEffects
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.uniquedByID
import us.neotechnica.panther.modules.content.user.extensions.users
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction
import us.neotechnica.panther.modules.content.user.models.MessageDeliveryServiceEffectID
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources.RecipientBarLayoutService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.MessageSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.util.Date
import java.util.UUID

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
    // MARK: - Dependencies

    private val analyticsService: AnalyticsService by Dependency { it.commonServices.analytics }
    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val hapticsService: HapticsService by Dependency { it.commonServices.haptics }
    private val messageSession: MessageSessionService by Dependency { it.clientSession.entity.message }
    private val outbox: MessageOutboxService by Dependency { it.clientSession.outbox }
    private val userSession: UserSessionService by Dependency { it.clientSession.entity.user }

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
        get() = conversationSession.currentConversation

    private val conversationContext: Pair<Conversation?, Boolean>
        get() = (if (conversation?.isMock ?: true) null else conversation) to isPenPalsConversation

    private val isExistingConversation: Boolean
        get() = conversation != null && conversation?.isMock != true

    private val isPenPalsConversation: Boolean
        get() = conversation?.metadata?.isPenPalsConversation == true

    // The recipient bar's layout service while the new chat page is
    // presented, or `null` otherwise.
    private val recipientBarLayoutService: RecipientBarLayoutService?
        get() {
            val isNewChatPagePresented =
                DependencyValues.current.navigation.state.value.userContent.stack
                    .lastOrNull() == UserContentNavigatorState.SeguePaths.NewChat
            return if (isNewChatPagePresented) RecipientBarService.layout else null
        }

    private val selectedContactPairs: List<ContactPair>
        get() = RecipientBarService.contactSelectionUI.selectedContactPairs.value

    private val users: List<User>
        get() = (conversation?.users ?: selectedContactPairs.users).uniquedByID

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

        hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)

        // On the new chat page, the recipient bar and the done button stay
        // disabled until the page receives the sent message or the send
        // throws.
        recipientBarLayoutService?.setIsUserInteractionEnabled(false)

        val currentConversation = conversation
        val currentUser = userSession.currentUser
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
            outbox.enqueue(entry)
        }

        val startedConversationIDKey = beginSend()

        try {
            val (contextConversation, contextIsPenPalsConversation) = conversationContext
            val updated =
                messageSession.sendTextMessage(
                    text = text.trimEnd(),
                    toUsers = recipients,
                    inConversation = contextConversation,
                    isPenPalsConversation = contextIsPenPalsConversation,
                )
            outboxEntryID?.let { outbox.remove(it) }
            analyticsService.logEvent(AnalyticsService.AnalyticsEvent.SEND_TEXT_MESSAGE)
            setCurrentConversationIfApplicable(updated)
        } catch (exception: Exception) {
            val id = outboxEntryID
            if (id != null) {
                outbox.markFailed(id)
                Logger.log(exception)
            } else {
                recipientBarLayoutService?.setIsUserInteractionEnabled(true)
                throw exception
            }
        } finally {
            cleanUpAfterSend(startedConversationIDKey)
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

        hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)

        // On the new chat page, the recipient bar and the done button stay
        // disabled until the page receives the sent message or the send
        // throws.
        recipientBarLayoutService?.setIsUserInteractionEnabled(false)

        val currentConversation = conversation
        val currentUser = userSession.currentUser
        var outboxEntryID: String? = null
        if (isExistingConversation && currentConversation != null && currentUser != null) {
            val stagedFileName = mediaFile.localPathFile?.let { outbox.storePayloadFile(from = it) }
            if (stagedFileName == null) {
                Logger.log(Exception("Failed to stage media payload.", metadata = ExceptionMetadata(this)))
            } else {
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
                outbox.enqueue(entry)
            }
        }

        val startedConversationIDKey = beginSend()

        val (contextConversation, contextIsPenPalsConversation) = conversationContext
        try {
            val updated =
                messageSession.sendMediaMessage(
                    mediaFile = mediaFile,
                    toUsers = recipients,
                    inConversation = contextConversation,
                    isPenPalsConversation = contextIsPenPalsConversation,
                )
            outboxEntryID?.let { outbox.remove(it) }
            analyticsService.logEvent(AnalyticsService.AnalyticsEvent.SEND_MEDIA_MESSAGE)
            setCurrentConversationIfApplicable(updated)
        } catch (exception: Exception) {
            val id = outboxEntryID
            if (id != null) {
                outbox.markFailed(id)
                Logger.log(exception)
            } else {
                recipientBarLayoutService?.setIsUserInteractionEnabled(true)
                throw exception
            }
        } finally {
            cleanUpAfterSend(startedConversationIDKey)
        }
    }

    // MARK: - Auxiliary

    private fun setCurrentConversationIfApplicable(conversation: Conversation) {
        val current = conversationSession.currentConversation
        if (current != null && !current.isMock && current.id.key != conversation.id.key) return
        conversationSession.setCurrentConversation(conversation)
    }

    private suspend fun beginSend(): String? {
        setIsSendingMessage(true)
        withContext(Dispatchers.Main) {
            DependencyValues.current.clientSession
                .deliveryProgressIndicator
                ?.startAnimatingDeliveryProgress()
        }
        return conversation?.id?.key
    }

    // The progress indicator stops only while the conversation the send
    // started in is still current, so a send that completes after the
    // user moved on never interrupts another conversation's indicator.
    private suspend fun cleanUpAfterSend(startedConversationIDKey: String?) {
        setIsSendingMessage(false)
        val currentConversationIDKey = conversation?.id?.key
        if (startedConversationIDKey != null && currentConversationIDKey != null && startedConversationIDKey != currentConversationIDKey) {
            return
        }

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
                val effects = uponIsSendingMessageChangedToTrue.drainEffects()
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
                val effects = uponIsSendingMessageChangedToFalse.drainEffects()
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
}
