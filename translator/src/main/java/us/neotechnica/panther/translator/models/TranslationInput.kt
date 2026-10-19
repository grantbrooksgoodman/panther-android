//
//  TranslationInput.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.interfaces.Validatable
import us.neotechnica.panther.translator.services.DataDetector

/**
 * The source text for a translation request.
 *
 * A `TranslationInput` supports an optional [alternate] string
 * that, when provided, takes precedence over the [original]
 * during translation, letting a pre-processed variant be
 * translated while the unmodified original is retained.
 *
 * ```kotlin
 * val input = TranslationInput("Hello, world!")
 * val sanitized = TranslationInput("Hello, world!", alternate = "Hello world")
 * ```
 */
data class TranslationInput(
    /** The original source text provided at initialization. */
    val original: String,
    /**
     * An optional replacement string to translate in place of
     * [original].
     */
    val alternate: String? = null,
) : Validatable {
    // MARK: - Computed Properties

    /**
     * The string that translation uses: [alternate] if present,
     * else [original].
     */
    val value: String get() = alternate ?: original

    override val isWellFormed: Boolean get() = !value.isBlank()

    /**
     * A copy whose detected addresses, links, and phone numbers are
     * wrapped in the processing delimiter, so translation preserves
     * them.
     *
     * Detected spans never overlap; a phone number is tokenized only
     * when it is plausibly formatted. Postal addresses are detected
     * only when the system text classifier is reachable.
     */
    val withTokenizedDetectorAttributes: TranslationInput
        get() = withTokenizedDetectorAttributes(DataDetector.shared)

    // MARK: - Auxiliary

    internal fun withTokenizedDetectorAttributes(dataDetector: DataDetector): TranslationInput {
        val delimiter = Translator.Constants.PROCESSING_DELIMITER
        var stringValue = value

        for (taggableString in dataDetector.matches(value)) {
            stringValue =
                stringValue.replace(
                    taggableString,
                    "$delimiter$taggableString$delimiter",
                )
        }

        if (stringValue == value) return this
        return TranslationInput(stringValue)
    }
}
