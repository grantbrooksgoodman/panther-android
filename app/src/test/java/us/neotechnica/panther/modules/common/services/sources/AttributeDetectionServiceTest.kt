//
//  AttributeDetectionServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.common.interfaces.AttributeDetectionServiceDelegate
import us.neotechnica.panther.modules.common.models.DetectedAttribute
import java.util.Calendar
import java.util.Date

/**
 * Exercises attribute detection: links, phone numbers, and dates are
 * found with their ranges, and a tap on one reaches the delegate with
 * the tapped substring.
 */
class AttributeDetectionServiceTest {
    // MARK: - Setup

    private class RecordingDelegate : AttributeDetectionServiceDelegate {
        var selectedDate: Date? = null
        var selectedPhoneNumber: String? = null
        var selectedURL: String? = null
        var selectedSubstring: String? = null

        override fun didSelectDate(
            date: Date,
            substring: String,
            fullText: String,
        ) {
            selectedDate = date
            selectedSubstring = substring
        }

        override fun didSelectPhoneNumber(phoneNumber: String) {
            selectedPhoneNumber = phoneNumber
        }

        override fun didSelectURL(
            url: String,
            substring: String,
            fullText: String,
        ) {
            selectedURL = url
            selectedSubstring = substring
        }
    }

    @After
    fun tearDown() {
        AttributeDetectionService.registerDelegate(DefaultAttributeDetectionServiceDelegate)
    }

    // MARK: - Detection

    @Test
    fun `a bare domain and a scheme link are detected with their ranges`() {
        val text = "see apple.com or https://example.org/path now"
        val attributes = AttributeDetectionService.detectAttributes(text).filterIsInstance<DetectedAttribute.URL>()

        assertEquals(2, attributes.size)
        assertEquals("https://apple.com", attributes[0].url)
        assertEquals("apple.com", text.substring(attributes[0].range))
        assertEquals("https://example.org/path", attributes[1].url)
    }

    @Test
    fun `a phone number is detected and short digit runs are ignored`() {
        val attributes = AttributeDetectionService.detectAttributes("call (555) 555-5555 at 9")
        val phoneNumbers = attributes.filterIsInstance<DetectedAttribute.PhoneNumber>()

        assertEquals(1, phoneNumbers.size)
        assertEquals("(555) 555-5555", phoneNumbers[0].phoneNumber)
        assertTrue(attributes.none { it is DetectedAttribute.Date })
    }

    @Test
    fun `a month-name date parses into the current year`() {
        val attributes = AttributeDetectionService.detectAttributes("lunch on March 3rd?")
        val date = attributes.filterIsInstance<DetectedAttribute.Date>().single()

        val calendar = Calendar.getInstance().apply { time = date.date }
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH))
        assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), calendar.get(Calendar.YEAR))
    }

    @Test
    fun `plain text yields no attributes`() {
        assertTrue(AttributeDetectionService.detectAttributes("hello there").isEmpty())
        assertTrue(AttributeDetectionService.detectAttributes("").isEmpty())
    }

    // MARK: - Gesture

    @Test
    fun `a tap inside a link reaches the delegate with the substring`() {
        val delegate = RecordingDelegate()
        AttributeDetectionService.registerDelegate(delegate)

        val text = "open apple.com today"
        AttributeDetectionService.handleGesture(text, text.indexOf("apple") + 2)

        assertEquals("https://apple.com", delegate.selectedURL)
        assertEquals("apple.com", delegate.selectedSubstring)
    }

    @Test
    fun `a tap outside every attribute reaches no delegate method`() {
        val delegate = RecordingDelegate()
        AttributeDetectionService.registerDelegate(delegate)

        AttributeDetectionService.handleGesture("open apple.com today", 0)

        assertNull(delegate.selectedURL)
        assertNull(delegate.selectedPhoneNumber)
        assertNull(delegate.selectedDate)
    }

    @Test
    fun `a tap on a phone number dials through the delegate`() {
        val delegate = RecordingDelegate()
        AttributeDetectionService.registerDelegate(delegate)

        val text = "ring 555-555-5555"
        AttributeDetectionService.handleGesture(text, text.length - 1)

        assertEquals("555-555-5555", delegate.selectedPhoneNumber)
    }
}
