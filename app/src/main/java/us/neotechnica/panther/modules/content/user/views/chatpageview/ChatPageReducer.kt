//
//  ChatPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.user.extensions.chatPageHeaderLabelText
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.session.extensions.resolvedTranslation
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.MessageDeliveryService
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.models.Translation
import java.util.UUID

/**
 * The reducer for a single conversation's chat page.
 *
 * Sets the conversation current on appearance, observes its displayed
 * messages, resolves each message's translation for display, sends text
 * through the outbox-backed delivery service, and marks incoming
 * messages read.
 */
class ChatPageReducer : Reducer<ChatPageReducer.State, ChatPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data class ViewFirstAppeared(
            val conversationIDKey: String,
        ) : Action

        data class MessagesUpdated(
            val messages: List<Message>,
        ) : Action

        data class TranslationsResolved(
            val translations: Map<String, Translation>,
        ) : Action

        data class TitleResolved(
            val title: String,
        ) : Action

        data class InputChanged(
            val text: String,
        ) : Action

        data object SendTapped : Action

        data object SendFinished : Action

        data class ToggleAlternate(
            val messageID: String,
        ) : Action

        data object StoreChanged : Action

        data object ConversationUnavailable : Action

        data object ViewDisappeared : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val messages: List<Message> = emptyList(),
        val translationsByID: Map<String, Translation> = emptyMap(),
        val alternateTextMessageIDs: Set<String> = emptySet(),
        val inputText: String = "",
        val isSending: Boolean = false,
        val languageCode: String = "en",
        val title: String = "",
        val changeToken: UUID = UUID.randomUUID(),
        val viewState: ViewState = ViewState.Loading,
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            is Action.ViewFirstAppeared ->
                ReduceResult(
                    state.copy(conversationIDKey = action.conversationIDKey, languageCode = RuntimeStorage.languageCode),
                    startEffect(action.conversationIDKey),
                )

            is Action.MessagesUpdated ->
                ReduceResult(
                    state.copy(messages = action.messages, viewState = ViewState.Loaded, changeToken = UUID.randomUUID()),
                    resolveEffect(action.messages, state.languageCode, state.translationsByID),
                )

            is Action.TranslationsResolved ->
                ReduceResult(state.copy(translationsByID = state.translationsByID + action.translations))

            is Action.TitleResolved ->
                ReduceResult(state.copy(title = action.title))

            is Action.InputChanged ->
                ReduceResult(state.copy(inputText = action.text))

            Action.SendTapped -> {
                if (state.isSending || state.inputText.isBlank()) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state.copy(inputText = "", isSending = true), sendEffect(state.inputText))
                }
            }

            Action.SendFinished ->
                ReduceResult(state.copy(isSending = false))

            is Action.ToggleAlternate -> {
                val updated =
                    if (action.messageID in state.alternateTextMessageIDs) {
                        state.alternateTextMessageIDs - action.messageID
                    } else {
                        state.alternateTextMessageIDs + action.messageID
                    }
                ReduceResult(state.copy(alternateTextMessageIDs = updated))
            }

            Action.StoreChanged ->
                ReduceResult(state.copy(changeToken = UUID.randomUUID()), markReadEffect())

            Action.ConversationUnavailable -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.ViewDisappeared -> {
                ConversationSessionService.setCurrentConversation(null)
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    private fun startEffect(conversationIDKey: String): Effect<Action> =
        Effect.run { send ->
            val conversation = SessionStore.getConversation(conversationIDKey)
            if (conversation == null) {
                send(Action.ConversationUnavailable)
                return@run
            }

            ConversationSessionService.setCurrentConversation(conversation)
            runCatching {
                val title =
                    conversation.chatPageHeaderLabelText
                        ?: ConversationCellViewData.build(conversation, RuntimeStorage.languageCode).title
                send(Action.TitleResolved(title))
            }
            markCurrentConversationAsRead()
        }

    private fun resolveEffect(
        messages: List<Message>,
        languageCode: String,
        existing: Map<String, Translation>,
    ): Effect<Action> =
        Effect.run { send ->
            val resolved = mutableMapOf<String, Translation>()
            for (message in messages) {
                if (message.id in existing) continue
                message.resolvedTranslation(languageCode)?.let { resolved[message.id] = it }
            }
            if (resolved.isNotEmpty()) send(Action.TranslationsResolved(resolved))
            markCurrentConversationAsRead()
        }

    private fun sendEffect(text: String): Effect<Action> =
        Effect.run { send ->
            MessageDeliveryService.sendTextMessage(text)
            send(Action.SendFinished)
        }

    private fun markReadEffect(): Effect<Action> = Effect.run { markCurrentConversationAsRead() }

    private suspend fun markCurrentConversationAsRead() {
        try {
            ConversationSessionService.markCurrentConversationAsRead()
        } catch (exception: Exception) {
            Logger.log(exception)
        }
    }
}
