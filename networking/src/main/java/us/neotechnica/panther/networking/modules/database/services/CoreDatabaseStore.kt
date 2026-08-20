//
//  CoreDatabaseStore.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.services

import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * An in-memory cache for database query results.
 *
 * [CoreDatabaseStore] stores [DataSample] instances keyed by
 * their database path. Expired samples are automatically
 * discarded on retrieval. The database implementation uses this
 * store internally to support cache-strategy behavior; you can
 * also interact with it directly to manage cached data.
 */
object CoreDatabaseStore {
    // MARK: - Properties

    private val store = LockIsolated(mapOf<String, DataSample>())

    // MARK: - Methods

    /** Stores a data sample in the cache for the specified key. */
    fun addValue(
        value: DataSample,
        key: String,
    ) {
        store.withValue { it.value = it.value + (key to value) }
    }

    /** Stores multiple data samples in the cache in a single operation. */
    fun addValues(values: Map<String, DataSample>) {
        if (values.isEmpty()) return
        store.withValue { it.value = it.value + values }
    }

    /** Removes all cached data samples from the store. */
    fun clearStore() {
        store.withValue { it.value = mapOf() }
    }

    /** Removes all data samples that do not satisfy the given predicate. */
    fun filter(isIncluded: (Map.Entry<String, DataSample>) -> Boolean) {
        store.withValue { it.value = it.value.filter(isIncluded) }
    }

    /**
     * Returns the cached data for the specified key, or `null` if
     * no unexpired sample exists.
     *
     * If the stored sample has expired, it is removed from the
     * store.
     */
    fun getValue(key: String): Any? =
        store.withValue {
            val sample = it.value[key]
            if (sample == null || sample.isExpired) {
                it.value = it.value - key
                null
            } else {
                sample.data
            }
        }

    /** Removes the cached data sample for the specified key. */
    fun removeValue(key: String) {
        store.withValue { it.value = it.value - key }
    }
}
