//
//  Primitives+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.ceil

// MARK: - Float

/** A formatted duration string in `H:MM:SS` or `M:SS` format. */
val Float.durationString: String
    get() {
        if (this < SECONDS_PER_MINUTE) {
            return String.format(Locale.US, "0:%02d", ceil(this).toInt())
        } else if (this < SECONDS_PER_HOUR) {
            return String.format(Locale.US, "%02d:%02d", (this / SECONDS_PER_MINUTE).toInt(), toInt() % SECONDS_PER_MINUTE)
        }

        val hours = (this / SECONDS_PER_HOUR).toInt()
        val remainingMinutesInSeconds = toInt() - hours * SECONDS_PER_HOUR
        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            hours,
            remainingMinutesInSeconds / SECONDS_PER_MINUTE,
            remainingMinutesInSeconds % SECONDS_PER_MINUTE,
        )
    }

// MARK: - String

/**
 * A Boolean value that indicates whether the string contains at least
 * one letter.
 */
val String.containsLetters: Boolean
    get() = any { it.isLetter() }

/**
 * The exonym of the language this string identifies, or `null` if the
 * string is not a known language code.
 *
 * For example, "Spanish" for the code `es` on devices with English
 * language codes.
 */
val String.languageExonym: String?
    get() {
        val languageName = languageName ?: return null
        val components = languageName.split(" (")
        if (components.isEmpty()) return languageName
        return components[0].trim()
    }

/**
 * The display name of the language this string identifies, or `null`
 * if the string is not a known language code.
 *
 * For example, "Spanish (Español)" for the code `es` on devices with
 * English language codes.
 */
val String.languageName: String?
    get() {
        if (isEmpty() || lowercasedTrimmingWhitespaceAndNewlines.isEmpty()) return null
        val languageCodes = CoreUtilities.localizedLanguageCodeDictionary(RuntimeStorage.languageCode) ?: return null
        val name = languageCodes[this] ?: languageCodes[lowercasedTrimmingWhitespaceAndNewlines] ?: return null
        return name.trim()
    }

/** The string lowercased with whitespace and newlines removed. */
val String.lowercasedTrimmingWhitespaceAndNewlines: String
    get() = lowercase().trim()

// Derives a four-character code from the string: stop words
// removed, letters only, lowercased, SHA-256, first two + last
// two hex characters, uppercased.
internal val String.errorCode: String
    get() {
        if (isEmpty()) return "0000"

        val stopWords =
            setOf(
                "a",
                "an",
                "is",
                "that",
                "the",
                "this",
                "was",
            )

        val joinedWords =
            split(" ")
                .filter { it.lowercase() !in stopWords }
                .joinToString("")

        val lettersOnly =
            joinedWords
                .replace(Regex("[^A-Za-z]"), "")
                .lowercase()

        val digest = MessageDigest.getInstance("SHA-256")
        val hexString =
            digest
                .digest(lettersOnly.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        return (hexString.take(2) + hexString.takeLast(2)).uppercase()
    }

// MARK: - Auxiliary

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60
