//
//  MetadataService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Reads app configuration values hosted in the remote database.
 *
 * Each value is persisted across launches and served immediately
 * through its corresponding property. Call [resolveValues] to
 * revalidate the persisted snapshot against the network, so callers
 * converge on authoritative data without blocking on the fetch.
 *
 * **Note:** this port resolves the values the forced-update flow
 * consumes – the app share link, App Store build number, and
 * force-update flag; the remaining hosted values (prevarication
 * mode, storage reference URL, redirection key) arrive with the
 * services that consume them.
 */
object MetadataService {
    // MARK: - Types

    /** The hosted keys the service reads from [NetworkPath.shared]. */
    enum class MetadataServiceKey(
        val rawValue: String,
    ) {
        APP_SHARE_LINK("appShareLink"),
        APP_STORE_BUILD_NUMBER("appStoreBuildNumber"),
        SHOULD_FORCE_UPDATE("shouldForceUpdate"),
    }

    // MARK: - Computed Properties

    /** The app's share link, or `null` if it has not been resolved. */
    val appShareLink: String?
        get() = Persistent.string(PersistentStorageKey.appShareLink)

    /** The App Store build number, or `null` if it has not been resolved. */
    val appStoreBuildNumber: Int?
        get() = Persistent.int(PersistentStorageKey.appStoreBuildNumber)

    /** A Boolean value that indicates whether the app should force an update. */
    val shouldForceUpdate: Boolean
        get() = Persistent.boolean(PersistentStorageKey.shouldForceUpdate)

    private val canRevalidate: Boolean
        get() = appShareLink == null || appStoreBuildNumber == null

    // MARK: - Resolve All Values

    /**
     * Revalidates the hosted values against the network,
     * overwriting the persisted snapshot.
     *
     * The persisted values are served immediately on launch;
     * revalidation runs only while a value remains unresolved.
     *
     * @throws Exception If fetching fails, or if a hosted value is
     *   missing or of an unexpected type.
     */
    suspend fun resolveValues() {
        if (!canRevalidate) return
        assignValues(
            Networking.config.databaseDelegate.getValues(
                NetworkPath.shared.rawValue,
                prependingEnvironment = false,
                cacheStrategy = CacheStrategy.RETURN_CACHE_ON_FAILURE,
            ),
        )
    }

    // MARK: - Auxiliary

    private fun assignValues(dictionary: Map<String, Any>) {
        val appShareLink =
            dictionary[MetadataServiceKey.APP_SHARE_LINK.rawValue] as? String
                ?: throw Exception("Failed to read hosted app share link.", metadata = ExceptionMetadata(this))
        val appStoreBuildNumber =
            (dictionary[MetadataServiceKey.APP_STORE_BUILD_NUMBER.rawValue] as? Number)?.toInt()
                ?: throw Exception("Failed to read hosted App Store build number.", metadata = ExceptionMetadata(this))
        val shouldForceUpdate =
            dictionary[MetadataServiceKey.SHOULD_FORCE_UPDATE.rawValue] as? Boolean
                ?: throw Exception("Failed to read hosted force-update flag.", metadata = ExceptionMetadata(this))

        Persistent.setString(PersistentStorageKey.appShareLink, appShareLink)
        Persistent.setInt(PersistentStorageKey.appStoreBuildNumber, appStoreBuildNumber)
        Persistent.setBoolean(PersistentStorageKey.shouldForceUpdate, shouldForceUpdate)
    }
}
