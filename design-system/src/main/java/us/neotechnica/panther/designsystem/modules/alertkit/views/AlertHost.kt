//
//  AlertHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import us.neotechnica.panther.designsystem.modules.alertkit.components.AlertProgressView
import us.neotechnica.panther.designsystem.modules.alertkit.components.AlertTextFieldView
import us.neotechnica.panther.designsystem.modules.alertkit.components.LocalAlertWithdrawal
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.styled
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.DisplayedAlerts
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert

/**
 * Renders the alert currently requested through [AlertPresenter].
 *
 * Place a single [AlertHost] near the root of the composition, above
 * the app's content, so that alerts presented from anywhere appear over
 * the current screen.
 */
@Composable
fun AlertHost() {
    val current by AlertPresenter.current.collectAsState()
    val displayedAlerts = remember { DisplayedAlerts() }

    SideEffect { displayedAlerts.update(current) }

    // An alert dismissed other than through one of its actions stays
    // composed until it finishes animating out.
    displayedAlerts.alerts(current).forEach { alert ->
        key(alert) {
            val onWithdrawn = if (alert === current) null else remember { { displayedAlerts.remove(alert) } }
            CompositionLocalProvider(LocalAlertWithdrawal provides onWithdrawn) {
                when (alert) {
                    is PresentedAlert.ActionSheet -> ActionSheetView(alert)
                    is PresentedAlert.Confirmation -> ConfirmationAlertView(alert)
                    is PresentedAlert.ErrorContent -> ErrorAlertView(alert)
                    is PresentedAlert.Progress -> ProgressAlertView(alert)
                    is PresentedAlert.Standard -> StandardAlertView(alert)
                    is PresentedAlert.TextInput -> TextInputAlertView(alert)
                }
            }
        }
    }
}

// MARK: - Alerts

@Composable
private fun ConfirmationAlertView(alert: PresentedAlert.Confirmation) {
    AlertView(
        actions = remember(alert) { listOf(alert.cancelAction, alert.confirmAction) },
        message = alert.message.styled(alert.messageAttributes),
        title = alert.title?.styled(alert.titleAttributes),
    ) { index -> alert.onResult(index == CONFIRMATION_CONFIRM_ACTION_INDEX) }
}

@Composable
private fun ErrorAlertView(alert: PresentedAlert.ErrorContent) {
    // The dismiss action cancels; the report action, when offered, is
    // preferred.
    val actions =
        remember(alert) {
            listOfNotNull(
                Action(alert.dismissButtonTitle, style = ActionStyle.CANCEL) {},
                alert.sendReportButtonTitle?.let { Action(it, style = ActionStyle.PREFERRED) {} },
            )
        }

    AlertView(
        actions = actions,
        message = alert.message.styled(null),
        title = alert.title?.styled(null),
    ) { index -> if (index == ERROR_DISMISS_ACTION_INDEX) alert.onDismiss() else alert.onSendReport?.invoke() }
}

@Composable
private fun ProgressAlertView(alert: PresentedAlert.Progress) {
    val actions =
        remember(alert) {
            listOfNotNull(alert.cancelButtonTitle?.let { Action(it, style = alert.cancelButtonStyle) {} })
        }

    AlertView(
        accessory = {
            AlertProgressView(
                hasCancelAction = actions.isNotEmpty(),
                progress = alert.progress,
            )
        },
        actions = actions,
        message = alert.message.styled(alert.messageAttributes),
        title = alert.title?.styled(alert.titleAttributes),
    ) { alert.onCancel?.invoke() }
}

@Composable
private fun StandardAlertView(alert: PresentedAlert.Standard) {
    AlertView(
        actions = alert.actions,
        message = alert.message?.styled(alert.messageAttributes),
        onSelect = alert.onSelect,
        title = alert.title?.styled(alert.titleAttributes),
    )
}

@Composable
private fun TextInputAlertView(alert: PresentedAlert.TextInput) {
    val attributes = alert.attributes
    var text by remember(alert) { mutableStateOf(attributes.sampleText?.sanitized.orEmpty()) }
    val actions =
        remember(alert) {
            listOf(
                Action(alert.cancelButtonTitle, style = alert.cancelButtonStyle) {},
                Action(alert.confirmButtonTitle, style = alert.confirmButtonStyle) {},
            )
        }

    AlertView(
        accessory = { onSubmit ->
            AlertTextFieldView(
                attributes = attributes,
                onDone = onSubmit,
                onValueChange = {
                    text = it
                    alert.onTextFieldChange?.invoke(it)
                },
            )
        },
        actions = actions,
        message = alert.message.styled(alert.messageAttributes),
        title = alert.title?.styled(alert.titleAttributes),
    ) { index -> alert.onResult(if (index == TEXT_INPUT_CONFIRM_ACTION_INDEX) text else null) }
}

// MARK: - Auxiliary

private const val CONFIRMATION_CONFIRM_ACTION_INDEX = 1
private const val ERROR_DISMISS_ACTION_INDEX = 0
private const val TEXT_INPUT_CONFIRM_ACTION_INDEX = 1
