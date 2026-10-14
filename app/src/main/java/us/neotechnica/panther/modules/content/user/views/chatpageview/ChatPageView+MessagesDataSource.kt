//
//  ChatPageView+MessagesDataSource.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.modules.content.user.components.ChatMessageCell
import us.neotechnica.panther.modules.content.user.components.ChatMessageRowData
import us.neotechnica.panther.modules.content.user.components.messageTimestampLabelAttributedText
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isOutboxMessage
import us.neotechnica.panther.modules.content.user.extensions.systemLocalized
import us.neotechnica.panther.modules.content.user.services.ChatPageViewService
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.SessionStore

// MARK: - Constants Accessors

private typealias TimestampFloats = ChatPageViewFloats.MessageTimestamp

// The maximum number of frames to keep re-pinning the newest message while
// its content settles into place, guarding against an unbounded loop.
private const val SCROLL_SETTLE_MAX_FRAMES = 8

/**
 * Keeps the message list pinned to the newest message.
 *
 * The list starts pinned and scrolls to the bottom on initial load, when
 * the user sends a message, or whenever pinned content grows – a
 * reaction landing below the last message. It unpins the moment the
 * user drags the list to read earlier messages and re-pins once the
 * list next rests at the very bottom, so content growth is never
 * mistaken for the user scrolling away.
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
    var previousNewestMessageID by remember { mutableStateOf<String?>(null) }
    val newestMessageID = messages.lastOrNull()?.id
    val lastMessageReactionCount = messages.lastOrNull()?.reactions?.size ?: 0
    LaunchedEffect(
        messages.size,
        newestMessageID,
        lastMessageReactionCount,
    ) {
        if (messages.isEmpty()) {
            previousMessageCount = 0
            previousNewestMessageID = null
            return@LaunchedEffect
        }

        // Older messages loading in grow the list without changing its
        // newest message, so they never count as a message sent.
        val isInitialLoad = previousMessageCount == 0
        val newestMessageIsOwn =
            messages.size > previousMessageCount &&
                newestMessageID != previousNewestMessageID &&
                messages.last().isFromCurrentUser
        previousMessageCount = messages.size
        previousNewestMessageID = newestMessageID

        // On first load, jump to a message navigated to from search rather
        // than to the bottom, so the highlighted message is visible.
        val focusedIndex = state.focusedMessageID?.let { id -> messages.indexOfFirst { it.id == id } } ?: -1
        if (isInitialLoad && focusedIndex >= 0) {
            stickToBottom = false
            listState.scrollToItem(focusedIndex)
            return@LaunchedEffect
        }

        if (isInitialLoad || newestMessageIsOwn || stickToBottom) {
            // Re-pin across frames until multi-step content growth (decoded
            // media, a reaction chip) settles, so the newest message rests
            // fully at the bottom rather than short of it.
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

/**
 * Loads older messages when the list comes to rest after the user
 * scrolls toward them, and scrolls to the topmost message after a
 * scroll-to-top load.
 *
 * As on iOS, where older messages load once the list stops
 * decelerating, a load follows a fling toward older messages –
 * wherever it comes to rest – or a release after pulling the list past
 * its topmost message.
 */
@Composable
private fun rememberLoadMoreMessagesConnection(listState: LazyListState): NestedScrollConnection {
    val didLoadFromScrollToTop by ChatPageViewService.didLoadMoreMessagesFromScrollToTop.collectAsState()
    LaunchedEffect(didLoadFromScrollToTop) {
        if (!didLoadFromScrollToTop) return@LaunchedEffect
        delay(ChatPageViewServiceFloats.LOAD_MORE_MESSAGES_DELAY_MILLISECONDS)
        listState.animateScrollToItem(0)
        ChatPageViewService.didScrollToTopAfterLoading()
    }

    return remember(listState) {
        object : NestedScrollConnection {
            // Whether the current drag has pulled the list past its topmost
            // message, and whether the released drag had.
            private var isPullingPastTop = false
            private var didReleasePastTop = false

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Only the topmost message leaves a drag toward older messages
                // unconsumed.
                if (source == NestedScrollSource.UserInput && available.y > 0f) isPullingPastTop = true
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                didReleasePastTop = isPullingPastTop
                isPullingPastTop = false
                return Velocity.Zero
            }

            // Called once the released drag's fling comes to rest – at once,
            // for a release without velocity.
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                val isTowardOlderMessages = (consumed + available).y > 0f || didReleasePastTop
                if (isTowardOlderMessages) ChatPageViewService.loadMoreMessages(fromScrollToTop = false)
                return Velocity.Zero
            }
        }
    }
}

