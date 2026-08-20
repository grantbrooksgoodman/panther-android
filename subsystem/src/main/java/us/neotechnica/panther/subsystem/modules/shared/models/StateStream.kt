//
//  StateStream.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.shared.models

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.UUID

/**
 * A thread-safe container for a value whose changes are shared
 * with asynchronous subscribers.
 *
 * Use [StateStream] for values that cross feature boundaries and
 * have a meaningful current value at every point in time –
 * authentication state, visibility flags, or the most recently
 * published configuration. When only the occurrence of something
 * matters, use [EventStream] instead.
 *
 * Declare shared state as extension properties on
 * [SharedStates]:
 *
 * ```kotlin
 * val SharedStates.isLoggedIn: StateStream<Boolean>
 *     get() = state("isLoggedIn") { false }
 * ```
 *
 * ## Subscribing to Changes
 *
 * The [changes] property vends an independent flow for each
 * collection. The flow yields the current value immediately upon
 * subscription, then each subsequently written value. If the
 * consumer suspends while multiple writes occur, only the most
 * recent value is retained; intermediate values are discarded.
 * Values that must never be dropped are events, not state –
 * model them with [EventStream].
 *
 * ## Thread Safety
 *
 * All stored state is protected by [LockIsolated]. The [value]
 * property can be read and written from any thread. Writes are
 * delivered to every subscriber in write order.
 */
class StateStream<Value>(
    initialValue: Value,
) {
    // MARK: - Types

    private data class Storage<Value>(
        val continuations: Map<UUID, SendChannel<Value>>,
        val value: Value,
    )

    // MARK: - Properties

    private val storage =
        LockIsolated(
            Storage(
                continuations = mapOf<UUID, SendChannel<Value>>(),
                value = initialValue,
            ),
        )

    // MARK: - Computed Properties

    /**
     * An asynchronous sequence of the current value and its
     * changes.
     *
     * Each collection creates an independent flow. The flow
     * yields the current value immediately upon subscription,
     * then each value written to [value], in write order. The
     * flow completes only when its collector is cancelled.
     */
    val changes: Flow<Value>
        get() =
            callbackFlow {
                val id = UUID.randomUUID()

                storage.withValue {
                    it.value =
                        it.value.copy(
                            continuations = it.value.continuations + (id to channel),
                        )

                    trySend(it.value.value)
                }

                awaitClose {
                    storage.withValue {
                        it.value =
                            it.value.copy(
                                continuations = it.value.continuations - id,
                            )
                    }
                }
            }.buffer(Channel.CONFLATED)

    /**
     * The current value.
     *
     * Reading this property returns the latest value. Writing a
     * new value stores it and yields it to every active
     * [changes] subscriber. The store and the yields are
     * performed as a single atomic operation, so concurrent
     * writers cannot interleave and every subscriber observes
     * writes in the same order.
     */
    var value: Value
        get() = storage.withValue { it.value.value }
        set(newValue) {
            storage.withValue {
                it.value = it.value.copy(value = newValue)
                for (continuation in it.value.continuations.values) {
                    continuation.trySend(newValue)
                }
            }
        }

    // MARK: - Methods

    /**
     * Atomically reads and mutates the current value as a single
     * isolated operation, then yields the result to every active
     * [changes] subscriber.
     *
     * Use this method instead of separate reads and writes of
     * [value] when the new value depends on the old one;
     * performing the mutation in a single isolated operation
     * prevents concurrent writers from interleaving. The
     * resulting value is yielded to subscribers whether or not
     * `transform` changed it.
     *
     * @param transform A closure that receives the current value
     *   and returns the new value.
     *
     * @return The new value.
     */
    fun withValue(transform: (Value) -> Value): Value =
        storage.withValue {
            val newValue = transform(it.value.value)
            it.value = it.value.copy(value = newValue)
            for (continuation in it.value.continuations.values) {
                continuation.trySend(newValue)
            }

            newValue
        }
}
