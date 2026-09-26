//
//  CoreUtilities.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain

/**
 * Cross-cutting utilities shared across the app.
 */
object CoreUtilities {
    // MARK: - Methods

    /**
     * Clears the given cache domains, or every registered cache
     * domain when none are specified.
     *
     * @param domains The domains to clear, or `null` to clear all
     *   registered domains.
     */
    fun clearCaches(domains: List<CacheDomain>? = null) {
        (domains ?: CacheDomain.allCases).forEach { it.clear() }
    }
}
