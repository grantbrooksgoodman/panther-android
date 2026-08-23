//
//  AuthCodePageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.authcodepageview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import us.neotechnica.panther.BuildConfig
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.constants.AuthCodePageViewColors
import us.neotechnica.panther.modules.content.onboarding.constants.AuthCodePageViewFloats
import us.neotechnica.panther.modules.content.shared.components.UnderlinedTextField
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Colors = AuthCodePageViewColors
private typealias Floats = AuthCodePageViewFloats
private typealias Strings = us.neotechnica.panther.modules.content.onboarding.constants.AuthCodePageViewStrings

/**
 * The onboarding page for entering the verification code during
 * sign-up.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun AuthCodePageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(AuthCodePageReducer.State(), AuthCodePageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(AuthCodePageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(
        state = state.viewState,
        modifier = modifier,
        onRetry = { viewModel.send(AuthCodePageReducer.Action.ViewAppeared) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InstructionView(state.instructionViewStrings)

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = Floats.innerVStackBottomPadding),
            ) {
                Components.Text(
                    state.strings.value(AuthCodePageViewStrings.instructionLabelText),
                    color = colors.subtitleText,
                    font = Font.systemSemibold(),
                    modifier = Modifier.padding(vertical = Floats.instructionLabelVerticalPadding),
                )

                UnderlinedTextField(
                    value = state.verificationCode,
                    placeholder = Strings.TEXT_FIELD_PLACEHOLDER,
                    onValueChange = { viewModel.send(AuthCodePageReducer.Action.VerificationCodeChanged(it)) },
                    keyboardType = KeyboardType.Number,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Floats.textFieldHorizontalPadding)
                            .padding(top = Floats.textFieldTopPadding)
                            .padding(bottom = Floats.textFieldBottomPadding),
                )

                Components.CapsuleButton(
                    text = state.strings.value(AuthCodePageViewStrings.continueButtonText),
                    onClick = { viewModel.send(AuthCodePageReducer.Action.ContinueButtonTapped) },
                    isEnabled = state.isContinueButtonEnabled,
                    primary = true,
                    modifier = Modifier.padding(vertical = Floats.continueButtonVerticalPadding),
                )

                Components.Button(
                    text = state.strings.value(AuthCodePageViewStrings.backButtonText),
                    color = if (state.isBackButtonEnabled) colors.titleText else colors.disabled,
                    onClick = { if (state.isBackButtonEnabled) viewModel.send(AuthCodePageReducer.Action.BackButtonTapped) },
                    font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                    modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                )

                if (BuildConfig.DEBUG && state.hasError) {
                    Components.Button(
                        text = "Force Continue (Debug)",
                        color = Colors.debugForeground,
                        onClick = { viewModel.send(AuthCodePageReducer.Action.DebugForceContinueTapped) },
                        font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                        modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
