//
//  ContactSelectorPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.SearchBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.content.user.components.ContactRow
import us.neotechnica.panther.modules.content.user.constants.ContactSelectorPageViewFloats
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = ContactSelectorPageViewFloats

/**
 * A contact picker: search the device's matched contacts, or enter a
 * phone number to look up its registered user. Presented as a sheet
 * either from the chat info page – to add a participant – or from the
 * new chat page – to choose a recipient.
 *
 * @param entryPoint The context the page was presented from.
 * @param onSelectRecipient Invoked with a selection's user id and
 *   display name when presented from the new chat page.
 * @param modifier The modifier for this view.
 */
@Composable
fun ContactSelectorPageView(
    entryPoint: ContactSelectorPageReducer.EntryPoint,
    onSelectRecipient: (userID: String, displayName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val viewModel =
        remember(entryPoint) {
            ViewModel(ContactSelectorPageReducer.State(entryPoint), ContactSelectorPageReducer(onSelectRecipient))
        }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(ContactSelectorPageReducer.Action.ViewAppeared) }
    BackHandler { viewModel.send(ContactSelectorPageReducer.Action.CancelToolbarButtonTapped) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(state = state.viewState, modifier = modifier.background(colors.groupedContentBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Header(
                title = state.navigationTitle,
                showInvite = state.shouldShowInviteButton,
                inviteText = state.inviteToolbarButtonText,
                onInvite = { viewModel.send(ContactSelectorPageReducer.Action.InviteToolbarButtonTapped) },
                onCancel = { viewModel.send(ContactSelectorPageReducer.Action.CancelToolbarButtonTapped) },
            )

            SearchBar(
                value = state.searchQuery,
                placeholder = state.searchBarPlaceholderText,
                onValueChange = { viewModel.send(ContactSelectorPageReducer.Action.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = Floats.searchHorizontalPadding, vertical = Floats.searchVerticalPadding),
                containerColor = colors.background,
            )

            if (state.queriedContactPairs.isNotEmpty()) {
                ContactList(
                    state = state,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onSelect = { viewModel.send(ContactSelectorPageReducer.Action.SelectedContactPairChanged(it)) },
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    NoResultsView(
                        text = state.noResultsLabelText,
                        isFindUser = state.noResultsLabelText != LocalizedStringKey.NoResults.localized(),
                        onFindUser = { viewModel.send(ContactSelectorPageReducer.Action.FindUserButtonTapped) },
                    )
                }
            }
        }
    }
}

// MARK: - Header

@Composable
private fun Header(
    title: String,
    showInvite: Boolean,
    inviteText: String,
    onInvite: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding),
    ) {
        if (showInvite) {
            Components.Text(
                inviteText,
                color = colors.accent,
                modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onInvite),
            )
        }
        Components.Text(
            title,
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        Components.Text(
            LocalizedStringKey.Cancel.localized(),
            color = colors.accent,
            modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onCancel),
        )
    }
}

// MARK: - Contact List

@Composable
private fun ContactList(
    state: ContactSelectorPageReducer.State,
    modifier: Modifier,
    onSelect: (ContactMatch) -> Unit,
) {
    val colors = LocalPantherColors.current
    val sections = state.sections
    LazyColumn(modifier = modifier) {
        sections.keys.sorted().forEach { letter ->
            item(key = "section-$letter") {
                Components.Text(
                    letter,
                    color = colors.subtitleText,
                    font = Font.systemSemibold(FontScale.Small),
                    modifier =
                        Modifier.padding(
                            horizontal = Floats.sectionHeaderHorizontalPadding,
                            vertical = Floats.sectionHeaderVerticalPadding,
                        ),
                )
            }
            items(sections[letter].orEmpty(), key = { it.userID }) { contact ->
                val (enabled, annotation) = contactRowState(contact, state.selectedContactPair)
                ContactRow(
                    name = contact.fullName,
                    initials = contact.initials,
                    onClick = { onSelect(contact) },
                    enabled = enabled,
                    annotation = annotation,
                )
                HorizontalDivider(color = colors.groupedContentBackground)
            }
        }
    }
}

// MARK: - No Results View

@Composable
private fun NoResultsView(
    text: String,
    isFindUser: Boolean,
    onFindUser: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val modifier =
        Modifier
            .then(if (isFindUser) Modifier.clickable(onClick = onFindUser) else Modifier)
            .padding(horizontal = Floats.emptyStateHorizontalPadding, vertical = Floats.emptyStateVerticalPadding)
    Components.Text(
        text,
        color = if (isFindUser) colors.accent else colors.subtitleText,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

// MARK: - Auxiliary

private fun contactRowState(
    contact: ContactMatch,
    selected: ContactMatch?,
): Pair<Boolean, String?> {
    val isBlocked = UserSessionService.currentUser?.blockedUserIDs?.contains(contact.userID) == true
    val isCurrentUser = contact.userID == User.currentUserID
    val isSelected = contact.userID == selected?.userID
    val isParticipant = ConversationSessionService.currentConversation?.participants?.any { it.userID == contact.userID } == true
    val annotation =
        when {
            isBlocked -> "(${LocalizedStringKey.Blocked.localized()})"
            isCurrentUser -> LocalizedStringKey.MyAccount.localized()
            else -> null
        }
    return !(isBlocked || isCurrentUser || isSelected || isParticipant) to annotation
}
