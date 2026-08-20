//
//  StoredItemKey.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A key that identifies a value in
 * [RuntimeStorage][us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage].
 *
 * Declare app-specific keys as top-level constants:
 *
 * ```kotlin
 * val CURRENT_SESSION_STORED_ITEM_KEY = StoredItemKey("currentSession")
 * ```
 */
@JvmInline
value class StoredItemKey(
    /** The key's raw string identifier. */
    val rawValue: String,
)
