//
//  CacheDomain+CommonNetworkingExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.networking.modules.database.services.CoreDatabaseStore
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain

/** The framework-layer networking cache domains. */
object NetworkingCacheDomains {
    /** The domain for the database read cache. */
    val database = CacheDomain("database") { CoreDatabaseStore.clearStore() }
}

/** The framework-layer networking cache domains. */
val CacheDomain.Companion.Networking: NetworkingCacheDomains
    get() = NetworkingCacheDomains
