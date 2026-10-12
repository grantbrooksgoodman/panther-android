//
//  NavigationCoordinatorDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import us.neotechnica.panther.designsystem.modules.foundation.extensions.presentedViewsCount
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState

/**
 * A convenience alias for the app's navigation coordinator.
 *
 * Use `Navigation` wherever you need to reference the coordinator
 * type without repeating the full generic signature.
 */
typealias Navigation = NavigationCoordinator<Route, RootNavigatorState>

// MARK: - Dependency

/**
 * The dependency key that provides access to the app's navigation
 * coordinator.
 *
 * The coordinator starts on the splash modal and publishes the
 * number of presented views to the shared state on every change.
 */
private object NavigationCoordinatorDependency : DependencyKey<Navigation> {
    override fun resolve(dependencies: DependencyValues): Navigation =
        NavigationCoordinator(
            RootNavigatorState(modal = RootNavigatorState.ModalPaths.Splash),
            navigating = RootNavigationService,
        ) { SharedState { it.presentedViewsCount }.wrappedValue = it.presentedViewsCount }
}

/** The app's navigation coordinator. */
val DependencyValues.navigation: Navigation
    get() = this[NavigationCoordinatorDependency]

// MARK: - Auxiliary

// The modal, the active modal's stack, and every presented sheet.
private val RootNavigatorState.presentedViewsCount: Int
    get() {
        val modalPath = modal ?: return 0
        val stackDepth =
            when (modalPath) {
                RootNavigatorState.ModalPaths.Onboarding -> onboarding.stack.size
                RootNavigatorState.ModalPaths.Splash -> 0
                RootNavigatorState.ModalPaths.UserContent -> userContent.stack.size
            }

        return 1 + stackDepth + listOfNotNull(sheet, chat.sheet, settings.sheet).size
    }
