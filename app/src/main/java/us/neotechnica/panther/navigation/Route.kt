//
//  Route.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

/**
 * The set of navigation actions available in the app, grouped by the
 * navigator responsible for handling each.
 *
 * Dispatch a route through
 * [Navigation.navigate][us.neotechnica.panther.navigation.Navigation.navigate]:
 *
 * ```kotlin
 * navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.UserContent)))
 * ```
 */
sealed interface Route {
    data class Chat(
        val route: ChatRoute,
    ) : Route

    data class Onboarding(
        val route: OnboardingRoute,
    ) : Route

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

/** Routes handled by the root navigator. */
sealed interface RootRoute {
    /** Presents the given full-screen modal, or dismisses it when `null`. */
    data class Modal(
        val path: RootNavigatorState.ModalPaths?,
    ) : RootRoute
}

/** Routes handled by the onboarding navigator. */
sealed interface OnboardingRoute {
    data object Pop : OnboardingRoute

    data class Push(
        val path: OnboardingNavigatorState.SeguePaths,
    ) : OnboardingRoute

    data class Stack(
        val paths: List<OnboardingNavigatorState.SeguePaths>,
    ) : OnboardingRoute
}

/** Routes handled by the signed-in content navigator. */
sealed interface UserContentRoute {
    data object Pop : UserContentRoute

    data class Push(
        val path: UserContentNavigatorState.SeguePaths,
    ) : UserContentRoute

    data class Stack(
        val paths: List<UserContentNavigatorState.SeguePaths>,
    ) : UserContentRoute
}

/** Routes handled by the settings navigator. */
sealed interface SettingsRoute {
    /** Presents the given sheet over the settings flow, or dismisses it when `null`. */
    data class Sheet(
        val path: SettingsNavigatorState.SheetPaths?,
    ) : SettingsRoute
}

/** Routes handled by the chat navigator. */
sealed interface ChatRoute {
    /** Presents the given sheet over the chat flow, or dismisses it when `null`. */
    data class Sheet(
        val path: ChatNavigatorState.SheetPaths?,
    ) : ChatRoute
}
