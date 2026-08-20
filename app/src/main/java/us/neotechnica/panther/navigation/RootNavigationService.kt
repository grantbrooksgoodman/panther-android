//
//  RootNavigationService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

/**
 * The app's top-level navigation reducer.
 *
 * Applies a [Route] to the current [RootNavigatorState] by delegating
 * to the responsible navigator, returning the new state.
 */
object RootNavigationService {
    fun reduce(
        route: Route,
        state: RootNavigatorState,
    ): RootNavigatorState =
        when (route) {
            is Route.Chat -> state.copy(chat = ChatNavigator.reduce(route.route, state.chat))
            is Route.Onboarding ->
                state.copy(onboarding = OnboardingNavigator.reduce(route.route, state.onboarding))

            is Route.Root -> RootNavigator.reduce(route.route, state)
            is Route.Settings ->
                state.copy(settings = SettingsNavigator.reduce(route.route, state.settings))

            is Route.UserContent ->
                state.copy(userContent = UserContentNavigator.reduce(route.route, state.userContent))
        }
}

private object RootNavigator {
    fun reduce(
        route: RootRoute,
        state: RootNavigatorState,
    ): RootNavigatorState =
        when (route) {
            is RootRoute.SetModal -> state.copy(modal = route.path)
        }
}

private object OnboardingNavigator {
    fun reduce(
        route: OnboardingRoute,
        state: OnboardingNavigatorState,
    ): OnboardingNavigatorState =
        when (route) {
            OnboardingRoute.Pop -> state.copy(stack = state.stack.dropLast(1))
            is OnboardingRoute.Push -> state.copy(stack = state.stack + route.path)
            is OnboardingRoute.Stack -> state.copy(stack = route.paths)
        }
}

private object UserContentNavigator {
    fun reduce(
        route: UserContentRoute,
        state: UserContentNavigatorState,
    ): UserContentNavigatorState =
        when (route) {
            UserContentRoute.Pop -> state.copy(stack = state.stack.dropLast(1))
            is UserContentRoute.Push -> state.copy(stack = state.stack + route.path)
            is UserContentRoute.Stack -> state.copy(stack = route.paths)
        }
}

private object SettingsNavigator {
    fun reduce(
        route: SettingsRoute,
        state: SettingsNavigatorState,
    ): SettingsNavigatorState =
        when (route) {
            SettingsRoute.Pop -> state.copy(stack = state.stack.dropLast(1))
            is SettingsRoute.Push -> state.copy(stack = state.stack + route.path)
            is SettingsRoute.Stack -> state.copy(stack = route.paths)
        }
}

private object ChatNavigator {
    fun reduce(
        route: ChatRoute,
        state: ChatNavigatorState,
    ): ChatNavigatorState =
        when (route) {
            ChatRoute.Pop -> state.copy(stack = state.stack.dropLast(1))
            is ChatRoute.Push -> state.copy(stack = state.stack + route.path)
            is ChatRoute.Stack -> state.copy(stack = route.paths)
        }
}
