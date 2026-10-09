//
//  CoreDatabase.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.services

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.prependingCurrentEnvironment
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.common.models.GuardedOperation
import us.neotechnica.panther.networking.modules.database.models.DatabaseOperation
import us.neotechnica.panther.networking.modules.database.models.QueryStrategy
import us.neotechnica.panther.networking.modules.health.models.HealthEvidence
import us.neotechnica.panther.networking.modules.health.models.HealthSampleToken
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration

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
    fun getValue(key: String): Any? {
        val data =
            store.withValue {
                val sample = it.value[key]
                if (sample == null || sample.isExpired) {
                    it.value = it.value - key
                    null
                } else {
                    sample.data
                }
            } ?: return null

        Logger.log(
            "Returning stored value for data at path \"$key\".",
            domain = LoggerDomain.caches,
        )

        return data
    }

    /** Removes the cached data sample for the specified key. */
    fun removeValue(key: String) {
        store.withValue { it.value = it.value - key }
    }
}

// This service exceeds the file-length and type-body-length limits.

@Suppress("LargeClass", "TooManyFunctions")
internal class CoreDatabase {
    // MARK: - Properties

    private val globalCacheStrategy = LockIsolated<CacheStrategy?>(null)

    private val reference by lazy { FirebaseDatabase.getInstance().reference }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - Companion

    private companion object {
        val coalescer = Coalescer<String, Result<Any?>>()
    }

    // MARK: - ID Key Generation

    fun generateKey(path: String): String? = reference.child(path).push().key

    // MARK: - Global Cache Strategy

    fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) {
        this.globalCacheStrategy.wrappedValue = globalCacheStrategy
    }

    // MARK: - Prewarming

    suspend fun awaitRealtimeConnection(timeout: Duration): Boolean {
        val connectedReference = reference.database.getReference(CONNECTED_PATH)
        val didFinish = LockIsolated(false)

        return suspendCancellableCoroutine { continuation ->
            lateinit var listener: ValueEventListener

            fun finish(connected: Boolean) {
                val shouldFinish =
                    didFinish.withValue {
                        if (it.value) {
                            false
                        } else {
                            it.value = true
                            true
                        }
                    }

                if (!shouldFinish) return
                connectedReference.removeEventListener(listener)
                if (continuation.isActive) continuation.resume(connected)
            }

            listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.getValue(Boolean::class.java) != true) return
                        finish(true)
                    }

                    override fun onCancelled(error: DatabaseError) = Unit
                }

            connectedReference.addValueEventListener(listener)
            continuation.invokeOnCancellation { finish(false) }

            scope.launch {
                delay(timeout)
                finish(false)
            }
        }
    }

    fun prewarm() {
        Logger.log(
            "Prewarming database connection.",
            domain = LoggerDomain.Networking.database,
        )

        // Retain a persistent observer on the special .info/connected
        // location until the realtime socket first reports connected,
        // then detach. A one-shot read fires on the immediate local
        // "false" and detaches without holding the connection
        // establishing; a retained observer forces the SDK to open and
        // keep the authenticated socket from launch, so it is ready
        // sooner for the first writes and for the observers that stream
        // fresh data. Long-lived connection tracking is owned by the
        // connection-stability observer, so this releases as soon as the
        // connection is up.
        val connectedReference = reference.database.getReference(CONNECTED_PATH)
        val listenerHolder = LockIsolated<ValueEventListener?>(null)
        val listener =
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) != true) return

                    // Atomically take the listener so only the first
                    // connected event detaches, and re-entrant events
                    // see null.
                    val listenerToRemove =
                        listenerHolder.withValue { holder ->
                            val current = holder.value
                            holder.value = null
                            current
                        } ?: return

                    connectedReference.removeEventListener(listenerToRemove)
                }

                override fun onCancelled(error: DatabaseError) = Unit
            }

        listenerHolder.wrappedValue = listener
        connectedReference.addValueEventListener(listener)
    }

    // MARK: - Data Integrity Validation

    fun isEncodable(value: Any?): Boolean =
        when (value) {
            null, is String, is Boolean, is Number -> true
            is List<*> -> value.all { isEncodable(it) }
            is Map<*, *> -> value.keys.all { it is String } && value.values.all { isEncodable(it) }
            else -> false
        }

    // MARK: - Atomic Increment

    suspend fun increment(
        path: String,
        delta: Int,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        val resolvedPath = if (prependingEnvironment) path.prependingCurrentEnvironment else path

        GuardedOperation.run(
            timeout = timeout,
            recordsCensoredSampleOnTimeout = true,
            showsActivityIndicator = true,
            sender = this,
        ) { healthToken ->
            Logger.log(
                "Incrementing value at path \"$resolvedPath\" by $delta.",
                domain = LoggerDomain.Networking.database,
            )

            measure(healthToken) {
                reference
                    .child(resolvedPath)
                    .setValue(ServerValue.increment(delta.toLong()))
                    .await()
            }
        }

        // Server-side increment produces an unknown local
        // result; invalidate the cache so the next read
        // fetches fresh.
        CoreDatabaseStore.removeValue(resolvedPath)
    }

    // MARK: - Observation

    fun observe(
        path: String,
        prependingEnvironment: Boolean,
    ): Flow<Any> =
        callbackFlow {
            val resolvedPath = if (prependingEnvironment) path.prependingCurrentEnvironment else path

            if (!Networking.isReadWriteEnabled) {
                close(Exception.Networking.readWriteAccessDisabled(ExceptionMetadata(this@CoreDatabase)))
                return@callbackFlow
            }

            Logger.log(
                "Started observing values at path \"$resolvedPath\".",
                domain = LoggerDomain.Networking.database,
            )

            // The indicator reflects the initial connection; it is
            // cleared once the first snapshot arrives or the
            // observation terminates, whichever comes first.
            Networking.config.activityIndicatorDelegate.show()

            val didHideActivity = LockIsolated(false)

            fun hideActivityIfNeeded() {
                val shouldHide =
                    didHideActivity.withValue {
                        if (it.value) {
                            false
                        } else {
                            it.value = true
                            true
                        }
                    }

                if (shouldHide) Networking.config.activityIndicatorDelegate.hide()
            }

            val child = reference.child(resolvedPath)
            val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        hideActivityIfNeeded()

                        val value = snapshot.value
                        if (value == null) {
                            close(
                                Exception(
                                    "No value exists at the specified key path.",
                                    userInfo = mapOf("Path" to resolvedPath),
                                    metadata = ExceptionMetadata(this@CoreDatabase),
                                ),
                            )
                            return
                        }

                        CoreDatabaseStore.addValue(
                            DataSample(value, Networking.cacheExpiryMilliseconds(System.currentTimeMillis())),
                            resolvedPath,
                        )

                        trySend(value)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        hideActivityIfNeeded()
                        close(Exception.from(error.toException(), ExceptionMetadata(this@CoreDatabase)))
                    }
                }

            child.addValueEventListener(listener)
            awaitClose {
                hideActivityIfNeeded()

                Logger.log(
                    "Stopped observing values at path \"$resolvedPath\".",
                    domain = LoggerDomain.Networking.database,
                )

                child.removeEventListener(listener)
            }
        }.buffer(Channel.UNLIMITED)

    // MARK: - Perform Operation

    suspend fun performOperation(
        operation: DatabaseOperation,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Any? {
        val resolvedOperation = operation.resolvingAdaptiveCacheStrategy()
        val resolvedGlobalRawValue = globalCacheStrategy.wrappedValue?.resolved?.rawValue ?: ""

        return coalescer
            .submitUnlessCancelled(
                "CoreDatabase.performOperation/" +
                    encodedHashOf(
                        listOf(
                            resolvedOperation.encodedHash +
                                resolvedGlobalRawValue +
                                prependingEnvironment.toString() +
                                timeout.toString(),
                        ),
                    ),
            ) {
                runCatching {
                    _performOperation(
                        resolvedOperation,
                        prependingEnvironment = prependingEnvironment,
                        timeout = timeout,
                    )
                }
            }.getOrThrow()
    }

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _performOperation(
        operation: DatabaseOperation,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Any? =
        GuardedOperation.run(
            timeout = timeout,
            recordsCensoredSampleOnTimeout = true,
            showsActivityIndicator = true,
            sender = this,
        ) { healthToken ->
            when (operation) {
                is DatabaseOperation.GetValues ->
                    getValues(
                        path = if (prependingEnvironment) operation.path.prependingCurrentEnvironment else operation.path,
                        cacheStrategy = (globalCacheStrategy.wrappedValue ?: operation.cacheStrategy).resolved,
                        healthToken = healthToken,
                    )

                is DatabaseOperation.QueryValues ->
                    queryValues(
                        path = if (prependingEnvironment) operation.path.prependingCurrentEnvironment else operation.path,
                        strategy = operation.strategy,
                        cacheStrategy = (globalCacheStrategy.wrappedValue ?: operation.cacheStrategy).resolved,
                        healthToken = healthToken,
                    )

                is DatabaseOperation.SetValue ->
                    setValue(
                        operation.value,
                        key = if (prependingEnvironment) operation.key.prependingCurrentEnvironment else operation.key,
                        healthToken = healthToken,
                    )

                is DatabaseOperation.UpdateChildValues ->
                    updateChildValues(
                        key = if (prependingEnvironment) operation.key.prependingCurrentEnvironment else operation.key,
                        data = operation.data,
                        healthToken = healthToken,
                    )
            }
        }

    // MARK: - Transaction

    suspend fun runTransaction(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
        block: (Any?) -> Any?,
    ): Any? {
        val resolvedPath = if (prependingEnvironment) path.prependingCurrentEnvironment else path

        val committedValue =
            GuardedOperation.run(
                timeout = timeout,
                recordsCensoredSampleOnTimeout = true,
                showsActivityIndicator = true,
                sender = this,
            ) { healthToken ->
                Logger.log(
                    "Running transaction at path \"$resolvedPath\".",
                    domain = LoggerDomain.Networking.database,
                )

                measure(healthToken) { runFirebaseTransaction(resolvedPath, block) }
            }

        if (committedValue == null) {
            CoreDatabaseStore.removeValue(resolvedPath)
        } else {
            CoreDatabaseStore.addValue(
                DataSample(committedValue, Networking.cacheExpiryMilliseconds(System.currentTimeMillis())),
                resolvedPath,
            )
        }

        return committedValue
    }

    // MARK: - Value Retrieval

    private suspend fun getValues(
        path: String,
        cacheStrategy: CacheStrategy,
        healthToken: HealthSampleToken,
    ): Any? {
        if (cacheStrategy == CacheStrategy.RETURN_CACHE_FIRST) {
            CoreDatabaseStore.getValue(path)?.let { return it }
        }

        Logger.log(
            "Getting values at path \"$path\".",
            domain = LoggerDomain.Networking.database,
        )

        val getValuesStartMillis = System.currentTimeMillis()
        try {
            val values = _getValues(path, healthToken)

            CoreDatabaseStore.addValue(
                DataSample(values, Networking.cacheExpiryMilliseconds(getValuesStartMillis)),
                path,
            )

            return values
        } catch (exception: Exception) {
            if (cacheStrategy == CacheStrategy.RETURN_CACHE_ON_FAILURE) {
                CoreDatabaseStore.getValue(path)?.let { return it }
            }

            throw exception
        }
    }

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    private suspend fun _getValues(
        path: String,
        healthToken: HealthSampleToken,
    ): Any =
        measure(healthToken) {
            reference
                .child(path)
                .get()
                .await()
                .value ?: throw Exception(
                "No value exists at the specified key path.",
                userInfo = mapOf("Path" to path),
                metadata = ExceptionMetadata(this),
            )
        }

    private suspend fun queryValues(
        path: String,
        strategy: QueryStrategy,
        cacheStrategy: CacheStrategy,
        healthToken: HealthSampleToken,
    ): Any? {
        if (cacheStrategy == CacheStrategy.RETURN_CACHE_FIRST) {
            CoreDatabaseStore.getValue(path)?.let { return it }
        }

        Logger.log(
            "Querying values at path \"$path\".",
            domain = LoggerDomain.Networking.database,
        )

        val queryValuesStartMillis = System.currentTimeMillis()
        val query =
            when (strategy) {
                is QueryStrategy.First -> reference.child(path).limitToFirst(strategy.limit)
                is QueryStrategy.Last -> reference.child(path).limitToLast(strategy.limit)
            }

        try {
            val snapshot = measure(healthToken) { query.get().await() }
            val value =
                snapshot.value ?: throw Exception(
                    "No value exists at the specified key path.",
                    userInfo = mapOf("Path" to path),
                    metadata = ExceptionMetadata(this),
                )

            CoreDatabaseStore.addValue(
                DataSample(value, Networking.cacheExpiryMilliseconds(queryValuesStartMillis)),
                path,
            )

            return value
        } catch (exception: Exception) {
            val cachedValue = CoreDatabaseStore.getValue(path)
            if (cacheStrategy != CacheStrategy.RETURN_CACHE_ON_FAILURE || cachedValue == null) throw exception

            Logger.log(exception, domain = LoggerDomain.Networking.database)
            return cachedValue
        }
    }

    // MARK: - Value Setting

    private suspend fun setValue(
        value: Any?,
        key: String,
        healthToken: HealthSampleToken,
    ): Any? {
        if (!isEncodable(value)) {
            throw Exception.Networking.invalidType(
                value ?: "null",
                ExceptionMetadata(this),
            )
        }

        Logger.log(
            "Setting value \"$value\" for key \"$key\".",
            domain = LoggerDomain.Networking.database,
        )

        measure(healthToken) {
            reference
                .child(key)
                .setValue(value)
                .await()
        }

        if (value == null) {
            CoreDatabaseStore.removeValue(key)
        } else {
            CoreDatabaseStore.addValue(
                DataSample(value, Networking.cacheExpiryMilliseconds(System.currentTimeMillis())),
                key,
            )
        }

        return null
    }

    private suspend fun updateChildValues(
        key: String,
        data: Map<String, Any?>,
        healthToken: HealthSampleToken,
    ): Any? {
        if (!data.values.all { isEncodable(it) }) {
            throw Exception.Networking.invalidType(
                data,
                ExceptionMetadata(this),
            )
        }

        Logger.log(
            "Updating child values for key \"$key\" with \"$data\".",
            domain = LoggerDomain.Networking.database,
        )

        measure(healthToken) {
            reference
                .child(key)
                .updateChildren(data)
                .await()
        }

        // When data keys contain "/" the payload is a multi-path
        // update (e.g. a fan-out anchored at the environment root).
        // Caching the partial map at the anchor key would poison
        // reads for the entire subtree. Cache each resolved leaf
        // path individually instead.
        val expiryMilliseconds = Networking.cacheExpiryMilliseconds(System.currentTimeMillis())
        if (data.keys.any { it.contains("/") }) {
            val resolved = mutableMapOf<String, DataSample>()
            for ((childKey, value) in data) {
                value ?: continue
                resolved["$key/$childKey"] = DataSample(value, expiryMilliseconds)
            }

            CoreDatabaseStore.addValues(resolved)
        } else {
            CoreDatabaseStore.addValue(DataSample(data, expiryMilliseconds), key)
        }

        return null
    }

    // MARK: - Auxiliary

    private suspend fun <T> measure(
        healthToken: HealthSampleToken,
        operation: suspend () -> T,
    ): T =
        HealthEvidence.measure(token = healthToken) {
            try {
                operation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw (throwable as? Exception) ?: Exception.from(throwable, ExceptionMetadata(this))
            }
        }

    private suspend fun runFirebaseTransaction(
        path: String,
        block: (Any?) -> Any?,
    ): Any? =
        suspendCancellableCoroutine { continuation ->
            reference.child(path).runTransaction(
                object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        currentData.value = block(currentData.value)
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        snapshot: DataSnapshot?,
                    ) {
                        if (!continuation.isActive) return

                        if (error != null) {
                            continuation.resumeWithException(
                                Exception.from(error.toException(), ExceptionMetadata(this@CoreDatabase)),
                            )
                        } else {
                            continuation.resume(snapshot?.value)
                        }
                    }
                },
            )
        }
}

private const val CONNECTED_PATH = ".info/connected"
