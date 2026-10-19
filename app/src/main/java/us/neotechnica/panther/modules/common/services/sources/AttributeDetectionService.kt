//
//  AttributeDetectionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.neotechnica.panther.modules.common.interfaces.AttributeDetectionServiceDelegate
import us.neotechnica.panther.modules.common.models.DetectedAttribute
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Use [AttributeDetectionService] to handle taps on attributes
 * detected in text.
 *
 * The service detects dates, links, and phone numbers in a
 * message's text and notifies its delegate when the user taps
 * one. Register your own conformance with [registerDelegate] to
 * customize how selections are handled; otherwise,
 * [DefaultAttributeDetectionServiceDelegate] is used.
 */
object AttributeDetectionService {
    // MARK: - Types

    /** The detectors the service runs over a text. */
    internal class Dependencies(
        val urlPattern: Pattern = Patterns.WEB_URL,
        val phonePattern: Pattern = Patterns.PHONE,
        val isGlobalPhoneNumber: (String) -> Boolean = PhoneNumberUtils::isGlobalPhoneNumber,
        val classifiedAttributes: (String) -> List<DetectedAttribute> = ::classifiedAttributes,
    )

    // MARK: - Properties

    private var appContext: Context? = null
    private var delegate: AttributeDetectionServiceDelegate? = DefaultAttributeDetectionServiceDelegate
    private var dependencies: Dependencies? = null

    private val cachedAttributesForMessageIDs = LockIsolated(mapOf<String, Pair<String, List<DetectedAttribute>>>())

    // MARK: - Initialization

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /** Replaces the detectors and forgets every cached result. */
    internal fun setDependencies(dependencies: Dependencies?) {
        this.dependencies = dependencies
        cachedAttributesForMessageIDs.wrappedValue = emptyMap()
    }

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
     * Returns the dates, links, and phone numbers detected in the
     * given message's text, computing them off the main thread
     * once per message.
     *
     * @param text The text to inspect.
     * @param messageID The identifier of the message the text
     *   belongs to, under which the result is cached.
     *
     * @return The detected attributes, each with the character
     *   range it occupies.
     */
    suspend fun attributes(
        text: String,
        messageID: String,
    ): List<DetectedAttribute> {
        cachedAttributes(text, messageID)?.let { return it }
        val attributes = withContext(Dispatchers.Default) { detectAttributes(text) }
        cachedAttributesForMessageIDs.withValue { it.value = it.value + (messageID to (text to attributes)) }
        return attributes
    }

    /**
     * Returns the attributes already computed for the given message's
     * text, or `null` if none have been.
     */
    fun cachedAttributes(
        text: String,
        messageID: String,
    ): List<DetectedAttribute>? = cachedAttributesForMessageIDs.wrappedValue[messageID]?.takeIf { it.first == text }?.second

    /**
     * Returns the dates, links, and phone numbers detected in the
     * given text, in order of appearance.
     *
     * @param text The text to inspect.
     *
     * @return The detected attributes, each with the character
     *   range it occupies.
     */
    fun detectAttributes(text: String): List<DetectedAttribute> {
        if (text.isEmpty()) return emptyList()
        val dependencies = resolvedDependencies()

        val attributes = mutableListOf<DetectedAttribute>()

        fun add(attribute: DetectedAttribute) {
            if (!attributes.overlaps(attribute.range)) attributes.add(attribute)
        }

        dependencies.classifiedAttributes(text).forEach(::add)
        matchRanges(dependencies.urlPattern, text).forEach { range ->
            add(DetectedAttribute.URL(range, normalizedURL(text.substring(range))))
        }
        matchRanges(dependencies.phonePattern, text)
            .filter { isPlausiblePhoneNumber(text.substring(it), dependencies) }
            .forEach { add(DetectedAttribute.PhoneNumber(it, text.substring(it).trim())) }

        return attributes.sortedBy { it.range.first }
    }

    // MARK: - Handle Gesture

