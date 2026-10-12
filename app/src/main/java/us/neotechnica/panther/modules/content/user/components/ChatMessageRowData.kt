//
//  ChatMessageRowData.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.ui.graphics.ImageBitmap
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.Message

/**
 * The display inputs for a single chat row.
 *
 * @property message The message to render.
 * @property previousMessage The prior row's message, for day-separator
 *   gaps and sender-run detection.
 * @property isLastConfirmedOwnMessage Whether this is the last confirmed
 *   own message (drives the delivery status label).
 * @property isGroup Whether the conversation has more than two
 *   participants.
 * @property isFailed Whether this is a failed outbox message.
 * @property senderName The sender's display name, shown above the first
 *   message in a run from a group participant, or `null` when it should
 *   be hidden.
 * @property senderImage The sender's contact image, or `null` for the
 *   generic avatar glyph.
 * @property reactions The message's reactions, or empty when it has
 *   none.
 */
data class ChatMessageRowData(
    val message: Message,
    val previousMessage: Message?,
    val isLastConfirmedOwnMessage: Boolean,
    val isGroup: Boolean,
    val isFailed: Boolean,
    val senderName: String?,
    val senderImage: ImageBitmap? = null,
    val reactions: List<Reaction> = emptyList(),
)
