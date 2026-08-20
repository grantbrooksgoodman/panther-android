//
//  Translatorable.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.interfaces

import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.models.TranslationPlatform

/**
 * A single-platform translator.
 *
 * Each [TranslationPlatform] vends a [Translatorable] instance that
 * performs one translation against that platform, either through a
 * direct HTTP API or the web-view harness.
 */
internal interface Translatorable {
    /** The platform this translator targets. */
    val platform: TranslationPlatform

    /**
     * Translates the given input into the target language.
     *
     * @param input The text to translate.
     * @param languagePair The source and target languages.
     *
     * @return The completed translation.
     */
    suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): us.neotechnica.panther.translator.models.Translation
}
