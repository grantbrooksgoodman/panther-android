//
//  NewChatPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.ContactRow
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageView
import us.neotechnica.panther.modules.content.user.views.newchatpageview.NewChatPageReducer.Action
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

/**
 * The new-conversation page: add recipients via the recipient bar (typing
 * a phone number or picking from contacts), compose a first message, and
 * create the conversation on send. Mirrors the iOS `NewChatPageView`.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun NewChatPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(NewChatPageReducer.State(), NewChatPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(Action.ViewFirstAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Header(onClose = { viewModel.send(Action.BackTapped) })

            RecipientBar(
                recipients = state.recipients,
                query = state.recipientQuery,
                onQueryChange = { viewModel.send(Action.RecipientQueryChanged(it)) },
                onSubmit = { viewModel.send(Action.RecipientQuerySubmitted) },
                onRemove = { viewModel.send(Action.RemoveRecipient(it)) },
                onAdd = { viewModel.send(Action.ShowContactSelector) },
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.suggestions, key = { it.userID }) { contact ->
                    ContactRow(
                        name = contact.fullName,
                        initials = contact.initials,
                        onClick = { viewModel.send(Action.AddRecipient(contact.userID, contact.fullName)) },
                    )
                    HorizontalDivider(color = colors.groupedContentBackground)
                }
            }

            MessageInputBar(
                text = state.inputText,
                placeholder = LocalizedStringKey.NewMessage.localized(),
                isSending = state.isSending,
                onTextChange = { viewModel.send(Action.InputChanged(it)) },
                onSend = { viewModel.send(Action.SendTapped) },
                onAttach = {},
                enabled = state.recipients.isNotEmpty(),
            )
        }

        if (state.isShowingContactSelector) {
            ContactSelectorPageView(
                onSelect = { userID, displayName -> viewModel.send(Action.AddRecipient(userID, displayName)) },
                onDismiss = { viewModel.send(Action.DismissContactSelector) },
            )
        }
    }
}

// MARK: - Header

@Composable
private fun Header(onClose: () -> Unit) {
    val colors = LocalPantherColors.current
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Components.Text(
            "New Message",
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.background)
                    .clickable(onClick = onClose),
        ) {
            Components.Symbol("xmark", color = colors.titleText, modifier = Modifier.size(18.dp))
        }
    }
}

// MARK: - Recipient Bar

@Composable
@Suppress("LongParameterList")
private fun RecipientBar(
    recipients: List<NewChatPageReducer.Recipient>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(RECIPIENT_BAR_RADIUS))
                .background(colors.background)
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Components.Text("To:", color = colors.subtitleText, modifier = Modifier.padding(end = 6.dp))

        RecipientBarContent(
            recipients = recipients,
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = onSubmit,
            onRemove = onRemove,
            modifier = Modifier.weight(1f),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(start = 6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.groupedContentBackground)
                    .clickable(onClick = onAdd),
        ) {
            Components.Symbol("plus", color = colors.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
private fun RecipientBarContent(
    recipients: List<NewChatPageReducer.Recipient>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        recipients.forEach { recipient ->
            RecipientChip(recipient = recipient, onRemove = { onRemove(recipient.userID) })
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = Font.system.textStyle.copy(color = colors.titleText),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.defaultMinSize(minWidth = RECIPIENT_FIELD_MIN_WIDTH).padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun RecipientChip(
    recipient: NewChatPageReducer.Recipient,
    onRemove: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = CHIP_BACKGROUND_ALPHA))
                .clickable(onClick = onRemove)
                .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Components.Text(recipient.displayName, color = colors.accent, font = Font.systemMedium(FontScale.Small))
        Components.Symbol("xmark", color = colors.accent, modifier = Modifier.padding(start = 4.dp).size(12.dp))
    }
}

private val RECIPIENT_BAR_RADIUS = 20.dp
private val RECIPIENT_FIELD_MIN_WIDTH = 120.dp
private const val CHIP_BACKGROUND_ALPHA = 0.15f
