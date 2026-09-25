//
//  AlertKitTranslationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.TranslationDelegate
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Supplies AlertKit's translations by forwarding to the hosted
 * translation archive.
 */
object AlertKitTranslationService : TranslationDelegate {
    override suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
    ): List<Translation> = Networking.config.hostedTranslationDelegate.getTranslations(inputs, languagePair)
}
