//
//  OutboxEntry.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.models

import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import java.util.Date

/**
 * A message queued in the outbox for delivery.
 *
 * An entry carries either a text payload or a media payload (its staged
 * local media path); audio payloads arrive with the audio phase.
 *
 * @property id The entry's unique identifier (prefixed `outbox-`).
 * @property conversationIDKey The identifier key of the conversation the
 *   entry belongs to.
 * @property fromAccountID The identifier of the account that sent the
 *   entry.
 * @property recipientUserIDs The identifiers of the users the entry is
 *   addressed to.
 * @property text The entry's text content, or empty for a media entry.
 * @property mediaRelativePath The staged media file's path relative to the
 *   documents directory, or `null` for a text entry.
 * @property isPenPalsConversation Whether the entry belongs to a PenPals
 *   conversation.
 * @property createdDate The date the entry was created.
 * @property attemptCount The number of delivery attempts made.
 * @property lastAttemptDate The date of the last delivery attempt, or
 *   `null` if none has been made.
 * @property reservedRemoteID The remote identifier reserved for the
 *   entry's message, or `null` if none has been reserved.
 * @property state The entry's delivery state.
 */
data class OutboxEntry(
    val id: String,
    val conversationIDKey: String,
    val fromAccountID: String,
    val recipientUserIDs: List<String>,
    val text: String,
    val mediaRelativePath: String? = null,
    val isPenPalsConversation: Boolean,
    val createdDate: Date,
    val attemptCount: Int,
    val lastAttemptDate: Date?,
    val reservedRemoteID: String?,
    val state: State,
) {
    // MARK: - Computed Properties

    /** Whether the entry carries a media payload. */
    val isMediaEntry: Boolean
        get() = mediaRelativePath != null

    /** The staged media file for a media entry, or `null` if none exists on disk. */
    val mediaFile: MediaFile?
        get() = mediaRelativePath?.let { MediaFile.from(it) }

    /** The delivery state of an outbox entry. */
    enum class State(
        val rawValue: String,
    ) {
        /** The entry's last delivery attempt failed. */
        FAILED("failed"),

        /** The entry is currently being delivered. */
        SENDING("sending"),
        ;

        companion object {
            /** Returns the state for [rawValue], or `null` if unknown. */
            fun from(rawValue: String): State? = entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    companion object {
        /** The maximum number of times an entry is automatically retried. */
        const val AUTO_RETRY_CAP = 3

        /** The prefix identifying an outbox message's identifier. */
        const val ID_PREFIX = "outbox-"
    }
}
