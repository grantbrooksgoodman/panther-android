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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import us.neotechnica.panther.bundle.navigators.OnboardingNavigatorState
import us.neotechnica.panther.bundle.navigators.OnboardingRoute
import us.neotechnica.panther.designsystem.modules.foundation.components.NavigationStackView
import us.neotechnica.panther.modules.content.onboarding.views.authcodepageview.AuthCodePageReducer
import us.neotechnica.panther.modules.content.onboarding.views.authcodepageview.AuthCodePageView
import us.neotechnica.panther.modules.content.onboarding.views.permissionpageview.PermissionPageReducer
import us.neotechnica.panther.modules.content.onboarding.views.permissionpageview.PermissionPageView
import us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview.SelectLanguagePageReducer
import us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview.SelectLanguagePageView
import us.neotechnica.panther.modules.content.onboarding.views.signinpageview.SignInPageReducer
import us.neotechnica.panther.modules.content.onboarding.views.signinpageview.SignInPageView
import us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview.VerifyNumberPageReducer
import us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview.VerifyNumberPageView
import us.neotechnica.panther.modules.content.onboarding.views.welcomepageview.WelcomePageReducer
import us.neotechnica.panther.modules.content.onboarding.views.welcomepageview.WelcomePageView
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * Hosts the onboarding navigation stack.
 *
 * The top of the coordinator's onboarding stack determines the
 * visible page (an empty stack shows the welcome page); pushing
 * and popping animate on the navigation transition, and the
 * system back gesture pops. Every page keeps its view model – and
 * so its state – for the flow's lifetime: the welcome page
 * persists as the root, and a pushed page keeps its state beneath
 * a push and across a pop and re-push.
 *
 * @param modifier The modifier for this container.
 */
@Composable
fun OnboardingContainerView(modifier: Modifier = Modifier) {
    val navigation = remember { DependencyValues.current.navigation }
    val state by navigation.state.collectAsState()
    val stack = state.onboarding.stack

    val viewModels = remember { OnboardingPageViewModels() }
    DisposableEffect(Unit) { onDispose { viewModels.closeAll() } }

    // The welcome page appears on every return to the root and runs its
    // first-appearance work once.
    LaunchedEffect(stack.isEmpty()) {
        if (stack.isNotEmpty()) return@LaunchedEffect
        viewModels.welcome.send(WelcomePageReducer.Action.ViewAppeared)
        viewModels.onFirstAppear(null) { viewModels.welcome.send(WelcomePageReducer.Action.ViewFirstAppeared) }
    }

    BackHandler(enabled = stack.isNotEmpty()) {
        navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
    }

    NavigationStackView(
        path = stack,
        modifier = modifier,
        isRootPersistent = true,
        root = { rootModifier -> WelcomePageView(viewModels.welcome, rootModifier) },
    ) { path ->
        // A pushed page appears once per view-model lifetime.
        when (path) {
            OnboardingNavigatorState.SeguePaths.SelectLanguage -> {
                val viewModel = viewModels.selectLanguage
                LaunchedEffect(viewModel) { viewModels.onFirstAppear(path) { viewModel.send(SelectLanguagePageReducer.Action.ViewAppeared) } }
                SelectLanguagePageView(viewModel, Modifier.fillMaxSize())
            }

            OnboardingNavigatorState.SeguePaths.VerifyNumber -> {
                val viewModel = viewModels.verifyNumber
                LaunchedEffect(viewModel) { viewModels.onFirstAppear(path) { viewModel.send(VerifyNumberPageReducer.Action.ViewAppeared) } }
                VerifyNumberPageView(viewModel, Modifier.fillMaxSize())
            }

            OnboardingNavigatorState.SeguePaths.AuthCode -> {
                val viewModel = viewModels.authCode
                LaunchedEffect(viewModel) { viewModels.onFirstAppear(path) { viewModel.send(AuthCodePageReducer.Action.ViewAppeared) } }
                AuthCodePageView(viewModel, Modifier.fillMaxSize())
            }

            OnboardingNavigatorState.SeguePaths.Permission -> {
                val viewModel = viewModels.permission
                LaunchedEffect(viewModel) { viewModels.onFirstAppear(path) { viewModel.send(PermissionPageReducer.Action.ViewAppeared) } }
                PermissionPageView(viewModel, Modifier.fillMaxSize())
            }

            OnboardingNavigatorState.SeguePaths.SignIn -> {
                val viewModel = viewModels.signIn
                LaunchedEffect(viewModel) { viewModels.onFirstAppear(path) { viewModel.send(SignInPageReducer.Action.ViewAppeared) } }
                SignInPageView(viewModel, Modifier.fillMaxSize())
            }
        }
    }
}
