//
//  TranslationValidator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.reference
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
        inputs: List<TranslationInput>? = null,
        languagePair: LanguagePair? = null,
        translation: Translation? = null,
        metadata: ExceptionMetadata,
    ) {
        val userInfo = mutableMapOf<String, Any>()
        inputs?.let { userInfo["InputValues"] = it.joinToString(", ") { input -> input.value } }
        languagePair?.let { userInfo["LanguagePair"] = it.string }
        translation?.let { userInfo["TranslationReferenceHostingKey"] = it.reference.hostingKey }

        if (inputs != null && !inputs.all { it.isWellFormed }) {
            throw Exception.Networking.inputsFailValidation(userInfo, metadata)
        }

        if (languagePair != null && !languagePair.isWellFormed) {
            throw Exception.Networking.languagePairFailsValidation(userInfo, metadata)
        }

        if (translation != null && !translation.isWellFormed) {
            throw Exception.Networking.translationFailsValidation(userInfo, metadata)
        }
    }
}
