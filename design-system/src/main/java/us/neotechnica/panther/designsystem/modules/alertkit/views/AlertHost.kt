//
//  AlertHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 02/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
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
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
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

/**
 * A bottom action sheet: a title, an optional message, one filled
 * rounded-pill button per action, and a plain cancel button. A binary
 * confirm/cancel sheet is the single-action case, so its confirm button
 * matches every multi-action option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSheetSheet(alert: PresentedAlert.ActionSheet) {
    ModalBottomSheet(onDismissRequest = alert.onCancel) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            alert.title?.let { Text(it.sanitized, style = MaterialTheme.typography.titleMedium) }
            alert.message?.let { Text(it.sanitized, style = MaterialTheme.typography.bodyMedium) }
            alert.actions.forEachIndexed { index, action ->
                SheetActionButton(
                    title = action.title,
                    isDestructive = action.style.isDestructive,
                    isEnabled = action.isEnabled,
                    onClick = { alert.onSelect(index) },
                )
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = alert.onCancel,
            ) {
                Text(alert.cancelButtonTitle.sanitized)
            }
        }
    }
}

@Composable
private fun SheetActionButton(
    title: String,
    isDestructive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        colors =
            if (isDestructive) {
                ButtonDefaults.buttonColors(containerColor = ACTION_SHEET_DESTRUCTIVE_COLOR, contentColor = Color.White)
            } else {
                ButtonDefaults.buttonColors(contentColor = Color.White)
            },
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(title.sanitized)
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
        text = alert.message?.let { { Text(it.sanitized) } },
        title = alert.title?.let { { Text(it.sanitized) } },
    )
}

// MARK: - Confirmation

@Composable
private fun ConfirmationDialog(alert: PresentedAlert.Confirmation) {
    AlertDialog(
        confirmButton = { ActionButton(alert.confirmAction) { alert.onResult(true) } },
        dismissButton = { ActionButton(alert.cancelAction) { alert.onResult(false) } },
        onDismissRequest = { alert.onResult(false) },
        text = { Text(alert.message.sanitized) },
        title = alert.title?.let { { Text(it.sanitized) } },
    )
}

// MARK: - Error

@Composable
private fun ErrorDialog(alert: PresentedAlert.ErrorContent) {
    AlertDialog(
        confirmButton = {
            val reportTitle = alert.sendReportButtonTitle
            if (reportTitle != null) {
                TextButton(onClick = { alert.onSendReport?.invoke() }) {
                    Text(reportTitle.sanitized, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = alert.onDismiss) { Text(alert.dismissButtonTitle.sanitized) }
            }
        },
        dismissButton =
            alert.sendReportButtonTitle?.let {
                { TextButton(onClick = alert.onDismiss) { Text(alert.dismissButtonTitle.sanitized) } }
            },
        onDismissRequest = alert.onDismiss,
        text = { Text(alert.message.sanitized) },
        title = alert.title?.let { { Text(it.sanitized) } },
    )
}

// MARK: - Text Input

@Composable
private fun TextInputDialog(alert: PresentedAlert.TextInput) {
    val attributes = alert.attributes
    var text by rememberSaveable(alert) { mutableStateOf(attributes.sampleText.orEmpty()) }
    AlertDialog(
        confirmButton = {
            TextButton(
                enabled = alert.isConfirmEnabled?.invoke(text) ?: true,
                onClick = { alert.onResult(text) },
            ) {
                Text(
                    alert.confirmButtonTitle.sanitized,
                    fontWeight = if (alert.confirmButtonStyle.isPreferred) FontWeight.Bold else FontWeight.Normal,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { alert.onResult(null) }) {
                Text(
                    alert.cancelButtonTitle.sanitized,
                    fontWeight = if (alert.cancelButtonStyle.isPreferred) FontWeight.Bold else FontWeight.Normal,
                )
            }
        },
        onDismissRequest = { alert.onResult(null) },
        text = {
            Column {
                Text(alert.message.sanitized)
                OutlinedTextField(
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = attributes.capitalizationType,
                            autoCorrectEnabled = attributes.correctionType != TextFieldAttributes.CorrectionType.NO,
                            keyboardType = attributes.keyboardType,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    onValueChange = { text = it },
                    placeholder = attributes.placeholderText?.let { { Text(it.sanitized) } },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = attributes.textAlignment),
                    trailingIcon =
                        if (attributes.clearButtonMode != TextFieldAttributes.ClearButtonMode.NEVER && text.isNotEmpty()) {
                            { IconButton(onClick = { text = "" }) { Icon(Icons.Filled.Clear, contentDescription = null) } }
                        } else {
                            null
                        },
                    value = text,
                    visualTransformation =
                        if (attributes.isSecureTextEntry) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                )
            }
        },
        title = alert.title?.let { { Text(it.sanitized) } },
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
            val progress by alert.progress.collectAsState()
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                alert.title?.let { Text(it.sanitized, style = MaterialTheme.typography.titleMedium) }
                LinearProgressIndicator(
                    progress = { progress.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(alert.message.sanitized)
                alert.cancelButtonTitle?.let { title ->
                    TextButton(onClick = { alert.onCancel?.invoke() }) {
                        Text(
                            title.sanitized,
                            fontWeight = if (alert.cancelButtonStyle.isPreferred) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
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
            action.style.isDestructive -> ACTION_SHEET_DESTRUCTIVE_COLOR
            else -> Color.Unspecified
        }

    TextButton(
        enabled = action.isEnabled,
        onClick = onClick,
    ) {
        Text(
            action.title.sanitized,
            color = color,
            fontWeight = if (action.style.isPreferred) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private val ACTION_SHEET_DESTRUCTIVE_COLOR = Color(0xFFFF3B30)
