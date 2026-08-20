//
//  TranslationOutputMapExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.extensions

import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap

/**
 * The resolved, sanitized value for the given label-string [key], or
 * the key's own identifier if no entry matches.
 *
 * Mirrors the iOS `[TranslationOutputMap].value(for:)` accessor the
 * page reducers use to read their resolved strings.
 */
fun List<TranslationOutputMap>.value(key: TranslatedLabelStringCollection): String =
    (firstOrNull { it.key == key }?.value ?: key.key).sanitized
