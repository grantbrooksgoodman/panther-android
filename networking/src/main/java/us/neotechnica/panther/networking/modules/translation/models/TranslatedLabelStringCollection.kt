//
//  TranslatedLabelStringCollection.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

/**
 * A key identifying one label string within a page's translated
 * string collection.
 *
 * **Note:** the iOS original is a namespaced enum extended per page
 * with strongly-typed nested key enums. Until the pages are ported
 * (Phase 5+), this Android port carries the key as a plain string.
 */
@JvmInline
value class TranslatedLabelStringCollection(
    /** The stable identifier for the label string. */
    val key: String,
)
