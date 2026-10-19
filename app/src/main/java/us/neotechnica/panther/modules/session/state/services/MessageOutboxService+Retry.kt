//
//  MessageOutboxService+Retry.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.neotechnica.panther.bundle.messages
import us.neotechnica.panther.bundle.outbox
import us.neotechnica.panther.modules.common.extensions.toException
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.networking.sources.user.services.UserService
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Retries delivery of the outbox entry with the given identifier.
 *
 * Claims the entry, resolves its conversation and recipients, and
 * attempts to send its payload. The entry is removed on success or
 * when its conversation no longer exists, and marked failed if
 * delivery fails.
 *
 * @param entryID The identifier of the outbox entry to retry.
 */
suspend fun MessageOutboxService.retry(entryID: String) {
    val candidateRemoteID =
        DependencyValues.current.networking.database
            .generateKey(NetworkPath.messages.rawValue) ?: return
    val entry = claimForRetry(entryID, candidateRemoteID) ?: return

    val conversation =
        DependencyValues.current.clientSession.store
            .getConversation(entry.conversationIDKey)
    if (conversation == null) {
        remove(entryID)
        Logger.log("Removed outbox entry $entryID: conversation no longer exists.", domain = LoggerDomain.outbox)
        return
    }

    // Resolve recipient users from the store, fetching any missing ones.
    val recipients = mutableListOf<User>()
    for (userID in entry.recipientUserIDs) {
        val storedUser = DependencyValues.current.clientSession.store.users[userID]
        if (storedUser != null) {
            recipients.add(storedUser)
            continue
        }

        try {
            recipients.add(UserService.getUser(userID))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Logger.log(throwable.toException())
        }
    }

    if (recipients.isEmpty()) {
        markFailed(entryID)
        Logger.log("Failed to resolve any recipient users for outbox entry $entryID.", domain = LoggerDomain.outbox)
        return
    }

    startDeliveryProgressIfCurrent(entry)
    try {
        sendPayload(entry, conversation, recipients)
        remove(entryID)
        Logger.log("Retry succeeded for outbox entry $entryID.", domain = LoggerDomain.outbox)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        // Every failure marks the entry failed; the entry is retried later.
        markFailed(entryID)
        Logger.log("Retry failed for outbox entry $entryID.", domain = LoggerDomain.outbox)
    } finally {
        stopDeliveryProgressIfCurrent(entry)
    }
}

private suspend fun sendPayload(
    entry: OutboxEntry,
    conversation: Conversation,
    recipients: List<User>,
) {
    when (val payload = entry.payload) {
        is OutboxEntry.Payload.Audio ->
            throw Exception(
                "Failed to reconstruct AudioFile from payload.",
                metadata = ExceptionMetadata(MessageOutboxService),
            )

        is OutboxEntry.Payload.Media -> {
            val mediaFile =
                MediaFile(
                    relativePath = "outbox/${payload.fileName}",
                    name = payload.fileName.substringBeforeLast("."),
                    fileExtension = payload.fileExtension,
                )
            DependencyValues.current.clientSession.entity.message.sendMediaMessage(
                mediaFile = mediaFile,
                presetID = entry.reservedRemoteID,
                toUsers = recipients,
                inConversation = conversation,
                isPenPalsConversation = entry.isPenPalsConversation,
            )
        }

        is OutboxEntry.Payload.Text ->
            DependencyValues.current.clientSession.entity.message.sendTextMessage(
                text = payload.value,
                presetID = entry.reservedRemoteID,
                toUsers = recipients,
                inConversation = conversation,
                isPenPalsConversation = entry.isPenPalsConversation,
            )
    }
}

private suspend fun startDeliveryProgressIfCurrent(entry: OutboxEntry) {
    if (DependencyValues.current.clientSession.entity.conversation.currentConversation
            ?.id
            ?.key != entry.conversationIDKey
    ) {
        return
    }
    withContext(Dispatchers.Main) {
        DependencyValues
            .current
            .clientSession
            .deliveryProgressIndicator
            ?.startAnimatingDeliveryProgress()
    }
}

private suspend fun stopDeliveryProgressIfCurrent(entry: OutboxEntry) {
    if (DependencyValues.current.clientSession.entity.conversation.currentConversation
            ?.id
            ?.key != entry.conversationIDKey
    ) {
        return
    }
    withContext(Dispatchers.Main) {
        DependencyValues
            .current
            .clientSession
            .deliveryProgressIndicator
            ?.stopAnimatingDeliveryProgress()
    }
}

/**
 * Retries every failed outbox entry that has not exceeded the
 * auto-retry cap, serialized per conversation in creation order.
 */
suspend fun MessageOutboxService.retryAllEligible() {
    val failedEntries = allEntries.filter { it.state == OutboxEntry.State.FAILED }
    if (failedEntries.isEmpty()) return

    val grouped = failedEntries.groupBy { it.conversationIDKey }
    Logger.log(
        "Auto-retrying ${failedEntries.size} eligible entries across ${grouped.size} conversations.",
        domain = LoggerDomain.outbox,
    )

    for ((_, conversationEntries) in grouped) {
        for (entry in conversationEntries) {
            if (entry.attemptCount >= OutboxEntry.AUTO_RETRY_CAP) {
                Logger.log(
                    "Skipping outbox entry ${entry.id}: attempt count ${entry.attemptCount} exceeds cap.",
                    domain = LoggerDomain.outbox,
                )
                continue
            }
            retry(entry.id)
        }
    }
}
