//
//  MessageDeliveryService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.modules.common.services.AnalyticsService
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.networking.modules.session.extensions.users
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.util.Date
import java.util.UUID

/**
 * Sends messages from the chat page, staging each in the outbox so a
 * failed send can be retried.
 *
 * **Note:** this Phase 7 port sends text into existing conversations
 * only; new-conversation composition, audio, and media arrive with later
 * phases.
 */
object MessageDeliveryService {
    /**
     * Sends [text] to the current conversation's participants.
     *
     * The message is staged in the outbox, then delivered; on success
     * the staged entry is removed, and on failure it is marked failed so
     * it can be retried. Does nothing when the text is blank or no
     * recipients are resolved.
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
        }
    }

    /**
     * Sends [mediaFile] to the current conversation's participants.
     *
     * **Note:** this Phase R3.3 port sends directly; outbox staging,
     * delivery-progress states, and retry semantics arrive with the send
     * pipeline (Phase R3.4). Failures surface as a toast.
     */
    suspend fun sendMediaMessage(mediaFile: MediaFile) {
        val conversation = ConversationSessionService.currentConversation ?: return
        val users = conversation.users.orEmpty()
        if (users.isEmpty()) return

        try {
            val updated =
                MessageSessionService.sendMediaMessage(
                    mediaFile = mediaFile,
                    users = users,
                    conversation = conversation,
                    isPenPalsConversation = conversation.metadata.isPenPalsConversation,
                )
            ConversationSessionService.setCurrentConversation(updated)
        } catch (exception: Exception) {
            Logger.log(exception, with = AlertType.toast)
        }
    }
}
