//
//  ChatMessageRowData.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.translator.models.Translation

/**
 * The display inputs for a single chat row.
 *
 * @property message The message to render.
 * @property previousMessage The prior row's message, for day-separator gaps.
 * @property translation The resolved translation, or `null` if unresolved.
 * @property showAlternate Whether to show the alternate (original vs.
 *   translated) text.
 * @property isLastConfirmedOwnMessage Whether this is the last confirmed
 *   own message (drives the status label).
 * @property isGroup Whether the conversation has more than two participants.
 * @property isFailed Whether this is a failed outbox message.
 * @property senderName The sender's display name (contact name or number),
 *   shown above the first message in a run from a group participant, or
 *   `null` when it should be hidden.
 * @property senderInitials The sender's initials, for the avatar fallback.
 * @property showSenderAvatar Whether to render the sender's avatar beside
 *   this row (the last message in a run from a group participant).
 * @property reactionsText The message's reaction emoji, joined in display
 *   order, or an empty string when the message has no reactions.
 */
data class ChatMessageRowData(
    val message: Message,
    val previousMessage: Message?,
    val translation: Translation?,
    val showAlternate: Boolean,
    val isLastConfirmedOwnMessage: Boolean,
    val isGroup: Boolean,
    val isFailed: Boolean,
    val senderName: String?,
    val senderInitials: String = "",
    val showSenderAvatar: Boolean = false,
    val reactionsText: String = "",
)
