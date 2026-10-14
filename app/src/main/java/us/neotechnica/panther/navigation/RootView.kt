//
//  RootView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.foundation.extensions.resignFirstResponders
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.modules.content.onboarding.views.OnboardingContainerView
import us.neotechnica.panther.modules.content.shared.views.splashpageview.SplashPageReducer
import us.neotechnica.panther.modules.content.shared.views.splashpageview.SplashPageView
import us.neotechnica.panther.modules.content.user.views.UserContentContainerView
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import kotlin.time.Duration.Companion.milliseconds

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

    // Fulfills app-wide keyboard-dismissal requests sent through the
    // `resignFirstResponders` shared event.
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        DependencyValues.current.sharedEvents.resignFirstResponders.events.collect {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    // Announces interface style changes through the
    // `traitCollectionChanged` shared event.
    val isSystemInDarkTheme = isSystemInDarkTheme()
    var didObserveInitialTheme by remember { mutableStateOf(false) }
    LaunchedEffect(isSystemInDarkTheme) {
        val sharedEvents = DependencyValues.current.sharedEvents
        if (didObserveInitialTheme) sharedEvents.traitCollectionChanged.send(Unit)
        didObserveInitialTheme = true
    }

    AnimatedContent(
        contentKey = { it?.let { path -> path::class } },
        label = "RootView",
        modifier = modifier.fillMaxSize(),
        targetState = state.modal,
        transitionSpec = {
            fadeIn(SystemAnimation.easeIn(TRANSITION_DURATION)) togetherWith fadeOut(SystemAnimation.easeIn(TRANSITION_DURATION))
        },
    ) { modal ->
        when (modal) {
            // The keyboard shrinks the onboarding pages' safe area, moving
            // their centered content up to stay clear of it.
            RootNavigatorState.ModalPaths.Onboarding ->
                OnboardingContainerView(Modifier.fillMaxSize().systemBarsPadding().imePadding())
            RootNavigatorState.ModalPaths.Splash ->
                SplashPageView(
                    viewModel = remember { ViewModel(SplashPageReducer.State(), SplashPageReducer()) },
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                )
            RootNavigatorState.ModalPaths.UserContent -> UserContentContainerView(Modifier.fillMaxSize())
            null -> Box(Modifier.fillMaxSize())
        }
    }
}

private val TRANSITION_DURATION = 200.milliseconds
