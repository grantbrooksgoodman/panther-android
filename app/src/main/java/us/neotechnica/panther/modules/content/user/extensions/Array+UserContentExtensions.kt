//
//  Array+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.QueriedContactPairCache
import us.neotechnica.panther.modules.content.user.models.QueriedConversationCache
import us.neotechnica.panther.modules.networking.common.sortedByLatestMessageSentDate
import us.neotechnica.panther.modules.networking.common.uniquedByID
import us.neotechnica.panther.modules.networking.common.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.MessageRecipientConsentAcknowledgementData
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User

// MARK: - ContactPair

/**
 * Returns the contact pairs matching the given search term, excluding
 * those already selected as recipients.
 *
 * Matches against each contact's name and phone numbers. Results are
 * cached in memory per search term.
 *
 * @param searchTerm The term to filter the contact pairs by.
 *
 * @return The matching contact pairs, excluding any currently selected
 *   as recipients.
 */
@JvmName("queriedContactPairs")
fun List<ContactPair>.queried(searchTerm: String): List<ContactPair> {
    // Captures pure whitespace too, hence isEmpty and not isBlank.
    if (searchTerm.isEmpty()) return this

    QueriedContactPairCache.cachedValue(searchTerm)?.let { cached ->
        return cached.filter { !it.isSelected }
    }

    val normalizedSearchTerm = searchTerm.trim().lowercase()
    val queriedContactPairs =
        filter { contactPair ->
            val validTerms =
                listOf(
                    contactPair.contact.fullName,
                    contactPair.contact.firstName,
                    contactPair.contact.lastName,
                ) + contactPair.compiledNumberStrings
            validTerms.any { it.trim().lowercase().contains(normalizedSearchTerm) }
        }

    if (QueriedContactPairCache.canWriteToCache) {
        QueriedContactPairCache.cache(searchTerm, queriedContactPairs)
    }

    return queriedContactPairs.filter { !it.isSelected }
}

/**
 * The contact pairs with duplicates removed by phone number, keeping the
 * first occurrence of each.
 */
val List<ContactPair>.uniquedByPhoneNumber: List<ContactPair>
    get() {
        val contactPairs = mutableListOf<ContactPair>()
        val seenNumbers = mutableSetOf<String>()

        for (contactPair in this) {
            if (contactPair.compiledNumberStrings.any { it in seenNumbers }) continue
            contactPairs.add(contactPair)
            seenNumbers.addAll(contactPair.compiledNumberStrings)
        }

        return contactPairs
    }

/** The user identifiers across all of the contact pairs. */
val List<ContactPair>.userIDs: List<String>
    get() = flatMap { it.userIDs }

/** The users across all of the contact pairs. */
val List<ContactPair>.users: List<User>
    get() = flatMap { it.users }

// MARK: - Conversation

/**
 * The unique conversations among the list which are visible for the
 * current user, sorted by latest message sent date, and hydrated with
 * system messages.
 */
val List<Conversation>.filteredAndSorted: List<Conversation>
    get() =
        visibleForCurrentUser
            .sortedByLatestMessageSentDate
            .distinct()
            .map { it.filteringSystemMessages }

/**
 * Returns the visible conversations matching the given search term.
 *
 * Matches against each conversation's name, title, and message text.
 * Results are cached in memory per search term.
 *
 * @param searchTerm The term to filter the conversations by.
 *
 * @return The matching conversations that are visible to the current
 *   user.
 */
@JvmName("queriedConversations")
fun List<Conversation>.queried(searchTerm: String): List<Conversation> {
    val normalizedSearchTerm = searchTerm.lowercase().trim()
    if (normalizedSearchTerm.isBlank()) return this

    QueriedConversationCache.cachedValue(normalizedSearchTerm)?.let { return it.visibleForCurrentUser }

    fun satisfiesConstraints(conversation: Conversation): Boolean {
        val metadataContainsSearchTerm =
            conversation.metadata.name
                .lowercase()
                .trim()
                .contains(normalizedSearchTerm)

        val cellViewDataTitleLabelTextContainsSearchTerm =
            ConversationCellViewData
                .title(conversation)
                .lowercase()
                .trim()
                .contains(normalizedSearchTerm)

        val messages =
            conversation.withMessagesOffsetFromCurrentUserAdditionDate.messages
                ?: return cellViewDataTitleLabelTextContainsSearchTerm || metadataContainsSearchTerm

        val messagesContainsSearchTerm = messages.any { it.textContains(normalizedSearchTerm) }

        return cellViewDataTitleLabelTextContainsSearchTerm || messagesContainsSearchTerm || metadataContainsSearchTerm
    }

    val queriedConversations = filter { satisfiesConstraints(it) }
    QueriedConversationCache.cache(normalizedSearchTerm, queriedConversations)
    return queriedConversations
}

// MARK: - Message

/** The messages with system messages removed. */
val List<Message>.filteringSystemMessages: List<Message>
    get() = filter { !it.isSystemMessage }

/** The messages sorted from oldest to newest. */
val List<Message>.sortedByAscendingSentDate: List<Message>
    get() = sortedBy { it.sentDate }

/** The messages sorted from newest to oldest. */
val List<Message>.sortedByDescendingSentDate: List<Message>
    get() = sortedByDescending { it.sentDate }

/**
 * Returns the messages merged with the given activities' system
 * messages, de-duplicated and sorted from oldest to newest.
 *
 * @param activities The activities whose system messages to merge in.
 *
 * @return The combined, sorted messages.
 */
fun List<Message>.hydrated(activities: List<Activity>?): List<Message> {
    if (activities == null || activities.all { it == Activity.empty }) return this
    return (this + activities.map { it.message })
        .uniquedByID
        .sortedByAscendingSentDate
}

/**
 * Returns the messages limited to those sent after the current user
 * joined the conversation.
 *
 * @param activities The conversation's activities, used to determine
 *   when the current user joined.
 *
 * @return The filtered messages.
 */
fun List<Message>.offsetFromCurrentUserAdditionDate(activities: List<Activity>?): List<Message> {
    val currentUserAddedActivity = activities?.lastOrNull { it.action.isCurrentUserAdded } ?: return this
    return filter { it.sentDate >= currentUserAddedActivity.date }
}

// MARK: - MessageRecipientConsentAcknowledgementData

/** The consent acknowledgement record belonging to the current user, if any. */
val List<MessageRecipientConsentAcknowledgementData>.firstWithCurrentUserID: MessageRecipientConsentAcknowledgementData?
    get() = firstOrNull { it.userID == User.currentUserID }

// MARK: - Participant

/** The participant representing the current user, if any. */
val List<Participant>.firstWithCurrentUserID: Participant?
    get() = firstOrNull { it.userID == User.currentUserID }

// MARK: - String

/** Sorts the list with alphabetically-prefixed strings taking priority. */
val List<String>.alphabeticallySorted: List<String>
    get() {
        val alphabetical = mutableListOf<String>()
        val notAlphabetical = mutableListOf<String>()

        for (string in this) {
            val firstCharacter = string.lowercase().trim().firstOrNull()
            if (firstCharacter == null || !firstCharacter.isLetter()) {
                notAlphabetical.add(string)
                continue
            }

            alphabetical.add(string)
        }

        return alphabetical.sorted() + notAlphabetical.sorted()
    }

// MARK: - User

/**
 * The users with duplicates removed by identifier, keeping the first
 * occurrence of each.
 */
val List<User>.uniquedByID: List<User>
    get() {
        val seen = mutableSetOf<String>()
        return filter { seen.add(it.id) }
    }
