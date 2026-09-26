//
//  PermanentPersistentStorageKeyDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey

/**
 * A delegate that declares which persistent storage keys should
 * survive a reset.
 *
 * Register an implementation at launch and return the keys the app
 * must preserve across calls to
 * [Persistent.reset][us.neotechnica.panther.subsystem.modules.foundation.services.Persistent].
 */
interface PermanentPersistentStorageKeyDelegate {
    /**
     * The keys that should be preserved during a persistent storage
     * reset.
     */
    val permanentKeys: List<PersistentStorageKey>
}
