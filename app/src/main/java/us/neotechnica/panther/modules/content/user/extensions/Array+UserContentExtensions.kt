//
//  Array+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 26/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.networking.user.models.User

// MARK: - ContactPair

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
