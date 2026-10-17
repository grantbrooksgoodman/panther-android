//
//  Locale+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import androidx.core.os.LocaleListCompat
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver

/**
 * The two-letter language code of the device, falling back to
 * `"en"` when unavailable.
 *
 * The code is the first of the device's preferred languages that
 * the app localizes; when the app localizes none of them, it is the
 * device's primary language. Legacy codes resolve to their current
 * forms (for example, `he` rather than `iw`).
 */
val systemLanguageCode: String
    get() =
        systemLanguageCode(
            preferredLanguageTags = preferredLanguageTags,
            supportedLanguageCodes = LocalizedStringResolver.supportedLanguageCodes,
        )

internal fun systemLanguageCode(
    preferredLanguageTags: List<String>,
    supportedLanguageCodes: Set<String>,
): String {
    val languageCodes = preferredLanguageTags.map { it.substringBefore("-") }
    val languageCode =
        languageCodes.firstOrNull { it in supportedLanguageCodes }
            ?: languageCodes.firstOrNull()

    if (languageCode == null || languageCode.length < MINIMUM_LANGUAGE_CODE_LENGTH) return FALLBACK_LANGUAGE_CODE
    return languageCode.take(MINIMUM_LANGUAGE_CODE_LENGTH)
}

// The device's preferred languages as language tags, the app's
// adjusted default first.
private val preferredLanguageTags: List<String>
    get() = (LocaleListCompat.getAdjustedDefault().languageTags + LocaleListCompat.getDefault().languageTags).distinct()

private val LocaleListCompat.languageTags: List<String>
    get() = (0 until size()).mapNotNull { index -> get(index)?.toLanguageTag() }

private const val FALLBACK_LANGUAGE_CODE = "en"
private const val MINIMUM_LANGUAGE_CODE_LENGTH = 2
