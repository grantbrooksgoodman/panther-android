//
//  NavigationCoordinator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.navigation.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.Navigating
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.NavigatorState

/**
 * A coordinator that holds an observable navigation state and applies
 * routes to it through a [Navigating] service.
 *
 * Create a coordinator with an initial state and a navigator, then
 * dispatch routes through [navigate]; views observe [state].
 *
 * @param Route The set of navigation actions the coordinator
 *   dispatches.
 * @param State The navigation state the coordinator holds.
 * @param initialState The state the coordinator starts with.
 * @param navigating The service that applies routes.
 * @param onStateChanged The handler to run with each new state.
 */
class NavigationCoordinator<Route, State : NavigatorState<*, *, *>>(
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
