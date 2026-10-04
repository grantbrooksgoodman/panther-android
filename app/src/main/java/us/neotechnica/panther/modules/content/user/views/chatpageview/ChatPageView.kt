//
//  ChatPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.components.LocalContextMenuController
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.components.ChatMessageCell
import us.neotechnica.panther.modules.content.user.components.ChatMessageRowData
import us.neotechnica.panther.modules.content.user.components.ContentPickers
import us.neotechnica.panther.modules.content.user.components.DeliveryProgressView
import us.neotechnica.panther.modules.content.user.components.MediaPreviewOverlay
import us.neotechnica.panther.modules.content.user.components.anyOutboxSending
import us.neotechnica.panther.modules.content.user.components.chatpageheaderview.ChatPageHeaderView
import us.neotechnica.panther.modules.content.user.components.rememberContentPickers
import us.neotechnica.panther.modules.content.user.components.rememberRegisteredDeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.content.user.services.ContextMenuActionHandlerService
import us.neotechnica.panther.modules.content.user.services.DeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.services.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.SearchInteractionService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.isMediaMessage
import us.neotechnica.panther.modules.session.entity.extensions.isOutboxMessage
import us.neotechnica.panther.modules.session.entity.extensions.isSystemMessage
import us.neotechnica.panther.modules.session.entity.extensions.messageOutboxDidChange
import us.neotechnica.panther.modules.session.entity.extensions.reactions
import us.neotechnica.panther.modules.session.entity.extensions.sessionStoreDidChange
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.retry
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// The maximum number of frames to keep re-pinning the newest message while
// its content settles into place, guarding against an unbounded loop.
private const val SCROLL_SETTLE_MAX_FRAMES = 8

/**
 * The chat page for a single conversation.
 *
 * Renders the conversation's messages live, translated for the current
 * user, with a long-press context menu, delivery status, and an input
 * bar that sends through the outbox-backed delivery pipeline.
 *
 * @param conversationIDKey The identifier key of the conversation.
 * @param focusedMessageID The identifier of a message to reveal and
 *   highlight – for example, one navigated to from search – or `null`.
 * @param modifier The modifier for this view.
 */
