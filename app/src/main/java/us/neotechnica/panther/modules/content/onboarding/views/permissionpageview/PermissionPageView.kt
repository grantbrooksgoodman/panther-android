//
//  PermissionPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.permissionpageview

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import us.neotechnica.panther.modules.content.onboarding.constants.PermissionPageViewFloats
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = PermissionPageViewFloats

/**
 * The final onboarding page: granting notification and contact
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

    val contactLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.send(PermissionPageReducer.Action.RequestContactPermissionReturned(granted))
        }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.send(PermissionPageReducer.Action.RequestNotificationPermissionReturned(granted))
        }

    StatefulView(
        state = state.viewState,
        modifier = modifier,
        onRetry = { viewModel.send(PermissionPageReducer.Action.ViewAppeared) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InstructionView(state.instructionViewStrings)

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = Floats.innerVStackBottomPadding),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Floats.buttonSpacing),
                    modifier = Modifier.padding(bottom = Floats.buttonVStackBottomPadding),
                ) {
                    StatusIndicatorButton(
                        label = state.strings.value(PermissionPageViewStrings.contactPermissionCapsuleButtonText),
                        isGranted = state.isContactPermissionGranted,
                        onClick = { contactLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    )

                    StatusIndicatorButton(
                        label = state.strings.value(PermissionPageViewStrings.notificationPermissionCapsuleButtonText),
                        isGranted = state.isNotificationPermissionGranted,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.send(PermissionPageReducer.Action.RequestNotificationPermissionReturned(true))
                            }
                        },
                    )
                }

                Components.CapsuleButton(
                    text = state.strings.value(PermissionPageViewStrings.finishButtonText),
                    onClick = { viewModel.send(PermissionPageReducer.Action.FinishButtonTapped) },
                    isEnabled = state.isFinishButtonEnabled,
                    primary = true,
                    modifier = Modifier.padding(vertical = Floats.finishButtonVerticalPadding),
                )

                Components.Button(
                    text = state.strings.value(PermissionPageViewStrings.backButtonText),
                    color = if (state.isBackButtonEnabled) colors.titleText else colors.disabled,
                    onClick = { if (state.isBackButtonEnabled) viewModel.send(PermissionPageReducer.Action.BackButtonTapped) },
                    font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                    modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
