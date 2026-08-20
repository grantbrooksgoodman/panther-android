//
//  LanguagePairExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.extensions

import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.LanguagePair

/**
 * The system language pair: English to the app's active language.
 *
 * Display-string resolution translates against this pair. When the
 * active language is English the pair is idempotent, so no translation
 * is performed.
 */
val LanguagePair.Companion.system: LanguagePair
    get() = LanguagePair(from = "en", to = RuntimeStorage.languageCode)
