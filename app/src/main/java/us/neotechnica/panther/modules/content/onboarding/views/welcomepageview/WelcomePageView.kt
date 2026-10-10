//
//  WelcomePageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.R
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.constants.WelcomePageViewColors
import us.neotechnica.panther.modules.content.onboarding.constants.WelcomePageViewFloats
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Colors = WelcomePageViewColors
private typealias Floats = WelcomePageViewFloats

/**
 * The onboarding welcome page: the wordmark, the cycling greeting,
 * "Get Started", and "Sign In".
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun WelcomePageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(WelcomePageReducer.State(), WelcomePageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }

    LaunchedEffect(Unit) {
        viewModel.send(WelcomePageReducer.Action.ViewAppeared)
        viewModel.send(WelcomePageReducer.Action.ViewFirstAppeared)
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())

    StatefulView(state = state.viewState, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.hello_wordmark),
                contentDescription = null,
                colorFilter = if (isDarkModeActive) ColorFilter.tint(Colors.imageDarkForeground) else null,
                contentScale = ContentScale.FillBounds,
                modifier =
                    Modifier
                        .width(Floats.imageFrameWidth)
                        .height(Floats.imageFrameHeight)
                        .padding(bottom = Floats.imageBottomPadding),
            )

            AnimatedContent(
                targetState = state.welcomeLabelText,
                transitionSpec = { fadeIn(tween(easing = EaseIn)) togetherWith fadeOut(tween(easing = EaseIn)) },
                label = "WelcomeLabelText",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { viewModel.send(WelcomePageReducer.Action.WelcomeLabelTapped) }
                        .padding(
                            horizontal = Floats.instructionLabelHorizontalPadding,
                            vertical = Floats.instructionLabelVerticalPadding,
                        ),
            ) { welcomeLabelText ->
                Components.Text(
                    welcomeLabelText,
                    foregroundColor = colors.titleText,
                    font = Font.systemBold(FontScale.Large),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Components.CapsuleButton(
                text = state.strings.value(WelcomePageViewStrings.continueButtonText),
                action = { viewModel.send(WelcomePageReducer.Action.ContinueButtonTapped) },
                modifier = Modifier.padding(vertical = Floats.continueButtonVerticalPadding),
            )

            Components.Button(
                text = state.strings.value(WelcomePageViewStrings.signInButtonText),
                foregroundColor = colors.accent,
                onClick = { viewModel.send(WelcomePageReducer.Action.SignInButtonTapped) },
                font = Font.system(FontScale.Custom(Floats.SIGN_IN_BUTTON_LABEL_FONT_SIZE)),
            )
        }
    }
}
