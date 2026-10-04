//
//  QueriedContactPairCache.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Manages the in-memory queried contact pair cache.
 *
 * Caches the contact pairs matching each search term so repeated
 * queries for the same term do not re-filter the archive.
 */
object QueriedContactPairCache {
    // MARK: - Properties

    /** A Boolean value that indicates whether new values may be written to the cache. */
    var canWriteToCache = false

    private val cachedContactPairsForSearchTerms = LockIsolated<Map<String, List<ContactPair>>?>(null)

    // MARK: - Methods

    /** Removes every cached contact pair. */
    fun clearCache() {
        cachedContactPairsForSearchTerms.wrappedValue = null
    }

    internal fun cachedValue(searchTerm: String): List<ContactPair>? = cachedContactPairsForSearchTerms.wrappedValue?.get(searchTerm)

    internal fun cache(
        searchTerm: String,
        contactPairs: List<ContactPair>,
    ) {
        val cache = (cachedContactPairsForSearchTerms.wrappedValue ?: emptyMap()).toMutableMap()
        cache[searchTerm] = contactPairs
        cachedContactPairsForSearchTerms.wrappedValue = cache
    }
}
