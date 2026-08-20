//
//  ConversationListSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import java.util.Date

/**
 * The conversations still visible to the current user.
 */
val List<Conversation>.visibleForCurrentUser: List<Conversation>
    get() = filter { it.isVisibleForCurrentUser }

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
