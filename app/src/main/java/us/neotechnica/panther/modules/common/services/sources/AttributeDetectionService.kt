//
//  AttributeDetectionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import us.neotechnica.panther.modules.common.interfaces.AttributeDetectionServiceDelegate
import us.neotechnica.panther.modules.common.models.DetectedAttribute
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Use [AttributeDetectionService] to handle taps on attributes detected
 * in text.
 *
 * The service detects dates, links, and phone numbers in a message's
 * text and notifies its delegate when the user taps one. Register your
 * own conformance with [registerDelegate] to customize how selections
 * are handled; otherwise, [DefaultAttributeDetectionServiceDelegate]
 * is used.
 */
object AttributeDetectionService {
    // MARK: - Properties

    private var delegate: AttributeDetectionServiceDelegate? = DefaultAttributeDetectionServiceDelegate

    // MARK: - Register Delegate

    /**
     * Registers the delegate that handles attribute selections.
     *
     * Registering a new delegate replaces any existing one.
     *
     * @param delegate The delegate to register.
     */
    fun registerDelegate(delegate: AttributeDetectionServiceDelegate) {
        this.delegate = delegate
    }

    // MARK: - Detect Attributes

    /**
     * Returns the dates, links, and phone numbers detected in the given
     * text, in order of appearance.
     *
     * @param text The text to inspect.
     *
     * @return The detected attributes, each with the character range it
     *   occupies.
     */
    fun detectAttributes(text: String): List<DetectedAttribute> {
        if (text.isEmpty()) return emptyList()

        val attributes = mutableListOf<DetectedAttribute>()
        URL_REGEX.findAll(text).forEach { match ->
            attributes.add(DetectedAttribute.URL(match.range, normalizedURL(match.value)))
        }

        PHONE_NUMBER_REGEX.findAll(text).forEach { match ->
            val digits = match.value.filter { it.isDigit() }
            if (digits.length in MINIMUM_PHONE_NUMBER_DIGITS..MAXIMUM_PHONE_NUMBER_DIGITS && !attributes.overlaps(match.range)) {
                attributes.add(DetectedAttribute.PhoneNumber(match.range, match.value.trim()))
            }
        }

        DATE_REGEX.findAll(text).forEach { match ->
            val date = parseDate(match.value) ?: return@forEach
            if (!attributes.overlaps(match.range)) attributes.add(DetectedAttribute.Date(match.range, date))
        }

        return attributes.sortedBy { it.range.first }
    }

    // MARK: - Handle Gesture

    /**
     * Handles a tap on the given text, notifying the delegate if the
     * tapped character falls on a detected attribute.
     *
     * @param text The text that was tapped.
     * @param characterIndex The index of the tapped character.
     */
    fun handleGesture(
        text: String,
        characterIndex: Int,
    ) {
        val attribute = detectAttributes(text).firstOrNull { characterIndex in it.range } ?: return
        handleSelection(attribute, text)
    }

    /**
     * Notifies the delegate of a selected attribute.
     *
     * @param attribute The selected attribute.
     * @param text The complete text containing the attribute.
     */
    fun handleSelection(
        attribute: DetectedAttribute,
        text: String,
    ) {
        val substring = text.substring(attribute.range.first, (attribute.range.last + 1).coerceAtMost(text.length))
        when (attribute) {
            is DetectedAttribute.Date -> delegate?.didSelectDate(attribute.date, substring, text)
            is DetectedAttribute.PhoneNumber -> delegate?.didSelectPhoneNumber(attribute.phoneNumber)
            is DetectedAttribute.URL -> delegate?.didSelectURL(attribute.url, substring, text)
        }
    }

    // MARK: - Auxiliary

    private fun List<DetectedAttribute>.overlaps(range: IntRange): Boolean =
        any { it.range.first <= range.last && range.first <= it.range.last }

    private fun normalizedURL(value: String): String =
        if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            value
        } else {
            "https://$value"
        }

    private fun parseDate(value: String): Date? {
        val trimmed = value.trim().replace(ORDINAL_SUFFIX_REGEX, "")
        for (pattern in DATE_PATTERNS) {
            val formatter = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val parsed =
                try {
                    formatter.parse(trimmed)
                } catch (_: ParseException) {
                    null
                } ?: continue

            // Patterns without a year parse into 1970; shift those to the
            // current year.
            if (!pattern.contains('y')) {
                val calendar = Calendar.getInstance().apply { time = parsed }
                calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                return calendar.time
            }

            return parsed
        }

        return null
    }

    private const val MAXIMUM_PHONE_NUMBER_DIGITS = 15
    private const val MINIMUM_PHONE_NUMBER_DIGITS = 7

    private val DATE_PATTERNS =
        listOf(
            "MM/dd/yyyy",
            "MM/dd/yy",
            "MM/dd",
            "MMMM d, yyyy",
            "MMMM d yyyy",
            "MMM d, yyyy",
            "MMM d yyyy",
            "MMMM d",
            "MMM d",
            "d MMMM yyyy",
            "d MMM yyyy",
            "d MMMM",
            "d MMM",
        )

    private val DATE_REGEX =
        Regex(
            "\\b(\\d{1,2}/\\d{1,2}(/\\d{2,4})?|" +
                "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\\.? \\d{1,2}(st|nd|rd|th)?(,? \\d{4})?|" +
                "\\d{1,2}(st|nd|rd|th)? (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\\.?( \\d{4})?)\\b",
            RegexOption.IGNORE_CASE,
        )

    private val ORDINAL_SUFFIX_REGEX = Regex("(?<=\\d)(st|nd|rd|th)", RegexOption.IGNORE_CASE)

    private val PHONE_NUMBER_REGEX =
        Regex("(?<!\\d)(\\+?\\d{1,3}[\\s.-]?)?(\\(?\\d{2,4}\\)?[\\s.-]?)\\d{3,4}[\\s.-]?\\d{3,4}(?!\\d)")

    private val URL_REGEX =
        Regex(
            "\\b((https?://|www\\.)[^\\s<>\"']+|" +
                "[a-z0-9-]+(\\.[a-z0-9-]+)*\\.(com|org|net|edu|gov|io|co|us|uk|de|fr|es|it|app|dev|me|ai)(/[^\\s<>\"']*)?)",
            RegexOption.IGNORE_CASE,
        )
}
