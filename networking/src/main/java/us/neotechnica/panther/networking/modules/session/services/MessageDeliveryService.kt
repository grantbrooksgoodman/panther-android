//
//  MessageDeliveryService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import us.neotechnica.panther.networking.modules.common.services.AnalyticsService
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.networking.modules.session.extensions.users
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
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
    // MARK: - Properties

    private val internalIsSendingMessage = MutableStateFlow(false)

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether a message send is in
     * flight.
     */
    val isSendingMessage: StateFlow<Boolean> = internalIsSendingMessage.asStateFlow()

    // MARK: - Methods

    /**
     * Sends [text] to the current conversation's participants.
     *
     * The message is staged in the outbox, then delivered; on
     * success the staged entry is removed, and on failure it is
     * marked failed so it can be retried. Does nothing when the
     * text is blank or no recipients are resolved.
     */
    suspend fun sendTextMessage(text: String) {
        val conversation = ConversationSessionService.currentConversation ?: return
        val currentUser = UserSessionService.currentUser ?: return
        val users = conversation.users.orEmpty()
        if (users.isEmpty() || text.isBlank()) return

        val entry =
            OutboxEntry(
                id = "${OutboxEntry.ID_PREFIX}${UUID.randomUUID()}",
                conversationIDKey = conversation.id.key,
                fromAccountID = currentUser.id,
                recipientUserIDs = users.map { it.id },
                text = text.trimEnd(),
                isPenPalsConversation = conversation.metadata.isPenPalsConversation,
                createdDate = Date(),
                attemptCount = 1,
                lastAttemptDate = Date(),
                reservedRemoteID = null,
                state = OutboxEntry.State.SENDING,
            )
        MessageOutboxService.enqueue(entry)
        internalIsSendingMessage.value = true
        withContext(Dispatchers.Main) {
            MessageSessionService.deliveryProgressIndicator?.startAnimatingDeliveryProgress()
        }

        try {
            val updated =
                MessageSessionService.sendTextMessage(
                    text = entry.text,
                    presetID = null,
                    users = users,
                    conversation = conversation,
                )
            MessageOutboxService.remove(entry.id)
            ConversationSessionService.setCurrentConversation(updated)
            AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.SEND_TEXT_MESSAGE)
        } catch (exception: Exception) {
            MessageOutboxService.markFailed(entry.id)
            Logger.log(exception)
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
        val conversation = ConversationSessionService.currentConversation ?: return
        val currentUser = UserSessionService.currentUser ?: return
        val users = conversation.users.orEmpty()
        if (users.isEmpty()) return

        val entry =
            OutboxEntry(
                id = "${OutboxEntry.ID_PREFIX}${UUID.randomUUID()}",
                conversationIDKey = conversation.id.key,
                fromAccountID = currentUser.id,
                recipientUserIDs = users.map { it.id },
                text = "",
                mediaRelativePath = mediaFile.relativePath,
                isPenPalsConversation = conversation.metadata.isPenPalsConversation,
                createdDate = Date(),
                attemptCount = 1,
                lastAttemptDate = Date(),
                reservedRemoteID = null,
                state = OutboxEntry.State.SENDING,
            )
        MessageOutboxService.enqueue(entry)
        internalIsSendingMessage.value = true
        withContext(Dispatchers.Main) {
            MessageSessionService.deliveryProgressIndicator?.startAnimatingDeliveryProgress()
        }

        try {
            val updated =
                MessageSessionService.sendMediaMessage(
                    mediaFile = mediaFile,
                    users = users,
                    conversation = conversation,
                    isPenPalsConversation = conversation.metadata.isPenPalsConversation,
                )
            MessageOutboxService.remove(entry.id)
            ConversationSessionService.setCurrentConversation(updated)
            AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.SEND_MEDIA_MESSAGE)
        } catch (exception: Exception) {
            MessageOutboxService.markFailed(entry.id)
            Logger.log(exception)
        } finally {
            cleanUpAfterSend()
        }
    }

    // MARK: - Auxiliary

    private suspend fun cleanUpAfterSend() {
        internalIsSendingMessage.value = false
        withContext(Dispatchers.Main) {
            MessageSessionService.deliveryProgressIndicator?.stopAnimatingDeliveryProgress()
        }
    }
}
