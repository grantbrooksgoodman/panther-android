//
//  Array+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.modules.content.user.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.content.user.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.withMessagesOffsetFromCurrentUserAdditionDate
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.message.models.Message
import java.util.Date

// MARK: - Conversation

/**
 * The conversations sorted by their most recent message's sent date,
 * newest first, with conversations having no messages last.
 */
val List<Conversation>.sortedByLatestMessageSentDate: List<Conversation>
    get() {
        val withSentDate = mutableListOf<Pair<Conversation, Date>>()
        val withoutSentDate = mutableListOf<Conversation>()

        for (conversation in this) {
            val latestMessageSentDate = conversation.latestMessageSentDate
            if (latestMessageSentDate == null) {
                withoutSentDate.add(conversation)
                continue
            }

            withSentDate.add(conversation to latestMessageSentDate)
        }

        return withSentDate
            .sortedWith { left, right ->
                if (left.second != right.second) {
                    right.second.compareTo(left.second)
                } else {
                    left.first.id.key
                        .compareTo(right.first.id.key)
                }
            }.map { it.first } + withoutSentDate
    }

/**
 * The conversations among the list in which the current user is
 * participating, has not deleted, and which do not contain any
 * participants the user has blocked.
 */
val List<Conversation>.visibleForCurrentUser: List<Conversation>
    get() = filter { it.isVisibleForCurrentUser }

// MARK: - Message

/**
 * The unique messages among the list according to their `id` value,
 * preserving original order, where those with populated
 * `readReceipts` fields take priority.
 */
val List<Message>.uniquedByID: List<Message>
    get() {
        val indicesForIDs = mutableMapOf<String, Int>()
        val uniqueMessages = mutableListOf<Message>()

        for (message in this) {
            val index = indicesForIDs[message.id]
            if (index != null) {
                if (uniqueMessages[index].readReceipts != null || message.readReceipts == null) continue
                uniqueMessages[index] = message
            } else {
                indicesForIDs[message.id] = uniqueMessages.size
                uniqueMessages.add(message)
            }
        }

        return uniqueMessages
    }

// MARK: - Auxiliary

private val Conversation.latestMessageSentDate: Date?
    get() =
        listOfNotNull(
            // Messages sent before the current user's addition date are hidden
            // from display, so they don't factor into sort order.
            withMessagesOffsetFromCurrentUserAdditionDate
                .filteringSystemMessages
                .messages
                ?.maxOfOrNull { it.sentDate },
            // Session store does not store system messages; their sent dates resolve from activities.
            activities
                ?.filter { it != Activity.empty }
                ?.maxOfOrNull { it.date },
        ).maxOrNull()
