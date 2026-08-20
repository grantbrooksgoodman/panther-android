//
//  TranslationInput.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

import android.util.Patterns
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.interfaces.Validatable

/**
 * The source text for a translation request.
 *
 * A `TranslationInput` supports an optional [alternate] string that,
 * when provided, takes precedence over the [original] during
 * translation, letting a pre-processed variant be translated while
 * the unmodified original is retained.
 *
 * ```kotlin
 * val input = TranslationInput("Hello, world!")
 * val sanitized = TranslationInput("Hello, world!", alternate = "Hello world")
 * ```
 */
data class TranslationInput(
    /** The original source text provided at initialization. */
    val original: String,
    /** An optional replacement string to translate in place of [original]. */
    val alternate: String? = null,
) : Validatable {
    // MARK: - Computed Properties

    /** The string that translation uses: [alternate] if present, else [original]. */
    val value: String get() = alternate ?: original

    override val isWellFormed: Boolean get() = !value.isBlank()

    /**
     * A copy whose detected links and phone numbers are wrapped in
     * the processing delimiter, so translation preserves them.
     *
     * **Note:** the iOS original also detects postal addresses via
     * `NSDataDetector`; Android has no equivalent detector, so
     * addresses are not tokenized here.
     */
    val withTokenizedDetectorAttributes: TranslationInput
        get() {
            val delimiter = Translator.Constants.PROCESSING_DELIMITER
            var stringValue = value

            for (taggableString in detectedTaggableSubstrings(value)) {
                stringValue =
                    stringValue.replace(
                        taggableString,
                        "$delimiter$taggableString$delimiter",
                    )
            }

            if (stringValue == value) return this
            return TranslationInput(stringValue)
        }

    // MARK: - Auxiliary

    private fun detectedTaggableSubstrings(string: String): List<String> {
        val results = mutableListOf<String>()
        for (pattern in listOf(Patterns.WEB_URL, Patterns.PHONE)) {
            val matcher = pattern.matcher(string)
            while (matcher.find()) {
                val match = matcher.group()
                if (!match.isNullOrBlank()) results.add(match)
            }
        }
        return results
    }
}
