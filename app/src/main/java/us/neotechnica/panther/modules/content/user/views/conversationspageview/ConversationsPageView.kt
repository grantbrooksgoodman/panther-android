//
//  ConversationsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.bundle.updatedContactPairArchive
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.LocalSwipeActionsCoordinator
import us.neotechnica.panther.designsystem.modules.componentkit.models.SwipeActionsCoordinator
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.components.SearchBar
import us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellView
import us.neotechnica.panther.modules.content.user.components.conversationcellview.buildConversationCellViewModel
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewFloats
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Colors = ConversationsPageViewColors
private typealias Constants = ConversationsPageViewConstants
private typealias Floats = ConversationsPageViewFloats

/**
 * The conversations list page. Renders the current user's
 * conversations live from the session store, with search and
 * pull-to-refresh.
 *
 * Tapping the search bar begins a search: the toolbar and title
 * collapse, leaving the search bar and its Cancel button at the
 * top of the page. The search lasts until the user taps Cancel,
 * which clears the query and dismisses the keyboard. The system
 * back gesture ends a search the same way while the page is
 * uncovered.
 *
 * @param viewModel The conversations page view model.
 * @param listState The list's scroll state.
 * @param isCovered A Boolean value that indicates whether a
 *   pushed page covers this page. While it is covered, the system
 *   back gesture is left to the navigation stack, even during a
 *   search.
 * @param modifier The modifier for this view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsPageView(
    viewModel: ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action>,
    listState: LazyListState,
    isCovered: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val focusManager = LocalFocusManager.current
    val swipeCoordinator = remember { SwipeActionsCoordinator() }
    val endSearch: () -> Unit = {
        viewModel.send(ConversationsPageReducer.Action.SearchQueryChanged(""))
        viewModel.send(ConversationsPageReducer.Action.IsSearchingChanged(false))
    }

    BackHandler(enabled = state.isSearching && !isCovered) {
        focusManager.clearFocus()
        endSearch()
    }

    // A page pushed over the list – for example, a chat opened from the
    // search results – clears the search bar's focus, dismissing the
    // keyboard. The search itself stays active for the user's return.
    LaunchedEffect(isCovered) {
        if (isCovered && state.isSearching) focusManager.clearFocus()
    }

    ContextMenuHost(modifier = modifier.then(swipeCoordinator.dismissalModifier())) {
        CompositionLocalProvider(LocalSwipeActionsCoordinator provides swipeCoordinator) {
            StatefulView(state = state.viewState) {
                // The host fills the screen so the cell context menu's scrim covers
                // the system bars; the content insets itself below the status bar
                // here, and the list scrolls beneath the navigation bar.
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                ) {
                    Header(
                        isSearching = state.isSearching,
                        toolbar = { Toolbar(state = state, viewModel = viewModel) },
                        title = {
                            Components.Text(
                                state.strings.value(ConversationsPageViewStrings.navigationTitle, ConversationsPageViewStrings),
                                foregroundColor = colors.titleText,
                                font = Font.systemBold(FontScale.Large),
                                modifier =
                                    Modifier.padding(
                                        horizontal = Floats.titleHorizontalPadding,
                                        vertical = Floats.titleVerticalPadding,
                                    ),
                            )
                        },
                    ) {
                        // A search begins when the field gains focus and lasts until
                        // the user taps Cancel or goes back, whether or not the keyboard
                        // is shown.
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.send(ConversationsPageReducer.Action.SearchQueryChanged(it)) },
                            backgroundColor = colors.background,
                            placeholderText = LocalizedStringKey.Search.localized(),
                            onFocusChanged = { isFocused ->
                                if (isFocused) viewModel.send(ConversationsPageReducer.Action.IsSearchingChanged(true))
                            },
                            showsCancelButton = state.isSearching,
                            onCancel = endSearch,
                        )
                    }

                    ConversationList(state = state, listState = listState, viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Builds the conversations page view model, wired to session store,
 * trait collection, and contact pair archive changes. Hoisted to the
 * user content container so it – and the list's scroll position –
 * survive pushing to a chat and back.
 */
internal fun buildConversationsPageViewModel(): ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action> =
    ViewModel(ConversationsPageReducer.State(), ConversationsPageReducer())
        .observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) {
            ConversationsPageReducer.Action.SessionStoreDidChange
        }.observing(DependencyValues.current.sharedEvents.traitCollectionChanged.events) {
            ConversationsPageReducer.Action.TraitCollectionChanged
        }.observing(DependencyValues.current.sharedEvents.updatedContactPairArchive.events) {
            ConversationsPageReducer.Action.TraitCollectionChanged
        }

// MARK: - Conversation List

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationList(
    state: ConversationsPageReducer.State,
    listState: LazyListState,
    viewModel: ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action>,
) {
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
                    enabled = isPullToRefreshEnabled && !state.isSearching,
                    threshold = Floats.pullToRefreshThreshold,
                    onRefresh = { viewModel.send(ConversationsPageReducer.Action.PulledToRefresh) },
                ),
    ) {
        val conversations = state.conversations
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Search results scroll clear of the keyboard.
            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
            verticalArrangement = Arrangement.Top,
        ) {
            item {
                RowSeparator()
            }
            items(conversations, key = { it.id.key }) { conversation ->
                key(conversation.id.key) {
                    val cellViewModel =
                        remember(conversation.id.key) {
                            buildConversationCellViewModel(conversation.id.key, searchQuery = state.searchQuery)
                        }
                    ConversationCellView(viewModel = cellViewModel)
                }
                RowSeparator()
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

// The separator between rows, inset to align with the cell's title.
@Composable
private fun RowSeparator() {
    HorizontalDivider(
        color = LocalPantherColors.current.groupedContentBackground,
        modifier =
            Modifier.padding(
                start = ConversationCellViewFloats.separatorInset,
                end = ConversationCellViewFloats.rowEndPadding,
            ),
    )
}
