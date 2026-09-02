//
//  ConversationsPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.components.SearchBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.ConversationCell
import us.neotechnica.panther.modules.content.user.constants.ConversationCellFloats
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.session.extensions.sessionStoreDidChange
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Floats = ConversationsPageViewFloats

/**
 * The conversations list page. Renders the current user's
 * conversations live from the session store, with search and
 * pull-to-refresh.
 *
 * @param modifier The modifier for this view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsPageView(modifier: Modifier = Modifier) {
    val viewModel =
        remember {
            ViewModel(ConversationsPageReducer.State(), ConversationsPageReducer())
                .observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) {
                    ConversationsPageReducer.Action.SessionStoreDidChange
                }
        }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(ConversationsPageReducer.Action.ViewFirstAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val languageCode = RuntimeStorage.languageCode
    val navigation = remember { DependencyValues.current.navigation }

    StatefulView(state = state.viewState, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                onSettings = {
                    navigation.navigate(
                        Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePath.Settings)),
                    )
                },
                onNewChat = {
                    navigation.navigate(
                        Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePath.NewChat)),
                    )
                },
            )

            Components.Text(
                state.strings.value(ConversationsPageViewStrings.navigationBarTitle),
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
                modifier = Modifier.padding(horizontal = Floats.titleHorizontalPadding, vertical = Floats.titleVerticalPadding),
            )

            SearchBar(
                value = state.searchQuery,
                placeholder = state.strings.value(ConversationsPageViewStrings.searchBarPlaceholder),
                onValueChange = { viewModel.send(ConversationsPageReducer.Action.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = Floats.searchHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(Floats.searchBottomSpacing))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.send(ConversationsPageReducer.Action.PulledToRefresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                val conversations = state.conversations
                if (conversations.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Components.Text(
                            state.strings.value(ConversationsPageViewStrings.noConversationsLabelText),
                            color = colors.subtitleText,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        item {
                            HorizontalDivider(
                                color = colors.groupedContentBackground,
                                modifier = Modifier.padding(start = ConversationCellFloats.textInset),
                            )
                        }
                        items(conversations, key = { it.id.key }) { conversation ->
                            ConversationCell(
                                conversation = conversation,
                                languageCode = languageCode,
                                changeToken = state.changeToken,
                                searchQuery = state.searchQuery,
                                modifier =
                                    Modifier.clickable {
                                        navigation.navigate(
                                            Route.UserContent(
                                                UserContentRoute.Push(
                                                    UserContentNavigatorState.SeguePath.Chat(conversation.id.key),
                                                ),
                                            ),
                                        )
                                    },
                            )
                            HorizontalDivider(
                                color = colors.groupedContentBackground,
                                modifier = Modifier.padding(start = ConversationCellFloats.textInset),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    onSettings: () -> Unit,
    onNewChat: () -> Unit,
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
        Spacer(modifier = Modifier.weight(1f))
        CircleChipButton(systemName = "square.and.pencil", contentDescription = "New conversation", onClick = onNewChat)
    }
}
