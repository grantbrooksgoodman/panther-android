//
//  DateUserContentExtensionsTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.extensions.weekdayString
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Exercises the chat page message separator string: the Today,
 * Yesterday, weekday, under-a-year, and over-a-year forms, and the bold
 * span over the day portion.
 */
class DateUserContentExtensionsTest {
    // MARK: - Setup

    private val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }
        CoreUtilities.setLanguageCode("en")
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        LocalizedStringResolver.clearCache()
    }

    // MARK: - Tests

    @Test
    fun `today renders the Today word in bold followed by the short time`() {
        val date = Date()
        val string = date.chatPageMessageSeparatorAttributedDateString

        assertNotNull(string)
        assertTrue(string!!.text, TIME_REGEX.containsMatchIn(string.text))
        assertTrue(string.text, string.text.startsWith("Today "))
        assertEquals("Today".length, boldLength(string))
    }

    @Test
    fun `yesterday renders the Yesterday word`() {
        val date = daysAgo(1)
        val string = date.chatPageMessageSeparatorAttributedDateString

        assertNotNull(string)
        assertTrue(string!!.text, string.text.startsWith("Yesterday "))
        assertEquals("Yesterday".length, boldLength(string))
    }

    @Test
    fun `a date within the week renders its weekday name`() {
        val date = daysAgo(3)
        val string = date.chatPageMessageSeparatorAttributedDateString

        assertNotNull(string)
        val weekday = date.weekdayString()
        assertTrue(string!!.text, string.text.startsWith("$weekday "))
        assertEquals(weekday.length, boldLength(string))
    }

    @Test
    fun `a week-old date on the same weekday renders the under-year form`() {
        val date = daysAgo(7)
        val string = date.chatPageMessageSeparatorAttributedDateString

        assertNotNull(string)
        val expectedPrefix = SimpleDateFormat("E MMM d,", Locale.US).format(date)
        assertTrue(string!!.text, string.text.startsWith("$expectedPrefix "))
        assertEquals(expectedPrefix.length, boldLength(string))
    }

    @Test
    fun `a date within the year renders the under-year form`() {
        val date = daysAgo(40)
        val string = date.chatPageMessageSeparatorAttributedDateString

        assertNotNull(string)
        val expectedPrefix = SimpleDateFormat("E MMM d,", Locale.US).format(date)
        assertTrue(string!!.text, string.text.startsWith("$expectedPrefix "))
    }

    @Test
    fun `a date over a year old renders the over-year form`() {
        val date = daysAgo(400)
        val string = date.chatPageMessageSeparatorAttributedDateString

        assertNotNull(string)
        val expectedPrefix = SimpleDateFormat("MMM dd yyyy,", Locale.US).format(date)
        assertTrue(string!!.text, string.text.startsWith("$expectedPrefix "))
        assertEquals(expectedPrefix.length, boldLength(string))
    }

    // MARK: - Auxiliary

    private fun boldLength(string: androidx.compose.ui.text.AnnotatedString): Int =
        string.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }.maxOf { it.end - it.start }

    private fun daysAgo(days: Int): Date =
        Calendar
            .getInstance()
            .apply {
                time = Date()
                add(Calendar.DAY_OF_YEAR, -days)
            }.time

    private companion object {
        val TIME_REGEX = Regex("\\d{1,2}:\\d{2}[\\s\\u202F]?(AM|PM)")
    }
}
