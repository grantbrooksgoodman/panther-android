//
//  ChatPageView+MessagesDataSource.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import us.neotechnica.panther.modules.content.user.components.ChatMessageCell
import us.neotechnica.panther.modules.content.user.components.ChatMessageRowData
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.isOutboxMessage
import us.neotechnica.panther.modules.session.entity.extensions.isSystemMessage
import us.neotechnica.panther.modules.session.entity.extensions.reactions
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.SessionStore

// The maximum number of frames to keep re-pinning the newest message while
// its content settles into place, guarding against an unbounded loop.
private const val SCROLL_SETTLE_MAX_FRAMES = 8

/**
 * Keeps the message list pinned to the newest message.
 *
 * The list starts pinned and scrolls to the bottom on initial load, when
 * the user sends a message, or whenever pinned content grows — the first
 * asynchronous resolution of a cold-opened chat, or a reaction landing
 * below the last message. It unpins the moment the user drags the list to
 * read earlier messages and re-pins once the list next rests at the very
 * bottom, so content growth is never mistaken for the user scrolling away.
 */
@Composable
private fun StickToBottomEffect(
    listState: LazyListState,
    state: ChatPageReducer.State,
) {
    val messages = state.messages
    var stickToBottom by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) stickToBottom = false
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollForward }.collect { canScrollForward ->
            if (!canScrollForward) stickToBottom = true
        }
    }

    // Keyed on the specific growth signals (including the last message's
    // reaction count) so unrelated store churn never cancels the settle loop.
    var previousMessageCount by remember { mutableStateOf(0) }
    val lastMessageReactionCount = messages.lastOrNull()?.reactions?.size ?: 0
    LaunchedEffect(
        messages.size,
        state.translationsByID.size,
        state.mediaByID.size,
        state.audioByID.size,
        lastMessageReactionCount,
    ) {
        if (messages.isEmpty()) {
            previousMessageCount = 0
            return@LaunchedEffect
        }

        val isInitialLoad = previousMessageCount == 0
        val newestMessageIsOwn = messages.size > previousMessageCount && messages.last().isFromCurrentUser
        previousMessageCount = messages.size

        // On first load, jump to a message navigated to from search rather
        // than to the bottom, so the highlighted message is visible.
        val focusedIndex = state.focusedMessageID?.let { id -> messages.indexOfFirst { it.id == id } } ?: -1
        if (isInitialLoad && focusedIndex >= 0) {
            stickToBottom = false
            listState.scrollToItem(focusedIndex)
            return@LaunchedEffect
        }

        if (isInitialLoad || newestMessageIsOwn || stickToBottom) {
            // Re-pin across frames until multi-step content growth (async text,
            // decoded media, resolved audio, a reaction chip) settles, so the
            // newest message rests fully at the bottom rather than short of it.
            var settleFrames = 0
            do {
                listState.scrollToItem(messages.lastIndex)
                withFrameNanos {}
                settleFrames++
            } while (listState.canScrollForward && settleFrames < SCROLL_SETTLE_MAX_FRAMES)
            listState.scrollToItem(messages.lastIndex)
        }
    }
}

@Composable
@Suppress("LongParameterList")
internal fun MessageList(
    state: ChatPageReducer.State,
    modifier: Modifier,
    onToggleAlternate: (String) -> Unit,
    onToggleAudioTranscription: (String) -> Unit,
    onTapMedia: (String) -> Unit,
    onReact: (Message, Reaction.Style) -> Unit,
    onSpeak: (String, String) -> Unit,
    onFailedIndicatorTapped: (String) -> Unit,
    onSaveMedia: (MediaFile) -> Unit,
) {
    val listState = rememberLazyListState()
    val messages = state.messages
    val users = ConversationSessionService.currentConversation?.users.orEmpty()
    val isGroup = (ConversationSessionService.currentConversation?.participants?.size ?: 2) > 2
    val lastConfirmedOwnIndex = messages.indexOfLast { it.isFromCurrentUser && !it.isOutboxMessage }

    StickToBottomEffect(listState, state)

    LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.Top) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
            val isFailed =
                message.isOutboxMessage &&
                    MessageOutboxService.entry(message.id)?.state == OutboxEntry.State.FAILED

            val showSender = isGroup && !message.isFromCurrentUser && !message.isSystemMessage
            val firstOfRun = messages.getOrNull(index - 1)?.fromAccountID != message.fromAccountID
            val lastOfRun = messages.getOrNull(index + 1)?.fromAccountID != message.fromAccountID
            val senderUser =
                if (showSender) {
                    users.firstOrNull { it.id == message.fromAccountID } ?: SessionStore.users[message.fromAccountID]
                } else {
                    null
                }
            val senderDisplayName = senderUser?.displayName

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
                        senderName = if (showSender && firstOfRun) (senderDisplayName ?: message.fromAccountID) else null,
                        senderInitials = senderDisplayName?.contactInitials() ?: "",
                        showSenderAvatar = showSender && lastOfRun,
                        reactions = message.reactions.orEmpty(),
                        mediaFile = state.mediaByID[message.id],
                        audioReference = state.audioByID[message.id],
                        isDisplayingAudioTranscription = message.id in state.audioTranscriptionMessageIDs,
                    ),
                onToggleAlternate = onToggleAlternate,
                onToggleAudioTranscription = onToggleAudioTranscription,
                onTapMedia = onTapMedia,
                onReact = onReact,
                onSpeak = onSpeak,
                onFailedIndicatorTapped = onFailedIndicatorTapped,
                onSaveMedia = onSaveMedia,
            )
        }
    }
}

/** The uppercased first letters of each word of the name. */
private fun String.contactInitials(): String = split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
