//
//  SignInPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.signinpageview

import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.input.KeyboardType
import us.neotechnica.panther.R
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.PhoneNumberEntry
import us.neotechnica.panther.modules.content.onboarding.constants.SignInPageViewFloats
import us.neotechnica.panther.modules.content.shared.components.UnderlinedTextField
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = SignInPageViewFloats
private typealias Strings = us.neotechnica.panther.modules.content.onboarding.constants.SignInPageViewStrings

/**
 * The sign-in page, entering an existing account's phone number and
 * verification code, both configurations shown in place.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun SignInPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(SignInPageReducer.State(), SignInPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(SignInPageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(
        state = state.viewState,
        modifier = modifier,
        onRetry = { viewModel.send(SignInPageReducer.Action.ViewAppeared) },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(R.drawable.hello_wordmark),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.titleText),
                contentScale = ContentScale.FillBounds,
                modifier =
                    Modifier
                        .width(Floats.imageFrameWidth)
                        .height(Floats.imageFrameHeight)
                        .padding(bottom = Floats.imageBottomPadding),
            )

            Components.Text(
                state.instructionLabelText,
                color = colors.titleText,
                modifier =
                    Modifier.padding(
                        horizontal = Floats.instructionLabelHorizontalPadding,
                        vertical = Floats.instructionLabelVerticalPadding,
                    ),
            )

            when (state.configuration) {
                SignInPageReducer.Configuration.PHONE_NUMBER ->
                    PhoneNumberEntry(
                        selectedRegionCode = state.selectedRegionCode,
                        phoneNumber = state.phoneNumberString.digits,
                        onRegionCodeSelected = { viewModel.send(SignInPageReducer.Action.SelectedRegionCodeChanged(it)) },
                        onPhoneNumberChange = { viewModel.send(SignInPageReducer.Action.PhoneNumberStringChanged(it)) },
                    )

                SignInPageReducer.Configuration.VERIFICATION_CODE ->
                    UnderlinedTextField(
                        value = state.verificationCode,
                        placeholder = Strings.TEXT_FIELD_PLACEHOLDER,
                        onValueChange = { viewModel.send(SignInPageReducer.Action.VerificationCodeChanged(it)) },
                        keyboardType = KeyboardType.Number,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = Floats.textFieldHorizontalPadding,
                                    vertical = Floats.textFieldVerticalPadding,
                                ),
                    )
            }

            Components.CapsuleButton(
                text = state.continueButtonText,
                onClick = { viewModel.send(SignInPageReducer.Action.ContinueButtonTapped) },
                isEnabled = state.isContinueButtonEnabled,
                primary = true,
                modifier = Modifier.padding(vertical = Floats.continueButtonVerticalPadding),
            )

            Components.Button(
                text = state.strings.value(SignInPageViewStrings.backButtonText),
                color = if (state.isBackButtonEnabled) colors.titleText else colors.disabled,
                onClick = { if (state.isBackButtonEnabled) viewModel.send(SignInPageReducer.Action.BackButtonTapped) },
                font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                modifier = Modifier.padding(top = Floats.backButtonTopPadding),
            )
        }
    }
}
