//
//  ChatParticipant.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.networking.user.models.User

/**
 * A participant in a conversation, prepared for display.
 *
 * Combines a participant's contact information with their display
 * name.
 *
 * @property displayName The name the participant row displays.
 * @property contactPair The contact pair describing the participant.
 */
data class ChatParticipant(
    val displayName: String,
    val contactPair: ContactPair,
) {
    /** The first user associated with the participant's contact pair. */
    val firstUser: User?
        get() = contactPair.numberPairs.flatMap { it.users }.firstOrNull()
}
