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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import androidx.compose.material3.Text as Material3Text

/**
 * The new-conversation page: pick recipients and compose a first
 * message, creating the conversation on send.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun NewChatPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(NewChatPageReducer.State(), NewChatPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(NewChatPageReducer.Action.ViewFirstAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            IconButton(onClick = { viewModel.send(NewChatPageReducer.Action.BackTapped) }) {
                Components.Symbol("chevron.left", color = colors.accent, modifier = Modifier.size(24.dp))
            }
            Components.Text("New Message", color = colors.titleText, font = Font.systemBold(FontScale.Large))
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.send(NewChatPageReducer.Action.SearchChanged(it)) },
            label = { Material3Text(LocalizedStringKey.Search.localized()) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Top) {
            items(state.filteredContacts, key = { it.userID }) { contact ->
                ContactRow(
                    contact = contact,
                    isSelected = contact.userID in state.selectedUserIDs,
                    onClick = { viewModel.send(NewChatPageReducer.Action.ToggleSelected(contact.userID)) },
                )
                HorizontalDivider(color = colors.groupedContentBackground)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = { viewModel.send(NewChatPageReducer.Action.InputChanged(it)) },
                placeholder = { Material3Text(LocalizedStringKey.NewMessage.localized()) },
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { viewModel.send(NewChatPageReducer.Action.SendTapped) },
                enabled = state.canSend,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Components.Symbol(
                    "paperplane.fill",
                    color = if (state.canSend) colors.accent else colors.accent.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactMatch,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(
                        40.dp,
                    ).clip(CircleShape)
                    .background(if (isSelected) colors.accent else colors.groupedContentBackground),
        ) {
            Components.Symbol(
                if (isSelected) "checkmark" else "person",
                color = if (isSelected) colors.background else colors.subtitleText,
                modifier = Modifier.size(20.dp),
            )
        }
        Components.Text(contact.fullName, color = colors.titleText, font = Font.systemSemibold(), modifier = Modifier.weight(1f))
    }
}
