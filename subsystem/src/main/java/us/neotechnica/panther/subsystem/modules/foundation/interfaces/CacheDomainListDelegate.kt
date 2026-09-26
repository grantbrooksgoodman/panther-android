//
//  CacheDomainListDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain

/**
 * A delegate that supplies app-specific cache domains to the
 * subsystem.
 *
 * Register an implementation at launch and return the cache domains
 * the app defines. The subsystem merges these with its own built-in
 * domains so that operations like clear-all-caches cover the entire
 * app.
 */
interface CacheDomainListDelegate {
    /** The cache domains defined by the host app. */
    val appCacheDomains: List<CacheDomain>
}
