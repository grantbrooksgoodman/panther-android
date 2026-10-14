//
//  AlertKitDialog.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitDialogState
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitPalette
import us.neotechnica.panther.designsystem.modules.foundation.components.DialogWindowEffect
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation

// A full-screen dialog window that dims the content behind it, fading
// the dimming view in and out with the given state on the alert
// animations. The content animates with the transition it receives; once the state finishes
// animating out, the state performs its pending actions. When the
// composition provides a withdrawal action, the dialog animates out,
// then performs it.
@Composable
internal fun AlertKitDialog(
    state: AlertKitDialogState,
    onBackPressed: () -> Unit = {},
    onDimmingViewTapped: (() -> Unit)? = null,
    content: @Composable BoxScope.(Transition<Boolean>) -> Unit,
) {
    val palette = AlertKitPalette.current
    val transition = rememberTransition(state.visibleState)

    val onWithdrawn = LocalAlertWithdrawal.current
    LaunchedEffect(state.visibleState.isIdle) { state.performPendingActions() }
    LaunchedEffect(onWithdrawn) { onWithdrawn?.let { state.withdraw(it) } }

    Dialog(
        onDismissRequest = onBackPressed,
        properties =
            DialogProperties(
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        DialogWindowEffect(palette.isDarkModeActive)
        Box(modifier = Modifier.fillMaxSize()) {
            transition.AnimatedVisibility(
                enter = fadeIn(SystemAnimation.alertPresentation()),
                exit = fadeOut(SystemAnimation.alertDismissal()),
                visible = { it },
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(palette.dimming)
                            .then(
                                if (onDimmingViewTapped == null) {
                                    Modifier
                                } else {
                                    Modifier.clickable(
                                        indication = null,
                                        interactionSource = null,
                                        onClick = onDimmingViewTapped,
                                    )
                                },
                            ),
                )
            }

            content(transition)
        }
    }
}

// The action performed once a withdrawn alert's dialog finishes
// animating out, or `null` while the alert is presented.
internal val LocalAlertWithdrawal = staticCompositionLocalOf<(() -> Unit)?> { null }
