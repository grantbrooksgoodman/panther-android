//
//  RootView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import us.neotechnica.panther.modules.content.onboarding.views.OnboardingContainer
import us.neotechnica.panther.modules.content.shared.views.SplashView
import us.neotechnica.panther.modules.content.user.views.UserContentContainer
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

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
            RootNavigatorState.ModalPath.Onboarding -> OnboardingContainer(Modifier.fillMaxSize())
            RootNavigatorState.ModalPath.Splash -> SplashView(Modifier.fillMaxSize())
            RootNavigatorState.ModalPath.UserContent -> UserContentContainer(Modifier.fillMaxSize())
            null -> Box(Modifier.fillMaxSize())
        }
    }
}

private const val TRANSITION_MILLIS = 250
