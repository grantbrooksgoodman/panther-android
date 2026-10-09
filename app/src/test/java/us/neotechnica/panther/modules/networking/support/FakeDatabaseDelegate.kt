//
//  FakeDatabaseDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.support

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.models.QueryStrategy
import kotlin.time.Duration

/**
 * A recording [DatabaseDelegate] for tests.
 *
 * Captures every [commit], [setValue], and [updateChildValues] call and
 * lets a test seed the value a [runTransaction] block operates on, the
 * values [getValues] returns, and the snapshots [observe] emits.
 */
class FakeDatabaseDelegate : DatabaseDelegate {
    // MARK: - Recorded Operations

    val committedUpdates = mutableListOf<Map<String, Any?>>()
    val setValues = mutableListOf<Pair<Any?, String>>()
    val childValueUpdates = mutableListOf<Pair<String, Map<String, Any?>>>()

    /** The value passed to the next [runTransaction] block. */
    var transactionSeed: Any? = null

    /** The value returned by the last [runTransaction] block. */
    var lastTransactionResult: Any? = null

    /** The value returned by [getValues] for paths absent from [getValuesResults]. */
    var getValuesResult: Any? = null

    /** The values returned by [getValues], keyed by path. */
    val getValuesResults = mutableMapOf<String, Any?>()

    /** The paths passed to [getValues] so far. */
    val getValuesPaths = mutableListOf<String>()

    /** The flows served by [observe], keyed by path. */
    val observedFlows = mutableMapOf<String, MutableSharedFlow<Any>>()

    // MARK: - DatabaseDelegate

    override suspend fun commit(updates: Map<String, Any?>) {
        committedUpdates.add(updates)
    }

    override suspend fun setValue(
        value: Any?,
        key: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        setValues.add(value to key)
    }

    override suspend fun updateChildValues(
        key: String,
        data: Map<String, Any?>,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        childValueUpdates.add(key to data)
    }

    override suspend fun runTransaction(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
        block: (Any?) -> Any?,
    ): Any? {
        val result = block(transactionSeed)
        lastTransactionResult = result
        return result
    }

    override suspend fun getValues(
        path: String,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Any {
        getValuesPaths.add(path)
        if (getValuesResults.containsKey(path)) {
            return checkNotNull(getValuesResults[path]) { "getValuesResults[$path] is null." }
        }

        return checkNotNull(getValuesResult) { "getValuesResult has not been seeded." }
    }

    override fun isEncodable(value: Any?): Boolean =
        when (value) {
            null, is String, is Boolean, is Number -> true
            is List<*> -> value.all { isEncodable(it) }
            is Map<*, *> -> value.values.all { isEncodable(it) }
            else -> false
        }

    override fun generateKey(path: String): String = "-fakeGeneratedKey"

    override fun observe(
        path: String,
        prependingEnvironment: Boolean,
    ): Flow<Any> = flow(path)

    override suspend fun awaitRealtimeConnection(timeout: Duration): Boolean = true

    override fun prewarm() = Unit

    override suspend fun increment(
        path: String,
        delta: Int,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) = Unit

    override suspend fun queryValues(
        path: String,
        strategy: QueryStrategy,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): Any = error("queryValues is not supported by FakeDatabaseDelegate.")

    override fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) = Unit

    // MARK: - Observation

    /** Emits [value] to the observers of [path]; the latest value replays to late observers. */
    suspend fun emit(
        path: String,
        value: Any,
    ) {
        flow(path).emit(value)
    }

    private fun flow(path: String): MutableSharedFlow<Any> =
        synchronized(observedFlows) {
            observedFlows.getOrPut(path) { MutableSharedFlow(replay = 1, extraBufferCapacity = 16) }
        }
}
