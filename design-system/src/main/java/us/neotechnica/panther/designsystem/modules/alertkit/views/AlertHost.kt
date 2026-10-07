//
//  AlertHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.attributed
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.AttributedStringConfig
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
 * A bottom action sheet: an optional title and message, one plain
 * full-width text row per action separated by dividers, and a cancel
 * row separated at the bottom. Destructive rows are red and preferred
 * rows are bold. A title-only sheet promotes its title into the gray
 * message slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSheetSheet(alert: PresentedAlert.ActionSheet) {
    ModalBottomSheet(onDismissRequest = alert.onCancel) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
        ) {
            // A title-only sheet promotes its title into the gray message slot.
            val message = alert.message ?: alert.title?.takeIf { alert.title != null && alert.message == null }
            val heading = if (alert.message == null) null else alert.title
            val enabledOverrides by AlertPresenter.actionEnabledOverrides.collectAsState()
            heading?.let {
                Text(
                    it.styled(alert.titleAttributes),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            message?.let {
                Text(
                    it.styled(if (alert.message == null) alert.titleAttributes else alert.messageAttributes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            alert.actions.forEachIndexed { index, action ->
                HorizontalDivider()
                SheetActionRow(
                    title = action.title,
                    isDestructive = action.style.isDestructive,
                    isPreferred = action.style.isPreferred,
                    isEnabled = enabledOverrides[index] ?: action.isEnabled,
                    onClick = { alert.onSelect(index) },
                )
            }
            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            SheetActionRow(
                title = alert.cancelButtonTitle,
                isDestructive = false,
                isPreferred = true,
                isEnabled = true,
                onClick = alert.onCancel,
            )
        }
    }
}

@Composable
private fun SheetActionRow(
    title: String,
    isDestructive: Boolean,
    isPreferred: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            title.sanitized,
            color = if (isDestructive) ACTION_SHEET_DESTRUCTIVE_COLOR else MaterialTheme.colorScheme.primary,
            fontWeight = if (isPreferred) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

// MARK: - Standard

/**
 * A standard alert. Two actions sit in a row with the cancel action
 * leftmost; more than two stack in a column with the cancel action
 * at the bottom.
 */
@Composable
private fun StandardDialog(alert: PresentedAlert.Standard) {
    val cancelIndex = alert.actions.indexOfFirst { it.style == ActionStyle.CANCEL }
    val indexedActions = alert.actions.withIndex().toList()
    val enabledOverrides by AlertPresenter.actionEnabledOverrides.collectAsState()
    AlertDialog(
        properties = NON_DISMISSIBLE_PROPERTIES,
        confirmButton = {
            if (alert.actions.size > 2) {
                val columnActions =
                    indexedActions.filter { it.index != cancelIndex } +
                        indexedActions.filter { it.index == cancelIndex }

                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    columnActions.forEach { (index, action) ->
                        ActionButton(action, isEnabled = enabledOverrides[index] ?: action.isEnabled) {
                            alert.onSelect(index)
                        }
                    }
                }
            } else {
                val rowActions =
                    indexedActions.filter { it.index == cancelIndex } +
                        indexedActions.filter { it.index != cancelIndex }

                Row(horizontalArrangement = Arrangement.End) {
                    rowActions.forEach { (index, action) ->
                        ActionButton(action, isEnabled = enabledOverrides[index] ?: action.isEnabled) {
                            alert.onSelect(index)
                        }
                    }
                }
            }
        },
        onDismissRequest = {
            alert.onSelect(if (cancelIndex >= 0) cancelIndex else alert.actions.lastIndex)
        },
        text = alert.message?.let { { Text(it.styled(alert.messageAttributes)) } },
        title = alert.title?.let { { Text(it.styled(alert.titleAttributes)) } },
    )
}

// MARK: - Confirmation

@Composable
private fun ConfirmationDialog(alert: PresentedAlert.Confirmation) {
    val enabledOverrides by AlertPresenter.actionEnabledOverrides.collectAsState()
    AlertDialog(
        properties = NON_DISMISSIBLE_PROPERTIES,
        confirmButton = {
            ActionButton(alert.confirmAction, isEnabled = enabledOverrides[1] ?: alert.confirmAction.isEnabled) {
                alert.onResult(true)
            }
        },
        dismissButton = {
            ActionButton(alert.cancelAction, isEnabled = enabledOverrides[0] ?: alert.cancelAction.isEnabled) {
                alert.onResult(false)
            }
        },
        onDismissRequest = { alert.onResult(false) },
        text = { Text(alert.message.styled(alert.messageAttributes)) },
        title = alert.title?.let { { Text(it.styled(alert.titleAttributes)) } },
    )
}

// MARK: - Error

@Composable
private fun ErrorDialog(alert: PresentedAlert.ErrorContent) {
    AlertDialog(
        properties = NON_DISMISSIBLE_PROPERTIES,
        confirmButton = {
            val reportTitle = alert.sendReportButtonTitle
            if (reportTitle != null) {
                TextButton(onClick = { alert.onSendReport?.invoke() }) {
                    Text(reportTitle.sanitized, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = alert.onDismiss) {
                    Text(alert.dismissButtonTitle.sanitized, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton =
            alert.sendReportButtonTitle?.let {
                {
                    TextButton(onClick = alert.onDismiss) {
                        Text(alert.dismissButtonTitle.sanitized, fontWeight = FontWeight.Bold)
                    }
                }
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
    var isFocused by rememberSaveable(alert) { mutableStateOf(false) }
    val enabledOverrides by AlertPresenter.actionEnabledOverrides.collectAsState()
    AlertDialog(
        properties = NON_DISMISSIBLE_PROPERTIES,
        confirmButton = {
            TextButton(
                enabled = enabledOverrides[1] ?: true,
                onClick = { alert.onResult(text) },
            ) {
                Text(
                    alert.confirmButtonTitle.sanitized,
                    fontWeight = alert.confirmButtonStyle.buttonFontWeight,
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = enabledOverrides[0] ?: true,
                onClick = { alert.onResult(null) },
            ) {
                Text(
                    alert.cancelButtonTitle.sanitized,
                    fontWeight = alert.cancelButtonStyle.buttonFontWeight,
                )
            }
        },
        onDismissRequest = { alert.onResult(null) },
        text = {
            Column {
                Text(alert.message.styled(alert.messageAttributes))
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
                            .padding(top = 12.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                    onValueChange = {
                        text = it
                        alert.onTextFieldChange?.invoke(it)
                    },
                    placeholder = attributes.placeholderText?.let { { Text(it.sanitized) } },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = attributes.textAlignment),
                    trailingIcon =
                        if (showsClearButton(attributes.clearButtonMode, text, isFocused)) {
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
        title = alert.title?.let { { Text(it.styled(alert.titleAttributes)) } },
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
                val enabledOverrides by AlertPresenter.actionEnabledOverrides.collectAsState()
                alert.title?.let { Text(it.styled(alert.titleAttributes), style = MaterialTheme.typography.titleMedium) }
                Text(alert.message.styled(alert.messageAttributes))
                LinearProgressIndicator(
                    progress = { progress.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                alert.cancelButtonTitle?.let { title ->
                    TextButton(
                        enabled = enabledOverrides[0] ?: true,
                        onClick = { alert.onCancel?.invoke() },
                    ) {
                        Text(title.sanitized, fontWeight = alert.cancelButtonStyle.buttonFontWeight)
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
    isEnabled: Boolean = action.isEnabled,
    onClick: () -> Unit,
) {
    val color =
        when {
            action.style.isDestructive -> ACTION_SHEET_DESTRUCTIVE_COLOR
            else -> Color.Unspecified
        }

    TextButton(
        enabled = isEnabled,
        onClick = onClick,
    ) {
        Text(
            action.title.sanitized,
            color = color,
            fontWeight = action.style.buttonFontWeight,
        )
    }
}

// Applies the attributed-string configuration to the sanitized text.
private fun String.styled(config: AttributedStringConfig?): AnnotatedString =
    config?.let { sanitized.attributed(it) } ?: AnnotatedString(sanitized)

private fun showsClearButton(
    clearButtonMode: TextFieldAttributes.ClearButtonMode,
    text: String,
    isFocused: Boolean,
): Boolean {
    if (text.isEmpty()) return false
    return when (clearButtonMode) {
        TextFieldAttributes.ClearButtonMode.NEVER -> false
        TextFieldAttributes.ClearButtonMode.WHILE_EDITING -> isFocused
        TextFieldAttributes.ClearButtonMode.UNLESS_EDITING -> !isFocused
        TextFieldAttributes.ClearButtonMode.ALWAYS -> true
    }
}

// Preferred and cancel actions render bold.
private val ActionStyle.buttonFontWeight: FontWeight
    get() = if (isPreferred || this == ActionStyle.CANCEL) FontWeight.Bold else FontWeight.Normal

private val ACTION_SHEET_DESTRUCTIVE_COLOR = Color(0xFFFF3B30)

// Alerts dismiss only through an action, never by an outside tap or
// the system back gesture.
private val NON_DISMISSIBLE_PROPERTIES =
    DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
    )
