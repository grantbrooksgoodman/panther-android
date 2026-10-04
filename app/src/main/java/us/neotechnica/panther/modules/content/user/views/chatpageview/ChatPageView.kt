//
//  ChatPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.neotechnica.panther.bundle.messageOutboxDidChange
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.components.LocalContextMenuController
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.content.user.components.ContentPickers
import us.neotechnica.panther.modules.content.user.components.DeliveryProgressView
import us.neotechnica.panther.modules.content.user.components.MediaPreviewOverlay
import us.neotechnica.panther.modules.content.user.components.anyOutboxSending
import us.neotechnica.panther.modules.content.user.components.chatpageheaderview.ChatPageHeaderView
import us.neotechnica.panther.modules.content.user.components.rememberContentPickers
import us.neotechnica.panther.modules.content.user.components.rememberRegisteredDeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.content.user.services.DeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.services.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.SearchInteractionService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.session.entity.extensions.isMediaMessage
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.retry
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

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

/**
 * Saves the given media file to the device, showing a success HUD on
 * completion.
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
 * delete it.
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

// The attach-media action sheet.
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
