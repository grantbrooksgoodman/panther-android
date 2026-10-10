//
//  SignInPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.signinpageview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.R
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.modifiers.Swipe
import us.neotechnica.panther.designsystem.modules.foundation.modifiers.onSwipe
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.constants.SignInPageViewColors
import us.neotechnica.panther.modules.content.onboarding.constants.SignInPageViewFloats
import us.neotechnica.panther.modules.content.shared.components.GenericTextField
import us.neotechnica.panther.modules.content.shared.components.PhoneNumberTextField
import us.neotechnica.panther.modules.content.shared.components.regionmenu.RegionMenu
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.modules.content.onboarding.constants.SignInPageViewStrings as Strings

// MARK: - Constants Accessors

private typealias Colors = SignInPageViewColors
private typealias Floats = SignInPageViewFloats

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
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())

    // The system back returns to the phone-number configuration rather than popping the page.
    BackHandler(enabled = state.configuration == SignInPageReducer.State.Configuration.VERIFICATION_CODE) {
        viewModel.send(SignInPageReducer.Action.BackButtonTapped)
    }

    StatefulView(state = state.viewState, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().onSwipe(Swipe.DOWN) { viewModel.send(SignInPageReducer.Action.DidSwipeDown) },
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

            Components.Text(
                state.instructionLabelText,
                foregroundColor = colors.titleText,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.padding(
                        horizontal = Floats.instructionLabelHorizontalPadding,
                        vertical = Floats.instructionLabelVerticalPadding,
                    ),
            )

            if (state.configuration == SignInPageReducer.State.Configuration.PHONE_NUMBER) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RegionMenu(
                        selectedRegionCode = state.selectedRegionCode,
                        onSelectedRegionCodeChange = { viewModel.send(SignInPageReducer.Action.SelectedRegionCodeChanged(it)) },
                        modifier =
                            Modifier.padding(
                                start = Floats.regionMenuLeadingPadding,
                                end = Floats.regionMenuTrailingPadding,
                            ),
                    )

                    PhoneNumberTextField(
                        text = state.phoneNumberString,
                        onTextChange = { viewModel.send(SignInPageReducer.Action.PhoneNumberStringChanged(it)) },
                        regionCode = state.selectedRegionCode,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(end = Floats.phoneNumberTextFieldTrailingPadding)
                                .padding(vertical = Floats.phoneNumberTextFieldVerticalPadding),
                    )
                }
            } else {
                GenericTextField(
                    text = state.verificationCode,
                    onTextChange = { viewModel.send(SignInPageReducer.Action.VerificationCodeChanged(it)) },
                    keyboardType = KeyboardType.Number,
                    placeholderText = Strings.TEXT_FIELD_PLACEHOLDER,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Floats.textFieldHorizontalPadding)
                            .padding(vertical = Floats.textFieldVerticalPadding),
                )
            }

            Components.CapsuleButton(
                text = state.continueButtonText,
                foregroundColor = if (state.isContinueButtonEnabled) colors.background else colors.disabled,
                isEnabled = state.isContinueButtonEnabled,
                action = { viewModel.send(SignInPageReducer.Action.ContinueButtonTapped) },
                modifier = Modifier.padding(top = Floats.continueButtonTopPadding),
            )

            Components.Button(
                text = state.strings.value(SignInPageViewStrings.backButtonText),
                foregroundColor = if (state.isBackButtonEnabled) colors.accent else colors.disabled,
                onClick = { if (state.isBackButtonEnabled) viewModel.send(SignInPageReducer.Action.BackButtonTapped) },
                font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                modifier = Modifier.padding(top = Floats.backButtonTopPadding),
            )
        }
    }
}
