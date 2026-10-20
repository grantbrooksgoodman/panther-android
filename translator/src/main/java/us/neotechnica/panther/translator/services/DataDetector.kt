//
//  DataDetector.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import us.neotechnica.panther.translator.Translator
import java.util.regex.Pattern

// Finds the addresses, links, and phone numbers in a string as
// non-overlapping substrings, keeping a phone number only when it
// is plausibly formatted.
internal class DataDetector(
    private val patterns: List<Pattern> = listOf(Patterns.EMAIL_ADDRESS, Patterns.WEB_URL),
    private val phonePattern: Pattern = Patterns.PHONE,
    private val isGlobalPhoneNumber: (String) -> Boolean = PhoneNumberUtils::isGlobalPhoneNumber,
    private val addressRanges: (String) -> List<IntRange> = ::classifiedAddressRanges,
) {
    // MARK: - Methods

    fun matches(string: String): List<String> {
        val ranges = mutableListOf<IntRange>()

        fun add(range: IntRange) {
            if (ranges.none { it.overlaps(range) }) ranges.add(range)
        }

        addressRanges(string).forEach(::add)
        patterns.forEach { pattern -> matchRanges(pattern, string).forEach(::add) }
        matchRanges(phonePattern, string)
            .filter { isPlausiblePhoneNumber(string.substring(it)) }
            .forEach(::add)

        return ranges.sortedBy { it.first }.map { string.substring(it) }
    }

    // MARK: - Auxiliary

    // Plausibly formatted: an optional leading plus, then digits,
    // dots, and dashes once the grouping spaces and parentheses are
    // set aside, with at least seven digits in all.
    private fun isPlausiblePhoneNumber(match: String): Boolean =
        isGlobalPhoneNumber(match.filterNot { it.isWhitespace() || it == '(' || it == ')' }) &&
            match.count { it.isDigit() } >= MINIMUM_PHONE_NUMBER_DIGITS

    private fun matchRanges(
        pattern: Pattern,
        string: String,
    ): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val matcher = pattern.matcher(string)
        while (matcher.find()) {
            if (!matcher.group().isNullOrBlank()) ranges.add(matcher.start() until matcher.end())
        }

        return ranges
    }

    private fun IntRange.overlaps(other: IntRange): Boolean = first <= other.last && other.first <= last

    // MARK: - Companion

    companion object {
        val shared by lazy { DataDetector() }

        private const val MINIMUM_PHONE_NUMBER_DIGITS = 7
    }
}

// The address spans the system text classifier finds, or none when
// no classifier is reachable.
private fun classifiedAddressRanges(string: String): List<IntRange> {
    val context =
        Translator.config.currentActivityProvider
            ?.invoke()
            ?.applicationContext ?: return emptyList()
    return runCatching {
        val manager = context.getSystemService(TextClassificationManager::class.java) ?: return emptyList()
        val request =
            TextLinks.Request
                .Builder(string)
                .setEntityConfig(explicitEntityConfig(listOf(TextClassifier.TYPE_ADDRESS)))
                .build()

        manager.textClassifier
            .generateLinks(request)
            .links
            .filter { link -> (0 until link.entityCount).any { link.getEntity(it) == TextClassifier.TYPE_ADDRESS } }
            .map { it.start until it.end }
    }.getOrDefault(emptyList())
}

// A classifier configuration that finds exactly the given entity
// types.
private fun explicitEntityConfig(entityTypes: List<String>): TextClassifier.EntityConfig =
    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
        TextClassifier.EntityConfig
            .Builder()
            .setIncludedTypes(entityTypes)
            .includeTypesFromTextClassifier(false)
            .build()
    } else {
        @Suppress("DEPRECATION")
        TextClassifier.EntityConfig.createWithExplicitEntityList(entityTypes)
    }
