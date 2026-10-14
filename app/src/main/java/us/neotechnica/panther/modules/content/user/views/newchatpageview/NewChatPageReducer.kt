//
//  NewChatPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer that drives the new chat page.
 *
 * This page composes a new conversation. The user chooses recipients
 * through the recipient bar, which [RecipientBarService] drives, and
 * sends the first message through the page's input bar.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page configures its toolbar – titling it and
 *   labeling the done button as Cancel.
 * - Once the first message is sent, the toolbar updates to reflect the
 *   now-created conversation: its title becomes the conversation's
 *   title and the done button is relabeled; the page then navigates
 *   into that conversation.
 * - Tapping done dismisses the page.
 */
class NewChatPageReducer : Reducer<NewChatPageReducer.State, NewChatPageReducer.Action> {
    // MARK: - Dependencies

    private val analyticsService: AnalyticsService by Dependency { it.commonServices.analytics }
    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }

    // MARK: - Actions

    /** The actions the new chat page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Configures the toolbar. */
        data object ViewAppeared : Action

        /** An action that indicates the view disappeared. Releases the recipient bar and the composed conversation. */
        data object ViewDisappeared : Action

        /** An action that indicates the user tapped the done button. Dismisses the page. */
        data object DoneToolbarButtonTapped : Action

        /** An action that indicates the first message was sent. Updates the toolbar to reflect the now-created conversation. */
        data object FirstMessageSent : Action

        /** An action that indicates the user tapped the send button. Sends the entered text as the first message. */
        data object SendTapped : Action

        /** An action that indicates the input bar's text changed, carrying the new value. */
        data class InputChanged(
            val text: String,
        ) : Action

        /** An action that indicates whether the done button is enabled changed, carrying the new value. */
        data class IsDoneToolbarButtonEnabledChanged(
            val isDoneToolbarButtonEnabled: Boolean,
        ) : Action

        /** An action that indicates whether a message send is in flight changed, carrying the new value. */
        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        /** An action that indicates the outbox changed, carrying whether an entry is still sending. */
        data class MessageOutboxChanged(
            val hasSendingOutboxEntry: Boolean,
        ) : Action

        /** An action that indicates the first message failed to send, carrying the resulting [Exception] and the text to restore. */
        data class SendFailed(
            val exception: Exception,
            val restoredText: String,
        ) : Action

        /** An action that indicates the first message was sent, carrying the created conversation's key. */
        data class SendReturned(
            val conversationIDKey: String,
        ) : Action
    }

    // MARK: - State

    /** The state of the new chat page. */
    data class State(
        /** The text the done button displays. */
        val doneToolbarButtonText: String = "",
        /** A Boolean value that indicates whether the first message has been sent. */
        val hasSentFirstMessage: Boolean = false,
        /** A Boolean value that indicates whether an outbox entry is still sending. */
        val hasSendingOutboxEntry: Boolean = false,
        /** The text the input bar displays. */
        val inputText: String = "",
        /** A Boolean value that indicates whether the done button is enabled. */
        val isDoneToolbarButtonEnabled: Boolean = true,
        /** A Boolean value that indicates whether a message send is in flight. */
        val isSendingMessage: Boolean = false,
        /** The page's navigation title. */
        val navigationTitle: String = "",
        /** A Boolean value that indicates whether the done button uses a bold font. */
        val shouldUseBoldDoneToolbarButton: Boolean = false,
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                analyticsService.logEvent(AnalyticsService.AnalyticsEvent.ACCESS_NEW_CHAT_PAGE)
                ReduceResult(
                    state.copy(
                        doneToolbarButtonText = LocalizedStringKey.Cancel.localized(),
                        navigationTitle = LocalizedStringKey.NewMessage.localized(),
                    ),
                )
            }

            Action.ViewDisappeared -> {
                if (!state.hasSentFirstMessage) conversationSession.setCurrentConversation(null)
                RecipientBarService.reset()
                ReduceResult(state)
            }

            Action.DoneToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.FirstMessageSent -> reduceFirstMessageSent(state)

            is Action.InputChanged -> ReduceResult(state.copy(inputText = action.text))

            is Action.IsDoneToolbarButtonEnabledChanged ->
                ReduceResult(state.copy(isDoneToolbarButtonEnabled = action.isDoneToolbarButtonEnabled))

            is Action.IsSendingMessageChanged -> ReduceResult(state.copy(isSendingMessage = action.isSendingMessage))

            is Action.MessageOutboxChanged -> ReduceResult(state.copy(hasSendingOutboxEntry = action.hasSendingOutboxEntry))

            is Action.SendFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(inputText = action.restoredText))
            }

            is Action.SendReturned -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Stack(listOf(UserContentNavigatorState.SeguePaths.Chat(action.conversationIDKey))),
                    ),
                )
                ReduceResult(state.copy(hasSentFirstMessage = true))
            }

            Action.SendTapped ->
                if (state.inputText.isBlank() || state.isSendingMessage) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state.copy(inputText = ""), sendEffect(state.inputText))
                }
        }

    // MARK: - Auxiliary

    private fun reduceFirstMessageSent(state: State): ReduceResult<State, Action> {
        val currentConversation = conversationSession.currentConversation ?: return ReduceResult(state)
        val titleLabelText = ConversationCellViewData(currentConversation)?.titleLabelText ?: return ReduceResult(state)

        return ReduceResult(
            state.copy(
                doneToolbarButtonText = LocalizedStringKey.Done.localized(LocalizationSource.SUBSYSTEM),
                navigationTitle = titleLabelText,
                shouldUseBoldDoneToolbarButton = true,
            ),
        )
    }

    private fun sendEffect(text: String): Effect<Action> =
        Effect.run { send ->
            try {
                MessageDeliveryService.sendTextMessage(text)
                val conversationIDKey = conversationSession.currentConversation?.id?.key
                if (conversationIDKey != null) {
                    send(Action.FirstMessageSent)
                    send(Action.SendReturned(conversationIDKey))
                } else {
                    send(Action.SendFailed(Exception("No conversation resolved.", metadata = ExceptionMetadata(this)), text))
                }
            } catch (exception: Exception) {
                send(Action.SendFailed(exception, text))
            }
        }
}
