//
//  MessageOutboxServiceRetry.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Retries delivery of the outbox entry with the given identifier.
 *
 * Claims the entry, resolves its conversation and recipients, and
 * attempts to send its payload. The entry is removed on success or when
 * its conversation no longer exists, and marked failed if delivery
 * fails.
 *
 * @param entryID The identifier of the outbox entry to retry.
 */
suspend fun MessageOutboxService.retry(entryID: String) {
    val candidateRemoteID =
        Networking.config.databaseDelegate.generateKey(NetworkPath.messages.rawValue) ?: return
    val entry = claimForRetry(entryID, candidateRemoteID) ?: return

    val conversation = SessionStore.getConversation(entry.conversationIDKey)
    if (conversation == null) {
        remove(entryID)
        Logger.log("Removed outbox entry $entryID: conversation no longer exists.")
        return
    }

    val recipients =
        entry.recipientUserIDs.mapNotNull { userID ->
            SessionStore.users[userID] ?: runCatching { UserService.getUser(userID) }.getOrNull()
        }
    if (recipients.isEmpty()) {
        markFailed(entryID)
        Logger.log("Failed to resolve any recipient users for outbox entry $entryID.")
        return
    }

    try {
        MessageSessionService.sendTextMessage(
            text = entry.text,
            presetID = entry.reservedRemoteID,
            users = recipients,
            conversation = conversation,
        )
        remove(entryID)
        Logger.log("Retry succeeded for outbox entry $entryID.")
    } catch (exception: Exception) {
        markFailed(entryID)
        Logger.log(exception)
    }
}

/**
 * Retries every failed outbox entry that has not exceeded the auto-retry
 * cap, serialized per conversation in creation order.
 */
suspend fun MessageOutboxService.retryAllEligible() {
    val failedEntries = allEntries.filter { it.state == OutboxEntry.State.FAILED }
    if (failedEntries.isEmpty()) return

    val grouped = failedEntries.groupBy { it.conversationIDKey }
    Logger.log("Auto-retrying ${failedEntries.size} eligible entries across ${grouped.size} conversations.")

    for ((_, conversationEntries) in grouped) {
        for (entry in conversationEntries) {
            if (entry.attemptCount >= OutboxEntry.AUTO_RETRY_CAP) {
                Logger.log("Skipping outbox entry ${entry.id}: attempt count ${entry.attemptCount} exceeds cap.")
                continue
            }
            retry(entry.id)
        }
    }
}
