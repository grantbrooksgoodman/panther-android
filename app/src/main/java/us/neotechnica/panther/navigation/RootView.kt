//
//  RootView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import us.neotechnica.panther.bundle.resignFirstResponders
import us.neotechnica.panther.modules.content.onboarding.views.OnboardingContainerView
import us.neotechnica.panther.modules.content.shared.views.splashpageview.SplashPageReducer
import us.neotechnica.panther.modules.content.shared.views.splashpageview.SplashPageView
import us.neotechnica.panther.modules.content.user.views.UserContentContainer
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * The app's root view.
 *
 * [RootView] observes the navigation coordinator and renders the
 * top-level screen for the current [RootNavigatorState.modal] value,
 * crossfading between the splash, onboarding, and signed-in flows.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun RootView(modifier: Modifier = Modifier) {
    val navigation = remember { DependencyValues.current.navigation }
    val state by navigation.state.collectAsState()

    // Fulfills app-wide keyboard-dismissal requests sent through
    // KeyboardService, mirroring iOS's uiApplication.resignFirstResponders().
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        DependencyValues.current.sharedEvents.resignFirstResponders.events.collect {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    AnimatedContent(
        contentKey = { it?.let { path -> path::class } },
        label = "RootView",
        modifier = modifier.fillMaxSize(),
        targetState = state.modal,
        transitionSpec = {
            fadeIn(tween(TRANSITION_MILLIS)) togetherWith fadeOut(tween(TRANSITION_MILLIS))
        },
    ) { modal ->
        when (modal) {
            RootNavigatorState.ModalPath.Onboarding -> OnboardingContainerView(Modifier.fillMaxSize().systemBarsPadding())
            RootNavigatorState.ModalPath.Splash ->
                SplashPageView(
                    viewModel = remember { ViewModel(SplashPageReducer.State(), SplashPageReducer()) },
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                )
            RootNavigatorState.ModalPath.UserContent -> UserContentContainer(Modifier.fillMaxSize())
            null -> Box(Modifier.fillMaxSize())
        }
    }
}

private const val TRANSITION_MILLIS = 250
