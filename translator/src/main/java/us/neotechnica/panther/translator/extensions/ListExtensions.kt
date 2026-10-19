//
//  ListExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.extensions

import us.neotechnica.panther.translator.models.TranslationInput

/**
 * A Boolean value that indicates whether the list is non-empty and
 * every element passes validation.
 *
 * This property returns `true` when the list contains at least one
 * element and every [TranslationInput] in the list satisfies
 * [TranslationInput.isWellFormed]. An empty list always returns
 * `false`.
 */
val List<TranslationInput>.isWellFormed: Boolean
    get() = isNotEmpty() && all { it.isWellFormed }
