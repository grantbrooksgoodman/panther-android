//
//  MessageListSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.schema.conversation.models.Activity
import us.neotechnica.panther.networking.modules.schema.message.models.Message

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
