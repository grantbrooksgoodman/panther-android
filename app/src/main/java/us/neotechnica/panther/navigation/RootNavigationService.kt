//
//  RootNavigationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import us.neotechnica.panther.bundle.navigators.ChatNavigator
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.bundle.navigators.OnboardingNavigator
import us.neotechnica.panther.bundle.navigators.OnboardingRoute
import us.neotechnica.panther.bundle.navigators.SettingsNavigator
import us.neotechnica.panther.bundle.navigators.SettingsRoute
import us.neotechnica.panther.bundle.navigators.UserContentNavigator
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.Navigating
import us.neotechnica.panther.subsystem.modules.navigation.services.NavigationCoordinator

/**
 * A convenience alias for the app's navigation coordinator.
 *
 * Use `Navigation` wherever you need to reference the coordinator
 * type without repeating the full generic signature.
 */
typealias Navigation = NavigationCoordinator<RootNavigationService.Route, RootNavigatorState>

/**
 * The app's top-level navigation service.
 *
 * Applies a [Route] to the current [RootNavigatorState] by delegating
 * to the responsible navigator, returning the new state. Dispatch
 * routes through the coordinator rather than calling this service
 * directly:
 *
 * ```kotlin
 * navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.UserContent)))
 * ```
 */
object RootNavigationService : Navigating<RootNavigationService.Route, RootNavigatorState> {
    // MARK: - Types

    /**
     * The set of navigation actions available in the app, grouped by
     * the navigator responsible for handling each.
     */
    sealed interface Route {
        data class Chat(
            val route: ChatRoute,
        ) : Route

        data class Onboarding(
            val route: OnboardingRoute,
        ) : Route

        /** A route handled by [RootNavigator]. */
        data class Root(
            val route: RootRoute,
        ) : Route

        data class Settings(
            val route: SettingsRoute,
        ) : Route

        data class UserContent(
            val route: UserContentRoute,
        ) : Route
    }

    // MARK: - Navigate to Route

    override fun navigate(
        to: Route,
        on: RootNavigatorState,
    ): RootNavigatorState =
        when (to) {
            is Route.Chat -> on.copy(chat = ChatNavigator.navigate(to.route, on.chat))
            is Route.Onboarding -> on.copy(onboarding = OnboardingNavigator.navigate(to.route, on.onboarding))
            is Route.Root -> RootNavigator.navigate(to.route, on)
            is Route.Settings -> on.copy(settings = SettingsNavigator.navigate(to.route, on.settings))
            is Route.UserContent -> on.copy(userContent = UserContentNavigator.navigate(to.route, on.userContent))
        }
}
