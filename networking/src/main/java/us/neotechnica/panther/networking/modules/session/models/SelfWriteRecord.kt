//
//  SelfWriteRecord.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.models

import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import java.util.Date
import kotlin.math.abs

/**
 * A record of a recent local write to a conversation, used to
 * distinguish the app's own writes from remote changes.
 *
 * @property conversationID The identifier of the conversation that was
 *   written.
 * @property date The date of the write.
 */
data class SelfWriteRecord(
    val conversationID: ConversationID,
    val date: Date = Date(),
) {
    /** Whether the record has expired (older than [EXPIRY_MILLIS]). */
    val isExpired: Boolean
        get() = abs(System.currentTimeMillis() - date.time) >= EXPIRY_MILLIS

    private companion object {
        const val EXPIRY_MILLIS = 10_000L
    }
}
