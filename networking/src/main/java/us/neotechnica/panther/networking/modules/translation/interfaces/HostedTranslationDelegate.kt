//
//  HostedTranslationDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.interfaces

import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.networking.modules.translation.models.ArchiveStrategy
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * The app's entry point for translating against the hosted archive.
 *
 * The delegate coordinates the local archive, the hosted RTDB archive,
 * and the underlying
 * [TranslationService][us.neotechnica.panther.translator.services.TranslationService]:
 * it resolves display strings, translates single inputs and batches,
 * and reads and writes hosted-archive entries.
 */
interface HostedTranslationDelegate {
    /**
     * Reads a translation from the hosted archive by its input hash.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if no archived translation exists.
     */
    suspend fun findArchivedTranslation(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation

    /**
     * Translates multiple inputs for the specified language pair.
     *
     * @param inputs The translation inputs to translate.
     * @param languagePair The language pair for the translations.
     * @param hudConfig An optional HUD configuration specifying
     *   how long to wait before showing the HUD and whether it is
     *   modal.
     *
     * @return A list of translations corresponding to the inputs.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if the translation fails.
     */
    suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
        hudConfig: HUDConfig? = null,
    ): List<Translation>

    /** The `(key, value)` hosted-archive entry for a translation, or `null` if ineligible. */
    fun hostedArchiveEntry(translation: Translation): Pair<String, Any>?

    /** Resolves a page's label strings for the active language. */
    suspend fun resolve(strings: TranslatedLabelStrings): List<TranslationOutputMap>

    /**
     * Translates a single input, consulting and updating the
     * archives.
     *
     * @param input The translation input to translate.
     * @param languagePair The language pair for the translation.
     * @param hudConfig An optional HUD configuration specifying
     *   how long to wait before showing the HUD and whether it is
     *   modal.
     * @param archiveStrategy The strategy that determines when the
     *   translation is written to the hosted archive.
     *
     * @return The completed translation.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if the translation fails.
     */
    suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
        hudConfig: HUDConfig? = null,
        archiveStrategy: ArchiveStrategy = ArchiveStrategy.IMMEDIATE,
    ): Translation
}
