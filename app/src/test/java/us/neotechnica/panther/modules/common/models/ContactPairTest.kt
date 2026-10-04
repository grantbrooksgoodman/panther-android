//
//  ContactPairTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactPairTest {
    // MARK: - Round Trip

    @Test
    fun `contact pair encodes and decodes to an equal value`() {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "5551234567",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        val contactPair =
            ContactPair(
                contact =
                    Contact(
                        id = "contact-1",
                        firstName = "Jane",
                        lastName = "Doe",
                        phoneNumbers = listOf(phoneNumber),
                        imageData = null,
                    ),
                numberPairs =
                    listOf(
                        NumberPair(phoneNumber = phoneNumber, userIDs = listOf("user-1")),
                    ),
            )

        val decoded = ContactPair.decode(contactPair.encoded)

        assertEquals(contactPair, decoded)
    }

    // MARK: - Derived Values

    @Test
    fun `contact full name and initials derive from name components`() {
        val contact =
            Contact(
                id = "contact-2",
                firstName = "Ada",
                lastName = "Lovelace",
                phoneNumbers = emptyList(),
                imageData = null,
            )

        assertEquals("Ada Lovelace", contact.fullName)
        assertEquals("AL", contact.initials)
    }

    @Test
    fun `contact pair with multiple numbers encodes and decodes to an equal value`() {
        val homeNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "5551234567",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        val mobileNumber =
            PhoneNumber(
                callingCode = "44",
                nationalNumberString = "2079460958",
                regionCode = "GB",
                label = null,
                internalFormattedString = null,
            )
        val contactPair =
            ContactPair(
                contact =
                    Contact(
                        id = "contact-grouped",
                        firstName = "Jane",
                        lastName = "Doe",
                        phoneNumbers = listOf(homeNumber, mobileNumber),
                        imageData = null,
                    ),
                numberPairs =
                    listOf(
                        NumberPair(phoneNumber = homeNumber, userIDs = listOf("user-1")),
                        NumberPair(phoneNumber = mobileNumber, userIDs = listOf("user-2")),
                    ),
            )

        val decoded = ContactPair.decode(contactPair.encoded)

        assertEquals(contactPair, decoded)
        assertEquals(2, decoded?.numberPairs?.size)
    }

    // MARK: - Malformed Input

    @Test
    fun `decode returns null when number pairs are absent`() {
        val malformed =
            mapOf<String, Any?>(
                "contact" to
                    mapOf<String, Any?>(
                        "id" to "contact-3",
                        "firstName" to "Grace",
                        "lastName" to "Hopper",
                        "phoneNumbers" to emptyList<Any?>(),
                        "imageData" to null,
                    ),
                "numberPairs" to emptyList<Any?>(),
            )

        assertNull(ContactPair.decode(malformed))
    }
}
