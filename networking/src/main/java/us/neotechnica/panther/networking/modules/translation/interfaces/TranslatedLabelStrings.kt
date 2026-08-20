//
//  TranslatedLabelStrings.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.interfaces

import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap

/**
 * A page's collection of translatable label strings.
 *
 * A view exposes its user-facing strings as a [TranslatedLabelStrings]
 * so a reducer can resolve them for the active language in one call to
 * [HostedTranslationDelegate.resolve][us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate.resolve].
 */
interface TranslatedLabelStrings {
    /** The source key/input pairs for this collection. */
    val keyPairs: List<TranslationInputMap>

    /** The untranslated output map, used as a fallback. */
    val defaultOutputMap: List<TranslationOutputMap>
        get() = keyPairs.map { it.defaultOutputMap }
}

/**
 * A single translatable label-string key.
 *
 * The [alternate] supplies a pre-processed variant to translate in
 * place of the raw key text, when one is needed.
 */
interface TranslatedLabelStringKey {
    /** An optional pre-processed variant to translate. */
    val alternate: String?
}
