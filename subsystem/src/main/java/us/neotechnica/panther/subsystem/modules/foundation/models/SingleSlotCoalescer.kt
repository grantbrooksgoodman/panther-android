//
//  SingleSlotCoalescer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A single-lane async work coordinator that resolves overlapping
 * operations according to a [Coalescer.Policy].
 *
 * [SingleSlotCoalescer] maintains at most one in-flight task at a
 * time. When multiple callers request an operation concurrently, the
 * configured policy determines how the overlap is resolved – shared,
 * replaced, or rerun. It is the single-key specialization of
 * [Coalescer].
 *
 * ```kotlin
 * val coalescer = SingleSlotCoalescer<Unit>(Coalescer.Policy.RERUN)
 * ```
 *
 * **Warning:** The `operation` runs in the coalescer's own scope, not
 * the caller's. If a calling coroutine is cancelled, the shared
 * operation is *not* cancelled – it runs to completion so that other
 * coalesced callers still receive a result.
 */
class SingleSlotCoalescer<Output>(
    policy: Coalescer.Policy = Coalescer.Policy.COALESCE,
) {
    // MARK: - Properties

    private val coalescer = Coalescer<Unit, Output>(policy)

    // MARK: - Methods

    /**
     * Submits an operation, resolving any overlap with an in-flight
     * task according to the coalescer's policy.
     *
     * The wait is not cancellable; use [submitUnlessCancelled] to
     * abandon it on cancellation.
     *
     * @param operation The asynchronous work to perform.
     *
     * @return The output of whichever task the caller ultimately
     *   awaits.
     */
    suspend operator fun invoke(operation: suspend () -> Output): Output = coalescer(Unit, operation)

    /**
     * Submits an operation, abandoning the wait if the calling
     * coroutine is cancelled.
     *
     * @param operation The asynchronous work to perform.
     *
     * @return The output of whichever task the caller ultimately
     *   awaits.
     */
    suspend fun submitUnlessCancelled(operation: suspend () -> Output): Output = coalescer.submitUnlessCancelled(Unit, operation)
}
