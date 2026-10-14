//
//  ChatPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.messageOutboxDidChange
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.designsystem.modules.componentkit.components.ContextMenuHost
import us.neotechnica.panther.designsystem.modules.componentkit.components.LocalContextMenuController
import us.neotechnica.panther.designsystem.modules.componentkit.components.MessageInputBar
import us.neotechnica.panther.designsystem.modules.foundation.components.LocalPageHasAppeared
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.modules.common.models.ConnectionStatusServiceEffectID
import us.neotechnica.panther.modules.common.services.ConnectionStatusService
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.components.DeliveryProgressView
import us.neotechnica.panther.modules.content.user.components.MediaPreviewOverlay
import us.neotechnica.panther.modules.content.user.components.RegisterContentPickerLaunchers
import us.neotechnica.panther.modules.content.user.components.anyOutboxSending
import us.neotechnica.panther.modules.content.user.components.chatpageheaderview.ChatPageHeaderView
import us.neotechnica.panther.modules.content.user.components.rememberRegisteredDeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.models.ContextMenuInteraction
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.content.user.services.DeliveryProgressIndicatorService
import us.neotechnica.panther.modules.content.user.services.InputBarConfigService
import us.neotechnica.panther.modules.content.user.services.InputBarService
import us.neotechnica.panther.modules.content.user.services.MediaMessagePreviewService
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.SearchInteractionService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias PageFloats = ChatPageViewServiceFloats

/**
 * The chat page for a single conversation.
 *
 * Renders the conversation's messages live, translated for the current
 * user, with a long-press context menu, delivery status, and an input
 * bar that sends through the outbox-backed delivery pipeline.
 *
 * The page presents in one of three [Configuration]s: the default
 * configuration shows the full page; the new chat configuration renders
 * only the message list between the new chat page's recipient and
 * input bars, which own the page lifecycle; and the preview
 * configuration shows a read-only message list.
 */
object ChatPageView {
    // MARK: - Dependencies

    private val connectionStatusService: ConnectionStatusService by Dependency { it.commonServices.connectionStatus }

    // MARK: - Types

    /** The configurations the chat page presents in. */
    sealed interface Configuration {
        /** The identifier of a message to reveal and highlight, or `null`. */
        val focusedMessageID: String?

        /**
         * The default configuration, displaying an existing conversation
         * with its header and input bar.
         *
         * @property focusedMessageID The identifier of a message to reveal
         *   and highlight – for example, one navigated to from search – or
         *   `null`.
         */
        data class Default(
            override val focusedMessageID: String? = null,
        ) : Configuration

        /** The new chat configuration, previewing the conversation beneath the recipient bar. */
        data object NewChat : Configuration {
            override val focusedMessageID: String? get() = null
        }

        /**
         * The preview configuration, displaying a read-only message list
         * with the input bar hidden.
         *
         * @property focusedMessageID The identifier of a message to reveal
         *   and highlight, or `null`.
         */
        data class Preview(
            override val focusedMessageID: String? = null,
        ) : Configuration
    }

    // MARK: - View

    /**
     * Presents the chat page for the given conversation.
     *
     * @param conversationIDKey The identifier key of the conversation.
     * @param configuration The configuration to present the page in.
     * @param modifier The modifier for this view.
     */
    @Composable
    operator fun invoke(
        conversationIDKey: String,
        configuration: Configuration = Configuration.Default(),
        modifier: Modifier = Modifier,
    ) {
        val viewModel = remember { buildChatPageViewModel() }
        val inputBarService = remember { DependencyValues.current.inputBarService }
        val isDefault = configuration is Configuration.Default

        DisposableEffect(Unit) {
            ChatPageStateService.setIsPresented(true)
            connectionStatusService.addEffectUponConnectionChanged(id = ConnectionStatusServiceEffectID.CONFIGURE_INPUT_BAR) {
                inputBarService.configureInputBar(forceUpdate = true)
            }
            onDispose {
                ChatPageStateService.setIsPresented(false)
                connectionStatusService.removeEffect(ConnectionStatusServiceEffectID.CONFIGURE_INPUT_BAR)
                viewModel.send(ChatPageReducer.Action.ViewDisappeared)
                viewModel.close()
            }
        }
        LaunchedEffect(conversationIDKey) {
            viewModel.send(ChatPageReducer.Action.ViewAppeared(conversationIDKey, configuration))
            viewModel.send(ChatPageReducer.Action.HandleOutboxChange(anyOutboxSending()))
        }

        val state by viewModel.state.collectAsState()
        val deliveryProgressIndicatorService = rememberRegisteredDeliveryProgressIndicatorService()
        val isPreviewingMedia = MediaMessagePreviewService.isPreviewingMedia
        val previewingMessageID = MediaMessagePreviewService.previewingMessageID

        val mediaMessages = state.messages.filter { it.richContent?.mediaComponent != null }
        val previewMediaFiles = mediaMessages.mapNotNull { it.richContent?.mediaComponent }
        val previewStartIndex = mediaMessages.indexOfFirst { it.id == previewingMessageID }

        if (isDefault) RegisterContentPickerLaunchers()

        StatefulView(state = state.viewState, modifier = modifier) {
            Box(modifier = Modifier.fillMaxSize()) {
                ContextMenuHost(
                    modifier = Modifier.fillMaxSize(),
                    canBegin = ContextMenuInteraction.canBegin,
                ) {
                    SpeechSynthesizerDidFinishOrCancel()
                    FocusedMessageInteractionEffect(focusedMessageID = state.focusedMessageID)

                    Column(
                        modifier = if (isDefault) Modifier.fillMaxSize().systemBarsPadding().imePadding() else Modifier.fillMaxSize(),
                    ) {
                        if (isDefault) {
                            ChatHeaderWithDeliveryProgress(
                                conversationIDKey = state.conversationIDKey,
                                service = deliveryProgressIndicatorService,
                            )
                        }

                        MessageList(
                            state = state,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onReact = { message, style -> viewModel.send(ChatPageReducer.Action.ReactToSelectedMessage(message, style)) },
                        )

                        if (isDefault) {
                            ChatInputBar(viewModel = viewModel, inputBarService = inputBarService, state = state)
                        }
                    }
                }

                if (isPreviewingMedia && previewStartIndex >= 0) {
                    MediaPreviewOverlay(
                        mediaFiles = previewMediaFiles,
                        startIndex = previewStartIndex,
                        onDismiss = { MediaMessagePreviewService.dismissPreview() },
                        title = LocalizedStringKey.Attachment.localized().lowercase(),
                    )
                }
            }
        }
    }
}

