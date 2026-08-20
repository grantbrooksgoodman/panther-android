//
//  KeyedCoalescer.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * A per-key async work coordinator that deduplicates concurrent
 * callers.
 *
 * [KeyedCoalescer] maintains at most one in-flight task per key.
 * When multiple callers invoke the coalescer with the same key
 * while an operation is already running, they piggyback on the
 * existing task and receive the same result. Calls for different
 * keys proceed fully independently, each with their own slot.
 *
 * ```kotlin
 * val coalescer = KeyedCoalescer<String, Profile>()
 *
 * // Both callers share a single fetch:
 * val a = async { coalescer(userID) { fetchProfile(userID) } }
 * val b = async { coalescer(userID) { fetchProfile(userID) } }
 * ```
 *
 * The slot for a given key is cleared automatically when its
 * in-flight task completes – whether it succeeds or throws.
 *
 * **Warning:** The `operation` runs in the coalescer's own scope,
 * not the caller's. If a calling coroutine is cancelled, the
 * shared operation is *not* cancelled – it runs to completion so
 * that other coalesced callers still receive a result. [invoke]
 * keeps *waiting* for that result regardless of cancellation; use
 * [submitUnlessCancelled] to abandon the wait when the calling
 * coroutine is cancelled.
 */
class KeyedCoalescer<Key : Any, Output> {
    // MARK: - Properties

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tasks = mutableMapOf<Key, Pair<UUID, Deferred<Output>>>()

    // MARK: - Methods

    /**
     * Submits an operation for the given key, coalescing with any
     * in-flight task for the same key.
     *
     * If no operation is currently running for `key`, the
     * coalescer starts `operation` immediately. If an operation
     * *is* running, the caller awaits the existing task and shares
     * its result – `operation` is never invoked. The wait is not
     * cancellable; use [submitUnlessCancelled] to abandon it on
     * cancellation.
     *
     * @param key The value that identifies the logical work lane.
     * @param operation The asynchronous work to perform when no
     *   in-flight task exists for `key`.
     *
     * @return The output of the in-flight task for `key`.
     */
    suspend operator fun invoke(
        key: Key,
        operation: suspend () -> Output,
    ): Output {
        val task =
            task(
                key,
                operation,
            )

        return withContext(NonCancellable) { task.await() }
    }

    /**
     * Submits an operation for the given key, coalescing with any
     * in-flight task for the same key and abandoning the wait if
     * the calling coroutine is cancelled.
     *
     * Behaves identically to [invoke] while the calling coroutine
     * remains active. If the calling coroutine is cancelled before
     * the shared operation settles, this method rethrows the
     * cancellation; the shared operation itself keeps running so
     * other coalesced callers still receive its result.
     *
     * @param key The value that identifies the logical work lane.
     * @param operation The asynchronous work to perform when no
     *   in-flight task exists for `key`.
     *
     * @return The output of the in-flight task for `key`.
     */
    suspend fun submitUnlessCancelled(
        key: Key,
        operation: suspend () -> Output,
    ): Output =
        task(
            key,
            operation,
        ).await()

    // MARK: - Auxiliary

    private suspend fun task(
        key: Key,
        operation: suspend () -> Output,
    ): Deferred<Output> =
        mutex.withLock {
            tasks[key]?.let { return@withLock it.second }

            val id = UUID.randomUUID()
            val task = scope.async { operation() }
            tasks[key] = id to task

            // Always-clear finisher; runs regardless of who awaits.
            task.invokeOnCompletion {
                scope.launch {
                    mutex.withLock {
                        if (tasks[key]?.first == id) tasks.remove(key)
                    }
                }
            }

            task
        }
}
