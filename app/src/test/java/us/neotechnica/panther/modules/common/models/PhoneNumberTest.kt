//
//  PhoneNumberTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberTest {
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
}
