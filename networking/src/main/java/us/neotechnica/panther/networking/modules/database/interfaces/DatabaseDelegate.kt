//
//  DatabaseDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.interfaces

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.database.models.QueryStrategy
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import kotlin.time.Duration

/**
 * An interface for reading, writing, and observing data in the
 * network database.
 *
 * Use [DatabaseDelegate] to perform operations on the backend
 * database. Values can be read, written, queried, updated, and
 * observed at key paths, with built-in caching and configurable
 * timeouts.
 *
 * By default, paths are prefixed with the active
 * [NetworkEnvironment][us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment]
 * to isolate data across environments. Pass `false` for
 * `prependingEnvironment` to use a raw path.
 *
 * A default implementation backed by Firebase Realtime Database
 * is provided automatically. It coalesces identical concurrent
 * operations – when multiple callers perform the same operation
 * at the same time, only one network request is made and all
 * callers receive the same result.
 */
interface DatabaseDelegate {
    // MARK: - Companion

    companion object {
        /** The default result limit for [queryValues]. */
        const val DEFAULT_QUERY_LIMIT = 10
    }

    // MARK: - Methods

    /**
     * Suspends until the realtime connection is established, or
     * the given timeout elapses.
     *
     * Callers that must read authoritative server data – and
     * never act on a possibly-stale local cache – use this to
     * confirm connectivity before proceeding.
     *
     * @param timeout The maximum time to wait for the connection.
     *
     * @return `true` if the connection was established within the
     *   timeout; otherwise, `false`.
     */
    suspend fun awaitRealtimeConnection(timeout: Duration): Boolean

    /**
     * Generates a unique key at the specified path.
     *
     * @param path The database path at which to generate a key.
     *
     * @return A unique key string, or `null` if generation fails.
     */
    fun generateKey(path: String): String?

    /**
     * Reads the value stored at the specified path.
     *
     * @param path The database path to read from.
     * @param prependingEnvironment Whether the active environment
     *   is prepended to the path.
     * @param cacheStrategy The caching behavior for this operation.
     * @param timeout The maximum time to wait before timing out.
     *
     * @return The value stored at the path.
     *
     * @throws Exception if the read fails.
     */
    suspend fun getValues(
        path: String,
        prependingEnvironment: Boolean = true,
        cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
        timeout: Duration = Networking.defaultOperationTimeout,
    ): Any

    /**
     * Atomically increments a numeric value at the specified path
     * by the given delta, entirely on the server.
     *
     * @param path The database path containing the numeric value.
     * @param delta The integer amount to add to the current value.
     * @param prependingEnvironment Whether the active environment
     *   is prepended to the path.
     * @param timeout The maximum time to wait before timing out.
     *
     * @throws Exception if the increment fails.
     */
    suspend fun increment(
        path: String,
        delta: Int,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Returns a Boolean value that indicates whether the specified
     * value can be stored in the database.
     *
     * Values must be a `String`, `Boolean`, `Number`, `null`, or a
     * `List` or `Map` whose elements are themselves encodable.
     *
     * @param value The value to evaluate.
     *
     * @return `true` if the value is encodable; otherwise, `false`.
     */
    fun isEncodable(value: Any?): Boolean

    /**
     * Returns a flow that emits the value at the specified path
     * each time it changes.
     *
     * The flow attaches a real-time observer to the database and
     * removes it when the collector is cancelled. Each emitted
     * value also updates the in-memory cache used by [getValues].
     *
     * @param path The database path to observe.
     * @param prependingEnvironment Whether the active environment
     *   is prepended to the path.
     *
     * @return A flow that emits values as they change.
     */
    fun observe(
        path: String,
        prependingEnvironment: Boolean = true,
    ): Flow<Any>

    /**
     * Establishes the underlying connection to the database
     * without performing a data operation.
     */
    fun prewarm()

    /**
     * Queries a limited subset of values at the specified path.
     *
     * @param path The database path to query.
     * @param strategy The query strategy that determines which
     *   results to return.
     * @param prependingEnvironment Whether the active environment
     *   is prepended to the path.
     * @param cacheStrategy The caching behavior for this operation.
     * @param timeout The maximum time to wait before timing out.
     *
     * @return The queried values.
     *
     * @throws Exception if the query fails.
     */
    suspend fun queryValues(
        path: String,
        strategy: QueryStrategy = QueryStrategy.First(DEFAULT_QUERY_LIMIT),
        prependingEnvironment: Boolean = true,
        cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
        timeout: Duration = Networking.defaultOperationTimeout,
    ): Any

    /**
     * Executes a transaction at the specified path.
     *
     * A transaction reads the current value, passes it to `block`,
     * and attempts to commit the block's return value, retrying if
     * the value changes between the read and the commit.
     * Transactions bypass operation coalescing.
     *
     * @param path The database path to run the transaction against.
     * @param prependingEnvironment Whether the active environment
     *   is prepended to the path.
     * @param timeout The maximum time to wait before timing out.
     * @param block A closure that receives the current value and
     *   returns the new value to commit.
     *
     * @return The committed value, or `null` if the transaction
     *   committed a deletion.
     *
     * @throws Exception if the transaction fails.
     */
    suspend fun runTransaction(
        path: String,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
        block: (Any?) -> Any?,
    ): Any?

    /**
     * Overrides the cache strategy for all database operations, or
     * clears the override when `null`.
     *
     * @param globalCacheStrategy The cache strategy to apply
     *   globally, or `null` to clear the override.
     */
    fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?)

