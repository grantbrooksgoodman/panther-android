//
//  Task.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.UUID
import kotlin.time.Duration

/**
 * Schedules deferred and debounced asynchronous work.
 *
 * Use [Task] to run an operation after a delay, or to debounce a
 * burst of calls so that only the most recently scheduled
 * invocation runs.
 */
object Task {
    // MARK: - Types

    private data class Entry(
        val token: UUID,
        val job: Job,
    )

    // MARK: - Properties

    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val registry = LockIsolated(mapOf<Any, Entry>())
    private var scope: CoroutineScope = defaultScope

    // MARK: - Methods

    /**
     * Debounces an async operation by `key`, scheduling it to run
     * after `delay`.
     *
     * Each call registers a new pending job for `key`. If another
     * call is made with the same `key` before the delay elapses,
     * the previously registered job is cancelled and replaced,
     * yielding latest-call-wins behavior: after a burst of calls,
     * `operation` runs at most once, using the most recently
     * scheduled invocation. Calls with different keys debounce
     * independently.
     *
     * @param key Identifier used to group and debounce calls.
     * @param delay How long to wait before executing `operation`.
     * @param operation The async operation to run after the delay
     *   if not superseded by a later call.
     *
     * @return The newly created job. Cancelling it prevents
     *   `operation` from running if it has not yet begun.
     */
    fun debounced(
        key: Any,
        delay: Duration,
        operation: suspend () -> Unit,
    ): Job {
        val token = UUID.randomUUID()
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                kotlinx.coroutines.delay(delay)
                operation()
                clearIfTokenMatches(token, key)
            }

        // Registered synchronously – before starting – so that a
        // burst of calls cannot race their registrations.
        set(job, token, key)
        job.start()

        return job
    }

    /**
     * Runs an operation after the given delay.
     *
     * @param by How long to wait before executing `operation`.
     * @param operation The async operation to run after the delay.
     *
     * @return The newly created job.
     */
    fun delayed(
        by: Duration,
        operation: suspend () -> Unit,
    ): Job =
        scope.launch {
            kotlinx.coroutines.delay(by)
            operation()
        }

    /** Installs a custom coroutine scope for tests. */
    fun setScope(scope: CoroutineScope) {
        this.scope = scope
    }

    /** Restores the default coroutine scope. */
    fun resetScope() {
        scope = defaultScope
    }

    // MARK: - Auxiliary

    private fun clearIfTokenMatches(
        token: UUID,
        key: Any,
    ) {
        registry.withValue { entries ->
            // Clear only if we are still the latest job for this key.
            if (entries.value[key]?.token == token) {
                entries.value = entries.value - key
            }
        }
    }

    private fun set(
        job: Job,
        token: UUID,
        key: Any,
    ) {
        registry.withValue { entries ->
            entries.value[key]?.job?.cancel()
            entries.value = entries.value + (key to Entry(token, job))
        }
    }
}
