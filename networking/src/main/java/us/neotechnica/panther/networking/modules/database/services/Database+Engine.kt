//
//  Database+Engine.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.services

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.database.models.QueryStrategy
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads the value at `path`, honoring the cache strategy: returns
 * a cached value first when requested, and falls back to cache on
 * failure when requested. Successful reads refresh the cache.
 */
internal suspend fun getValuesEngine(
    reference: DatabaseReference,
    path: String,
    cacheStrategy: CacheStrategy,
    sender: Any,
): Any? {
    val cached = CoreDatabaseStore.getValue(path)
    if (cacheStrategy == CacheStrategy.RETURN_CACHE_FIRST && cached != null) return cached

    val startMillis = System.currentTimeMillis()
    return try {
        val value = firebaseGet(reference.child(path).get(), path, sender)
        CoreDatabaseStore.addValue(DataSample(value, Networking.cacheExpiryMillis(startMillis)), path)
        value
    } catch (exception: Exception) {
        if (cacheStrategy == CacheStrategy.RETURN_CACHE_ON_FAILURE && cached != null) cached else throw exception
    }
}

/**
 * Queries a limited subset of values at `path`, honoring the
 * cache strategy exactly as [getValuesEngine] does.
 */
internal suspend fun queryValuesEngine(
    reference: DatabaseReference,
    path: String,
    strategy: QueryStrategy,
    cacheStrategy: CacheStrategy,
    sender: Any,
): Any? {
    val cached = CoreDatabaseStore.getValue(path)
    if (cacheStrategy == CacheStrategy.RETURN_CACHE_FIRST && cached != null) return cached

    val query =
        when (strategy) {
            is QueryStrategy.First -> reference.child(path).limitToFirst(strategy.limit)
            is QueryStrategy.Last -> reference.child(path).limitToLast(strategy.limit)
        }

    val startMillis = System.currentTimeMillis()
    return try {
        val value = firebaseGet(query.get(), path, sender)
        CoreDatabaseStore.addValue(DataSample(value, Networking.cacheExpiryMillis(startMillis)), path)
        value
    } catch (exception: Exception) {
        if (cacheStrategy == CacheStrategy.RETURN_CACHE_ON_FAILURE && cached != null) cached else throw exception
    }
}

/**
 * Writes `value` at `key`, caching it (or invalidating the cache
 * when the value is `null`, which deletes the node).
 */
internal suspend fun setValueEngine(
    reference: DatabaseReference,
    value: Any?,
    key: String,
    sender: Any,
): Any? {
    if (!isFirebaseEncodable(value)) {
        throw Exception("The value is not encodable.", metadata = ExceptionMetadata(sender))
    }

    reference.child(key).setValue(value).await()
    if (value == null) {
        CoreDatabaseStore.removeValue(key)
    } else {
        CoreDatabaseStore.addValue(
            DataSample(value, Networking.cacheExpiryMillis(System.currentTimeMillis())),
            key,
        )
    }

    return null
}

/**
 * Merges `data` into the value at `key` without overwriting
 * siblings, then caches the written children.
 */
internal suspend fun updateChildValuesEngine(
    reference: DatabaseReference,
    key: String,
    data: Map<String, Any?>,
    sender: Any,
): Any? {
    if (!data.values.all { isFirebaseEncodable(it) }) {
        throw Exception("The update contains an unencodable value.", metadata = ExceptionMetadata(sender))
    }

    reference.child(key).updateChildren(data).await()
    cacheUpdatedChildren(key, data)
    return null
}

/**
 * Runs a Firebase transaction at `path`, bridging the callback API
 * to a suspending call.
 */
internal suspend fun runFirebaseTransaction(
    reference: DatabaseReference,
    path: String,
    sender: Any,
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
                    if (error != null) {
                        continuation.resumeWithException(
                            Exception.from(error.toException(), ExceptionMetadata(sender)),
                        )
                    } else {
                        continuation.resume(snapshot?.value)
                    }
                }
            },
        )
    }

/** Returns the awaited snapshot's value, or throws if it is absent. */
internal suspend fun firebaseGet(
    task: Task<DataSnapshot>,
    path: String,
    sender: Any,
): Any = task.await().value ?: throw noValueException(path, sender)

/** Returns the exception used when a path holds no value. */
internal fun noValueException(
    path: String,
    sender: Any,
): Exception =
    Exception(
        "No value exists at the specified key path.",
        userInfo = mapOf("Path" to path),
        metadata = ExceptionMetadata(sender),
    )

// A payload whose keys contain "/" is a multi-path fan-out; caching
// the partial map at the anchor would poison reads for the whole
// subtree. Cache each resolved leaf path individually instead.
private fun cacheUpdatedChildren(
    key: String,
    data: Map<String, Any?>,
) {
    val expiry = Networking.cacheExpiryMillis(System.currentTimeMillis())
    if (data.keys.any { it.contains("/") }) {
        val resolved =
            data
                .filterValues { it != null }
                .mapKeys { "$key/${it.key}" }
                .mapValues { DataSample(it.value!!, expiry) }
        CoreDatabaseStore.addValues(resolved)
    } else {
        CoreDatabaseStore.addValue(DataSample(data, expiry), key)
    }
}