/**
 * The message list: every displayed message as a [ChatMessageCell],
 * pinned to the newest message, loading older messages on an upward
 * drag at the top, and revealing each message's timestamp as the list
 * is dragged toward the leading edge.
 *
 * @param state The chat page state.
 * @param modifier The modifier for the list.
 * @param onReact Applies the given reaction style to the given message.
 */
@Composable
internal fun MessageList(
    state: ChatPageReducer.State,
    modifier: Modifier,
    onReact: (Message, Reaction.Style) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val messages = state.messages
    val users = ConversationSessionService.currentConversation?.users.orEmpty()
    val isGroup = (ConversationSessionService.currentConversation?.participants?.size ?: 2) > 2
    val lastConfirmedOwnIndex = messages.indexOfLast { it.isFromCurrentUser && !it.isOutboxMessage }
    val loadMoreConnection = rememberLoadMoreMessagesConnection(listState)

    StickToBottomEffect(listState, state)

    // Dragging toward the leading edge reveals the timestamps; releasing
    // springs the list back.
    val revealDistancePx = with(LocalDensity.current) { TimestampFloats.revealDistance.toPx() }
    var revealOffset by remember { mutableFloatStateOf(0f) }
    val revealFraction = (-revealOffset / revealDistancePx).coerceIn(0f, 1f)
    val revealModifier =
        Modifier.pointerInput(revealDistancePx) {
            detectHorizontalDragGestures(
                onDragEnd = { scope.launch { animate(revealOffset, 0f) { value, _ -> revealOffset = value } } },
                onDragCancel = { scope.launch { animate(revealOffset, 0f) { value, _ -> revealOffset = value } } },
            ) { change, dragAmount ->
                change.consume()
                revealOffset = (revealOffset + dragAmount).coerceIn(-revealDistancePx, 0f)
            }
        }

    LazyColumn(
        state = listState,
        modifier = modifier.nestedScroll(loadMoreConnection).then(revealModifier),
        verticalArrangement = Arrangement.Top,
    ) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, displayedMessage ->
            // System rows render the activity's localized description.
            val message = displayedMessage.systemLocalized
            val isFailed =
                message.isOutboxMessage &&
                    MessageOutboxService.entry(message.id)?.state == OutboxEntry.State.FAILED

            val previousMessage = messages.getOrNull(index - 1)
            val showSender = isGroup && !message.isFromCurrentUser
            val firstOfRun = previousMessage?.fromAccountID != message.fromAccountID
            val senderUser =
                if (showSender) {
                    users.firstOrNull { it.id == message.fromAccountID } ?: SessionStore.users[message.fromAccountID]
                } else {
                    null
                }
            val senderImage =
                remember(senderUser?.id) {
                    senderUser
                        ?.contactPair
                        ?.contact
                        ?.image
                        ?.asImageBitmap()
                }

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = revealOffset }) {
                    ChatMessageCell(
                        row =
                            ChatMessageRowData(
                                message = message,
                                previousMessage = previousMessage,
                                isLastConfirmedOwnMessage = index == lastConfirmedOwnIndex,
                                isGroup = isGroup,
                                isFailed = isFailed,
                                senderName = if (showSender && firstOfRun) senderUser?.displayName ?: message.fromAccountID else null,
                                senderImage = senderImage,
                                reactions = message.reactions.orEmpty(),
                            ),
                        onReact = onReact,
                        onFailedIndicatorTapped = { messageID ->
                            scope.launch { ChatPageViewService.presentFailedMessageActionSheet(forMessageID = messageID) }
                        },
                    )
                }

                if (revealFraction > 0f) {
                    Text(
                        text = messageTimestampLabelAttributedText(message),
                        textAlign = TextAlign.End,
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(TimestampFloats.labelWidth)
                                .padding(end = TIMESTAMP_END_PADDING)
                                .graphicsLayer {
                                    alpha = revealFraction
                                    translationX = revealOffset + revealDistancePx
                                },
                    )
                }
            }
        }
    }
}

private val TIMESTAMP_END_PADDING = 8.dp
