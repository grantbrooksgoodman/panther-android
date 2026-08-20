//
//  ConversationsPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.ConversationCell
import us.neotechnica.panther.networking.modules.session.extensions.sessionStoreDidChange
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import androidx.compose.material3.Text as Material3Text

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

    StatefulView(state = state.viewState, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Components.Text(
                state.strings.value(ConversationsPageViewStrings.navigationBarTitle),
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.send(ConversationsPageReducer.Action.SearchQueryChanged(it)) },
                label = { Material3Text(state.strings.value(ConversationsPageViewStrings.searchBarPlaceholder)) },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
            )

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
                        items(conversations, key = { it.id.key }) { conversation ->
                            ConversationCell(
                                conversation = conversation,
                                languageCode = languageCode,
                                changeToken = state.changeToken,
                            )
                            HorizontalDivider(color = colors.groupedContentBackground)
                        }
                    }
                }
            }
        }
    }
}
