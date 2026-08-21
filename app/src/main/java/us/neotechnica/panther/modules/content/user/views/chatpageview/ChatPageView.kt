//
//  ChatPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.ChatMessageCell
import us.neotechnica.panther.modules.content.user.components.ChatMessageRowData
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.session.extensions.currentConversationDidBecomeUnavailable
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.isOutboxMessage
import us.neotechnica.panther.networking.modules.session.extensions.sessionStoreDidChange
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.MessageOutboxService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import androidx.compose.material3.Text as Material3Text

/**
 * The chat page for a single conversation.
 *
 * Renders the conversation's messages live, translated for the current
 * user, with a long-press context menu, delivery status, and an input
 * bar that sends through the outbox-backed delivery pipeline.
 *
 * @param conversationIDKey The identifier key of the conversation.
 * @param modifier The modifier for this view.
 */
@Composable
fun ChatPageView(
    conversationIDKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        remember {
            ViewModel(ChatPageReducer.State(), ChatPageReducer())
                .observing(ConversationSessionService.displayedMessages) {
                    ChatPageReducer.Action.MessagesUpdated(it)
                }.observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) {
                    ChatPageReducer.Action.StoreChanged
                }.observing(DependencyValues.current.sharedEvents.currentConversationDidBecomeUnavailable.events) {
                    ChatPageReducer.Action.ConversationUnavailable
                }
        }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(ChatPageReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }
    LaunchedEffect(conversationIDKey) {
        viewModel.send(ChatPageReducer.Action.ViewFirstAppeared(conversationIDKey))
    }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(state = state.viewState, modifier = modifier) {
        ContextMenuHost(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatHeader(
                    title = state.title,
                    onBack = {
                        DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                    },
                    onInfo = {
                        DependencyValues.current.navigation.navigate(
                            Route.UserContent(
                                UserContentRoute.Push(
                                    UserContentNavigatorState.SeguePath.ChatInfo(state.conversationIDKey),
                                ),
                            ),
                        )
                    },
                )

                MessageList(
                    state = state,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onToggleAlternate = { viewModel.send(ChatPageReducer.Action.ToggleAlternate(it)) },
                )

                InputBar(
                    text = state.inputText,
                    isSending = state.isSending,
                    onTextChange = { viewModel.send(ChatPageReducer.Action.InputChanged(it)) },
                    onSend = { viewModel.send(ChatPageReducer.Action.SendTapped) },
                    accent = colors.accent,
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    title: String,
    onBack: () -> Unit,
    onInfo: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.background)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "Back" },
        ) {
            Components.Symbol("chevron.left", color = colors.accent, modifier = Modifier.size(22.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .clickable(onClick = onInfo)
                    .semantics { contentDescription = "Conversation info" },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(HEADER_AVATAR_BACKGROUND),
            ) {
                Components.Symbol("person", color = colors.background, modifier = Modifier.size(18.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.groupedContentBackground)
                        .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Components.Text(title.ifBlank { " " }, color = colors.titleText, font = Font.systemSemibold())
                Components.Symbol("chevron.right", color = colors.subtitleText, modifier = Modifier.size(14.dp).padding(start = 2.dp))
            }
        }
    }
}

private val HEADER_AVATAR_BACKGROUND = Color(0xFFC7C7CC)

@Composable
private fun MessageList(
    state: ChatPageReducer.State,
    modifier: Modifier,
    onToggleAlternate: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val messages = state.messages
    val isGroup = (ConversationSessionService.currentConversation?.participants?.size ?: 2) > 2
    val lastConfirmedOwnIndex = messages.indexOfLast { it.isFromCurrentUser && !it.isOutboxMessage }

    LaunchedEffect(messages.size, state.changeToken) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.Top) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
            val isFailed =
                message.isOutboxMessage &&
                    MessageOutboxService.entry(message.id)?.state == OutboxEntry.State.FAILED

            ChatMessageCell(
                row =
                    ChatMessageRowData(
                        message = message,
                        previousMessage = messages.getOrNull(index - 1),
                        translation = state.translationsByID[message.id] ?: message.translations?.firstOrNull(),
                        showAlternate = message.id in state.alternateTextMessageIDs,
                        isLastConfirmedOwnMessage = index == lastConfirmedOwnIndex,
                        isGroup = isGroup,
                        isFailed = isFailed,
                        senderName = null,
                    ),
                onToggleAlternate = onToggleAlternate,
            )
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    accent: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Material3Text(LocalizedStringKey.NewMessage.localized()) },
            modifier = Modifier.weight(1f),
        )

        val canSend = text.isNotBlank() && !isSending
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.padding(start = 4.dp).semantics { contentDescription = "Send" },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            ) {
                Components.Symbol(
                    "paperplane.fill",
                    color = if (canSend) accent else accent.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
