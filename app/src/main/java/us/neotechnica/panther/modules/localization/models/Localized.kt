//
//  Localized.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.localization.models

import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver

/**
 * The localized value for this key, resolved from [source].
 *
 * Resolves for the user's current language, falling back to English
 * and then to [LocalizedStringResolver.MISSING].
 *
 * @param source The table to resolve from; defaults to
 *   [LocalizationSource.APP].
 */
fun LocalizedStringKey.localized(source: LocalizationSource = LocalizationSource.APP): String = LocalizedStringResolver.string(this, source)
