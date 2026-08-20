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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * The app's root view.
 *
 * [RootView] observes the navigation coordinator and renders the
 * top-level screen for the current [RootNavigatorState.modal] value,
 * crossfading between screens as the modal changes.
 *
 * For this phase the destinations are placeholders; later phases
 * replace them with the onboarding and signed-in content flows.
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
            RootNavigatorState.ModalPath.Onboarding -> PlaceholderScreen("Onboarding", "Phase 5")
            RootNavigatorState.ModalPath.Splash -> PlaceholderScreen("Splash", "Launching…")
            RootNavigatorState.ModalPath.UserContent -> PlaceholderScreen("User Content", "Phase 6")
            null -> Box(Modifier.fillMaxSize())
        }
    }
}

private const val TRANSITION_MILLIS = 250

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .background(LocalPantherColors.current.background),
        verticalArrangement = Arrangement.Center,
    ) {
        Components.Text(
            title,
            color = LocalPantherColors.current.titleText,
            font = Font.systemBold(FontScale.Large),
        )

        Components.Text(
            subtitle,
            color = LocalPantherColors.current.subtitleText,
        )
    }
}
