//
//  LockIsolated.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A container that synchronizes access to a value using a lock.
 *
 * Use [LockIsolated] to protect mutable state that may be
 * accessed from multiple threads. The container provides two
 * access patterns:
 *
 * - Use [wrappedValue] for whole-value reads and writes.
 * - Use [withValue] for operations that must read and modify
 *   the value as a single isolated step.
 *
 * ```kotlin
 * private val cache = LockIsolated(mapOf<String, String>())
 *
 * val snapshot = cache.wrappedValue
 * cache.withValue { it.value = it.value + ("greeting" to "Hello") }
 * ```
 *
 * **Warning:** [LockIsolated] synchronizes individual accesses
 * to its stored value, but it does not make all uses of that
 * value automatically atomic. Reading [wrappedValue] produces a
 * snapshot; compound operations performed through separate reads
 * and writes are not atomic – use [withValue] instead. Prefer
 * immutable value types for the stored value, and avoid
 * performing long-running work from within [withValue].
 */
class LockIsolated<Value>(
    initialValue: Value,
) {
    // MARK: - Types

    /**
     * A mutable reference to the isolated value, passed to
     * [withValue] operations.
     *
     * Assign [value] to replace the stored value; the write is
     * applied when the operation returns.
     */
    class Ref<Value> internal constructor(
        var value: Value,
    )

    // MARK: - Properties

    private val lock = ReentrantLock()

    private var storedValue = initialValue

    // MARK: - Computed Properties

    /**
     * The current value.
     *
     * Reading returns a snapshot of the stored value; writing
     * replaces it. Both accesses are individually synchronized.
     */
    var wrappedValue: Value
        get() = lock.withLock { storedValue }
        set(newValue) = lock.withLock { storedValue = newValue }

    // MARK: - Methods

    /**
     * Atomically reads and mutates the current value as a single
     * isolated operation.
     *
     * Use this method instead of separate reads and writes of
     * [wrappedValue] when the new value depends on the old one;
     * performing the mutation in a single isolated operation
     * prevents concurrent writers from interleaving.
     *
     * The lock is reentrant, so the operation may safely read
     * [wrappedValue] on the same thread.
     *
     * @param operation A closure that receives a mutable
     *   reference to the current value.
     *
     * @return The value returned by `operation`.
     */
    fun <T> withValue(operation: (Ref<Value>) -> T): T =
        lock.withLock {
            // Operate on a local reference rather than the stored
            // value directly so reentrant reads observe a stable
            // value until the operation completes.
            val ref = Ref(storedValue)
            try {
                operation(ref)
            } finally {
                storedValue = ref.value
            }
        }
}
