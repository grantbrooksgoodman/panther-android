//
//  Locale+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import java.util.Locale

/**
 * Returns the localized name of the given region in this locale.
 *
 * @param regionCode The region code whose name to return.
 *
 * @return The region's localized name; otherwise, `null` if the
 *   platform has no name for the region.
 */
fun Locale.localizedString(regionCode: String): String? {
    val displayName =
        runCatching {
            Locale
                .Builder()
                .setRegion(regionCode)
                .build()
                .getDisplayCountry(this)
        }.getOrNull()

    return displayName?.takeIf { it.isNotBlank() && !it.equals(regionCode, ignoreCase = true) }
}
