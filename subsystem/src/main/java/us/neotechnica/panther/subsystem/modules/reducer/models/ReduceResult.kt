//
//  ReduceResult.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.reducer.models

import us.neotechnica.panther.subsystem.modules.effect.Effect

/**
 * The outcome of applying an action to a reducer's state.
 *
 * A reduce result pairs the new state with an [Effect]
 * describing any asynchronous follow-up work. Construct one
 * without an effect when no follow-up work is needed:
 *
 * ```kotlin
 * return ReduceResult(state.copy(count = state.count + 1))
 * ```
 *
 * Or carry an effect alongside the state change:
 *
 * ```kotlin
 * return ReduceResult(
 *     state.copy(isLoading = true),
 *     Effect.task { Action.ItemsLoaded(service.fetchItems()) }
 * )
 * ```
 */
data class ReduceResult<State, Action>(
    /** The state after applying the action. */
    val state: State,
    /** The asynchronous follow-up work, if any. */
    val effect: Effect<Action> = Effect.none(),
)
