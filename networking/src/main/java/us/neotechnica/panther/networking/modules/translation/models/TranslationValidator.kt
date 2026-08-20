//
//  TranslationValidator.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Validates translation inputs, language pairs, and results before
 * they are translated or archived, throwing an [Exception] on the
 * first malformed argument.
 */
internal object TranslationValidator {
    /**
     * Validates any of the provided arguments that are non-null.
     *
     * @throws Exception if any provided argument is malformed.
     */
    fun validate(
        sender: Any,
        inputs: List<TranslationInput>? = null,
        languagePair: LanguagePair? = null,
        translation: Translation? = null,
    ) {
        if (inputs != null && !inputs.all { it.isWellFormed }) {
            throw Exception("Translation inputs failed validation.", metadata = ExceptionMetadata(sender))
        }
        if (languagePair != null && !languagePair.isWellFormed) {
            throw Exception("Language pair failed validation.", metadata = ExceptionMetadata(sender))
        }
        if (translation != null && !translation.isWellFormed) {
            throw Exception("Translation failed validation.", metadata = ExceptionMetadata(sender))
        }
    }
}
