//
//  OnboardingContainer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import us.neotechnica.panther.modules.content.onboarding.constants.OnboardingContainerFloats
import us.neotechnica.panther.modules.content.onboarding.constants.OnboardingContainerStrings
import us.neotechnica.panther.modules.content.onboarding.views.authcodepageview.AuthCodePageView
import us.neotechnica.panther.modules.content.onboarding.views.permissionpageview.PermissionPageView
import us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview.SelectLanguagePageView
import us.neotechnica.panther.modules.content.onboarding.views.signinpageview.SignInPageView
import us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview.VerifyNumberPageView
import us.neotechnica.panther.modules.content.onboarding.views.welcomepageview.WelcomePageView
import us.neotechnica.panther.navigation.OnboardingNavigatorState
import us.neotechnica.panther.navigation.OnboardingRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Constants Accessors

private typealias Floats = OnboardingContainerFloats
private typealias Strings = OnboardingContainerStrings

/**
 * Hosts the onboarding navigation stack.
 *
 * The top of the coordinator's onboarding stack determines the visible
 * page (an empty stack shows the welcome page); pushing and popping
 * animate with a horizontal slide, and the system back gesture pops.
 *
 * @param modifier The modifier for this container.
 */
@Composable
fun OnboardingContainer(modifier: Modifier = Modifier) {
    val navigation = remember { DependencyValues.current.navigation }
    val state by navigation.state.collectAsState()
    val topPath = state.onboarding.stack.lastOrNull()

    BackHandler(enabled = topPath != null) {
        navigation.navigate(Route.Onboarding(OnboardingRoute.Pop))
    }

    val stackDepth = state.onboarding.stack.size
    val previousStackDepth = remember { mutableIntStateOf(stackDepth) }
    val isPush = stackDepth >= previousStackDepth.intValue
    SideEffect { previousStackDepth.intValue = stackDepth }

    AnimatedContent(
        contentKey = { it?.let { path -> path::class } },
        label = Strings.ANIMATION_LABEL,
        modifier = modifier.fillMaxSize(),
        targetState = topPath,
        transitionSpec = {
            val enter =
                slideInHorizontally(tween(Floats.TRANSITION_MILLIS)) { width -> if (isPush) width else -width } +
                    fadeIn(tween(Floats.TRANSITION_MILLIS))
            val exit =
                slideOutHorizontally(tween(Floats.TRANSITION_MILLIS)) { width -> if (isPush) -width else width } +
                    fadeOut(tween(Floats.TRANSITION_MILLIS))
            enter.togetherWith(exit).using(SizeTransform(clip = false))
        },
    ) { path ->
        when (path) {
            null -> WelcomePageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePath.SelectLanguage -> SelectLanguagePageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePath.VerifyNumber -> VerifyNumberPageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePath.AuthCode -> AuthCodePageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePath.Permission -> PermissionPageView(Modifier.fillMaxSize())
            OnboardingNavigatorState.SeguePath.SignIn -> SignInPageView(Modifier.fillMaxSize())
        }
    }
}