// MARK: - Input Bar

// The input bar fades in after a short delay once the page has appeared.
@Composable
private fun ChatInputBar(
    viewModel: ViewModel<ChatPageReducer.State, ChatPageReducer.Action>,
    inputBarService: InputBarService,
    state: ChatPageReducer.State,
) {
    val text by inputBarService.text.collectAsState()
    val isSendButtonEnabled by inputBarService.isSendButtonEnabled.collectAsState()
    val isAttachMediaButtonEnabled by inputBarService.isAttachMediaButtonEnabled.collectAsState()
    val isShowingSendingUI by inputBarService.isShowingSendingUI.collectAsState()
    val shouldBecomeFirstResponder by inputBarService.shouldBecomeFirstResponder.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // A standing first-responder request is honored once the field has
    // had its first layout pass.
    LaunchedEffect(shouldBecomeFirstResponder) {
        if (!shouldBecomeFirstResponder) return@LaunchedEffect
        withFrameNanos {}
        focusRequester.requestFocus()
    }

    var hasAppeared by remember { mutableStateOf(false) }
    val appearanceDurationMillis = (PageFloats.INPUT_BAR_APPEARANCE_ANIMATION_DURATION * MILLISECONDS_PER_SECOND).toInt()
    LaunchedEffect(Unit) {
        delay(appearanceDurationMillis.toLong())
        hasAppeared = true
    }
    val alpha by animateFloatAsState(
        if (hasAppeared) 1f else 0f,
        SystemAnimation.easeInOut(appearanceDurationMillis.milliseconds),
        label = "inputBarAlpha",
    )

    MessageInputBar(
        text = text,
        placeholder = LocalizedStringKey.NewMessage.localized(),
        isSending = isShowingSendingUI || state.isSendingOutboxEntry,
        onTextChange = { viewModel.send(ChatPageReducer.Action.TextViewDidChange(it)) },
        onSend = { viewModel.send(ChatPageReducer.Action.DidPressSendButton) },
        onAttach = { inputBarService.actionHandler.didPressAttachMediaButton() },
        isSendEnabled = isSendButtonEnabled,
        isAttachEnabled = isAttachMediaButtonEnabled,
        images = InputBarConfigService.images(),
        onFocusChanged = { inputBarService.setIsFirstResponder(it) },
        focusRequester = focusRequester,
        modifier = Modifier.alpha(alpha),
    )
}

// MARK: - Auxiliary

@Composable
private fun FocusedMessageInteractionEffect(focusedMessageID: String?) {
    val controller = LocalContextMenuController.current
    val hasAppeared = LocalPageHasAppeared.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller, focusedMessageID) {
        if (controller == null || focusedMessageID == null) return@LaunchedEffect
        scope.launch {
            // As on iOS, the message is focused only once the page has
            // finished appearing, so it lifts out of a page at rest.
            snapshotFlow { hasAppeared.value }.first { it }
            delay(PageFloats.TRIGGER_FOCUSED_MESSAGE_CELL_INTERACTION_DELAY_MILLISECONDS)
            SearchInteractionService(focusedMessageID) { controller.present(it) }
                .triggerFocusedMessageCellInteractionIfNeeded()
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

private fun buildChatPageViewModel(): ViewModel<ChatPageReducer.State, ChatPageReducer.Action> =
    ViewModel(ChatPageReducer.State(), ChatPageReducer())
        .observing(DependencyValues.current.clientSession.entity.conversation.displayedMessages) {
            ChatPageReducer.Action.MessagesUpdated(it)
        }.observing(DependencyValues.current.sharedEvents.sessionStoreDidChange.events) { change ->
            ChatPageReducer.Action.HandleSessionStoreChange(change)
        }.observing(MessageDeliveryService.isSendingMessage) {
            ChatPageReducer.Action.IsSendingMessageChanged(it)
        }.observing(DependencyValues.current.sharedEvents.messageOutboxDidChange.events) {
            ChatPageReducer.Action.HandleOutboxChange(anyOutboxSending())
        }

private const val MILLISECONDS_PER_SECOND = 1000f
