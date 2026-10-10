//
//  PermissionPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.permissionpageview

import androidx.compose.foundation.layout.Arrangement
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
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.components.StatusIndicatorButton
import us.neotechnica.panther.modules.content.onboarding.constants.PermissionsViewFloats
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = PermissionsViewFloats

/**
 * The final onboarding page: granting the contact and notification
 * permissions, then finishing to create the account.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun PermissionPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(PermissionPageReducer.State(), PermissionPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(PermissionPageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(state = state.viewState, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            InstructionView(state.instructionViewStrings)

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = Floats.innerVStackBottomPadding),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Floats.buttonVStackSpacing),
                    modifier = Modifier.padding(bottom = Floats.buttonVStackBottomPadding),
                ) {
                    StatusIndicatorButton(
                        text = state.strings.value(PermissionPageViewStrings.contactPermissionCapsuleButtonText),
                        isCompleted = state.isContactPermissionGranted,
                    ) {
                        viewModel.send(PermissionPageReducer.Action.ContactPermissionCapsuleButtonTapped)
                    }

                    StatusIndicatorButton(
                        text = state.strings.value(PermissionPageViewStrings.notificationPermissionCapsuleButtonText),
                        isCompleted = state.isNotificationPermissionGranted,
                    ) {
                        viewModel.send(PermissionPageReducer.Action.NotificationPermissionCapsuleButtonTapped)
                    }
                }

                Components.CapsuleButton(
                    text = state.strings.value(PermissionPageViewStrings.finishButtonText),
                    foregroundColor = if (state.isFinishButtonEnabled) colors.background else colors.disabled,
                    isEnabled = state.isFinishButtonEnabled,
                    action = { viewModel.send(PermissionPageReducer.Action.FinishButtonTapped) },
                    modifier = Modifier.padding(top = Floats.finishButtonTopPadding),
                )

                Components.Button(
                    text = state.strings.value(PermissionPageViewStrings.backButtonText),
                    foregroundColor = if (state.isBackButtonEnabled) colors.accent else colors.disabled,
                    onClick = { if (state.isBackButtonEnabled) viewModel.send(PermissionPageReducer.Action.BackButtonTapped) },
                    font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                    modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
