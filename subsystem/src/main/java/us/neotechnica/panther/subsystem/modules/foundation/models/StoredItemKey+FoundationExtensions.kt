//
//  StoredItemKey+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * The key for the app's active language code.
 *
 * Holds the explicitly chosen language code for the current session.
 * Falls back to the device language when no value is stored.
 */
val StoredItemKey.Companion.languageCode: StoredItemKey
    get() = StoredItemKey("languageCode")

/**
 * The key for the mapping of supported language codes to language
 * names.
 *
 * Holds the `language_codes` table loaded from the subsystem's
 * localized strings, stored at launch for later localized lookup.
 */
val StoredItemKey.Companion.languageCodeDictionary: StoredItemKey
    get() = StoredItemKey("languageCodeDictionary")

/**
 * The key for an override of the app's active language code.
 *
 * When present, this value takes precedence over
 * [languageCode][StoredItemKey.Companion.languageCode], forcing
 * translation and localization to the overridden language regardless
 * of the user's stored preference.
 */
val StoredItemKey.Companion.overriddenLanguageCode: StoredItemKey
    get() = StoredItemKey("overriddenLanguageCode")
