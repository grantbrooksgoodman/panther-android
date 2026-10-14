//
//  NavigatingProtocol.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.navigation.interfaces

/**
 * A service that applies navigation routes to a navigation state.
 *
 * @param Route The set of navigation actions the service handles.
 * @param State The navigation state the service modifies.
 */
interface Navigating<Route, State : NavigatorState<*, *, *>> {
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
