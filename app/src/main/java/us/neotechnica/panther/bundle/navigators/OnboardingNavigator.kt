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

    sealed interface ModalPaths : Paths

    /** The push destinations on the onboarding stack. */
    sealed interface SeguePaths : Paths {
        data object AuthCode : SeguePaths

        data object Permission : SeguePaths

        data object SelectLanguage : SeguePaths

        data object SignIn : SeguePaths

        data object VerifyNumber : SeguePaths
    }

    sealed interface SheetPaths : Paths
}

/** Applies [OnboardingRoute]s to the onboarding flow's navigation state. */
object OnboardingNavigator {
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
    data object Pop : OnboardingRoute

    data class Push(
        val path: OnboardingNavigatorState.SeguePaths,
    ) : OnboardingRoute

    data class Stack(
        val paths: List<OnboardingNavigatorState.SeguePaths>,
    ) : OnboardingRoute
}
