//
//  SignInPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.signinpageview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.OnboardingBackButton
import us.neotechnica.panther.modules.content.shared.components.RegionMenu
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import androidx.compose.material3.Text as Material3Text

/**
 * The sign-in page, which entering an existing account's phone number
 * and verification code, both configurations shown in place.
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingBackButton(
                text = state.strings.value(SignInPageViewStrings.backButtonText),
                isEnabled = state.isBackButtonEnabled,
                onClick = { viewModel.send(SignInPageReducer.Action.BackButtonTapped) },
            )

            Components.Text(
                state.instructionLabelText,
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
            )

            when (state.configuration) {
                SignInPageReducer.Configuration.PHONE_NUMBER -> {
                    RegionMenu(
                        selectedRegionCode = state.selectedRegionCode,
                        onRegionCodeSelected = {
                            viewModel.send(SignInPageReducer.Action.SelectedRegionCodeChanged(it))
                        },
                    )
                    OutlinedTextField(
                        value = state.phoneNumberString,
                        onValueChange = { viewModel.send(SignInPageReducer.Action.PhoneNumberStringChanged(it)) },
                        label = { Material3Text("Phone number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SignInPageReducer.Configuration.VERIFICATION_CODE ->
                    OutlinedTextField(
                        value = state.verificationCode,
                        onValueChange = { viewModel.send(SignInPageReducer.Action.VerificationCodeChanged(it)) },
                        label = { Material3Text("Verification code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
            }

            Components.CapsuleButton(
                text = state.continueButtonText,
                onClick = { viewModel.send(SignInPageReducer.Action.ContinueButtonTapped) },
                isEnabled = state.isContinueButtonEnabled,
                primary = true,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
