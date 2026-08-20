//
//  DependencyScopes.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.dependencyinjection.services

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/**
 * A namespace for functions that override or capture
 * dependencies for the duration of a closure.
 *
 * Use [DependencyScopes] to control which dependency values are
 * visible to a block of work.
 *
 * ## Overriding Dependencies
 *
 * Call [withDependencies] to replace one or more dependencies
 * for the duration of an operation. This is especially useful in
 * tests, where you can substitute a mock without changing
 * production code:
 *
 * ```kotlin
 * val items = DependencyScopes.withDependencies({
 *     it[URLSessionDependency] = mockSession
 * }) {
 *     service.fetchItems()
 * }
 * ```
 *
 * ## Escaping Closures
 *
 * Thread-local dependency values do not propagate into escaping
 * closures or new coroutines automatically. When you need to
 * preserve the current scope across such a boundary – as
 * [Effect][us.neotechnica.panther.subsystem.modules.effect.Effect]
 * does – use [withEscapedDependencies]. The closure receives a
 * [CapturedDependencies] value that can restore the scope later.
 */
object DependencyScopes {
    // MARK: - Methods

    /**
     * Overrides dependencies for the duration of a synchronous
     * operation.
     *
     * The `modifier` closure receives a builder over the current
     * dependency values. Any changes you make are visible to
     * code executed inside `operation`, but do not affect the
     * surrounding scope.
     *
     * @param modifier A closure that mutates the current
     *   dependency values.
     * @param operation The synchronous work to perform with the
     *   modified dependencies.
     *
     * @return The value returned by `operation`.
     */
    fun <T> withDependencies(
        modifier: (DependencyValues.Builder) -> Unit,
        operation: () -> T,
    ): T {
        val values = modifiedValues(modifier)
        val previous = DependencyValues.threadLocalValues.get()
        DependencyValues.threadLocalValues.set(values)
        return try {
            operation()
        } finally {
            DependencyValues.threadLocalValues.set(previous)
        }
    }

    /**
     * Overrides dependencies for the duration of a suspending
     * operation.
     *
     * The override propagates across suspension points and into
     * child coroutines started within `operation`.
     *
     * @param modifier A closure that mutates the current
     *   dependency values.
     * @param operation The suspending work to perform with the
     *   modified dependencies.
     *
     * @return The value returned by `operation`.
     */
    suspend fun <T> withDependenciesAsync(
        modifier: (DependencyValues.Builder) -> Unit,
        operation: suspend () -> T,
    ): T {
        val element =
            DependencyValues.threadLocalValues.asContextElement(
                modifiedValues(modifier),
            )

        return withContext(element) { operation() }
    }

    /**
     * Captures the current dependency scope for use in escaping
     * closures.
     *
     * @param operation A closure that receives the captured
     *   dependencies.
     *
     * @return The value returned by `operation`.
     */
    fun <T> withEscapedDependencies(operation: (CapturedDependencies) -> T): T = operation(CapturedDependencies())

    // MARK: - Auxiliary

    private fun modifiedValues(modifier: (DependencyValues.Builder) -> Unit): DependencyValues {
        val builder = DependencyValues.Builder(DependencyValues.current)
        modifier(builder)
        return builder.build()
    }
}
