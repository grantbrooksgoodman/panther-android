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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.content.user.components.ChatMessageCell
import us.neotechnica.panther.modules.content.user.components.ChatMessageRowData
import us.neotechnica.panther.modules.content.user.components.MediaPreviewOverlay
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentConversationDidBecomeUnavailable
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.isMediaMessage
import us.neotechnica.panther.networking.modules.session.extensions.isOutboxMessage
import us.neotechnica.panther.networking.modules.session.extensions.isSystemMessage
import us.neotechnica.panther.networking.modules.session.extensions.reactions
import us.neotechnica.panther.networking.modules.session.extensions.sessionStoreDidChange
import us.neotechnica.panther.networking.modules.session.extensions.users
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.MessageOutboxService
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Floats = ChatPageViewFloats

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
    var previewMessageID by remember { mutableStateOf<String?>(null) }

    val mediaMessages = state.messages.filter { it.isMediaMessage && state.mediaByID[it.id] != null }
    val previewMediaFiles = mediaMessages.mapNotNull { state.mediaByID[it.id] }
    val previewStartIndex = mediaMessages.indexOfFirst { it.id == previewMessageID }

    StatefulView(state = state.viewState, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            ContextMenuHost(Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
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
                        onTapMedia = { previewMessageID = it },
                    )

                    MessageInputBar(
                        text = state.inputText,
                        placeholder = LocalizedStringKey.NewMessage.localized(),
                        isSending = state.isSending,
                        onTextChange = { viewModel.send(ChatPageReducer.Action.InputChanged(it)) },
                        onSend = { viewModel.send(ChatPageReducer.Action.SendTapped) },
                        onAttach = {},
                    )
                }
            }

            if (previewMessageID != null && previewStartIndex >= 0) {
                MediaPreviewOverlay(
                    mediaFiles = previewMediaFiles,
                    startIndex = previewStartIndex,
                    onDismiss = { previewMessageID = null },
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
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding),
    ) {
        CircleChipButton(
            systemName = "chevron.left",
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
            tint = colors.titleText,
        )

        val conversation = ConversationSessionService.currentConversation
        val isGroup = (conversation?.participants?.size ?: 2) > 2
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .clickable(onClick = onInfo)
                    .semantics { contentDescription = "Conversation info" },
        ) {
            AvatarImageView(
                modifier = Modifier.size(Floats.headerAvatarSize).zIndex(1f),
                imageData = conversation?.metadata?.imageData,
                fallbackSymbol = if (isGroup) "person.2" else "person",
                glyphSize = Floats.headerAvatarGlyphSize,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .offset(y = -Floats.headerAvatarPillOverlap)
                        .clip(RoundedCornerShape(Floats.pillCornerRadius))
                        .background(colors.groupedContentBackground)
                        .padding(
                            start = Floats.pillStartPadding,
                            end = Floats.pillEndPadding,
                            top = Floats.pillVerticalPadding,
                            bottom = Floats.pillVerticalPadding,
                        ),
            ) {
                Components.Text(title.ifBlank { " " }, color = colors.titleText, font = Font.systemSemibold())
                Components.Symbol(
                    "chevron.right",
                    color = colors.subtitleText,
                    modifier = Modifier.size(Floats.pillChevronSize).padding(start = Floats.pillChevronStartPadding),
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    state: ChatPageReducer.State,
    modifier: Modifier,
    onToggleAlternate: (String) -> Unit,
    onTapMedia: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val messages = state.messages
    val users = ConversationSessionService.currentConversation?.users.orEmpty()
    val isGroup = (ConversationSessionService.currentConversation?.participants?.size ?: 2) > 2
    val lastConfirmedOwnIndex = messages.indexOfLast { it.isFromCurrentUser && !it.isOutboxMessage }

    // Auto-scroll to the newest message only on initial load, when the user
    // sends a message, or when they are already at the bottom — never yanking
    // them away while they read earlier messages. Mirrors the iOS chat page.
    var previousMessageCount by remember { mutableStateOf(0) }
    LaunchedEffect(messages.size, state.translationsByID.size, state.mediaByID.size) {
        if (messages.isEmpty()) {
            previousMessageCount = 0
            return@LaunchedEffect
        }

        val didAppendMessages = messages.size > previousMessageCount
        val isInitialLoad = previousMessageCount == 0
        val newestMessageIsOwn = didAppendMessages && messages.last().isFromCurrentUser
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val wasAtBottom = lastVisibleIndex == null || lastVisibleIndex >= messages.lastIndex - 1

        if (isInitialLoad || newestMessageIsOwn || wasAtBottom) {
            listState.scrollToItem(messages.lastIndex)
        }
        previousMessageCount = messages.size
    }

    LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.Top) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
            val isFailed =
                message.isOutboxMessage &&
                    MessageOutboxService.entry(message.id)?.state == OutboxEntry.State.FAILED

            val showSender = isGroup && !message.isFromCurrentUser && !message.isSystemMessage
            val firstOfRun = messages.getOrNull(index - 1)?.fromAccountID != message.fromAccountID
            val lastOfRun = messages.getOrNull(index + 1)?.fromAccountID != message.fromAccountID
            val senderMatch = if (showSender) ContactService.match(message.fromAccountID) else null

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
                        senderName = if (showSender && firstOfRun) senderName(message, senderMatch, users) else null,
                        senderInitials = senderMatch?.initials ?: "",
                        showSenderAvatar = showSender && lastOfRun,
                        reactionsText =
                            message.reactions
                                .orEmpty()
                                .map { it.style }
                                .sortedBy { it.orderValue }
                                .joinToString(separator = "") { it.emojiValue },
                        mediaFile = state.mediaByID[message.id],
                    ),
                onToggleAlternate = onToggleAlternate,
                onTapMedia = onTapMedia,
            )
        }
    }
}

private fun senderName(
    message: Message,
    match: ContactMatch?,
    users: List<User>,
): String {
    match?.let { return it.fullName }
    val user = users.firstOrNull { it.id == message.fromAccountID } ?: SessionStore.users[message.fromAccountID]
    return user?.phoneNumber?.formattedString() ?: message.fromAccountID
}
