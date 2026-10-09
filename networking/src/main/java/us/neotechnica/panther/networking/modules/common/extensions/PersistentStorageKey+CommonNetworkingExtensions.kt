//
//  PersistentStorageKey+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey

// MARK: - Types

/** The persistent storage keys scoped to the networking module. */
enum class NetworkingStorageKey(
    val rawValue: String,
) {
    IS_NETWORK_ACTIVITY_INDICATOR_ENABLED("isNetworkActivityIndicatorEnabled"),
    NETWORK_ENVIRONMENT("networkEnvironment"),
}

// MARK: - Methods

/** Returns the persistent storage key for the specified networking key. */
fun PersistentStorageKey.Companion.networking(key: NetworkingStorageKey): PersistentStorageKey = PersistentStorageKey(key.rawValue)
