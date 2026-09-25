//
//  TranslationDelegateProtocol.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.interfaces

import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * An interface for providing translation services to AlertKit.
 *
 * Implement this interface to supply translations for alert content.
 * AlertKit calls [getTranslations] before presenting any alert whose
 * translation keys are non-empty. Register an implementation through
 * [AlertKitConfig.registerTranslationDelegate][us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig.registerTranslationDelegate].
 */
interface TranslationDelegate {
    /**
     * Translates the given inputs into the target language.
     *
     * @param inputs The translation inputs to translate.
     * @param languagePair The source and target languages.
     *
     * @return The completed translations.
     *
     * @throws Exception if the translation operation fails.
     */
    suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
    ): List<Translation>
}
