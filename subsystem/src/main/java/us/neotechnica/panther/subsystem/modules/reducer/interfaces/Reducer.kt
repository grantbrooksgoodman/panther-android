//
//  Reducer.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.reducer.interfaces

import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * A type that describes how app state changes in response to
 * actions.
 *
 * The [Reducer] interface is the core building block of state
 * management. Each reducer defines a `State` type that holds the
 * data a feature needs, an `Action` type that enumerates the
 * events it can handle, and a [reduce] method that applies an
 * action to the state and optionally returns an
 * [Effect][us.neotechnica.panther.subsystem.modules.effect.Effect]
 * for asynchronous follow-up work.
 *
 * ## Implementing a Reducer
 *
 * Define a class that implements [Reducer], declare nested
 * `State` and `Action` types, and implement [reduce]:
 *
 * ```kotlin
 * class CounterReducer : Reducer<CounterReducer.State, CounterReducer.Action> {
 *     sealed interface Action {
 *         data object Decrement : Action
 *         data object Increment : Action
 *     }
 *
 *     data class State(
 *         val count: Int = 0,
 *     )
 *
 *     override fun reduce(
 *         state: State,
 *         action: Action,
 *     ): ReduceResult<State, Action> = when (action) {
 *         Action.Decrement -> ReduceResult(state.copy(count = state.count - 1))
 *         Action.Increment -> ReduceResult(state.copy(count = state.count + 1))
 *     }
 * }
 * ```
 *
 * When an action requires asynchronous work, return a
 * [ReduceResult] carrying an effect. The effect runs after the
 * state change and can send additional actions back into the
 * reducer.
 *
 * **Note:** Reducers are main-thread confined. State reduction
 * and effect creation always run on the main thread.
 */
interface Reducer<State, Action> {
    // MARK: - Methods

    /**
     * Applies an action to the state and returns the new state
     * with an effect.
     *
     * Return a [ReduceResult] whose state reflects the action,
     * carrying an [Effect][us.neotechnica.panther.subsystem.modules.effect.Effect]
     * describing any asynchronous work that should follow. Omit
     * the effect when no follow-up work is needed.
     *
     * @param state The current state.
     * @param action The action to apply.
     *
     * @return The new state and an effect describing any
     *   asynchronous follow-up work.
     */
    fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action>
}
