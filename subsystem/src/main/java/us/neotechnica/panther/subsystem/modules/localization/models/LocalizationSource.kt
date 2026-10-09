//
//  LocalizationSource.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.localization.models

/**
 * The table a localized string is resolved from.
 *
 * Each source maps to a bundled JSON asset of the form
 * `{ key: { languageCode: value } }`, generated from the source
 * localization tables. The subsystem maintains its own table,
 * separate from the app's, so subsystem components can resolve the
 * built-in [SubsystemStringKey] values without the app including
 * those keys in its own table.
 */
enum class LocalizationSource(
    val assetName: String,
) {
    /** App-level strings. */
    APP("localization/localized_strings_app.json"),

    /** Subsystem-level strings. */
    SUBSYSTEM("localization/localized_strings_subsystem.json"),
}
