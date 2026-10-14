//
//  OnboardingNavigator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.navigators

import us.neotechnica.panther.subsystem.modules.navigation.interfaces.NavigatorState
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.Paths

/** The onboarding flow's navigation state. */
data class OnboardingNavigatorState(
    override val modal: ModalPaths? = null,
    override val sheet: SheetPaths? = null,
    override val stack: List<SeguePaths> = emptyList(),
) : NavigatorState<OnboardingNavigatorState.ModalPaths, OnboardingNavigatorState.SeguePaths, OnboardingNavigatorState.SheetPaths> {
    // MARK: - Types

    /** The full-screen destinations in the onboarding flow; it presents none. */
    sealed interface ModalPaths : Paths

    /** The push destinations on the onboarding stack. */
    sealed interface SeguePaths : Paths {
        /** The verification code page. */
        data object AuthCode : SeguePaths

        /** The permissions page. */
        data object Permission : SeguePaths

        /** The language selection page. */
        data object SelectLanguage : SeguePaths

        /** The sign-in page. */
        data object SignIn : SeguePaths

        /** The phone number verification page. */
        data object VerifyNumber : SeguePaths
    }

    /** The sheet destinations in the onboarding flow; it presents none. */
    sealed interface SheetPaths : Paths
}

/** Applies [OnboardingRoute]s to the onboarding flow's navigation state. */
object OnboardingNavigator {
    /**
     * Returns the onboarding flow's state after applying the given
     * route.
     *
     * @param to The route to apply.
     * @param on The state to apply it to.
     */
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

/** Routes handled by the onboarding navigator. */
sealed interface OnboardingRoute {
    /** Pops the top page of the onboarding stack. */
    data object Pop : OnboardingRoute

    /** Pushes the given page onto the onboarding stack. */
    data class Push(
        val path: OnboardingNavigatorState.SeguePaths,
    ) : OnboardingRoute

    /** Replaces the onboarding stack with the given pages. */
    data class Stack(
        val paths: List<OnboardingNavigatorState.SeguePaths>,
    ) : OnboardingRoute
}
