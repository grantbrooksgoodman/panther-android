//
//  NavigationCoordinatorDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import us.neotechnica.panther.designsystem.modules.foundation.extensions.leafViewDescriptor
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.navigation.services.NavigationCoordinator
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState

// MARK: - Dependency

/**
 * The dependency key that provides access to the app's navigation
 * coordinator.
 *
 * The coordinator starts on the splash modal and publishes the
 * leaf view descriptor to the shared state on every change.
 */
private object NavigationCoordinatorDependency : DependencyKey<Navigation> {
    override fun resolve(dependencies: DependencyValues): Navigation =
        NavigationCoordinator(
            RootNavigatorState(modal = RootNavigatorState.ModalPaths.Splash),
            navigating = RootNavigationService,
        ) {
            SharedState { state -> state.leafViewDescriptor }.wrappedValue = it.descriptor
        }
}

/** The app's navigation coordinator. */
val DependencyValues.navigation: Navigation
    get() = this[NavigationCoordinatorDependency]
