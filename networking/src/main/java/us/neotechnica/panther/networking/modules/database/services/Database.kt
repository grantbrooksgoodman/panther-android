//
//  Database.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.services

import kotlinx.coroutines.flow.Flow
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.models.DatabaseOperation
import us.neotechnica.panther.networking.modules.database.models.QueryStrategy
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import kotlin.time.Duration

/**
 * The Firebase Realtime Database implementation of
 * [DatabaseDelegate].
 *
 * Read and write operations are coalesced by content so that
 * identical concurrent operations issue a single network request,
 * and results are cached per path with a short time-to-live.
 */
class Database : DatabaseDelegate {
    // MARK: - Properties

    private val coreDatabase = CoreDatabase()

    // MARK: - Atomic Increment

    override suspend fun increment(
        path: String,
        delta: Int,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        coreDatabase.increment(
            path,
            delta = delta,
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    // MARK: - Data Integrity Validation

    override fun isEncodable(value: Any?): Boolean = coreDatabase.isEncodable(value)

    // MARK: - Global Cache Strategy

    override fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) {
        coreDatabase.setGlobalCacheStrategy(globalCacheStrategy)
    }

    // MARK: - ID Key Generation

    override fun generateKey(path: String): String? = coreDatabase.generateKey(path)

    // MARK: - Observation

    override fun observe(
        path: String,
        prependingEnvironment: Boolean,
    ): Flow<Any> =
        coreDatabase.observe(
            path = path,
            prependingEnvironment = prependingEnvironment,
        )

    // MARK: - Prewarming

    override suspend fun awaitRealtimeConnection(timeout: Duration): Boolean =
        coreDatabase.awaitRealtimeConnection(
            timeout = timeout,
        )

    override fun prewarm() {
        coreDatabase.prewarm()
    }

    // MARK: - Transaction

    override suspend fun runTransaction(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
        block: (Any?) -> Any?,
    ): Any? =
        coreDatabase.runTransaction(
            path,
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
            block = block,
        )

    // MARK: - Value Retrieval

    override suspend fun getValues(
        path: String,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Any =
        coreDatabase.performOperation(
            DatabaseOperation.GetValues(
                path,
                cacheStrategy = cacheStrategy,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        ) ?: throw Exception(
            metadata = ExceptionMetadata(this),
        )

    override suspend fun queryValues(
        path: String,
        strategy: QueryStrategy,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Any =
        coreDatabase.performOperation(
            DatabaseOperation.QueryValues(
                path,
                strategy = strategy,
                cacheStrategy = cacheStrategy,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        ) ?: throw Exception(
            metadata = ExceptionMetadata(this),
        )

    // MARK: - Value Setting

    override suspend fun setValue(
        value: Any?,
        key: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        coreDatabase.performOperation(
            DatabaseOperation.SetValue(
                value,
                key = key,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    override suspend fun updateChildValues(
        key: String,
        data: Map<String, Any?>,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        coreDatabase.performOperation(
            DatabaseOperation.UpdateChildValues(
                key = key,
                data = data,
            ),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }
}
