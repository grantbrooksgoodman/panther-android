//
//  AlertHost.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
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
    val alert by AlertPresenter.current.collectAsState()

    when (val presented = alert) {
        null -> Unit
        is PresentedAlert.Standard -> StandardDialog(presented)
        is PresentedAlert.Confirmation -> ConfirmationDialog(presented)
        is PresentedAlert.ErrorContent -> ErrorDialog(presented)
        is PresentedAlert.TextInput -> TextInputDialog(presented)
        is PresentedAlert.ActionSheet -> ActionSheetSheet(presented)
        is PresentedAlert.Progress -> ProgressDialog(presented)
    }
}

// MARK: - Action Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSheetSheet(alert: PresentedAlert.ActionSheet) {
    ModalBottomSheet(onDismissRequest = { alert.onResult(false) }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            alert.title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { alert.onResult(true) },
            ) {
                Text(alert.confirmButtonTitle)
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { alert.onResult(false) },
            ) {
                Text(alert.cancelButtonTitle)
            }
        }
    }
}

// MARK: - Standard

@Composable
private fun StandardDialog(alert: PresentedAlert.Standard) {
    val cancelIndex = alert.actions.indexOfFirst { it.style == ActionStyle.CANCEL }
    AlertDialog(
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                alert.actions.forEachIndexed { index, action ->
                    ActionButton(action) { alert.onSelect(index) }
                }
            }
        },
        onDismissRequest = {
            alert.onSelect(if (cancelIndex >= 0) cancelIndex else alert.actions.lastIndex)
        },
        text = alert.message?.let { { Text(it) } },
        title = alert.title?.let { { Text(it) } },
    )
}

// MARK: - Confirmation

@Composable
private fun ConfirmationDialog(alert: PresentedAlert.Confirmation) {
    AlertDialog(
        confirmButton = { ActionButton(alert.confirmAction) { alert.onResult(true) } },
        dismissButton = { ActionButton(alert.cancelAction) { alert.onResult(false) } },
        onDismissRequest = { alert.onResult(false) },
        text = { Text(alert.message) },
        title = alert.title?.let { { Text(it) } },
    )
}

// MARK: - Error

@Composable
private fun ErrorDialog(alert: PresentedAlert.ErrorContent) {
    AlertDialog(
        confirmButton = {
            TextButton(onClick = alert.onDismiss) { Text(alert.dismissButtonTitle) }
        },
        dismissButton =
            alert.sendReportButtonTitle?.let { title ->
                { TextButton(onClick = { alert.onSendReport?.invoke() }) { Text(title) } }
            },
        onDismissRequest = alert.onDismiss,
        text = { Text(alert.message) },
        title = { Text(alert.title) },
    )
}

// MARK: - Text Input

@Composable
private fun TextInputDialog(alert: PresentedAlert.TextInput) {
    var text by rememberSaveable(alert) { mutableStateOf(alert.initialText) }
    AlertDialog(
        confirmButton = {
            TextButton(onClick = { alert.onResult(text) }) { Text(alert.confirmButtonTitle) }
        },
        dismissButton = {
            TextButton(onClick = { alert.onResult(null) }) { Text(alert.cancelButtonTitle) }
        },
        onDismissRequest = { alert.onResult(null) },
        text = {
            Column {
                Text(alert.message)
                OutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    onValueChange = { text = it },
                    placeholder = { Text(alert.placeholder) },
                    singleLine = true,
                    value = text,
                    visualTransformation =
                        if (alert.isSecure) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                )
            }
        },
        title = alert.title?.let { { Text(it) } },
    )
}

// MARK: - Progress

@Composable
private fun ProgressDialog(alert: PresentedAlert.Progress) {
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                alert.title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                CircularProgressIndicator()
                Text(alert.message)
                alert.cancelButtonTitle?.let { title ->
                    TextButton(onClick = { alert.onCancel?.invoke() }) { Text(title) }
                }
            }
        }
    }
}

// MARK: - Action Button

@Composable
private fun ActionButton(
    action: Action,
    onClick: () -> Unit,
) {
    val color =
        when {
            action.style.isDestructive -> MaterialTheme.colorScheme.error
            else -> Color.Unspecified
        }

    TextButton(
        enabled = action.isEnabled,
        onClick = onClick,
    ) {
        Text(
            action.title,
            color = color,
            fontWeight = if (action.style.isPreferred) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
