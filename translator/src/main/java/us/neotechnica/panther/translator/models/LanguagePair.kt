//
//  LanguagePair.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

import us.neotechnica.panther.translator.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.translator.interfaces.Validatable

/**
 * A pair of ISO 639-1 language codes representing the source and
 * target languages for a translation.
 *
 * Create a language pair from two-character codes:
 *
 * ```kotlin
 * val pair = LanguagePair(from = "en", to = "fr")
 * ```
 *
 * or by parsing a hyphenated string with [fromString]:
 *
 * ```kotlin
 * val pair = LanguagePair.fromString("en-fr")
 * ```
 *
 * Both [from] and [to] must be exactly two characters to pass
 * [isWellFormed].
 */
data class LanguagePair(
    /** The ISO 639-1 code of the source language. */
    val from: String,
    /** The ISO 639-1 code of the target language. */
    val to: String,
) : Validatable {
    // MARK: - Computed Properties

    /**
     * A Boolean value indicating whether the source and target
     * languages are identical.
     */
    val isIdempotent: Boolean get() = from == to

    /** A hyphenated `"from-to"` representation, e.g. `"en-fr"`. */
    val string: String get() = "$from-$to"

    override val isWellFormed: Boolean
        get() {
            val isFromValid = !from.isBlank() && from.length == 2
            val isToValid = !to.isBlank() && to.length == 2
            return isFromValid && isToValid
        }

    // MARK: - Companion

    companion object {
        /**
         * Creates a language pair by parsing a hyphenated string.
         *
         * The expected format is `"from-to"`. A single component is
         * used as both source and target, producing an idempotent
         * pair. Returns `null` if the string is empty.
         *
         * @param string A hyphenated language-pair string to parse.
         */
        fun fromString(string: String): LanguagePair? {
            val components = string.split("-")
            if (components.isEmpty()) return null

            val fromValue = components[0].lowercasedTrimmingWhitespaceAndNewlines()
            if (components.size <= 1) {
                return LanguagePair(from = fromValue, to = fromValue)
            }

            val toValue =
                components
                    .subList(1, components.size)
                    .joinToString("")
                    .lowercasedTrimmingWhitespaceAndNewlines()
            return LanguagePair(from = fromValue, to = toValue)
        }
    }
}
