//
//  Translation.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

import us.neotechnica.panther.translator.interfaces.Validatable

/**
 * A completed translation: the original input, the translated
 * output, and the language pair that produced it.
 *
 * Translation values are returned by
 * [TranslationService][us.neotechnica.panther.translator.services.TranslationService]
 * and cached by the archivers.
 */
data class Translation(
    /** The original input that was translated. */
    val input: TranslationInput,
    /** The translated text. */
    val output: String,
    /** The language pair used to produce this translation. */
    val languagePair: LanguagePair,
) : Validatable {
    /**
     * A Boolean value indicating whether this translation is valid:
     * a well-formed input and language pair, and a non-blank output.
     */
    override val isWellFormed: Boolean
        get() {
            val isInputValid = input.isWellFormed
            val isLanguagePairValid = languagePair.isWellFormed
            val isOutputValid = TranslationInput(output).isWellFormed
            return isInputValid && isLanguagePairValid && isOutputValid
        }
}
