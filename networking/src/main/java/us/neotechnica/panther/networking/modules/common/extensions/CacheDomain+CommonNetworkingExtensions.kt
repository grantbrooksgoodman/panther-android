//
//  CacheDomain+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.database.services.CoreDatabaseStore
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain

/** The framework-layer networking cache domains. */
object NetworkingCacheDomains {
    /** The domain for the database read cache. */
    val database = CacheDomain("database") { CoreDatabaseStore.clearStore() }

    /** The domain for the storage download and existence caches. */
    val storage = CacheDomain("storage") { Networking.config.storageDelegate.clearStore() }
}

/** The framework-layer networking cache domains. */
val CacheDomain.Companion.Networking: NetworkingCacheDomains
    get() = NetworkingCacheDomains
