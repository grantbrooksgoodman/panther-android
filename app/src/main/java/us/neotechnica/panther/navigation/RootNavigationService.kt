//
//  RootNavigationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

/**
 * The app's top-level navigation service.
 *
 * Applies a [Route] to the current [RootNavigatorState] by delegating
 * to the responsible navigator, returning the new state. Dispatch
 * routes through the coordinator rather than calling this service
 * directly.
 */
object RootNavigationService : Navigating<Route, RootNavigatorState> {
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

private object RootNavigator {
    fun navigate(
        to: RootRoute,
        on: RootNavigatorState,
    ): RootNavigatorState =
        when (to) {
            is RootRoute.Modal -> on.copy(modal = to.path)
        }
}

private object OnboardingNavigator {
    fun navigate(
        to: OnboardingRoute,
        on: OnboardingNavigatorState,
    ): OnboardingNavigatorState =
        when (to) {
            OnboardingRoute.Pop -> on.copy(stack = on.stack.dropLast(1))
            is OnboardingRoute.Push -> on.copy(stack = on.stack + to.path)
            is OnboardingRoute.Stack -> on.copy(stack = to.paths)
        }
}

private object UserContentNavigator {
    fun navigate(
        to: UserContentRoute,
        on: UserContentNavigatorState,
    ): UserContentNavigatorState =
        when (to) {
            UserContentRoute.Pop -> on.copy(stack = on.stack.dropLast(1))
            is UserContentRoute.Push -> on.copy(stack = on.stack + to.path)
            is UserContentRoute.Stack -> on.copy(stack = to.paths)
        }
}

private object SettingsNavigator {
    fun navigate(
        to: SettingsRoute,
        on: SettingsNavigatorState,
    ): SettingsNavigatorState =
        when (to) {
            is SettingsRoute.Sheet -> on.copy(sheet = to.path)
        }
}

private object ChatNavigator {
    fun navigate(
        to: ChatRoute,
        on: ChatNavigatorState,
    ): ChatNavigatorState =
        when (to) {
            is ChatRoute.Sheet -> on.copy(sheet = to.path)
        }
}
