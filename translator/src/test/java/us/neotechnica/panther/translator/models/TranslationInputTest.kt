//
//  TranslationInputTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.services.DataDetector
import java.util.regex.Pattern

class TranslationInputTest {
    // MARK: - Properties

    private val delimiter = Translator.Constants.PROCESSING_DELIMITER

    // Pure-JVM stand-ins for the platform patterns, in the platform's
    // order: email, link, phone (the phone pattern is the platform's
    // own expression).
    private val detector =
        DataDetector(
            patterns =
                listOf(
                    Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
                    Pattern.compile("https?://[^\\s]+"),
                ),
            phonePattern = Pattern.compile("(\\+[0-9]+[\\- \\.]*)?(\\([0-9]+\\)[\\- \\.]*)?([0-9][0-9\\- \\.]+[0-9])"),
            isGlobalPhoneNumber = { Regex("[+]?[0-9.-]+").matches(it) },
            addressRanges = { emptyList() },
        )

    // MARK: - Tests

    @Test
    fun `short digit runs are left untouched`() {
        for (string in listOf("I have 100 apples", "2 3 pm", "v1.2.3")) {
            assertEquals(string, tokenized(string))
        }
    }

    @Test
    fun `an email address becomes one token`() {
        assertEquals(
            "Write to ${delimiter}john@example.com$delimiter today",
            tokenized("Write to john@example.com today"),
        )
    }

    @Test
    fun `a plausible phone number becomes one token`() {
        assertEquals(
            "Call $delimiter+1 555-888-5555$delimiter now",
            tokenized("Call +1 555-888-5555 now"),
        )
    }

    @Test
    fun `overlapping matches keep the earlier span only`() {
        assertEquals(
            "See ${delimiter}https://example.com/path$delimiter",
            tokenized("See https://example.com/path"),
        )
    }

    @Test
    fun `a spaced digit run with fewer than seven digits is not a phone number`() {
        assertEquals("Room 12 34 56", tokenized("Room 12 34 56"))
    }

    // MARK: - Auxiliary

    private fun tokenized(string: String): String = TranslationInput(string).withTokenizedDetectorAttributes(detector).value
}
