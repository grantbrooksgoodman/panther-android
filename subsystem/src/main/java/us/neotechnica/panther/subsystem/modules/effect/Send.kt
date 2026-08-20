//
//  Send.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.effect

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * A callback used by [Effect] operations to send actions back to
 * the reducer.
 *
 * [Send] is passed to the closure in [Effect.run] and related
 * methods. Call it like a function to dispatch an action:
 *
 * ```kotlin
 * Effect.run<Action> { send ->
 *     val items = service.fetchItems()
 *     send(Action.ItemsLoaded(items))
 * }
 * ```
 *
 * [Send] automatically checks for cancellation before
 * dispatching. If the effect's coroutine has been cancelled, the
 * action is silently dropped. Dispatch always occurs on the main
 * thread.
 */
class Send<Action>(
    internal val send: (Action) -> Unit,
) {
    // MARK: - Methods

    /**
     * Sends an action to the reducer.
     *
     * The action is dropped if the current coroutine has been
     * cancelled.
     *
     * @param action The action to send.
     */
    suspend operator fun invoke(action: Action) {
        if (!currentCoroutineContext().isActive) return
        withContext(Dispatchers.Main.immediate) {
            if (isActive) send(action)
        }
    }
}
