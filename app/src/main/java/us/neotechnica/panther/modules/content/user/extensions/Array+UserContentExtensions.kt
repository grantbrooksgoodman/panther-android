//
//  Array+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 26/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.models.QueriedContactPairCache
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