@Composable
fun ChatPageView(
    conversationIDKey: String,
    focusedMessageID: String? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { buildChatPageViewModel() }

    DisposableEffect(Unit) {
        ChatPageStateService.setIsPresented(true)
        onDispose {
            ChatPageStateService.setIsPresented(false)
            viewModel.send(ChatPageReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }
    LaunchedEffect(conversationIDKey) {
        viewModel.send(ChatPageReducer.Action.ViewFirstAppeared(conversationIDKey, focusedMessageID))
        viewModel.send(ChatPageReducer.Action.MessageOutboxChanged(anyOutboxSending()))
    }

    val state by viewModel.state.collectAsState()
    val deliveryProgressIndicatorService = rememberRegisteredDeliveryProgressIndicatorService()
    var previewMessageID by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val pickers =
        rememberContentPickers(
            onPicked = { viewModel.send(ChatPageReducer.Action.AttachmentPicked(it)) },
            onFailed = { Logger.log(it, with = AlertType.toast) },
        )
    val pendingAttachment = state.pendingAttachment
    val attachmentPreview by
        produceState<ByteArray?>(null, pendingAttachment) {
            value =
                pendingAttachment?.let {
                    withContext(Dispatchers.IO) { (it.thumbnailFile ?: it.localPathFile)?.readBytes() }
                }
        }

    val mediaMessages = state.messages.filter { it.isMediaMessage && state.mediaByID[it.id] != null }
    val previewMediaFiles = mediaMessages.mapNotNull { state.mediaByID[it.id] }
    val previewStartIndex = mediaMessages.indexOfFirst { it.id == previewMessageID }

    StatefulView(state = state.viewState, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            ContextMenuHost(
                modifier = Modifier.fillMaxSize(),
                canBegin = ContextMenuInteraction.canBegin,
            ) {
                SpeechSynthesizerDidFinishOrCancel()
                FocusedMessageInteractionEffect(focusedMessageID = state.focusedMessageID)

                Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                    ChatHeaderWithDeliveryProgress(
                        conversationIDKey = state.conversationIDKey,
                        service = deliveryProgressIndicatorService,
                    )

                    MessageList(
                        state = state,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onToggleAlternate = { viewModel.send(ChatPageReducer.Action.ToggleAlternate(it)) },
                        onToggleAudioTranscription = { viewModel.send(ChatPageReducer.Action.ToggleAudioTranscription(it)) },
                        onTapMedia = { previewMessageID = it },
                        onReact = { message, style -> viewModel.send(ChatPageReducer.Action.React(message, style)) },
                        onSpeak = { messageID, text -> viewModel.send(ChatPageReducer.Action.Speak(messageID, text)) },
                        onFailedIndicatorTapped = { messageID ->
                            scope.launch { presentFailedMessageActionSheet(messageID, scope) }
                        },
                        onSaveMedia = { mediaFile -> scope.launch { saveMedia(mediaFile) } },
                    )

                    MessageInputBar(
                        text = state.inputText,
                        placeholder = LocalizedStringKey.NewMessage.localized(),
                        isSending = state.isSendingMessage || state.hasSendingOutboxEntry,
                        onTextChange = { viewModel.send(ChatPageReducer.Action.InputChanged(it)) },
                        onSend = {
                            if (state.pendingAttachment != null) {
                                viewModel.send(ChatPageReducer.Action.SendMedia)
                            } else {
                                viewModel.send(ChatPageReducer.Action.SendTapped)
                            }
                        },
                        onAttach = { scope.launch { presentAttachMediaSheet(pickers) } },
                        attachmentPreview = attachmentPreview,
                        onRemoveAttachment = { viewModel.send(ChatPageReducer.Action.RemoveAttachment) },
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
private fun FocusedMessageInteractionEffect(focusedMessageID: String?) {
    val controller = LocalContextMenuController.current
    LaunchedEffect(controller, focusedMessageID) {
        if (controller == null || focusedMessageID == null) return@LaunchedEffect
        SearchInteractionService(focusedMessageID) { controller.present(it) }
            .triggerFocusedMessageCellInteractionIfNeeded()
    }
}

@Composable
private fun SpeechSynthesizerDidFinishOrCancel() {
    val controller = LocalContextMenuController.current
    LaunchedEffect(controller) {
        var wasSpeaking = false
        snapshotFlow { TextToSpeechService.isSpeaking }.collect { isSpeaking ->
            if (wasSpeaking && !isSpeaking) {
                controller?.dismiss()
                ContextMenuActionHandlerService.resetSpeakingMessage()
            }
            wasSpeaking = isSpeaking
        }
    }
}

@Composable
private fun ChatHeaderWithDeliveryProgress(
    conversationIDKey: String,
    service: DeliveryProgressIndicatorService,
) {
    Box {
        ChatPageHeaderView(conversationIDKey = conversationIDKey)
        DeliveryProgressView(
            progress = service.progress,
            alpha = service.alpha,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Keeps the message list pinned to the newest message.
 *
 * The list starts pinned and scrolls to the bottom on initial load, when
 * the user sends a message, or whenever pinned content grows — the first
 * asynchronous resolution of a cold-opened chat, or a reaction landing
 * below the last message. It unpins the moment the user drags the list to
 * read earlier messages and re-pins once the list next rests at the very
 * bottom, so content growth is never mistaken for the user scrolling away.
 * Mirrors the iOS chat page's stick-to-bottom.
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
private fun MessageList(
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

private fun buildChatPageViewModel(): ViewModel<ChatPageReducer.State, ChatPageReducer.Action> =
    ViewModel(ChatPageReducer.State(), ChatPageReducer())
        .observing(ConversationSessionService.displayedMessages) {
            ChatPageReducer.Action.MessagesUpdated(it)
        }.observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) { change ->
            ChatPageReducer.Action.StoreChanged(change)
        }.observing(MessageDeliveryService.isSendingMessage) {
            ChatPageReducer.Action.IsSendingMessageChanged(it)
        }.observing(DependencyValues.current.sharedEvents.messageOutboxDidChange.events) {
            ChatPageReducer.Action.MessageOutboxChanged(anyOutboxSending())
        }

/** The uppercased first letters of each word of the name. */
private fun String.contactInitials(): String = split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

/**
 * Saves the given media file to the device, showing a success HUD on
 * completion. Mirrors iOS's `handleSaveAction`.
 */
private suspend fun saveMedia(mediaFile: MediaFile) {
    try {
        MediaActionHandlerService.saveMedia(mediaFile)
        HUD.showSuccess()
    } catch (exception: Exception) {
        Logger.log(exception, with = AlertType.toast)
    }
}

/**
 * Presents an action sheet for a failed message, offering to retry or
 * delete it. Mirrors iOS's `presentFailedMessageActionSheet`.
 */
private suspend fun presentFailedMessageActionSheet(
    messageID: String,
    scope: CoroutineScope,
) {
    ActionSheetAlert(
        actions =
            listOf(
                Action(
                    title = LocalizedStringKey.Delete.localized(),
                    style = ActionStyle.DESTRUCTIVE,
                ) { MessageOutboxService.remove(messageID) },
                Action(
                    title = LocalizedStringKey.TryAgain.localized(),
                ) { scope.launch { MessageOutboxService.retry(messageID) } },
            ),
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(translating = emptyList())
}

// Mirrors the iOS `MediaActionHandlerService.attachMediaButtonTapped` action sheet.
private suspend fun presentAttachMediaSheet(pickers: ContentPickers) {
    HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)
    ActionSheetAlert(
        title = "Attach media",
        actions =
            listOf(
                Action("Take photo") { pickers.launchCamera() },
                Action("Select document") { pickers.launchDocument() },
                Action("Select photo or video") { pickers.launchPhotoOrVideo() },
            ),
        cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
    ).present(
        translating =
            listOf(
                ActionSheetAlert.TranslationOptionKey.Title,
                ActionSheetAlert.TranslationOptionKey.Actions(),
            ),
    )
}
