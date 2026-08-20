//
//  Database.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.services

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.models.DatabaseOperation
import us.neotechnica.panther.networking.modules.database.models.QueryStrategy
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.KeyedCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
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

    private val globalCacheStrategy = LockIsolated<CacheStrategy?>(null)

    private val reference by lazy { FirebaseDatabase.getInstance().reference }

    // MARK: - Companion

    private companion object {
        val coalescer = KeyedCoalescer<String, Result<Any?>>()
    }

    // MARK: - DatabaseDelegate Conformance

    override fun generateKey(path: String): String? = reference.child(path).push().key

    override suspend fun <T> getValues(
        path: String,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): T =
        performOperation(
            DatabaseOperation.GetValues(path, cacheStrategy),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        ).cast()

    override suspend fun increment(
        path: String,
        delta: Int,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        val resolvedPath = path.prependingEnvironmentIfNeeded(prependingEnvironment)
        guardedFirebaseOperation(timeout, this) {
            reference.child(resolvedPath).setValue(ServerValue.increment(delta.toLong())).await()
        }

        // The server-side result is unknown locally; invalidate.
        CoreDatabaseStore.removeValue(resolvedPath)
    }

    override fun isEncodable(value: Any?): Boolean = isFirebaseEncodable(value)

    override fun <T> observe(
        path: String,
        prependingEnvironment: Boolean,
    ): Flow<T> =
        callbackFlow {
            val resolvedPath = path.prependingEnvironmentIfNeeded(prependingEnvironment)

            if (!Networking.isReadWriteEnabled) {
                close(Exception("Read/write access is currently disabled.", metadata = ExceptionMetadata(this@Database)))
                return@callbackFlow
            }

            Networking.config.activityIndicatorDelegate.show()
            val child = reference.child(resolvedPath)
            val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val value = snapshot.value
                        if (value == null) {
                            close(noValueException(resolvedPath, this@Database))
                            return
                        }

                        CoreDatabaseStore.addValue(
                            DataSample(value, Networking.cacheExpiryMillis(System.currentTimeMillis())),
                            resolvedPath,
                        )

                        @Suppress("UNCHECKED_CAST")
                        trySend(value as T)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(Exception.from(error.toException(), ExceptionMetadata(this@Database)))
                    }
                }

            child.addValueEventListener(listener)
            awaitClose {
                Networking.config.activityIndicatorDelegate.hide()
                child.removeEventListener(listener)
            }
        }.buffer(Channel.UNLIMITED)

    override fun prewarm() {
        reference.child(".info/connected").get()
    }

    override suspend fun <T> queryValues(
        path: String,
        strategy: QueryStrategy,
        prependingEnvironment: Boolean,
        cacheStrategy: CacheStrategy,
        timeout: Duration,
    ): T =
        performOperation(
            DatabaseOperation.QueryValues(path, strategy, cacheStrategy),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        ).cast()

    override suspend fun runTransaction(
        path: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
        block: (Any?) -> Any?,
    ): Any? {
        val resolvedPath = path.prependingEnvironmentIfNeeded(prependingEnvironment)
        val committed =
            guardedFirebaseOperation(timeout, this) {
                runFirebaseTransaction(reference, resolvedPath, this, block)
            }

        if (committed == null) {
            CoreDatabaseStore.removeValue(resolvedPath)
        } else {
            CoreDatabaseStore.addValue(
                DataSample(committed, Networking.cacheExpiryMillis(System.currentTimeMillis())),
                resolvedPath,
            )
        }

        return committed
    }

    override fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) {
        this.globalCacheStrategy.wrappedValue = globalCacheStrategy
    }

    override suspend fun setValue(
        value: Any?,
        key: String,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ) {
        performOperation(
            DatabaseOperation.SetValue(value, key),
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
        performOperation(
            DatabaseOperation.UpdateChildValues(key, data),
            prependingEnvironment = prependingEnvironment,
            timeout = timeout,
        )
    }

    // MARK: - Perform Operation

    private suspend fun performOperation(
        operation: DatabaseOperation,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Any? {
        val resolved = operation.resolvingAdaptiveCacheStrategy()
        val key =
            listOf(
                resolved.encodedHash,
                globalCacheStrategy.wrappedValue?.resolved?.rawValue ?: "",
                prependingEnvironment.toString(),
                timeout.toString(),
            ).joinToString("|")

        return coalescer
            .submitUnlessCancelled(key) {
                runCatching {
                    engine(
                        resolved,
                        prependingEnvironment = prependingEnvironment,
                        timeout = timeout,
                    )
                }
            }.getOrThrow()
    }

    private suspend fun engine(
        operation: DatabaseOperation,
        prependingEnvironment: Boolean,
        timeout: Duration,
    ): Any? =
        guardedFirebaseOperation(timeout, this) {
            when (operation) {
                is DatabaseOperation.GetValues ->
                    getValuesEngine(
                        reference,
                        operation.path.prependingEnvironmentIfNeeded(prependingEnvironment),
                        (globalCacheStrategy.wrappedValue ?: operation.cacheStrategy).resolved,
                        this,
                    )

                is DatabaseOperation.QueryValues ->
                    queryValuesEngine(
                        reference,
                        operation.path.prependingEnvironmentIfNeeded(prependingEnvironment),
                        operation.strategy,
                        (globalCacheStrategy.wrappedValue ?: operation.cacheStrategy).resolved,
                        this,
                    )

                is DatabaseOperation.SetValue ->
                    setValueEngine(
                        reference,
                        operation.value,
                        operation.key.prependingEnvironmentIfNeeded(prependingEnvironment),
                        this,
                    )

                is DatabaseOperation.UpdateChildValues ->
                    updateChildValuesEngine(
                        reference,
                        operation.key.prependingEnvironmentIfNeeded(prependingEnvironment),
                        operation.data,
                        this,
                    )
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any?.cast(): T = this as T

    private fun String.prependingEnvironmentIfNeeded(prepend: Boolean): String =
        if (prepend) "${Networking.config.environment.shortString}/${trim('/')}" else this
}
