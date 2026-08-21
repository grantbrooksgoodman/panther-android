//
//  ChatInfoPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import androidx.compose.material3.Text as Material3Text

/**
 * A conversation's info page: participants and group/moderation actions.
 *
 * @param conversationIDKey The identifier key of the conversation.
 * @param modifier The modifier for this view.
 */
@Composable
fun ChatInfoPageView(
    conversationIDKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { ViewModel(ChatInfoPageReducer.State(), ChatInfoPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(conversationIDKey) {
        viewModel.send(ChatInfoPageReducer.Action.ViewFirstAppeared(conversationIDKey))
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            IconButton(onClick = { viewModel.send(ChatInfoPageReducer.Action.BackTapped) }, enabled = !state.isBusy) {
                Components.Symbol("chevron.left", color = colors.accent, modifier = Modifier.size(24.dp))
            }
            Components.Text("Info", color = colors.titleText, font = Font.systemBold(FontScale.Large))
        }

        if (state.isGroup) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = state.renameText,
                    onValueChange = { viewModel.send(ChatInfoPageReducer.Action.RenameChanged(it)) },
                    label = { Material3Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Components.Button(
                    "Save",
                    color = colors.accent,
                    onClick = { viewModel.send(ChatInfoPageReducer.Action.RenameSubmitted) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        SectionLabel("Participants")
        state.conversation?.participants.orEmpty().forEach { participant ->
            ParticipantRow(
                name = displayName(participant.userID),
                canRemove = state.isGroup && participant.userID != currentUserIDOrEmpty(),
                onRemove = { viewModel.send(ChatInfoPageReducer.Action.RemoveParticipant(participant.userID)) },
            )
        }

        if (state.isGroup && state.addableContacts.isNotEmpty()) {
            SectionLabel("Add participant")
            state.addableContacts.forEach { contact ->
                ActionRow(contact.fullName, colors.accent) {
                    viewModel.send(ChatInfoPageReducer.Action.AddParticipant(contact.userID))
                }
            }
        }

        SectionLabel("Actions")
        if (state.isGroup) {
            ActionRow("Leave conversation", DESTRUCTIVE_COLOR) { viewModel.send(ChatInfoPageReducer.Action.LeaveTapped) }
        } else {
            ActionRow("Block", DESTRUCTIVE_COLOR) { viewModel.send(ChatInfoPageReducer.Action.BlockTapped) }
            ActionRow("Report", DESTRUCTIVE_COLOR) { viewModel.send(ChatInfoPageReducer.Action.ReportTapped) }
        }
        ActionRow("Delete conversation", DESTRUCTIVE_COLOR) { viewModel.send(ChatInfoPageReducer.Action.DeleteTapped) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalPantherColors.current
    Components.Text(
        text,
        color = colors.subtitleText,
        font = Font.systemSemibold(FontScale.Small),
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ParticipantRow(
    name: String,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Components.Text(name, color = colors.titleText, modifier = Modifier.weight(1f))
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Components.Symbol("xmark", color = DESTRUCTIVE_COLOR, modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(color = colors.groupedContentBackground)
}

@Composable
private fun ActionRow(
    title: String,
    color: Color,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Components.Text(title, color = color)
    }
    HorizontalDivider(color = colors.groupedContentBackground)
}

private fun displayName(userID: String): String =
    ContactService.match(userID)?.fullName
        ?: SessionStore.users[userID]?.phoneNumber?.formattedString()
        ?: userID

private fun currentUserIDOrEmpty(): String = User.currentUserID ?: ""

private val DESTRUCTIVE_COLOR = Color(0xFFFF3B30)
