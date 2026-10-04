//
//  Localized.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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

/**
 * The localized value for this key in [languageCode], resolved from
 * [source].
 *
 * Falls back to English and then to [LocalizedStringResolver.MISSING].
 * Use this overload to localize content for a specific recipient's
 * language, such as a push notification's body.
 *
 * @param languageCode The language to resolve for.
 * @param source The table to resolve from; defaults to
 *   [LocalizationSource.APP].
 */
fun LocalizedStringKey.localized(
    languageCode: String,
    source: LocalizationSource = LocalizationSource.APP,
): String = LocalizedStringResolver.string(this, source, languageCode)
