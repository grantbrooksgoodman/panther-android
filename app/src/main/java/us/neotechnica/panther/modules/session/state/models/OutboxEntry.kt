//
//  OutboxEntry.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.models

import us.neotechnica.panther.modules.common.models.MediaFileExtension
import java.util.Date

/**
 * A message queued in the outbox for delivery.
 */
data class OutboxEntry(
    /** The identifier key of the conversation the entry belongs to. */
    val conversationIDKey: String,
    /** The date the entry was created. */
    val createdDate: Date,
    /** The identifier of the account that sent the entry. */
    val fromAccountID: String,
    /** The entry's unique identifier. */
    val id: String,
    /** Whether the entry belongs to a PenPals conversation. */
    val isPenPalsConversation: Boolean,
    /** The entry's content. */
    val payload: Payload,
    /** The identifiers of the users the entry is addressed to. */
    val recipientUserIDs: List<String>,
    /** The number of delivery attempts made for the entry. */
    val attemptCount: Int,
    /** The date of the entry's last delivery attempt, or `null` if none has been made. */
    val lastAttemptDate: Date?,
    /** The remote identifier reserved for the entry's message, or `null` if none has been reserved. */
    val reservedRemoteID: String?,
    /** The entry's delivery state. */
    val state: State,
    /** The transcription of the entry's audio, or `null` if it has none. */
    val transcription: String?,
) {
    // MARK: - Types

    /** The content of an outbox entry. */
    sealed class Payload {
        /** An audio message, carrying the file name of its recorded input. */
        data class Audio(val inputFileName: String) : Payload()

        /** A media message, carrying its file name and extension. */
        data class Media(
            val fileName: String,
            val fileExtension: MediaFileExtension,
        ) : Payload()

        /** A text message, carrying its text. */
        data class Text(val value: String) : Payload()
    }

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

    // MARK: - Companion

    companion object {
        /** The maximum number of times an entry is automatically retried. */
        const val AUTO_RETRY_CAP = 3

        /** The prefix identifying an outbox message's identifier. */
        const val ID_PREFIX = "outbox-"
    }
}
