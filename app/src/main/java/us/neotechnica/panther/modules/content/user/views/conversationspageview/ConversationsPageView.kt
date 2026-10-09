//
//  ConversationsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.bundle.conversationsPageReappeared
import us.neotechnica.panther.bundle.conversationsSearchQuery
import us.neotechnica.panther.bundle.reloadingConversationIDKeys
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageContextMenu
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.components.SearchBar
import us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellReducer
import us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellView
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewFloats
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewStrings
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates

// MARK: - Constants Accessors

private typealias Floats = ConversationsPageViewFloats

/**
 * The conversations list page. Renders the current user's
 * conversations live from the session store, with search and
 * pull-to-refresh. Long-pressing a row lifts it and shows a menu to
 * delete the conversation or block or report its participants.
 *
 * @param viewModel The conversations page view model.
 * @param listState The list's scroll state.
 * @param modifier The modifier for this view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsPageView(
    viewModel: ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    ContextMenuHost(modifier = modifier) {
        StatefulView(state = state.viewState) {
            // The host fills the screen so the cell context menu's scrim covers
            // the system bars; the list content insets itself below them here.
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                Header(
                    showExtraButtons = state.shouldShowExtraToolbarButtons,
                    onSettings = { viewModel.send(ConversationsPageReducer.Action.SettingsToolbarButtonTapped) },
                    onNewChat = { viewModel.send(ConversationsPageReducer.Action.ComposeToolbarButtonTapped) },
                    onDeleteConversations = { viewModel.send(ConversationsPageReducer.Action.DeleteConversationsToolbarButtonTapped) },
                )

                Components.Text(
                    state.strings.value(ConversationsPageViewStrings.navigationTitle),
                    foregroundColor = colors.titleText,
                    font = Font.systemBold(FontScale.Large),
                    modifier = Modifier.padding(horizontal = Floats.titleHorizontalPadding, vertical = Floats.titleVerticalPadding),
                )

                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = {
                        viewModel.send(ConversationsPageReducer.Action.SearchQueryChanged(it))
                        viewModel.send(ConversationsPageReducer.Action.IsSearchingChanged(it.isNotBlank()))
                    },
                    backgroundColor = colors.background,
                    placeholderText = state.strings.value(ConversationsPageViewStrings.searchBarPlaceholder),
                )

                Spacer(modifier = Modifier.height(Floats.searchBottomSpacing))

                // Arm pull-to-refresh only when a scroll gesture begins with the
                // list already at its top. This keeps scrolling back up to the top
                // from converting into a refresh once the top is reached mid-drag.
                var isPullToRefreshEnabled by remember { mutableStateOf(true) }
                LaunchedEffect(listState) {
                    snapshotFlow { listState.isScrollInProgress }
                        .collect { isScrolling ->
                            if (isScrolling) {
                                isPullToRefreshEnabled =
                                    listState.firstVisibleItemIndex == 0 &&
                                    listState.firstVisibleItemScrollOffset == 0
                            }
                        }
                }

                val pullToRefreshState = rememberPullToRefreshState()
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pullToRefresh(
                                isRefreshing = state.isRefreshing,
                                state = pullToRefreshState,
                                enabled = isPullToRefreshEnabled,
                                threshold = Floats.pullToRefreshThreshold,
                                onRefresh = { viewModel.send(ConversationsPageReducer.Action.PulledToRefresh) },
                            ),
                ) {
                    val conversations = state.conversations
                    if (conversations.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Components.Text(
                                state.strings.value(ConversationsPageViewStrings.noConversationsLabelText),
                                foregroundColor = colors.subtitleText,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            item {
                                HorizontalDivider(
                                    color = colors.groupedContentBackground,
                                    modifier = Modifier.padding(start = ConversationCellViewFloats.textInset),
                                )
                            }
                            items(conversations, key = { it.id.key }) { conversation ->
                                ConversationCellMenu(conversation, state.searchQuery)
                                HorizontalDivider(
                                    color = colors.groupedContentBackground,
                                    modifier = Modifier.padding(start = ConversationCellViewFloats.textInset),
                                )
                            }
                        }
                    }

                    PullToRefreshDefaults.Indicator(
                        isRefreshing = state.isRefreshing,
                        state = pullToRefreshState,
                        maxDistance = Floats.pullToRefreshThreshold,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

/**
 * Builds the conversations page view-model, wired to session-store and
 * trait-collection changes. Hoisted to `UserContentContainer` so it –
 * and the list's scroll position – survive pushing to a chat and back.
 */
