//
//  Array+EntitySessionExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.conversation.models.ReactionMetadata
import us.neotechnica.panther.modules.networking.user.models.User

/**
 * Returns the reaction metadata with the current user's reactions to
 * the given message removed.
 *
 * @param messageID The identifier of the message whose current-user
 *   reactions to remove.
 *
 * @return The filtered reaction metadata.
 */
fun List<ReactionMetadata>.filteringCurrentUserReactions(messageID: String): List<ReactionMetadata> {
    val result = toMutableList()

    for (index in result.indices.reversed()) {
        val metadata = result[index]
        val containsCurrentUserReaction =
            metadata.messageID == messageID && metadata.reactions.any { it.userID == User.currentUserID }
        if (!containsCurrentUserReaction) continue

        val filteredReactions = metadata.reactions.filter { it.userID != User.currentUserID }
        result.removeAt(index)
        if (filteredReactions.isNotEmpty()) {
            result.add(index, ReactionMetadata(messageID = metadata.messageID, reactions = filteredReactions))
        }
    }

    return result.ifEmpty { listOf(ReactionMetadata.empty) }
}
