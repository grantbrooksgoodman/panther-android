//
//  ConversationList+SessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.QueriedConversationCache
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import java.util.Date

/**
 * The conversations still visible to the current user.
 */
val List<Conversation>.visibleForCurrentUser: List<Conversation>
    get() = filter { it.isVisibleForCurrentUser }

/**
 * The conversations matching the given search query by title or
 * message text, cached per search term.
 *
 * @param searchQuery The query to match against.
 *
 * @return The matching conversations, or the receiver unchanged
 *   when the query is blank.
 */
fun List<Conversation>.queried(searchQuery: String): List<Conversation> {
    if (searchQuery.isBlank()) return this
    QueriedConversationCache.cachedValue(searchQuery)?.let { return it }
    val result = filter { ConversationCellViewData.matches(it, searchQuery) }
    QueriedConversationCache.cache(searchQuery, result)
    return result
}

/**
 * The conversations sorted by their latest message's sent date,
 * newest first, falling back to the metadata's last-modified date.
 */
val List<Conversation>.sortedByLatestMessageSentDate: List<Conversation>
    get() = sortedByDescending { it.latestActivityDate.time }

/**
 * The visible conversations, de-duplicated and sorted newest-first –
 * the list the conversations page renders.
 */
val List<Conversation>.filteredAndSorted: List<Conversation>
    get() = visibleForCurrentUser.distinctBy { it.id.key }.sortedByLatestMessageSentDate

private val Conversation.latestActivityDate: Date
    get() = messages?.maxByOrNull { it.sentDate.time }?.sentDate ?: metadata.lastModifiedDate
