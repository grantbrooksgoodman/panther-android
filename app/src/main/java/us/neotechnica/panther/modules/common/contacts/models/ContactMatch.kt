//
//  ContactMatch.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.contacts.models

/**
 * A device contact matched to a registered user.
 *
 * @property userID The matched registered user's identifier.
 * @property fullName The contact's display name.
 * @property compiledNumberString The matched phone number's digits.
 */
data class ContactMatch(
    val userID: String,
    val fullName: String,
    val compiledNumberString: String,
) {
    /** The contact's initials, derived from [fullName]. */
    val initials: String
        get() {
            val words = fullName.split(" ").filter { it.firstOrNull()?.isLetter() == true }
            return when {
                words.isEmpty() -> fullName.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
                words.size == 1 -> words[0].take(1).uppercase()
                else -> (words[0].take(1) + words.last().take(1)).uppercase()
            }
        }
}
