//
//  LocalizedStringKeyRepresentable.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.localization.interfaces
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource

/**
 * A type that can identify a localized string by a stable key.
 *
 * Implement [LocalizedStringKeyRepresentable] on a localization key
 * enum. The [referent] is the key used to look the string up in a
 * localized-strings table.
 */
interface LocalizedStringKeyRepresentable {
    /**
     * The key used to resolve this string in a localized-strings
     * table.
     */
    val referent: String

    /** The table this key resolves from by default. */
    val source: LocalizationSource
        get() = LocalizationSource.APP
}
