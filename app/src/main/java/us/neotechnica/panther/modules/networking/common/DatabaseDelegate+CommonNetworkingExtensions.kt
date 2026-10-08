//
//  DatabaseDelegate+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.bundle.conversations
import us.neotechnica.panther.bundle.populatedTemporaryCaches
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.networking.modules.database.services.CoreDatabaseStore
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import kotlin.time.Duration.Companion.milliseconds

/**
 * Removes the temporary database cache entries populated by
 * [populateTemporaryCaches].
 */
fun DatabaseDelegate.clearTemporaryCaches() {
    CoreDatabaseStore.filter { it.value.expiryThreshold != TEMPORARY_CACHE_EXPIRY_MILLIS }
}

/**
 * Loads the conversations and users into a short-lived in-memory
 * database snapshot.
 *
 * This method has no effect once the snapshot has been populated
 * during the current app session.
 *
 * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
 *   if reading the database fails.
 */
suspend fun DatabaseDelegate.populateTemporaryCaches() {
    if (RuntimeStorage.populatedTemporaryCaches) return

    val conversationData: Map<String, Any?> = getValues<Map<String, Any?>>(NetworkPath.conversations.rawValue)
    val userData: Map<String, Any?> = getValues<Map<String, Any?>>(NetworkPath.users.rawValue)
    val environment = Networking.config.environment.shortString

    conversationData.forEach { (key, value) ->
        if (value == null) return@forEach
        CoreDatabaseStore.addValue(
            DataSample(value, TEMPORARY_CACHE_EXPIRY_MILLIS),
            "$environment/${NetworkPath.conversations.rawValue}/$key",
        )
    }

    userData.forEach { (key, value) ->
        if (value == null) return@forEach
        CoreDatabaseStore.addValue(
            DataSample(value, TEMPORARY_CACHE_EXPIRY_MILLIS),
            "$environment/${NetworkPath.users.rawValue}/$key",
        )
    }

    if (Build.milestone != Build.Milestone.GENERAL_RELEASE) {
        Toast.show(
            Toast(
                Toast.ToastType.Capsule(ToastStyle.INFO),
                message = "Established database snapshot.",
                perpetuation = Toast.PerpetuationStrategy.Ephemeral(SNAPSHOT_TOAST_MILLIS.milliseconds),
            ),
        )
    }

    Logger.log("Established database snapshot.", domain = LoggerDomain.Networking.database)
    RuntimeStorage.store(true, StoredItemKey.populatedTemporaryCaches)
}

/**
 * Performs the given operation with the given global cache strategy
 * applied, restoring the previous strategy afterward.
 *
 * @param strategy The global cache strategy to apply for the duration
 *   of the operation.
 * @param body The operation to perform.
 *
 * @return The value returned by the operation.
 */
suspend fun <T> DatabaseDelegate.withGlobalCacheStrategy(
    strategy: CacheStrategy,
    body: suspend () -> T,
): T {
    setGlobalCacheStrategy(strategy)
    return try {
        body()
    } finally {
        setGlobalCacheStrategy(null)
    }
}

private const val SNAPSHOT_TOAST_MILLIS = 1500L
private const val TEMPORARY_CACHE_EXPIRY_MILLIS = 300_000L
