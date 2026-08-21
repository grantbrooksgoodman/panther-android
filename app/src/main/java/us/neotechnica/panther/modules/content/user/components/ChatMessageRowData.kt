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
 * @property senderName The sender's display name, shown in group chats.
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
)
