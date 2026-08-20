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
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.components.OnboardingBackButton
import us.neotechnica.panther.modules.content.onboarding.components.StatusIndicatorButton
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingBackButton(
                text = state.strings.value(PermissionPageViewStrings.backButtonText),
                isEnabled = state.isBackButtonEnabled,
                onClick = { viewModel.send(PermissionPageReducer.Action.BackButtonTapped) },
                modifier = Modifier.align(Alignment.Start),
            )

            InstructionView(state.instructionViewStrings, modifier = Modifier.fillMaxWidth())

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

            StatusIndicatorButton(
                label = state.strings.value(PermissionPageViewStrings.contactPermissionCapsuleButtonText),
                isGranted = state.isContactPermissionGranted,
                onClick = { contactLauncher.launch(Manifest.permission.READ_CONTACTS) },
            )

            Components.CapsuleButton(
                text = state.strings.value(PermissionPageViewStrings.finishButtonText),
                onClick = { viewModel.send(PermissionPageReducer.Action.FinishButtonTapped) },
                isEnabled = state.isFinishButtonEnabled,
            )
        }
    }
}
