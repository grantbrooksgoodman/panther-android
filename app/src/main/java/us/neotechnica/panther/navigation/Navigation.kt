//
//  Navigation.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * The app's navigation coordinator.
 *
 * Holds the observable [state] and applies routes through
 * [RootNavigationService]. Resolve the shared instance through the
 * [navigation][us.neotechnica.panther.navigation.navigation] dependency
 * accessor.
 */
class Navigation {
    // MARK: - Properties

    private val mutableState = MutableStateFlow(RootNavigatorState())

    // MARK: - Computed Properties

    /** The current navigation state. */
    val state: StateFlow<RootNavigatorState> = mutableState.asStateFlow()

    // MARK: - Methods

    /**
     * Dispatches the given route, updating [state].
     *
     * @param route The navigation action to perform.
     */
    fun navigate(route: Route) {
        mutableState.update { RootNavigationService.reduce(route, it) }
    }
}

private object NavigationDependency : DependencyKey<Navigation> {
    override fun resolve(dependencies: DependencyValues): Navigation = Navigation()
}

/** The app's shared navigation coordinator. */
val DependencyValues.navigation: Navigation
    get() = this[NavigationDependency]
