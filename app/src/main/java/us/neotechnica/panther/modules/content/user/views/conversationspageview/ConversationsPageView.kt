//
//  ConversationsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.bundle.updatedContactPairArchive
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.components.SearchBar
import us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellSwipeCoordinator
import us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellView
import us.neotechnica.panther.modules.content.user.components.conversationcellview.LocalConversationCellSwipeCoordinator
import us.neotechnica.panther.modules.content.user.components.conversationcellview.buildConversationCellViewModel
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewFloats
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
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
    val swipeCoordinator = remember { ConversationCellSwipeCoordinator() }

    ContextMenuHost(modifier = modifier.then(swipeCoordinator.dismissalModifier())) {
        CompositionLocalProvider(LocalConversationCellSwipeCoordinator provides swipeCoordinator) {
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
                    Toolbar(state = state, viewModel = viewModel)

                    Components.Text(
                        state.strings.value(ConversationsPageViewStrings.navigationTitle),
                        foregroundColor = colors.titleText,
                        font = Font.systemBold(FontScale.Large),
                        modifier = Modifier.padding(horizontal = Floats.titleHorizontalPadding, vertical = Floats.titleVerticalPadding),
                    )

                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.send(ConversationsPageReducer.Action.SearchQueryChanged(it)) },
                        backgroundColor = colors.background,
                        placeholderText = LocalizedStringKey.Search.localized(),
                        onFocusChanged = { viewModel.send(ConversationsPageReducer.Action.IsSearchingChanged(it)) },
                    )

                    Spacer(modifier = Modifier.height(Floats.searchBottomSpacing))

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
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
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

// MARK: - Toolbar

@Composable
private fun Toolbar(
    state: ConversationsPageReducer.State,
    viewModel: ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action>,
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
        ToolbarButton(
            symbolName = Constants.SETTINGS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME,
            foregroundColor = Colors.settingsToolbarButtonForeground,
            onClick = { viewModel.send(ConversationsPageReducer.Action.SettingsToolbarButtonTapped) },
        )

        if (state.shouldShowExtraToolbarButtons) {
            ToolbarButton(
                symbolName = Constants.DELETE_CONVERSATIONS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME,
                foregroundColor = Colors.deleteConversationsToolbarButtonForeground,
                onClick = { viewModel.send(ConversationsPageReducer.Action.DeleteConversationsToolbarButtonTapped) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        key(state.composeToolbarButtonViewID) {
            ComposeToolbarButton(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun ComposeToolbarButton(
    state: ConversationsPageReducer.State,
    viewModel: ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action>,
) {
    val isAnimated = state.conversations.isEmpty()
    LaunchedEffect(isAnimated) {
        if (isAnimated) viewModel.send(ConversationsPageReducer.Action.AnimatedComposeToolbarButtonAppeared)
    }

    // Once the reducer advances the amount past rest, the button pulses
    // between rest and the amount until the list fills.
    val scale =
        if (isAnimated && state.animationAmount != 1f) {
            rememberInfiniteTransition(label = COMPOSE_ANIMATION_LABEL)
                .animateFloat(
                    initialValue = 1f,
                    targetValue = state.animationAmount,
                    animationSpec =
                        infiniteRepeatable(
                            animation =
                                tween(
                                    durationMillis = (Floats.COMPOSE_TOOLBAR_BUTTON_ANIMATION_DURATION * MILLISECONDS_PER_SECOND).toInt(),
                                    delayMillis = (Floats.COMPOSE_TOOLBAR_BUTTON_ANIMATION_DELAY * MILLISECONDS_PER_SECOND).toInt(),
                                    easing = LinearEasing,
                                ),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = COMPOSE_ANIMATION_LABEL,
                ).value
        } else {
            1f
        }

    ToolbarButton(
        symbolName = Constants.COMPOSE_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME,
        foregroundColor = Colors.composeToolbarButtonForeground,
        onClick = { viewModel.send(ConversationsPageReducer.Action.ComposeToolbarButtonTapped) },
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun ToolbarButton(
    symbolName: String,
    foregroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier.defaultMinSize(
                minWidth = Floats.toolbarButtonFrameMinWidth,
                minHeight = Floats.toolbarButtonFrameMinHeight,
            ),
    ) {
        Components.Button(symbolName = symbolName, foregroundColor = foregroundColor, onClick = onClick)
    }
}

private const val COMPOSE_ANIMATION_LABEL = "ComposeToolbarButton"
private const val MILLISECONDS_PER_SECOND = 1000f
