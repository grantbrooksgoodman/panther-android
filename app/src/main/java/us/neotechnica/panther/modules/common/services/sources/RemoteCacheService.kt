//
//  RemoteCacheService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import us.neotechnica.panther.bundle.invalidatedCaches
import us.neotechnica.panther.modules.common.models.RemoteCacheStatus
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency

/**
 * Use [RemoteCacheService] to read and write the remote cache
 * status of individual users.
 *
 * The remote cache status is stored per user in the remote
 * database, as a hosted list of the user IDs whose caches have been
 * invalidated.
 */
object RemoteCacheService {
    // MARK: - Dependencies

    private val database: DatabaseDelegate by Dependency { it.networking.database }

    // MARK: - Remote Cache Status Configuration

    /**
     * Returns the remote cache status for the given user.
     *
     * @param userID The ID of the user whose status to fetch.
     *
     * @return [RemoteCacheStatus.INVALID] if the user appears in the
     *   hosted invalidated caches list; otherwise,
     *   [RemoteCacheStatus.VALID].
     *
     * @throws Exception if fetching the list fails.
     */
    suspend fun cacheStatus(userID: String): RemoteCacheStatus {
        val invalidatedCaches: List<String> =
            database.getValues<List<String>>(
                NetworkPath.invalidatedCaches.rawValue,
                cacheStrategy = CacheStrategy.ADAPTIVE,
            )

        return if (invalidatedCaches.contains(userID)) RemoteCacheStatus.INVALID else RemoteCacheStatus.VALID
    }

    /**
     * Sets the remote cache status for the given user.
     *
     * The hosted invalidated caches list is modified atomically:
     * setting [RemoteCacheStatus.INVALID] adds the user to the list,
     * and setting [RemoteCacheStatus.VALID] removes them.
     *
     * @param cacheStatus The status to set.
     * @param userID The ID of the user whose status to set.
     *
     * @throws Exception if the update fails.
     */
    suspend fun setCacheStatus(
        cacheStatus: RemoteCacheStatus,
        userID: String,
    ) {
        database.runTransaction(
            NetworkPath.invalidatedCaches.rawValue,
        ) { currentValue ->
            val ids = (currentValue as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val updated =
                when (cacheStatus) {
                    RemoteCacheStatus.INVALID -> ids + userID
                    RemoteCacheStatus.VALID -> ids.filterNot { it == userID }
                }

            updated.distinct()
        }
    }
}
