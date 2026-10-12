//
//  NavigationCoordinator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// MARK: - Navigating

/**
 * A service that applies navigation routes to a navigation state.
 *
 * @param Route The set of navigation actions the service handles.
 * @param State The navigation state the service modifies.
 */
interface Navigating<Route, State> {
    /**
     * Applies the given route to the given state.
     *
     * @param to The navigation action to perform.
     * @param on The current navigation state.
     *
     * @return The navigation state after the route is applied.
     */
    fun navigate(
        to: Route,
        on: State,
    ): State
}

// MARK: - Navigation Coordinator

/**
 * A coordinator that holds an observable navigation state and applies
 * routes to it through a [Navigating] service.
 *
 * @param Route The set of navigation actions the coordinator
 *   dispatches.
 * @param State The navigation state the coordinator holds.
 * @param initialState The state the coordinator starts with.
 * @param navigating The service that applies routes.
 * @param onStateChanged The handler to run with each new state.
 */
class NavigationCoordinator<Route, State>(
    initialState: State,
    private val navigating: Navigating<Route, State>,
    private val onStateChanged: ((State) -> Unit)? = null,
) {
    // MARK: - Properties

    private val mutableState = MutableStateFlow(initialState)

    // MARK: - Computed Properties

    /** The current navigation state. */
    val state: StateFlow<State> = mutableState.asStateFlow()

    // MARK: - Methods

    /**
     * Dispatches the given route, updating [state].
     *
     * @param to The navigation action to perform.
     */
    fun navigate(to: Route) {
        mutableState.update { navigating.navigate(to, on = it) }
        onStateChanged?.invoke(mutableState.value)
    }
}
