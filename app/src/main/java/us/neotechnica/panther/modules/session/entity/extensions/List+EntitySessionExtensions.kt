//
//  List+EntitySessionExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.QueriedConversationCache
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ReactionMetadata
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import java.util.Date

// MARK: - Conversation

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

// MARK: - Message

/** The messages sorted by ascending sent date. */
val List<Message>.sortedByAscendingSentDate: List<Message>
    get() = sortedBy { it.sentDate.time }

/** The messages sorted by descending sent date. */
val List<Message>.sortedByDescendingSentDate: List<Message>
    get() = sortedByDescending { it.sentDate.time }

/** The messages deduplicated by identifier, preserving order. */
val List<Message>.uniquedByID: List<Message>
    get() {
        val seen = mutableSetOf<String>()
        return filter { seen.add(it.id) }
    }

/** The messages with system messages removed. */
val List<Message>.filteringSystemMessages: List<Message>
    get() = filter { !it.isSystemMessage }

/**
 * The messages merged with the given activities' system messages,
 * deduplicated and sorted by ascending sent date.
 *
 * @param activities The conversation's activities, or `null`.
 */
fun List<Message>.hydrated(activities: List<Activity>?): List<Message> {
    if (activities == null || activities.all { it == Activity.empty }) return this
    return (this + activities.map { it.message }).uniquedByID.sortedByAscendingSentDate
}

/**
 * The messages limited to those sent after the current user joined the
 * conversation.
 *
 * @param activities The conversation's activities, used to determine
 *   when the current user joined.
 */
fun List<Message>.offsetFromCurrentUserAdditionDate(activities: List<Activity>?): List<Message> {
    val addedActivity =
        activities?.lastOrNull { it.action.isCurrentUserAdded } ?: return this
    return filter { it.sentDate.time >= addedActivity.date.time }
}

// MARK: - ReactionMetadata

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
