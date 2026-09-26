//
//  SingleSlotCoalescer.kt
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
 * A single-lane async work coordinator that serializes or replaces
 * concurrent operations.
 *
 * [SingleSlotCoalescer] maintains at most one in-flight task at a
 * time. When multiple callers request an operation concurrently,
 * the coalescer's [Mode] determines how the overlap is resolved:
 *
 * - [Mode.COALESCE]: Subsequent callers piggyback on the
 *   already-running task and receive the same result.
 * - [Mode.LAST_CALLER_WINS]: The in-flight task is cancelled and a
 *   new one is started for the most recent caller.
 *
 * The slot is cleared automatically when the in-flight task
 * completes – whether it succeeds or throws – independent of which
 * caller awaits it.
 *
 * **Important:** [Mode.LAST_CALLER_WINS] relies on cooperative
 * cancellation. The cancelled operation must periodically check its
 * active state to stop promptly; an operation that ignores
 * cancellation continues running in the background and may produce
 * stale results after the replacement task has completed.
 *
 * **Warning:** The `operation` runs in the coalescer's own scope,
 * not the caller's. If a calling coroutine is cancelled, the shared
 * operation is *not* cancelled – it runs to completion so that
 * other coalesced callers still receive a result.
 */
class SingleSlotCoalescer<Output> {
    // MARK: - Types

    /**
     * The strategy used to resolve concurrent calls to the
     * coalescer.
     */
    enum class Mode {
        /** Subsequent callers share the in-flight task's result. */
        COALESCE,

        /** The in-flight task is cancelled and replaced by a new one. */
        LAST_CALLER_WINS,
    }

    // MARK: - Properties

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var slot: Pair<UUID, Deferred<Output>>? = null

    // MARK: - Methods

    /**
     * Submits an operation to the coalescer, resolving overlapping
     * calls according to the specified mode.
     *
     * If no operation is currently in flight, `operation` is started
     * immediately. Otherwise, the coalescer applies the given [Mode]:
     * [Mode.COALESCE] awaits the existing task, while
     * [Mode.LAST_CALLER_WINS] cancels the existing task and starts
     * `operation`.
     *
     * @param mode The resolution strategy for concurrent calls. The
     *   default is [Mode.COALESCE].
     * @param operation The asynchronous work to perform.
     *
     * @return The output of whichever task the caller ultimately
     *   awaits.
     */
    suspend operator fun invoke(
        mode: Mode = Mode.COALESCE,
        operation: suspend () -> Output,
    ): Output {
        val task = task(mode, operation)
        return withContext(NonCancellable) { task.await() }
    }

    // MARK: - Auxiliary

    private suspend fun task(
        mode: Mode,
        operation: suspend () -> Output,
    ): Deferred<Output> =
        mutex.withLock {
            slot?.let { (_, existing) ->
                when (mode) {
                    Mode.COALESCE -> return@withLock existing
                    Mode.LAST_CALLER_WINS -> existing.cancel()
                }
            }

            val id = UUID.randomUUID()
            val task = scope.async { operation() }
            slot = id to task

            // Always-clear finisher; runs regardless of who awaits.
            task.invokeOnCompletion {
                scope.launch {
                    mutex.withLock {
                        if (slot?.first == id) slot = null
                    }
                }
            }

            task
        }
}
