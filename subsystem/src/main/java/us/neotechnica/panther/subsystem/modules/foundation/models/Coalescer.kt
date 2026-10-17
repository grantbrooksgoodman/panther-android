//
//  Coalescer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import kotlinx.coroutines.CompletableDeferred
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
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.util.UUID

/**
 * A per-key async work coordinator that resolves overlapping calls
 * according to a [Policy].
 *
 * [Coalescer] maintains at most one in-flight task per key. Calls for
 * different keys proceed fully independently, each with their own
 * lane. The policy determines how a call that arrives while a task
 * for the same key is already running is resolved.
 *
 * ```kotlin
 * val coalescer = Coalescer<String, Profile>()
 *
 * // Both callers share a single fetch:
 * val a = async { coalescer(userID) { fetchProfile(userID) } }
 * val b = async { coalescer(userID) { fetchProfile(userID) } }
 * ```
 *
 * The lane for a given key is cleared automatically when its
 * in-flight task completes – whether it succeeds or throws.
 *
 * **Warning:** The `operation` runs in the coalescer's own scope, not
 * the caller's. If a calling coroutine is cancelled, the shared
 * operation is *not* cancelled – it runs to completion so that other
 * coalesced callers still receive a result. [invoke] keeps *waiting*
 * for that result regardless of cancellation; use
 * [submitUnlessCancelled] to abandon the wait when the calling
 * coroutine is cancelled.
 */
class Coalescer<Key : Any, Output>(
    private val policy: Policy = Policy.COALESCE,
) {
    // MARK: - Types

    /** The strategy used to resolve concurrent calls for one key. */
    enum class Policy {
        /** Subsequent callers share the in-flight operation's result. */
        COALESCE,

        /**
         * The in-flight operation is cancelled and replaced by a new
         * one; every waiting caller receives the replacement's result.
         */
        REPLACE,

        /**
         * The caller waits for the in-flight operation to finish, and
         * then for the operation to run once more.
         *
         * Calls that arrive during a run collapse into a single rerun,
         * which uses the most recent caller's operation; at most one
         * rerun is ever pending.
         */
        RERUN,
    }

    // A lane's callers await `settlement`, which the lane's current
    // task completes; replacing the task keeps the settlement, so the
    // displaced task's callers receive the replacement's result.
    private class Lane<Output>(
        val id: UUID,
        val task: Deferred<Output>,
        val settlement: CompletableDeferred<Output>,
        val rerun: CompletableDeferred<Output>?,
        val rerunOperation: (suspend () -> Output)?,
    )

    // MARK: - Properties

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lanes = mutableMapOf<Key, Lane<Output>>()

    // MARK: - Methods

    /**
     * Submits an operation for the given key, resolving any overlap
     * with an in-flight task for the same key according to the
     * coalescer's [Policy].
     *
     * The wait is not cancellable; use [submitUnlessCancelled] to
     * abandon it on cancellation.
     *
     * @param key The value that identifies the logical work lane.
     * @param operation The asynchronous work to perform.
     *
     * @return The output of whichever task the caller ultimately
     *   awaits.
     */
    suspend operator fun invoke(
        key: Key,
        operation: suspend () -> Output,
    ): Output = withContext(NonCancellable) { settlement(key, operation).await() }

    /**
     * Submits an operation for the given key, abandoning the wait if
     * the calling coroutine is cancelled.
     *
     * Behaves identically to [invoke] while the calling coroutine
     * remains active. If the calling coroutine is cancelled before the
     * awaited task settles, this method rethrows the cancellation; the
     * shared operation itself keeps running so other callers still
     * receive its result.
     *
     * @param key The value that identifies the logical work lane.
     * @param operation The asynchronous work to perform.
     *
     * @return The output of whichever task the caller ultimately
     *   awaits.
     */
    suspend fun submitUnlessCancelled(
        key: Key,
        operation: suspend () -> Output,
    ): Output = settlement(key, operation).await()

    // MARK: - Auxiliary

    private suspend fun settlement(
        key: Key,
        operation: suspend () -> Output,
    ): Deferred<Output> =
        mutex.withLock {
            lanes[key]?.let { lane ->
                when (policy) {
                    Policy.COALESCE -> {
                        log("Coalescing task with existing in-flight operation.", key, lane)
                        return@withLock lane.settlement
                    }

                    Policy.REPLACE -> {
                        log("Cancelling previous in-flight operation to prioritize last caller.", key, lane)
                        lane.task.cancel()
                        return@withLock startTask(key, operation, lane.settlement)
                    }

                    Policy.RERUN -> {
                        log("Scheduling rerun of in-flight operation for last caller.", key, lane)
                        val pending = lane.rerun ?: CompletableDeferred()
                        lanes[key] = Lane(lane.id, lane.task, lane.settlement, pending, operation)
                        return@withLock pending
                    }
                }
            }

            startTask(key, operation, CompletableDeferred())
        }

    private fun startTask(
        key: Key,
        operation: suspend () -> Output,
        settlement: CompletableDeferred<Output>,
    ): Deferred<Output> {
        val id = UUID.randomUUID()
        val task = scope.async { operation() }
        lanes[key] = Lane(id, task, settlement, null, null)
        task.invokeOnCompletion { scope.launch { handleCompletion(key, id) } }
        return settlement
    }

    private suspend fun handleCompletion(
        key: Key,
        id: UUID,
    ) {
        mutex.withLock {
            val lane = lanes[key] ?: return
            // A replaced task no longer owns the lane; its settlement
            // belongs to the replacement.
            if (lane.id != id) return

            val cause = lane.task.getCompletionExceptionOrNull()
            if (cause == null) {
                lane.settlement.complete(lane.task.getCompleted())
            } else {
                lane.settlement.completeExceptionally(cause)
            }

            val rerun = lane.rerun
            val rerunOperation = lane.rerunOperation
            if (rerun == null || rerunOperation == null) {
                lanes.remove(key)
                return
            }

            log("Rerunning operation for callers that arrived during the previous run.", key, lane)
            startTask(key, rerunOperation, rerun)
        }
    }

    private fun log(
        text: String,
        key: Key,
        lane: Lane<Output>,
    ) {
        Logger.log(
            Exception(
                text,
                isReportable = false,
                userInfo =
                    mapOf(
                        "Key" to key,
                        "TaskID" to lane.id,
                    ),
                metadata = ExceptionMetadata(this),
            ),
            domain = LoggerDomain.concurrency,
        )
    }
}
