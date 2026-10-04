//
//  QueriedContactPairTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.models.QueriedContactPairCache

class QueriedContactPairTest {
    // MARK: - Setup

    private val ada = contactPair("Ada", "Lovelace", "5551112222", "user-ada")
    private val alan = contactPair("Alan", "Turing", "5553334444", "user-alan")
    private val contactPairs = listOf(ada, alan)

    @Before
    fun setUp() {
        QueriedContactPairCache.clearCache()
    }

    // MARK: - Query Matrix

    @Test
    fun `an empty query returns every contact pair`() {
        assertEquals(contactPairs, contactPairs.queried(""))
    }

    @Test
    fun `a query matches against the full name`() {
        assertEquals(listOf(ada), contactPairs.queried("ada lovelace"))
    }

    @Test
    fun `a query matches against the first name`() {
        assertEquals(listOf(alan), contactPairs.queried("Alan"))
    }

    @Test
    fun `a query matches against the last name`() {
        assertEquals(listOf(ada), contactPairs.queried("lovelace"))
    }

    @Test
    fun `a query matches against the compiled number string`() {
        assertEquals(listOf(alan), contactPairs.queried("3334444"))
    }

    @Test
    fun `a query with no matches returns an empty list`() {
        assertEquals(emptyList<ContactPair>(), contactPairs.queried("zzz"))
    }

    // MARK: - Auxiliary

    private fun contactPair(
        firstName: String,
        lastName: String,
        nationalNumberString: String,
        userID: String,
    ): ContactPair {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = nationalNumberString,
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        return ContactPair(
            contact =
                Contact(
                    id = userID,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumbers = listOf(phoneNumber),
                    imageData = null,
                ),
            numberPairs = listOf(NumberPair(phoneNumber = phoneNumber, userIDs = listOf(userID))),
        )
    }
}
