//
//  OnboardingContainerView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import us.neotechnica.panther.bundle.navigators.OnboardingNavigatorState
import us.neotechnica.panther.bundle.navigators.OnboardingRoute
import us.neotechnica.panther.designsystem.modules.foundation.components.NavigationStackView
import us.neotechnica.panther.modules.content.onboarding.views.authcodepageview.AuthCodePageView
import us.neotechnica.panther.modules.content.onboarding.views.permissionpageview.PermissionPageView
import us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview.SelectLanguagePageView
import us.neotechnica.panther.modules.content.onboarding.views.signinpageview.SignInPageView
import us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview.VerifyNumberPageView
import us.neotechnica.panther.modules.content.onboarding.views.welcomepageview.WelcomePageView
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * Hosts the onboarding navigation stack.
 *
 * The top of the coordinator's onboarding stack determines the visible
 * page (an empty stack shows the welcome page); pushing and popping
 * animate as a UIKit navigation stack does, and the system back
 * gesture pops.
 *
 * @param modifier The modifier for this container.
 */
@Composable
fun OnboardingContainerView(modifier: Modifier = Modifier) {
    val navigation = remember { DependencyValues.current.navigation }
    val state by navigation.state.collectAsState()

    BackHandler(enabled = state.onboarding.stack.isNotEmpty()) {
        navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
    }

    NavigationStackView(
        path = state.onboarding.stack,
        modifier = modifier,
        root = { rootModifier -> WelcomePageView(rootModifier) },
    ) { path ->
        when (path) {
            OnboardingNavigatorState.SeguePaths.SelectLanguage -> SelectLanguagePageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePaths.VerifyNumber -> VerifyNumberPageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePaths.AuthCode -> AuthCodePageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePaths.Permission -> PermissionPageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePaths.SignIn -> SignInPageView(Modifier.fillMaxSize())
        }
    }
}
