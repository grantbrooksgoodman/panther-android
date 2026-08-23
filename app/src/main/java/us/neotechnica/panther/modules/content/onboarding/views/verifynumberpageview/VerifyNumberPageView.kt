//
//  VerifyNumberPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview

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
import us.neotechnica.panther.BuildConfig
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.components.PhoneNumberEntry
import us.neotechnica.panther.modules.content.onboarding.constants.VerifyNumberPageViewColors
import us.neotechnica.panther.modules.content.onboarding.constants.VerifyNumberPageViewFloats
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Colors = VerifyNumberPageViewColors
private typealias Floats = VerifyNumberPageViewFloats

/**
 * The onboarding page for entering a phone number during sign-up.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun VerifyNumberPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(VerifyNumberPageReducer.State(), VerifyNumberPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(VerifyNumberPageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(
        state = state.viewState,
        modifier = modifier,
        onRetry = { viewModel.send(VerifyNumberPageReducer.Action.ViewAppeared) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InstructionView(state.instructionViewStrings)

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = Floats.innerVStackBottomPadding),
            ) {
                Components.Text(
                    state.strings.value(VerifyNumberPageViewStrings.instructionLabelText),
                    color = colors.subtitleText,
                    font = Font.systemSemibold(),
                    modifier = Modifier.padding(vertical = Floats.instructionLabelVerticalPadding),
                )

                PhoneNumberEntry(
                    selectedRegionCode = state.selectedRegionCode,
                    phoneNumber = state.phoneNumberString.digits,
                    onRegionCodeSelected = { viewModel.send(VerifyNumberPageReducer.Action.SelectedRegionCodeChanged(it)) },
                    onPhoneNumberChange = { viewModel.send(VerifyNumberPageReducer.Action.PhoneNumberStringChanged(it)) },
                )

                Components.CapsuleButton(
                    text = state.strings.value(VerifyNumberPageViewStrings.continueButtonText),
                    onClick = { viewModel.send(VerifyNumberPageReducer.Action.ContinueButtonTapped) },
                    isEnabled = state.isContinueButtonEnabled,
                    primary = true,
                    modifier = Modifier.padding(vertical = Floats.continueButtonVerticalPadding),
                )

                Components.Button(
                    text = state.strings.value(VerifyNumberPageViewStrings.backButtonText),
                    color = if (state.isBackButtonEnabled) colors.titleText else colors.disabled,
                    onClick = { if (state.isBackButtonEnabled) viewModel.send(VerifyNumberPageReducer.Action.BackButtonTapped) },
                    font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                    modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                )

                if (BuildConfig.DEBUG && state.hasError) {
                    Components.Button(
                        text = "Force Continue (Debug)",
                        color = Colors.debugForeground,
                        onClick = { viewModel.send(VerifyNumberPageReducer.Action.DebugForceContinueTapped) },
                        font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                        modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
