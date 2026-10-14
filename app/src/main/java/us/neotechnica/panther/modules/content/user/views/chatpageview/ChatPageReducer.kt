//
//  ChatPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.AnalyticsService
import us.neotechnica.panther.modules.common.services.sources.audio.TextToSpeechService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.dependencies.chatPageViewService
import us.neotechnica.panther.modules.content.user.dependencies.contextMenuActionHandlerService
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.extensions.chatPageHeaderLabelText
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.chatpageview.ChatPageViewService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AlternateMessageService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AudioMessagePlaybackService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.ReadReceiptService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
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
    // MARK: - Dependencies

    private val analyticsService: AnalyticsService by Dependency { it.commonServices.analytics }
    private val audioMessagePlaybackService: AudioMessagePlaybackService by Dependency { it.chatPageViewService.audioMessagePlayback }
    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val sessionStore: SessionStore by Dependency { it.clientSession.store }
    private val textToSpeechService: TextToSpeechService by Dependency { it.commonServices.audio.textToSpeech }

    // MARK: - Action

    /** The actions the chat page can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared, carrying the
         * conversation's ID key and the page's configuration. Sets the
         * conversation current and begins observing its messages.
         */
        data class ViewAppeared(
            val conversationIDKey: String,
            val configuration: ChatPageView.Configuration,
        ) : Action

        /**
         * An action that indicates the displayed messages changed,
         * carrying them. Marks incoming messages read.
         */
        data class MessagesUpdated(
            val messages: List<Message>,
        ) : Action

        /** An action that indicates the input text changed, carrying the text. */
        data class TextViewDidChange(
            val text: String,
        ) : Action

        /**
         * An action that indicates the user pressed the send button.
         * Sends the input text unless a send is in flight or the text
         * is blank.
         */
        data object DidPressSendButton : Action

        /**
         * An action that toggles the given message between its
         * translation and its original text.
         */
        data class HandleViewAlternateAction(
            val message: Message,
        ) : Action

        /**
         * An action that toggles the given message between its audio
         * and its transcription.
         */
        data class HandleAudioMessageAction(
            val message: Message,
        ) : Action

        /** An action that applies the given reaction style to the given message. */
        data class ReactToSelectedMessage(
            val message: Message,
            val style: Reaction.Style,
        ) : Action

        /**
         * An action that indicates the session store changed, carrying
         * the change. Schedules a debounced reload when the change
         * affects the conversation.
         */
        data class HandleSessionStoreChange(
            val change: SessionStoreChange,
        ) : Action

        /**
         * An action that indicates whether a message send is in flight
         * changed, carrying the new value. Toggles the input bar's
         * sending state.
         */
        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        /**
         * An action that indicates the outbox changed, carrying whether
         * an entry is being sent. Schedules a debounced reload.
         */
        data class HandleOutboxChange(
            val isSendingOutboxEntry: Boolean,
        ) : Action

        /**
         * An action that rebuilds the message list from the latest
         * values and marks incoming messages read.
         */
        data object ReloadRequested : Action

        /** An action that indicates the conversation no longer exists. Pops the page. */
        data object ConversationUnavailable : Action

        /**
         * An action that indicates the view disappeared. Stops speech
         * and playback, restores alternate content, clears the input,
         * and – unless the conversation remains on screen – flushes
         * read receipts and clears the current conversation.
         */
        data object ViewDisappeared : Action
    }

    // MARK: - State

    /**
     * The state of the chat page.
     *
     * @property conversationIDKey The ID key of the conversation the
     *   page displays.
     * @property configuration The configuration the page was presented
     *   with.
     * @property messages The messages the page displays.
     * @property isSendingMessage A Boolean value that indicates whether
     *   a message send is in flight.
     * @property isSendingOutboxEntry A Boolean value that indicates
     *   whether an outbox entry is being sent.
     * @property languageCode The app's language code when the page
     *   appeared.
     * @property changeToken The identity of the message list's content.
     *   Regenerated to rebuild it from the latest values.
     * @property viewState The page's loading state. Remains loading
     *   until the messages first update.
     */
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
                    analyticsService.logEvent(AnalyticsService.AnalyticsEvent.ACCESS_CHAT)
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
            val conversation = sessionStore.getConversation(conversationIDKey)
            if (conversation == null) {
                send(Action.ConversationUnavailable)
                return@run
            }

            ChatPageViewService.setConfiguration(configuration)
            conversationSession.resetMessageOffset()
            conversationSession.setCurrentConversation(conversation)
            configuration.focusedMessageID?.let { conversationSession.incrementMessageOffset(it) }

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
        textToSpeechService.stopSpeakingIfNeeded()
        audioMessagePlaybackService.stopPlayback()
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
            conversationSession.setCurrentConversation(null)
            ChatPageViewService.clearNavigationTitle()
        }

        conversationSession.resetMessageOffset()
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
