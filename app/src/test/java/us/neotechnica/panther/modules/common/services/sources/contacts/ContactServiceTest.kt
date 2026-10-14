//
//  ContactServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contacts

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.ContactImageCache
import us.neotechnica.panther.modules.common.models.DeviceContact
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Exercises the contact pipeline: grouping registered users onto the
 * device contacts they match, carrying thumbnail image data, and the
 * exact-number matching rules.
 */
class ContactServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        Persistent.initializeForTesting()
        CommonPropertyLists.initializeForTesting(
            callingCodes = mapOf("GB" to "44", "US" to "1"),
            lookupTables = mapOf("10" to listOf("1", "44")),
        )
        RegionDetailService.clearCache()
        ContactImageCache.clearCache()
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        ContactImageCache.clearCache()
    }

    // MARK: - Grouping

    @Test
    fun `users matching one device contact share a contact pair sorted by calling code`() {
        val british = SessionTestEnvironment.user(id = "british", nationalNumber = "442079460958")
        val american = SessionTestEnvironment.user(id = "american", nationalNumber = "5558885555")
        val deviceContact = deviceContact(id = "1", givenName = "Ada", familyName = "Lovelace", "+442079460958", "5558885555")

        val contactPairs = ContactService.contactPairs(listOf(british, american), listOf(deviceContact))

        assertEquals(1, contactPairs.size)
        val contactPair = contactPairs.single()
        assertEquals("Ada", contactPair.contact.firstName)
        assertEquals("Lovelace", contactPair.contact.lastName)
        assertEquals(listOf("1", "44"), contactPair.numberPairs.map { it.phoneNumber.callingCode })
        assertEquals(listOf("american", "british"), contactPair.numberPairs.flatMap { it.userIDs })
    }

    @Test
    fun `device contacts matching no user are dropped and results sort by first name`() {
        val user = SessionTestEnvironment.user(id = "user", nationalNumber = "5558885555")
        val stranger = deviceContact(id = "1", givenName = "Zed", familyName = "", "5550000000")
        val friend = deviceContact(id = "2", givenName = "Bob", familyName = "Builder", "5558885555")
        val alias = deviceContact(id = "3", givenName = "Alice", familyName = "Alias", "+15558885555")

        val contactPairs = ContactService.contactPairs(listOf(user), listOf(stranger, friend, alias))

        // Both matching contacts share the number; the first by first name wins.
        assertEquals(listOf("Alice"), contactPairs.map { it.contact.firstName })
    }

    // MARK: - Images

    @Test
    fun `thumbnail image data flows onto the contact and seeds the image cache`() {
        val user = SessionTestEnvironment.user(id = "user", nationalNumber = "5558885555")
        val imageData = byteArrayOf(1, 2, 3)
        val deviceContact =
            DeviceContact(
                id = "photo",
                givenName = "Grace",
                familyName = "Hopper",
                phoneticGivenName = "",
                phoneticFamilyName = "",
                nickname = "",
                organizationName = "",
                phoneticOrganizationName = "",
                phoneNumbers = listOf(PhoneNumber("5558885555")),
                imageData = imageData,
            )

        val contact = ContactService.contactPairs(listOf(user), listOf(deviceContact)).single().contact

        assertArrayEquals(imageData, contact.imageData)
    }

    // MARK: - Matching

    @Test
    fun `a compiled number matches with or without its leading calling code`() {
        val user = PhoneNumber("5558885555")

        assertTrue(deviceContact(id = "1", givenName = "A", familyName = "", "+15558885555").matches(user))
        assertTrue(deviceContact(id = "2", givenName = "B", familyName = "", "15558885555").matches(user))
        assertTrue(deviceContact(id = "3", givenName = "C", familyName = "", "(555) 888-5555").matches(user))
    }

    @Test
    fun `a national number matches only when its length validates for the calling code`() {
        val user =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "5558885555",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        val sameNationalOtherCode =
            DeviceContact(
                id = "1",
                givenName = "A",
                familyName = "",
                phoneticGivenName = "",
                phoneticFamilyName = "",
                nickname = "",
                organizationName = "",
                phoneticOrganizationName = "",
                phoneNumbers =
                    listOf(
                        PhoneNumber(
                            callingCode = "44",
                            nationalNumberString = "5558885555",
                            regionCode = "GB",
                            label = null,
                            internalFormattedString = null,
                        ),
                    ),
                imageData = null,
            )

        assertTrue(sameNationalOtherCode.matches(user))

        val shortNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "88855",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        assertFalse(sameNationalOtherCode.matches(shortNumber))
    }

    @Test
    fun `a number that merely ends with the national number does not match`() {
        val user = PhoneNumber("5558885555")
        val longer = deviceContact(id = "1", givenName = "A", familyName = "", "995558885555")

        assertFalse(longer.matches(user))
    }

    // MARK: - Auxiliary

    private fun deviceContact(
        id: String,
        givenName: String,
        familyName: String,
        vararg numbers: String,
    ): DeviceContact =
        DeviceContact(
            id = id,
            givenName = givenName,
            familyName = familyName,
            phoneticGivenName = "",
            phoneticFamilyName = "",
            nickname = "",
            organizationName = "",
            phoneticOrganizationName = "",
            phoneNumbers = numbers.map { PhoneNumber(it) },
            imageData = null,
        )
}
