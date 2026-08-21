//
//  WelcomePageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.R
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

/**
 * The onboarding welcome page: greeting, "Get Started", and "Sign In".
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun WelcomePageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(WelcomePageReducer.State(), WelcomePageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }

    LaunchedEffect(Unit) {
        viewModel.send(WelcomePageReducer.Action.ViewFirstAppeared)
        viewModel.send(WelcomePageReducer.Action.ViewAppeared)
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(state = state.viewState, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Image(
                painter = painterResource(R.drawable.hello_wordmark),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.titleText),
                modifier = Modifier.height(72.dp),
            )

            Components.Text(
                state.welcomeLabelText,
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
            )

            Components.CapsuleButton(
                text = state.strings.value(WelcomePageViewStrings.continueButtonText),
                onClick = { viewModel.send(WelcomePageReducer.Action.ContinueButtonTapped) },
                primary = true,
            )

            Components.Button(
                text = state.strings.value(WelcomePageViewStrings.signInButtonText),
                color = colors.accent,
                onClick = { viewModel.send(WelcomePageReducer.Action.SignInButtonTapped) },
            )
        }
    }
}
