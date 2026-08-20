//
//  Effect+Cancellation.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.effect

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Creates an effect that cancels any in-flight effect with the
 * given identifier.
 *
 * ```kotlin
 * return ReduceResult(state, Effect.cancel(CancelIDs.Polling))
 * ```
 *
 * Any [Hashable][Any] value can serve as a cancellation
 * identifier. A common pattern is a private object per effect.
 *
 * @param id The identifier of the effect to cancel.
 */
fun <Action> Effect.Companion.cancel(id: Any): Effect<Action> =
    fireAndForget {
        CancellableTasks.cancel(id)
    }

/**
 * Creates an effect that cancels all in-flight effects matching
 * the given identifiers.
 *
 * @param ids The identifiers of the effects to cancel.
 */
fun <Action> Effect.Companion.cancel(ids: List<Any>): Effect<Action> =
    merge(
        ids.map { cancel<Action>(it) },
    )

/**
 * Marks this effect with an identifier so it can be cancelled
 * later.
 *
 * Use `cancellable` together with [cancel] to manage the
 * lifetime of long-running effects.
 *
 * @param id The identifier to associate with this effect.
 * @param cancelInFlight When `true`, any previously running
 *   effect with the same identifier is cancelled before this one
 *   starts. This is useful for search-as-you-type patterns where
 *   only the most recent request matters. Defaults to `false`.
 */
fun <Action> Effect<Action>.cancellable(
    id: Any,
    cancelInFlight: Boolean = false,
): Effect<Action> =
    Effect.run { send ->
        withTaskCancellation(
            id,
            cancelInFlight = cancelInFlight,
        ) {
            operation(send)
        }
    }

/**
 * Runs a suspending operation that can be cancelled by its
 * identifier.
 *
 * Use this function outside of a reducer when you need
 * cancellable async work that participates in the same
 * cancellation registry as [cancellable].
 *
 * If the operation is cancelled – by its identifier or by the
 * caller – the cancellation propagates to the caller as a
 * `CancellationException`.
 *
 * @param id The identifier to associate with this operation.
 * @param cancelInFlight When `true`, any previously running
 *   operation with the same identifier is cancelled before this
 *   one starts. Defaults to `false`.
 * @param operation The suspending work to perform.
 *
 * @return The value produced by `operation`.
 */
suspend fun <T> withTaskCancellation(
    id: Any,
    cancelInFlight: Boolean = false,
    operation: suspend () -> T,
): T {
    if (cancelInFlight) CancellableTasks.cancel(id)

    return coroutineScope {
        val task = async { operation() }
        CancellableTasks.insert(
            task,
            id = id,
        )

        try {
            task.await()
        } finally {
            CancellableTasks.remove(
                task,
                id = id,
            )
        }
    }
}

internal object CancellableTasks {
    // MARK: - Properties

    private val storage = LockIsolated(mapOf<Any, Set<Job>>())

    // MARK: - Methods

    fun cancel(id: Any) {
        val jobs =
            storage.withValue {
                val jobs = it.value[id]
                it.value = it.value - id
                jobs
            }

        jobs?.forEach { it.cancel() }
    }

    fun exists(id: Any): Boolean = storage.withValue { it.value[id] != null }

    fun insert(
        job: Job,
        id: Any,
    ) {
        storage.withValue {
            it.value = it.value + (id to (it.value[id] ?: emptySet()) + job)
        }
    }

    fun remove(
        job: Job,
        id: Any,
    ) {
        storage.withValue {
            val remaining = (it.value[id] ?: emptySet()) - job
            it.value =
                if (remaining.isEmpty()) {
                    it.value - id
                } else {
                    it.value + (id to remaining)
                }
        }
    }
}
