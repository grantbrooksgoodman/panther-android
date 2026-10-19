//
//  TranslationOutputMapExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.extensions

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap

/**
 * The resolved, sanitized value for the given label-string [key], or
 * – if no entry matches – the key's source text from the owning
 * [strings], and failing that the key's own identifier.
 *
 * The page reducers use this to read their resolved strings.
 *
 * @param key The label-string key to resolve.
 * @param strings The collection the key belongs to.
 */
fun List<TranslationOutputMap>.value(
    key: TranslatedLabelStringCollection,
    strings: TranslatedLabelStrings,
): String {
    val value =
        firstOrNull { it.key == key }?.value
            ?: strings.keyPairs
                .firstOrNull { it.key == key }
                ?.input
                ?.value
            ?: key.rawValue
    return value.sanitized
}
