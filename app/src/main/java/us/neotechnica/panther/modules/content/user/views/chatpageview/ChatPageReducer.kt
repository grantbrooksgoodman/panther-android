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
import us.neotechnica.panther.modules.content.user.services.ContextMenuActionHandlerService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.services.AnalyticsService
import us.neotechnica.panther.networking.modules.schema.conversation.models.Reaction
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.isMediaMessage
import us.neotechnica.panther.networking.modules.session.extensions.resolvedMediaFile
import us.neotechnica.panther.networking.modules.session.extensions.resolvedTranslation
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.MessageDeliveryService
import us.neotechnica.panther.networking.modules.session.services.ReactionSessionService
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
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

        data class MediaResolved(
            val media: Map<String, MediaFile>,
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

        data object StoreChanged : Action

        data object ConversationUnavailable : Action

        data object ViewDisappeared : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val messages: List<Message> = emptyList(),
        val translationsByID: Map<String, Translation> = emptyMap(),
        val mediaByID: Map<String, MediaFile> = emptyMap(),
        val pendingAttachment: MediaFile? = null,
        val alternateTextMessageIDs: Set<String> = emptySet(),
        val inputText: String = "",
        val isSending: Boolean = false,
        val languageCode: String = "en",
        val title: String = "",
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
                    state.copy(conversationIDKey = action.conversationIDKey, languageCode = RuntimeStorage.languageCode),
                    startEffect(action.conversationIDKey),
                )
            }

            is Action.MessagesUpdated ->
                ReduceResult(
                    state.copy(messages = action.messages, viewState = ViewState.Loaded, changeToken = UUID.randomUUID()),
                    resolveEffect(action.messages, state.languageCode, state.translationsByID, state.mediaByID),
                )

            is Action.TranslationsResolved ->
                ReduceResult(state.copy(translationsByID = state.translationsByID + action.translations))

            is Action.MediaResolved ->
                ReduceResult(
                    state.copy(mediaByID = state.mediaByID + action.media, changeToken = UUID.randomUUID()),
                )

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

    private fun reactEffect(
        message: Message,
        style: Reaction.Style,
    ): Effect<Action> =
        Effect.run {
            val currentUserID = User.currentUserID ?: return@run
            try {
                ReactionSessionService.react(Reaction(style, currentUserID), message)
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }

    private fun sendMediaEffect(mediaFile: MediaFile): Effect<Action> =
        Effect.run {
            MessageDeliveryService.sendMediaMessage(mediaFile)
        }

    private fun speakEffect(
        state: State,
        messageID: String,
        displayText: String,
    ): Effect<Action> =
        Effect.run {
            val message = state.messages.firstOrNull { it.id == messageID } ?: return@run
            val translation = state.translationsByID[messageID] ?: message.translations?.firstOrNull()
            val isDisplayingAlternateText = messageID in state.alternateTextMessageIDs
            try {
                ContextMenuActionHandlerService.handleSpeakAction(message, translation, displayText, isDisplayingAlternateText)
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }

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
        existingMedia: Map<String, MediaFile>,
    ): Effect<Action> =
        Effect.run { send ->
            val resolved = resolveTranslations(messages, languageCode, existing)
            val resolvedMedia = resolveMedia(messages, existingMedia)
            if (resolved.isNotEmpty()) send(Action.TranslationsResolved(resolved))
            if (resolvedMedia.isNotEmpty()) send(Action.MediaResolved(resolvedMedia))
            markCurrentConversationAsRead()
        }

    private suspend fun resolveTranslations(
        messages: List<Message>,
        languageCode: String,
        existing: Map<String, Translation>,
    ): Map<String, Translation> {
        val resolved = mutableMapOf<String, Translation>()
        for (message in messages) {
            if (message.id in existing) continue
            message.resolvedTranslation(languageCode)?.let { resolved[message.id] = it }
        }
        return resolved
    }

    private suspend fun resolveMedia(
        messages: List<Message>,
        existingMedia: Map<String, MediaFile>,
    ): Map<String, MediaFile> {
        val resolved = mutableMapOf<String, MediaFile>()
        for (message in messages) {
            if (!message.isMediaMessage || message.id in existingMedia) continue
            message.resolvedMediaFile()?.let { resolved[message.id] = it }
        }
        return resolved
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