internal fun buildConversationsPageViewModel(): ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action> =
    ViewModel(ConversationsPageReducer.State(), ConversationsPageReducer())
        .observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) {
            ConversationsPageReducer.Action.SessionStoreDidChange
        }.observing(DependencyValues.current.sharedEvents.traitCollectionChanged.events) {
            ConversationsPageReducer.Action.TraitCollectionChanged
        }

// MARK: - Conversation Cell Menu

@Composable
private fun ConversationCellMenu(
    conversation: Conversation,
    initialSearchQuery: String,
) {
    val viewModel =
        remember(conversation.id.key) {
            ViewModel(
                ConversationCellReducer.State(conversationIDKey = conversation.id.key, searchQuery = initialSearchQuery),
                ConversationCellReducer(),
            ).observing(DependencyValues.current.sharedStates.conversationsSearchQuery.changes) {
                ConversationCellReducer.Action.SearchQueryChanged(it)
            }.observing(DependencyValues.current.sharedStates.reloadingConversationIDKeys.changes) {
                ConversationCellReducer.Action.ReloadingConversationsChanged(it)
            }.observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) {
                ConversationCellReducer.Action.SessionStoreDidChange(it)
            }.observing(DependencyValues.current.sharedEvents.conversationsPageReappeared.events) {
                ConversationCellReducer.Action.ReloadData
            }
        }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    LaunchedEffect(viewModel) { viewModel.send(ConversationCellReducer.Action.ViewAppeared) }

    val cellState by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    MessageContextMenu(
        liftedBackground = colors.background,
        // Align the menu with the avatar's leading edge, past the row's start
        // padding and the unread-indicator slot that precede it.
        menuLeadingOffset = ConversationCellViewFloats.rowStartPadding + ConversationCellViewFloats.unreadSlotWidth,
        actions =
            listOf(
                ContextMenuAction(
                    title = cellState.deleteConversationButtonText,
                    systemImageName = ConversationCellViewStrings.DELETE_CONVERSATION_BUTTON_IMAGE_SYSTEM_NAME,
                    isDestructive = true,
                ) { viewModel.send(ConversationCellReducer.Action.DeleteConversationButtonTapped) },
                ContextMenuAction(
                    title = cellState.blockUsersButtonText,
                    systemImageName = ConversationCellViewStrings.BLOCK_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                ) { viewModel.send(ConversationCellReducer.Action.BlockUsersButtonTapped) },
                ContextMenuAction(
                    title = cellState.reportUsersButtonText,
                    systemImageName = ConversationCellViewStrings.REPORT_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                ) { viewModel.send(ConversationCellReducer.Action.ReportUsersButtonTapped) },
            ),
        alignment = ContextMenuAlignment.LEADING,
        reactionChoices = emptyList(),
        liftScale = 0f,
        onTap = { viewModel.send(ConversationCellReducer.Action.CellTapped) },
    ) {
        ConversationCellView(
            state = cellState,
            onUserInfoBadgeTapped = { viewModel.send(ConversationCellReducer.Action.UserInfoBadgeTapped) },
        )
    }
}

// MARK: - Header

@Composable
private fun Header(
    showExtraButtons: Boolean,
    onSettings: () -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversations: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = Floats.headerHorizontalPadding,
                    end = Floats.headerHorizontalPadding,
                    top = Floats.headerTopPadding,
                ),
    ) {
        CircleChipButton(systemName = "gearshape", contentDescription = "Settings", onClick = onSettings)
        if (showExtraButtons) {
            CircleChipButton(
                systemName = "trash",
                contentDescription = "Delete conversations",
                onClick = onDeleteConversations,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        CircleChipButton(systemName = "square.and.pencil", contentDescription = "New conversation", onClick = onNewChat)
    }
}
