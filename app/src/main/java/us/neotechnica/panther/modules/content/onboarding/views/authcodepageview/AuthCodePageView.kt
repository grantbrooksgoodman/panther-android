//
//  AuthCodePageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.authcodepageview

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
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.components.OnboardingBackButton
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import androidx.compose.material3.Text as Material3Text

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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingBackButton(
                text = state.strings.value(AuthCodePageViewStrings.backButtonText),
                isEnabled = state.isBackButtonEnabled,
                onClick = { viewModel.send(AuthCodePageReducer.Action.BackButtonTapped) },
            )

            InstructionView(state.instructionViewStrings, modifier = Modifier.fillMaxWidth())

            Components.Text(
                state.strings.value(AuthCodePageViewStrings.instructionLabelText),
                color = colors.subtitleText,
            )

            OutlinedTextField(
                value = state.verificationCode,
                onValueChange = { viewModel.send(AuthCodePageReducer.Action.VerificationCodeChanged(it)) },
                label = { Material3Text("Verification code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Components.CapsuleButton(
                text = state.strings.value(AuthCodePageViewStrings.continueButtonText),
                onClick = { viewModel.send(AuthCodePageReducer.Action.ContinueButtonTapped) },
                isEnabled = state.isContinueButtonEnabled,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
