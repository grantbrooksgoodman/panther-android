//
//  ChatPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.dependencies.contextMenuActionHandlerService
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.extensions.chatPageHeaderLabelText
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.AlternateMessageService
import us.neotechnica.panther.modules.content.user.services.AudioMessagePlaybackService
import us.neotechnica.panther.modules.content.user.services.ChatPageViewService
import us.neotechnica.panther.modules.content.user.services.ReadReceiptService
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias ReducerFloats = ChatPageViewServiceFloats

/**
 * The reducer for a single conversation's chat page.
 *
 * Sets the conversation current on appearance, observes its displayed
 * messages, sends text through the outbox-backed delivery service,
 * reloads on debounced session store and outbox changes, and marks
 * incoming messages read.
 */
class ChatPageReducer : Reducer<ChatPageReducer.State, ChatPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data class ViewAppeared(
            val conversationIDKey: String,
            val configuration: ChatPageView.Configuration,
        ) : Action

        data class MessagesUpdated(
            val messages: List<Message>,
        ) : Action

        data class TextViewDidChange(
            val text: String,
        ) : Action

        data object DidPressSendButton : Action

        data class HandleViewAlternateAction(
            val message: Message,
        ) : Action

        data class HandleAudioMessageAction(
            val message: Message,
        ) : Action

        data class ReactToSelectedMessage(
            val message: Message,
            val style: Reaction.Style,
        ) : Action

        data class HandleSessionStoreChange(
            val change: SessionStoreChange,
        ) : Action

        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        data class HandleOutboxChange(
            val isSendingOutboxEntry: Boolean,
        ) : Action

        data object ReloadRequested : Action

        data object ConversationUnavailable : Action

        data object ViewDisappeared : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val configuration: ChatPageView.Configuration = ChatPageView.Configuration.Default(),
        val messages: List<Message> = emptyList(),
        val isSendingMessage: Boolean = false,
        val isSendingOutboxEntry: Boolean = false,
        val languageCode: String = "en",
        val changeToken: UUID = UUID.randomUUID(),
        val viewState: ViewState = ViewState.Loading,
    ) {
        /** The identifier of the message to reveal and highlight, if any. */
        val focusedMessageID: String?
            get() = configuration.focusedMessageID
    }

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            is Action.ViewAppeared -> {
                if (action.configuration is ChatPageView.Configuration.Default) {
                    AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.ACCESS_CHAT)
                }
                ReduceResult(
                    state.copy(
                        conversationIDKey = action.conversationIDKey,
                        configuration = action.configuration,
                        languageCode = RuntimeStorage.languageCode,
                    ),
                    viewAppearedEffect(action.conversationIDKey, action.configuration),
                )
            }

            is Action.MessagesUpdated ->
                ReduceResult(
                    state.copy(
                        messages = action.messages,
                        viewState = ViewState.Loaded,
                        changeToken = UUID.randomUUID(),
                    ),
                    markReadEffect(),
                )

            is Action.TextViewDidChange -> {
                DependencyValues.current.inputBarService.textViewDidChange(action.text)
                ReduceResult(state)
            }

            Action.DidPressSendButton -> {
                val inputBarService = DependencyValues.current.inputBarService
                val text = inputBarService.text.value
                if (state.isSendingMessage || text.isBlank()) {
                    ReduceResult(state)
                } else {
                    inputBarService.toggleSendingUI(on = true)
                    ReduceResult(state, sendEffect(text))
                }
            }

            is Action.HandleViewAlternateAction -> {
                AlternateMessageService.toggle(AlternateMessageService.AlternateMessageType.ALTERNATE_TEXT, action.message)
                ReduceResult(state.copy(changeToken = UUID.randomUUID()))
            }

            is Action.HandleAudioMessageAction -> {
                AlternateMessageService.toggle(AlternateMessageService.AlternateMessageType.AUDIO_TRANSCRIPTION, action.message)
                ReduceResult(state.copy(changeToken = UUID.randomUUID()))
            }

            is Action.ReactToSelectedMessage ->
                ReduceResult(state, reactEffect(action.message, action.style))

            is Action.HandleSessionStoreChange ->
                if (shouldReload(action.change)) {
                    val effect = reloadEffect(ReducerFloats.SESSION_STORE_CHANGE_DEBOUNCE_MILLISECONDS, ReloadEffectID.SESSION_STORE)
                    ReduceResult(state, effect)
                } else {
                    ReduceResult(state)
                }

            is Action.IsSendingMessageChanged -> {
                DependencyValues.current.inputBarService.toggleSendingUI(on = action.isSendingMessage, clearInputTextViewText = false)
                ReduceResult(state.copy(isSendingMessage = action.isSendingMessage))
            }

            is Action.HandleOutboxChange ->
                ReduceResult(
                    state.copy(isSendingOutboxEntry = action.isSendingOutboxEntry),
                    reloadEffect(ReducerFloats.OUTBOX_CHANGE_DEBOUNCE_MILLISECONDS, ReloadEffectID.OUTBOX),
                )

            Action.ReloadRequested ->
                ReduceResult(state.copy(changeToken = UUID.randomUUID()), markReadEffect())

            Action.ConversationUnavailable -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.ViewDisappeared -> {
                viewDisappeared(state)
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    private enum class ReloadEffectID {
        OUTBOX,
        SESSION_STORE,
    }

    /**
     * Whether the chat page is being disposed while its conversation
     * remains on screen, as opposed to the conversation being left:
     * either a sub-page of the same conversation – its chat info or a
     * message's reaction details – was pushed over it, or another chat
     * page now presents the same conversation, as when the new chat page
     * hands off to the conversation after its first message is sent.
     */
    private fun isConversationStillPresented(conversationIDKey: String): Boolean {
        val topPath =
            DependencyValues.current.navigation.state.value.userContent.stack
                .lastOrNull()
        return when (topPath) {
            is UserContentNavigatorState.SeguePaths.Chat -> topPath.conversationIDKey == conversationIDKey
            is UserContentNavigatorState.SeguePaths.ChatInfo -> topPath.conversationIDKey == conversationIDKey
            else -> false
        }
    }

    private fun reloadEffect(
        debounceMilliseconds: Long,
        id: ReloadEffectID,
    ): Effect<Action> =
        Effect
            .task<Action>(delay = debounceMilliseconds.milliseconds) { Action.ReloadRequested }
            .cancellable(id, cancelInFlight = true)

    private fun viewAppearedEffect(
        conversationIDKey: String,
        configuration: ChatPageView.Configuration,
    ): Effect<Action> =
        Effect.run { send ->
            val conversation = SessionStore.getConversation(conversationIDKey)
            if (conversation == null) {
                send(Action.ConversationUnavailable)
                return@run
            }

            ChatPageViewService.setConfiguration(configuration)
            ConversationSessionService.resetMessageOffset()
            ConversationSessionService.setCurrentConversation(conversation)
            configuration.focusedMessageID?.let { ConversationSessionService.incrementMessageOffset(it) }

            val inputBarService = DependencyValues.current.inputBarService
            inputBarService.configureInputBar(forceUpdate = true)
            inputBarService.toggleSendingUI(on = false)

            val navigationTitle = conversation.chatPageHeaderLabelText ?: ConversationCellViewData(conversation)?.titleLabelText
            navigationTitle?.let { ChatPageViewService.setNavigationTitle(it) }
            markCurrentConversationAsRead()
        }

    // Speech, playback, and alternate content stop on every disappearance;
    // the current-conversation pointer clears only when the page is truly
    // being dismissed – not when covered by a sub-page of the same
    // conversation, which on Android is a push that disposes the chat, nor
    // when another chat page has taken over the same conversation. The
    // outgoing page is disposed only after its exit transition, by which
    // time the incoming page has already set the pointer.
    private fun viewDisappeared(state: State) {
        TextToSpeechService.stopSpeakingIfNeeded()
        AudioMessagePlaybackService.stopPlayback()
        DependencyValues.current.contextMenuActionHandlerService.resetSpeakingMessage()
        AlternateMessageService.restoreAllAlternateTextMessageIDs()
        AlternateMessageService.restoreAllAudioTranscriptionMessageIDs()
        DependencyValues.current.inputBarService.textViewDidChange("")

        if (state.configuration is ChatPageView.Configuration.Preview) return
        if (!isConversationStillPresented(state.conversationIDKey)) {
            // Flush read receipts for any message that arrived within the
            // mark-read debounce window just before the page was left; the
            // detached flush outlives the view model.
            ReadReceiptService.flushUnreadMessages(state.conversationIDKey)
            ConversationSessionService.setCurrentConversation(null)
            ChatPageViewService.clearNavigationTitle()
        }

        ConversationSessionService.resetMessageOffset()
    }

    private fun sendEffect(text: String): Effect<Action> =
        Effect.run {
            val inputBarService = DependencyValues.current.inputBarService
            try {
                inputBarService.actionHandler.didPressSendButton(text)
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }
}