    /**
     * Writes a value to the database at the specified key.
     *
     * @param value The value to write. Must be encodable.
     * @param key The database key to write to.
     * @param prependingEnvironment Whether the active environment
     *   is prepended to the key.
     * @param timeout The maximum time to wait before timing out.
     *
     * @throws Exception if the write fails.
     */
    suspend fun setValue(
        value: Any?,
        key: String,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Updates specific child values at the specified key without
     * overwriting sibling data.
     *
     * @param key The database key whose children to update.
     * @param data A map of child keys and their new values.
     * @param prependingEnvironment Whether the active environment
     *   is prepended to the key.
     * @param timeout The maximum time to wait before timing out.
     *
     * @throws Exception if the update fails.
     */
    suspend fun updateChildValues(
        key: String,
        data: Map<String, Any?>,
        prependingEnvironment: Boolean = true,
        timeout: Duration = Networking.defaultOperationTimeout,
    )

    /**
     * Atomically writes a set of values across multiple database
     * paths in a single operation.
     *
     * Each key is a slash-separated path relative to the active
     * environment (for example, `"conversations/<key>/hash"`).
     * Pass `null` as a value to delete the entry at a path.
     *
     * @param updates A map whose keys are environment-relative
     *   paths and whose values are the data to write.
     *
     * @throws Exception if the write fails or `updates` contains
     *   overlapping paths.
     */
    suspend fun commit(updates: Map<String, Any?>) {
        if (updates.isEmpty()) return

        val sortedKeys = updates.keys.sorted()
        for (index in 0 until sortedKeys.size - 1) {
            if (sortedKeys[index + 1].startsWith("${sortedKeys[index]}/")) {
                throw Exception(
                    "Overlapping fan-out paths: \"${sortedKeys[index]}\" " +
                        "is a prefix of \"${sortedKeys[index + 1]}\".",
                    metadata = ExceptionMetadata(this),
                )
            }
        }

        updateChildValues(
            key = Networking.config.environment.shortString,
            data = updates,
            prependingEnvironment = false,
        )
    }
}

/**
 * Reads the value stored at the specified path as the
 * specified type.
 *
 * @param path The database path to read from.
 * @param prependingEnvironment Whether the active environment
 *   is prepended to the path.
 * @param cacheStrategy The caching behavior for this operation.
 * @param timeout The maximum time to wait before timing out.
 *
 * @return The value stored at the path.
 *
 * @throws Exception if the read fails or the value cannot be
 *   cast to `T`.
 */
suspend inline fun <reified T> DatabaseDelegate.getValues(
    path: String,
    prependingEnvironment: Boolean = true,
    cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
    timeout: Duration = Networking.defaultOperationTimeout,
): T {
    val values =
        getValues(
            path,
            prependingEnvironment = prependingEnvironment,
            cacheStrategy = cacheStrategy,
            timeout = timeout,
        )

    return values as? T ?: throw Exception.Networking.typecastFailed(
        T::class.simpleName,
        metadata = ExceptionMetadata(this),
    )
}

/**
 * Returns a flow that emits the value at the specified path, as
 * the specified type, each time it changes.
 *
 * The flow attaches a real-time observer to the database and
 * removes it when the collector is cancelled. Each emitted
 * value also updates the in-memory cache used by [getValues].
 *
 * @param path The database path to observe.
 * @param prependingEnvironment Whether the active environment
 *   is prepended to the path.
 *
 * @return A flow that emits values of type `T` as they change.
 *
 * @throws Exception if an emitted value cannot be cast to `T`.
 */
inline fun <reified T> DatabaseDelegate.observe(
    path: String,
    prependingEnvironment: Boolean = true,
): Flow<T> =
    observe(
        path,
        prependingEnvironment = prependingEnvironment,
    ).map { value ->
        value as? T ?: throw Exception.Networking.typecastFailed(
            T::class.simpleName,
            metadata = ExceptionMetadata(this),
        )
    }

/**
 * Queries a limited subset of values at the specified path as
 * the specified type.
 *
 * @param path The database path to query.
 * @param strategy The query strategy that determines which
 *   results to return.
 * @param prependingEnvironment Whether the active environment
 *   is prepended to the path.
 * @param cacheStrategy The caching behavior for this operation.
 * @param timeout The maximum time to wait before timing out.
 *
 * @return The queried values.
 *
 * @throws Exception if the query fails or the values cannot be
 *   cast to `T`.
 */
suspend inline fun <reified T> DatabaseDelegate.queryValues(
    path: String,
    strategy: QueryStrategy = QueryStrategy.First(DatabaseDelegate.DEFAULT_QUERY_LIMIT),
    prependingEnvironment: Boolean = true,
    cacheStrategy: CacheStrategy = CacheStrategy.RETURN_CACHE_FIRST,
    timeout: Duration = Networking.defaultOperationTimeout,
): T {
    val values =
        queryValues(
            path,
            strategy = strategy,
            prependingEnvironment = prependingEnvironment,
            cacheStrategy = cacheStrategy,
            timeout = timeout,
        )

    return values as? T ?: throw Exception.Networking.typecastFailed(
        T::class.simpleName,
        metadata = ExceptionMetadata(this),
    )
}
