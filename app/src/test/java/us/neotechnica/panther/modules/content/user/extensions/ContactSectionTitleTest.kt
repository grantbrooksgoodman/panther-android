//
//  ContactSectionTitleTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.PhoneNumber

class ContactSectionTitleTest {
    // MARK: - Absolute Last Name

    @Test
    fun `absolute last name prefers the last name then the first name`() {
        assertEquals("Lovelace", contact("Ada", "Lovelace").absoluteLastName)
        assertEquals("Ada", contact("Ada", "").absoluteLastName)
    }

    // MARK: - Section Title

    @Test
    fun `section title is the first letter of the last name`() {
        assertEquals("L", contact("Ada", "Lovelace").tableViewSectionTitle)
    }

    @Test
    fun `section title falls back to the first name`() {
        assertEquals("A", contact("Ada", "").tableViewSectionTitle)
    }

    @Test
    fun `section title is a hash for a phone-number-only contact`() {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "5551234567",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        val phoneOnly =
            Contact(
                id = "contact-phone",
                firstName = "",
                lastName = "",
                phoneNumbers = listOf(phoneNumber),
                imageData = null,
            )
        assertEquals("#", phoneOnly.tableViewSectionTitle)
    }

    // MARK: - Auxiliary

    private fun contact(
        firstName: String,
        lastName: String,
    ): Contact =
        Contact(
            id = "contact-$firstName-$lastName",
            firstName = firstName,
            lastName = lastName,
            phoneNumbers = emptyList(),
            imageData = null,
        )
}
