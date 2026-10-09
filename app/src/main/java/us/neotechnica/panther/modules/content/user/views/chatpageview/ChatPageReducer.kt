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
import us.neotechnica.panther.modules.content.user.extensions.chatPageHeaderLabelText
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.AudioMessagePlaybackService
import us.neotechnica.panther.modules.content.user.services.ContextMenuActionHandlerService
import us.neotechnica.panther.modules.content.user.services.ReadReceiptService
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.MediaFile
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
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID

/**
 * The reducer for a single conversation's chat page.
 *
 * Sets the conversation current on appearance, observes its displayed
 * messages, sends text through the outbox-backed delivery service,
 * and marks incoming messages read.
 */
class ChatPageReducer : Reducer<ChatPageReducer.State, ChatPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data class ViewFirstAppeared(
            val conversationIDKey: String,
            val focusedMessageID: String? = null,
        ) : Action

        data class MessagesUpdated(
            val messages: List<Message>,
        ) : Action

        data class TitleResolved(
            val title: String,
        ) : Action

        data class InputChanged(
            val text: String,
        ) : Action

        data object SendTapped : Action

        data class ToggleAlternate(
            val messageID: String,
        ) : Action

        data class ToggleAudioTranscription(
            val messageID: String,
        ) : Action

        data class React(
            val message: Message,
            val style: Reaction.Style,
        ) : Action

        data class Speak(
            val messageID: String,
            val displayText: String,
        ) : Action

        data class AttachmentPicked(
            val mediaFile: MediaFile,
        ) : Action

        data object RemoveAttachment : Action

        data object SendMedia : Action

        data class StoreChanged(
            val change: SessionStoreChange,
        ) : Action

        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        data class MessageOutboxChanged(
            val hasSendingOutboxEntry: Boolean,
        ) : Action

        data object ConversationUnavailable : Action

        data object ViewDisappeared : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val messages: List<Message> = emptyList(),
        val pendingAttachment: MediaFile? = null,
        val alternateTextMessageIDs: Set<String> = emptySet(),
        val audioTranscriptionMessageIDs: Set<String> = emptySet(),
        val inputText: String = "",
        val isSendingMessage: Boolean = false,
        val hasSendingOutboxEntry: Boolean = false,
        val languageCode: String = "en",
        val title: String = "",
        val focusedMessageID: String? = null,
        val changeToken: UUID = UUID.randomUUID(),
        val viewState: ViewState = ViewState.Loading,
    )

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            is Action.ViewFirstAppeared -> {
                AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.ACCESS_CHAT)
                ReduceResult(
                    state.copy(
                        conversationIDKey = action.conversationIDKey,
                        languageCode = RuntimeStorage.languageCode,
                        focusedMessageID = action.focusedMessageID,
                    ),
                    startEffect(action.conversationIDKey, action.focusedMessageID),
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

            is Action.TitleResolved ->
                ReduceResult(state.copy(title = action.title))

            is Action.InputChanged ->
                ReduceResult(state.copy(inputText = action.text))

            Action.SendTapped -> {
                if (state.isSendingMessage || state.inputText.isBlank()) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state.copy(inputText = ""), sendEffect(state.inputText))
                }
            }

            is Action.ToggleAlternate -> {
                val isDisplayingAlternateText = action.messageID in state.alternateTextMessageIDs
                if (!isDisplayingAlternateText) {
                    AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.VIEW_ALTERNATE)
                }

                val updated =
                    if (isDisplayingAlternateText) {
                        state.alternateTextMessageIDs - action.messageID
                    } else {
                        state.alternateTextMessageIDs + action.messageID
                    }
                ReduceResult(state.copy(alternateTextMessageIDs = updated))
            }

            is Action.ToggleAudioTranscription -> {
                val isDisplayingAudioTranscription = action.messageID in state.audioTranscriptionMessageIDs
                if (!isDisplayingAudioTranscription) {
                    AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.VIEW_ALTERNATE)
                }

                val updated =
                    if (isDisplayingAudioTranscription) {
                        state.audioTranscriptionMessageIDs - action.messageID
                    } else {
                        state.audioTranscriptionMessageIDs + action.messageID
                    }
                ReduceResult(state.copy(audioTranscriptionMessageIDs = updated))
            }

            is Action.React ->
                ReduceResult(state, reactEffect(action.message, action.style))

            is Action.Speak ->
                ReduceResult(state, speakEffect(state, action.messageID, action.displayText))

            is Action.AttachmentPicked ->
                ReduceResult(state.copy(pendingAttachment = action.mediaFile))

            Action.RemoveAttachment ->
                ReduceResult(state.copy(pendingAttachment = null))

            Action.SendMedia ->
                state.pendingAttachment?.let { mediaFile ->
                    ReduceResult(state.copy(pendingAttachment = null), sendMediaEffect(mediaFile))
                } ?: ReduceResult(state)

            is Action.StoreChanged ->
                if (shouldReload(action.change)) {
                    ReduceResult(state.copy(changeToken = UUID.randomUUID()), markReadEffect())
                } else {
                    ReduceResult(state)
                }

            is Action.IsSendingMessageChanged ->
                ReduceResult(state.copy(isSendingMessage = action.isSendingMessage))

            is Action.MessageOutboxChanged ->
                ReduceResult(state.copy(hasSendingOutboxEntry = action.hasSendingOutboxEntry))

            Action.ConversationUnavailable -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.ViewDisappeared -> {
                // Clear the current-conversation pointer only when the page is
                // truly being dismissed – not when it
                // is covered by a sub-page of the same conversation, which on Android
                // is a push that disposes the chat. Clearing it there would strand
                // chat info's own actions, such as adding a participant, that read the
                // current conversation.
                if (!isCoveredBySubPage(state.conversationIDKey)) {
                    TextToSpeechService.stop()
                    AudioMessagePlaybackService.stopPlayback()
                    ContextMenuActionHandlerService.resetSpeakingMessage()
                    // Flush read receipts for any message that arrived within the
                    // mark-read debounce window just before the page was left. The
                    // page's own mark-read effect runs on the view model's scope, so
                    // disposing the page cancels it mid-flight; this detached flush
                    // outlives the view model and catches those stragglers.
                    ReadReceiptService.flushUnreadMessages(state.conversationIDKey)
                    ConversationSessionService.setCurrentConversation(null)
                }
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    /**
     * Whether the chat page is being disposed because a sub-page of the
     * same conversation – its chat info or a message's reaction details –
     * was pushed over it, as opposed to the conversation being left.
     */
    private fun isCoveredBySubPage(conversationIDKey: String): Boolean {
        val topPath =
            DependencyValues.current.navigation.state.value.userContent.stack
                .lastOrNull()
        return when (topPath) {
            is UserContentNavigatorState.SeguePath.ChatInfo -> topPath.conversationIDKey == conversationIDKey
            else -> false
        }
    }

    private fun speakEffect(
        state: State,
        messageID: String,
        displayText: String,
    ): Effect<Action> =
        Effect.run {
            val message = state.messages.firstOrNull { it.id == messageID } ?: return@run
            val isDisplayingAlternateText = messageID in state.alternateTextMessageIDs
            try {
                ContextMenuActionHandlerService.handleSpeakAction(message, message.translation, displayText, isDisplayingAlternateText)
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }

    private fun startEffect(
        conversationIDKey: String,
        focusedMessageID: String?,
    ): Effect<Action> =
        Effect.run { send ->
            val conversation = SessionStore.getConversation(conversationIDKey)
            if (conversation == null) {
                send(Action.ConversationUnavailable)
                return@run
            }

            ConversationSessionService.setCurrentConversation(conversation)
            focusedMessageID?.let { ConversationSessionService.incrementMessageOffset(it) }
            runCatching {
                val title =
                    conversation.chatPageHeaderLabelText
                        ?: ConversationCellViewData.build(conversation).title
                send(Action.TitleResolved(title))
            }
            markCurrentConversationAsRead()
        }
}
