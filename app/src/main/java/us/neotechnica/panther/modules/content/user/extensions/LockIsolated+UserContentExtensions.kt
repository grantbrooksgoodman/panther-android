//
//  LockIsolated+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Removes every enqueued effect from the registry and returns it,
 * keyed by its identifier.
 *
 * @return The effects that were enqueued, or an empty map if none
 *   was.
 */
fun <ID> LockIsolated<Map<ID, () -> Unit>>.drainEffects(): Map<ID, () -> Unit> =
    withValue { ref ->
        val drained = ref.value
        if (drained.isNotEmpty()) ref.value = emptyMap()
        drained
    }
