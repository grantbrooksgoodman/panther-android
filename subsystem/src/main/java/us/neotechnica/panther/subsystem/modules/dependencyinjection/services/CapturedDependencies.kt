//
//  CapturedDependencies.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.dependencyinjection.services

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/**
 * A snapshot of the dependency scope that was active when the
 * snapshot was created.
 *
 * Obtain a value of this type through
 * [DependencyScopes.withEscapedDependencies], then call
 * [withValue] to restore the captured scope inside an escaping
 * closure or a new coroutine:
 *
 * ```kotlin
 * DependencyScopes.withEscapedDependencies { captured ->
 *     Effect.run<Action> { send ->
 *         captured.withValue {
 *             // Dependencies are available here.
 *         }
 *     }
 * }
 * ```
 */
class CapturedDependencies internal constructor() {
    // MARK: - Properties

    private val captured = DependencyValues.current

    // MARK: - Methods

    /**
     * Restores the captured dependency scope for the duration of
     * a suspending operation.
     *
     * The restored scope propagates across suspension points and
     * into child coroutines started within `operation`.
     *
     * @param operation The suspending work to perform with the
     *   captured dependencies.
     *
     * @return The value returned by `operation`.
     */
    suspend fun <T> withValue(operation: suspend () -> T): T {
        val element = DependencyValues.threadLocalValues.asContextElement(captured)
        return withContext(element) { operation() }
    }

    /**
     * Restores the captured dependency scope for the duration of
     * a synchronous operation.
     *
     * @param operation The synchronous work to perform with the
     *   captured dependencies.
     *
     * @return The value returned by `operation`.
     */
    fun <T> withValueSync(operation: () -> T): T {
        val previous = DependencyValues.threadLocalValues.get()
        DependencyValues.threadLocalValues.set(captured)
        return try {
            operation()
        } finally {
            DependencyValues.threadLocalValues.set(previous)
        }
    }
}
