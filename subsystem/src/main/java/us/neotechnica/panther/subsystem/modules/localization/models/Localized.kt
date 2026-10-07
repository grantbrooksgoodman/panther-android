//
//  Localized.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.localization.models

import us.neotechnica.panther.subsystem.modules.localization.interfaces.LocalizedStringKeyRepresentable
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver

/**
 * A localized string resolved from a [LocalizationSource] by key and
 * language code at access time.
 *
 * Use [Localized] from subsystem and design-system code to resolve a
 * built-in [SubsystemStringKey], reading its [wrappedValue]:
 *
 * ```kotlin
 * val title = Localized(SubsystemStringKey.tryAgain).wrappedValue
 * ```
 *
 * The lookup is backed by an internal cache, so repeated accesses do
 * not re-read the table from disk.
 *
 * **Note:** If no translation is found for the requested language,
 * English is used as a fallback. If the English translation is also
 * missing, [wrappedValue] returns
 * [LocalizedStringResolver.MISSING].
 */
class Localized(
    private val key: LocalizedStringKeyRepresentable,
    private val languageCode: String = LocalizedStringResolver.languageCode,
    private val source: LocalizationSource = LocalizationSource.SUBSYSTEM,
) {
    /** The localized string for the specified key and language code. */
    val wrappedValue: String
        get() = LocalizedStringResolver.string(key, source, languageCode)
}

/**
 * The localized value for this key, resolved from [source].
 *
 * Resolves for the user's current language, falling back to English
 * and then to [LocalizedStringResolver.MISSING].
 *
 * @param source The table to resolve from; defaults to
 *   [LocalizationSource.APP].
 */
fun LocalizedStringKeyRepresentable.localized(source: LocalizationSource = LocalizationSource.APP): String =
    LocalizedStringResolver.string(this, source)

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
fun LocalizedStringKeyRepresentable.localized(
    languageCode: String,
    source: LocalizationSource = LocalizationSource.APP,
): String = LocalizedStringResolver.string(this, source, languageCode)
