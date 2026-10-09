//
//  PhoneNumberServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PhoneNumberServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        CommonPropertyLists.initializeForTesting(
            callingCodes =
                mapOf(
                    "CA" to "1",
                    "GB" to "44",
                    "US" to "1",
                ),
            lookupTables =
                mapOf(
                    "10" to listOf("1", "44"),
                ),
        )
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
    }

    // MARK: - Tests

    @Test
    fun `the United States example number short-circuits`() {
        assertEquals("(555) 555-5555", PhoneNumberService.exampleNationalNumberString("US"))
    }

    @Test
    fun `a calling code matches when the remaining digits form a valid length`() {
        assertEquals(listOf("1"), PhoneNumberService.possibleCallingCodes("12125550100"))
        assertEquals(listOf("44"), PhoneNumberService.possibleCallingCodes("447911123456"))
    }

    @Test
    fun `candidates fall back to the number length, and none derive to null`() {
        assertEquals(listOf("1", "44"), PhoneNumberService.possibleCallingCodes("2125550100"))
        assertNull(PhoneNumberService.possibleCallingCodes("123"))
    }

    @Test
    fun `plural candidates concatenate and skip numbers without candidates`() {
        assertEquals(
            listOf("1", "1", "44"),
            PhoneNumberService.possibleCallingCodes(listOf("12125550100", "123", "2125550100")),
        )

        assertNull(PhoneNumberService.possibleCallingCodes(listOf("123")))
    }
}