    /**
     * Handles a tap on the given text, notifying the delegate of
     * every detected attribute the tapped character falls on.
     *
     * @param text The text that was tapped.
     * @param characterIndex The index of the tapped character.
     */
    fun handleGesture(
        text: String,
        characterIndex: Int,
    ) {
        for (attribute in detectAttributes(text)) {
            if (characterIndex in attribute.range) handleSelection(attribute, text)
        }
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

    private fun resolvedDependencies(): Dependencies = dependencies ?: Dependencies().also { dependencies = it }

    private fun List<DetectedAttribute>.overlaps(range: IntRange): Boolean =
        any { it.range.first <= range.last && range.first <= it.range.last }

    // Plausibly formatted: an optional leading plus, then digits,
    // dots, and dashes once the grouping spaces and parentheses are
    // set aside, with at least seven digits in all.
    private fun isPlausiblePhoneNumber(
        match: String,
        dependencies: Dependencies,
    ): Boolean =
        dependencies.isGlobalPhoneNumber(match.filterNot { it.isWhitespace() || it == '(' || it == ')' }) &&
            match.count { it.isDigit() } >= MINIMUM_PHONE_NUMBER_DIGITS

    private fun matchRanges(
        pattern: Pattern,
        text: String,
    ): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            if (!matcher.group().isNullOrBlank()) ranges.add(matcher.start() until matcher.end())
        }

        return ranges
    }

    private fun normalizedURL(value: String): String =
        if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            value
        } else {
            "https://$value"
        }

    // The date, link, and phone-number spans the system text
    // classifier finds, or none when no classifier is reachable. A
    // date span is kept only when its text parses in the device
    // language or in English.
    private fun classifiedAttributes(text: String): List<DetectedAttribute> {
        val context = appContext ?: return emptyList()
        return runCatching {
            val manager = context.getSystemService(TextClassificationManager::class.java) ?: return emptyList()
            val request =
                TextLinks.Request
                    .Builder(text)
                    .setEntityConfig(
                        TextClassifier.EntityConfig.createWithExplicitEntityList(
                            listOf(
                                TextClassifier.TYPE_DATE,
                                TextClassifier.TYPE_DATE_TIME,
                                TextClassifier.TYPE_PHONE,
                                TextClassifier.TYPE_URL,
                            ),
                        ),
                    ).build()

            manager.textClassifier
                .generateLinks(request)
                .links
                .mapNotNull { link ->
                    val range = link.start until link.end
                    val substring = text.substring(range)
                    when (link.getEntity(0)) {
                        TextClassifier.TYPE_DATE, TextClassifier.TYPE_DATE_TIME ->
                            parseDate(substring)?.let { DetectedAttribute.Date(range, it) }

                        TextClassifier.TYPE_PHONE -> DetectedAttribute.PhoneNumber(range, substring.trim())

                        TextClassifier.TYPE_URL -> DetectedAttribute.URL(range, normalizedURL(substring))

                        else -> null
                    }
                }
        }.getOrDefault(emptyList())
    }

    /**
     * Parses a date the classifier found, trying the device
     * language's standard styles and then the English patterns.
     */
    internal fun parseDate(value: String): Date? {
        val trimmed = value.trim().replace(ORDINAL_SUFFIX_REGEX, "")
        val formatters =
            DATE_STYLES.map { DateFormat.getDateInstance(it, Locale.getDefault()) } +
                DATE_PATTERNS.map { SimpleDateFormat(it, Locale.US) }
        for (formatter in formatters) {
            formatter.isLenient = false
            val parsed =
                try {
                    formatter.parse(trimmed)
                } catch (_: ParseException) {
                    null
                } ?: continue

            // Patterns without a year parse into 1970; shift those to the
            // current year.
            val calendar = Calendar.getInstance().apply { time = parsed }
            if (calendar.get(Calendar.YEAR) == EPOCH_YEAR) {
                calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                return calendar.time
            }

            return parsed
        }

        return null
    }

    private const val EPOCH_YEAR = 1970
    private const val MINIMUM_PHONE_NUMBER_DIGITS = 7

    private val DATE_STYLES = listOf(DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL)

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

    private val ORDINAL_SUFFIX_REGEX = Regex("(?<=\\d)(st|nd|rd|th)", RegexOption.IGNORE_CASE)
}
