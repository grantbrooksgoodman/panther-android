//
//  ContactPair+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.extensions.compiledNumberStrings
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.services.RecipientBarContactSelectionUIService
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.services.UserSessionService

// MARK: - Properties

/** The compiled number strings of the contact's phone numbers. */
val ContactPair.compiledNumberStrings: List<String>
    get() = contact.phoneNumbers.compiledNumberStrings

/** Whether the contact pair contains a user the current user has blocked. */
val ContactPair.containsBlockedUser: Boolean
    get() {
        val blockedUserIDs = UserSessionService.currentUser?.blockedUserIDs ?: return false
        return userIDs.any { it in blockedUserIDs }
    }

/** Whether the contact pair contains only the current user. */
val ContactPair.containsCurrentUser: Boolean
    get() = userIDs.all { it == User.currentUserID }

/**
 * A Boolean value that indicates whether the contact pair is currently
 * selected as a recipient.
 */
val ContactPair.isSelected: Boolean
    get() = RecipientBarContactSelectionUIService.selectedContactPairs.value.contains(this)

/**
 * Whether the contact pair is a mock, representing an unresolved
 * recipient entered manually.
 */
val ContactPair.isMock: Boolean
    get() {
        val firstNumberPair = numberPairs.firstOrNull()
        return contact.id.isBlank() &&
            contact.lastName.isBlank() &&
            contact.phoneNumbers.isEmpty() &&
            contact.imageData == null &&
            numberPairs.size == 1 &&
            firstNumberPair?.userIDs?.size == 1 &&
            firstNumberPair.userIDs.firstOrNull()?.isBlank() == true
    }

/** The user identifiers across the contact pair's number pairs. */
val ContactPair.userIDs: List<String>
    get() = numberPairs.flatMap { it.userIDs }

/** Resolves users from the session store using this contact pair's user IDs. */
val ContactPair.users: List<User>
    get() = numberPairs.flatMap { it.users }

// MARK: - Methods

/** An empty contact pair with a blank name. */
val ContactPair.Companion.empty: ContactPair
    get() = mock(withName = "")

/**
 * Creates a mock contact pair with the given name, representing an
 * unresolved recipient.
 *
 * @param withName The name to display for the recipient.
 *
 * @return A mock contact pair.
 */
fun ContactPair.Companion.mock(withName: String): ContactPair =
    ContactPair(
        contact =
            Contact(
                id = "",
                firstName = withName,
                lastName = "",
                phoneNumbers = emptyList(),
                imageData = null,
            ),
        numberPairs =
            listOf(
                NumberPair(
                    phoneNumber =
                        PhoneNumber(
                            callingCode = "",
                            nationalNumberString = "",
                            regionCode = "",
                            label = null,
                            internalFormattedString = null,
                        ),
                    userIDs = listOf(""),
                ),
            ),
    )

/**
 * Creates a contact pair for the given registered user.
 *
 * @param user The registered user to represent.
 * @param name The name to display, or `null` to use the user's
 *   formatted phone number.
 *
 * @return A contact pair representing the given user.
 */
fun ContactPair.Companion.withUser(
    user: User,
    name: String? = null,
): ContactPair =
    ContactPair(
        contact =
            Contact(
                id = "",
                firstName = name ?: user.phoneNumber.formattedString(),
                lastName = "",
                phoneNumbers = listOf(user.phoneNumber),
                imageData = null,
            ),
        numberPairs =
            listOf(
                NumberPair(
                    phoneNumber = user.phoneNumber,
                    userIDs = listOf(user.id),
                ),
            ),
    )
