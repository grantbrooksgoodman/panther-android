//
//  PhoneNumberTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.RegionDetailService

class PhoneNumberTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        CommonPropertyLists.initializeForTesting(
            callingCodes = mapOf("FR" to "33", "US" to "1"),
            lookupTables = mapOf("10" to listOf("1"), "9" to listOf("33")),
        )
        RegionDetailService.clearCache()
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
    }

    // MARK: - String Initializer

    @Test
    fun `derives a national number and calling code from a national string`() {
        val phoneNumber = PhoneNumber("5551234567")

        assertEquals("1", phoneNumber.callingCode)
        assertEquals("5551234567", phoneNumber.nationalNumberString)
        assertEquals("US", phoneNumber.regionCode)
    }

    @Test
    fun `strips a leading calling code from the string`() {
        val phoneNumber = PhoneNumber("15551234567")

        assertEquals("1", phoneNumber.callingCode)
        assertEquals("5551234567", phoneNumber.nationalNumberString)
        assertEquals("15551234567", phoneNumber.compiledNumberString)
    }

    @Test
    fun `ignores non-digit characters and trims the label`() {
        val phoneNumber = PhoneNumber("(555) 123-4567", label = "  Mobile  ")

        assertEquals("5551234567", phoneNumber.nationalNumberString)
        assertEquals("Mobile", phoneNumber.label)
    }

    @Test
    fun `the primary constructor trims the label and reduces the digits`() {
        val phoneNumber =
            PhoneNumber(
                callingCode = "+1",
                nationalNumberString = "(555) 123-4567",
                regionCode = "US",
                label = "\n Home \n",
                internalFormattedString = null,
            )

        assertEquals("1", phoneNumber.callingCode)
        assertEquals("5551234567", phoneNumber.nationalNumberString)
        assertEquals("Home", phoneNumber.label)
    }

    // MARK: - Formatting

    @Test
    fun `national and international input format identically`() {
        val national = PhoneNumber("5558885555")
        val international = PhoneNumber("+1 555 888 5555")

        assertEquals("(555) 888-5555", national.partiallyFormatted())
        assertEquals(national.partiallyFormatted(), international.partiallyFormatted())
        assertEquals("+1 (555) 888-5555", national.formattedString())
        assertEquals(national.formattedString(), international.formattedString())
    }

    @Test
    fun `the formatted string is cached per region and recomputed for another`() {
        val phoneNumber = PhoneNumber("5558885555")

        assertEquals(phoneNumber.formattedString(), phoneNumber.formattedString())
        assertEquals("+1 (555) 888-5555", phoneNumber.formattedString(regionCode = "US"))
    }

    @Test
    fun `a region without a known calling code falls back to the full format attempt`() {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "5558885555",
                regionCode = "ZZ",
                label = null,
                internalFormattedString = null,
            )

        // No calling code is known for ZZ and the number cannot be parsed for
        // it, so the failsafe pairing of the compiled digits stands.
        assertEquals("1 55 58 88 55 55", phoneNumber.partiallyFormatted())
    }

    @Test
    fun `a North American internal formatted string wins over libphonenumber`() {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "5558885555",
                regionCode = "US",
                label = null,
                internalFormattedString = "+1 555-888-5555",
            )

        assertEquals("555-888-5555", phoneNumber.partiallyFormatted())
        assertEquals("+1 555-888-5555", phoneNumber.formattedString())
    }

    @Test
    fun `an empty number formats to an empty string`() {
        val phoneNumber =
            PhoneNumber(
                callingCode = "",
                nationalNumberString = "",
                regionCode = "",
                label = null,
                internalFormattedString = null,
            )

        assertEquals("", phoneNumber.partiallyFormatted())
    }
}
