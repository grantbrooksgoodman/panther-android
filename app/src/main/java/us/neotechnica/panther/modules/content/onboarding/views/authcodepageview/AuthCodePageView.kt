//
//  AuthCodePageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.modifiers.Swipe
import us.neotechnica.panther.designsystem.modules.foundation.modifiers.onSwipe
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.constants.AuthCodePageViewColors
import us.neotechnica.panther.modules.content.onboarding.constants.AuthCodePageViewFloats
import us.neotechnica.panther.modules.content.shared.components.GenericTextField
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.modules.content.onboarding.constants.AuthCodePageViewStrings as Strings

// MARK: - Constants Accessors

private typealias Colors = AuthCodePageViewColors
private typealias Floats = AuthCodePageViewFloats

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

    StatefulView(state = state.viewState, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().onSwipe(Swipe.DOWN) { viewModel.send(AuthCodePageReducer.Action.DidSwipeDown) }) {
            InstructionView(state.instructionViewStrings)

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = Floats.innerVStackBottomPadding),
            ) {
                Components.Text(
                    state.strings.value(AuthCodePageViewStrings.instructionLabelText),
                    foregroundColor = Colors.instructionLabelForeground,
                    font = Font.systemSemibold(),
                    modifier = Modifier.padding(vertical = Floats.instructionLabelVerticalPadding),
                )

                GenericTextField(
                    text = state.verificationCode,
                    onTextChange = { viewModel.send(AuthCodePageReducer.Action.VerificationCodeChanged(it)) },
                    keyboardType = KeyboardType.Number,
                    placeholderText = Strings.TEXT_FIELD_PLACEHOLDER,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Floats.textFieldHorizontalPadding)
                            .padding(vertical = Floats.textFieldVerticalPadding),
                )

                Components.CapsuleButton(
                    text = state.strings.value(AuthCodePageViewStrings.continueButtonText),
                    foregroundColor = if (state.isContinueButtonEnabled) colors.background else colors.disabled,
                    isEnabled = state.isContinueButtonEnabled,
                    action = { viewModel.send(AuthCodePageReducer.Action.ContinueButtonTapped) },
                    modifier = Modifier.padding(top = Floats.continueButtonTopPadding),
                )

                Components.Button(
                    text = state.strings.value(AuthCodePageViewStrings.backButtonText),
                    foregroundColor = if (state.isBackButtonEnabled) colors.accent else colors.disabled,
                    onClick = { if (state.isBackButtonEnabled) viewModel.send(AuthCodePageReducer.Action.BackButtonTapped) },
                    font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                    modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
