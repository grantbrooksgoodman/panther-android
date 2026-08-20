//
//  TranslationMap.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

import us.neotechnica.panther.networking.modules.translation.extensions.sanitized
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * A label-string key paired with its source [TranslationInput] – the
 * input to display-string resolution.
 */
data class TranslationInputMap(
    /** The label-string key this input resolves. */
    val key: TranslatedLabelStringCollection,
    /** The source text to translate. */
    val input: TranslationInput,
) {
    /**
     * The output map used when translation is unavailable: the
     * untranslated source, sanitized. English uses the original;
     * other languages use the (possibly alternate) value.
     */
    val defaultOutputMap: TranslationOutputMap
        get() =
            TranslationOutputMap(
                key = key,
                value = if (RuntimeStorage.languageCode == "en") input.original.sanitized else input.value.sanitized,
            )
}

/**
 * A label-string key paired with its resolved, translated value – the
 * output of display-string resolution.
 */
data class TranslationOutputMap(
    /** The label-string key this output resolves. */
    val key: TranslatedLabelStringCollection,
    /** The resolved, display-ready value. */
    val value: String,
)
