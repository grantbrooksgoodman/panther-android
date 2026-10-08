//
//  TranslatedLabelStringCollection.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

/**
 * A key identifying one label string within a page's translated
 * string collection.
 *
 * **Note:** the key is carried as a plain string.
 */
@JvmInline
value class TranslatedLabelStringCollection(
    /** The stable identifier for the label string. */
    val rawValue: String,
)
