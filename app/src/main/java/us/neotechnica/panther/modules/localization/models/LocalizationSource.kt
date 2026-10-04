//
//  LocalizationSource.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.localization.models

/**
 * The table a localized string is resolved from.
 *
 * Each source maps to a bundled JSON asset of the form
 * `{ key: { languageCode: value } }`, generated from the source localization tables.
 */
enum class LocalizationSource(
    val assetName: String,
) {
    /** App-level strings. */
    APP("localization/localized_strings_app.json"),

    /** Subsystem-level strings. */
    SUBSYSTEM("localization/localized_strings_subsystem.json"),
}
