//
//  NewChatPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import us.neotechnica.panther.modules.common.contacts.services.ContactPairArchiveService
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.isSelected
import us.neotechnica.panther.modules.content.user.extensions.mock
import us.neotechnica.panther.modules.content.user.extensions.queried
import us.neotechnica.panther.modules.content.user.extensions.uniquedByPhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.users
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.RecipientBarContactSelectionUIService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.empty
import us.neotechnica.panther.modules.session.entity.extensions.mock
import us.neotechnica.panther.modules.session.entity.extensions.sortedByLatestMessageSentDate
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.entity.extensions.visibleForCurrentUser
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer for starting a new conversation.
 *
 * Recipients are chosen through the recipient bar – from the contact
 * suggestions, the contact selector, or by entering a phone number,
 * which resolves to its registered user or an unregistered mock. The
 * current conversation is kept in sync with the selected recipients,
 * and the first message sends through the delivery service and
 * navigates into the resulting conversation.
 */
class NewChatPageReducer : Reducer<NewChatPageReducer.State, NewChatPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object ViewDisappeared : Action

        data object DoneToolbarButtonTapped : Action

        data class RecipientQueryChanged(
            val query: String,
        ) : Action

        data object RecipientQuerySubmitted : Action

        data object RecipientBackspaced : Action

        data class SuggestionTapped(
            val contactPair: ContactPair,
        ) : Action

        data class RecipientChipTapped(
            val viewID: String,
        ) : Action

        data class RemoveRecipientTapped(
            val viewID: String,
        ) : Action

        data class InputChanged(
            val text: String,
        ) : Action

        data object SendTapped : Action

        data class HighlightedViewIDChanged(
            val highlightedViewID: String?,
        ) : Action

        data class IsDoneToolbarButtonEnabledChanged(
            val isDoneToolbarButtonEnabled: Boolean,
        ) : Action

        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        data class MessageOutboxChanged(
            val hasSendingOutboxEntry: Boolean,
        ) : Action

        data class ResolvedContactPairsReturned(
            val contactPairs: List<ContactPair>,
        ) : Action

        data class SelectedContactPairsChanged(
            val selectedContactPairs: List<ContactPair>,
        ) : Action

        data class SendReturned(
            val conversationIDKey: String,
        ) : Action

        data class SendFailed(
            val exception: Exception,
            val restoredText: String,
        ) : Action
    }

    // MARK: - State

    data class State(
        val doneToolbarButtonText: String = LocalizedStringKey.Cancel.localized(),
        val hasSendingOutboxEntry: Boolean = false,
        val highlightedViewID: String? = null,
        val inputText: String = "",
        val isDoneToolbarButtonEnabled: Boolean = true,
        val isSendingMessage: Boolean = false,
        val navigationTitle: String = LocalizedStringKey.NewMessage.localized(),
        val recipientQuery: String = "",
        val resolvedContactPairs: List<ContactPair> = emptyList(),
        val selectedContactPairs: List<ContactPair> = emptyList(),
        val shouldUseBoldDoneToolbarButton: Boolean = false,
        val didNavigateToChat: Boolean = false,
    ) {
        /** The contact suggestions matching [recipientQuery], excluding already-selected recipients. */
        val suggestions: List<ContactPair>
            get() =
                when {
                    recipientQuery.isBlank() -> emptyList()
                    recipientQuery.trim() == "0" -> resolvedContactPairs.filter { !it.isSelected }
                    else -> resolvedContactPairs.queried(recipientQuery)
                }

        /** Whether the first message can be sent. */
        val canSend: Boolean
            get() = selectedContactPairs.isNotEmpty() && inputText.isNotBlank() && !isSendingMessage
    }

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.ACCESS_NEW_CHAT_PAGE)
                ReduceResult(state, resolveContactPairsEffect())
            }

            Action.ViewDisappeared -> {
                RecipientBarContactSelectionUIService.reset()
                if (!state.didNavigateToChat) ConversationSessionService.setCurrentConversation(null)
                ReduceResult(state)
            }

            Action.DoneToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            is Action.RecipientQueryChanged ->
                ReduceResult(state.copy(recipientQuery = action.query))

            Action.RecipientQuerySubmitted ->
                ReduceResult(state.copy(recipientQuery = ""), recipientQuerySubmittedEffect(state.recipientQuery))

            Action.RecipientBackspaced ->
                ReduceResult(state, Effect.fireAndForget { onSuperfluousBackspace() })

            is Action.SuggestionTapped ->
                ReduceResult(
                    state.copy(recipientQuery = ""),
                    Effect.fireAndForget { RecipientBarContactSelectionUIService.selectContactPair(action.contactPair) },
                )

            is Action.RecipientChipTapped ->
                ReduceResult(state, Effect.fireAndForget { RecipientBarContactSelectionUIService.toggleIsHighlighted(action.viewID) })

            is Action.RemoveRecipientTapped ->
                ReduceResult(state, Effect.fireAndForget { RecipientBarContactSelectionUIService.deselectContactPair(action.viewID) })

            is Action.InputChanged ->
                ReduceResult(state.copy(inputText = action.text))

            Action.SendTapped ->
                if (!state.canSend) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state.copy(inputText = ""), sendEffect(state.inputText))
                }

            is Action.HighlightedViewIDChanged ->
                ReduceResult(state.copy(highlightedViewID = action.highlightedViewID))

            is Action.IsDoneToolbarButtonEnabledChanged ->
                ReduceResult(state.copy(isDoneToolbarButtonEnabled = action.isDoneToolbarButtonEnabled))

            is Action.IsSendingMessageChanged ->
                ReduceResult(state.copy(isSendingMessage = action.isSendingMessage))

            is Action.MessageOutboxChanged ->
                ReduceResult(state.copy(hasSendingOutboxEntry = action.hasSendingOutboxEntry))

            is Action.ResolvedContactPairsReturned ->
                ReduceResult(state.copy(resolvedContactPairs = action.contactPairs))

            is Action.SelectedContactPairsChanged ->
                ReduceResult(
                    state.copy(selectedContactPairs = action.selectedContactPairs),
                    resolveConversationEffect(action.selectedContactPairs),
                )

            is Action.SendReturned -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Stack(listOf(UserContentNavigatorState.SeguePath.Chat(action.conversationIDKey))),
                    ),
                )
                ReduceResult(state.copy(didNavigateToChat = true))
            }

            is Action.SendFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(inputText = action.restoredText))
            }
        }

    // MARK: - Auxiliary

    private fun resolveContactPairsEffect(): Effect<Action> =
        Effect.run { send ->
            send(Action.ResolvedContactPairsReturned(resolveContactPairs()))
        }

    private fun resolveContactPairs(): List<ContactPair> {
        val knownContactPairs = ContactPairArchiveService.allValues()
        val visibleConversations =
            UserSessionService.currentUser
                ?.conversations
                ?.visibleForCurrentUser
                ?.filter { it.users != null }
        if (knownContactPairs.isEmpty() || visibleConversations == null) return knownContactPairs.uniquedByPhoneNumber

        val knownUserIDs = knownContactPairs.userIDs.toSet()
        val seenUserIDs = mutableSetOf<String>()
        val unknownContactPairs =
            visibleConversations
                .flatMap { it.users.orEmpty() }
                .filter { it.id !in knownUserIDs && seenUserIDs.add(it.id) }
                .map { ContactPair.withUser(it) }

        return (knownContactPairs + unknownContactPairs).uniquedByPhoneNumber
    }

    private fun recipientQuerySubmittedEffect(text: String): Effect<Action> =
        Effect.fireAndForget {
            if (text.isBlank()) {
                RecipientBarContactSelectionUIService.unhighlightAllViews()
                if (RecipientBarContactSelectionUIService.selectedContactPairs.value.any { it.isMock }) {
                    RecipientBarContactSelectionUIService.deselectMockContactPairs()
                }
                return@fireAndForget
            }

            val phoneNumber = PhoneNumber(text)
            val sanitizedLength =
                text
                    .replace("-", "")
                    .replace("+", "")
                    .trim()
                    .length
            val isPhoneNumber = phoneNumber.compiledNumberString.length > 1 && text.digits.length == sanitizedLength
            if (!isPhoneNumber) {
                RecipientBarContactSelectionUIService.selectContactPair(ContactPair.mock(withName = text))
                return@fireAndForget
            }

            try {
                val user = UserService.getUser(phoneNumber)
                RecipientBarContactSelectionUIService.selectContactPair(user.contactPair ?: ContactPair.withUser(user))
            } catch (exception: Exception) {
                Logger.log(exception)
                RecipientBarContactSelectionUIService.selectContactPair(ContactPair.mock(withName = text))
            }
        }

    private fun onSuperfluousBackspace() {
        val lastContactPair = RecipientBarContactSelectionUIService.selectedContactPairs.value.lastOrNull() ?: return
        val lastViewID = lastContactPair.contact.encodedHash
        if (RecipientBarContactSelectionUIService.isHighlighted(lastViewID)) {
            RecipientBarContactSelectionUIService.deselectContactPair(lastViewID)
        } else {
            RecipientBarContactSelectionUIService.toggleIsHighlighted(lastViewID)
        }
    }

    private fun resolveConversationEffect(selectedContactPairs: List<ContactPair>): Effect<Action> =
        Effect.run {
            val userIDs = selectedContactPairs.userIDs
            val users = selectedContactPairs.users
            val sortedUserIDs = userIDs.sorted()
            val existingConversation =
                UserSessionService.currentUser
                    ?.conversations
                    ?.visibleForCurrentUser
                    ?.filter { it.users != null }
                    ?.sortedByLatestMessageSentDate
                    ?.firstOrNull { conversation ->
                        val conversationUserIDs =
                            conversation.users
                                .orEmpty()
                                .map { it.id }
                                .sorted()
                        conversationUserIDs == sortedUserIDs
                    }

            val conversation =
                when {
                    existingConversation != null -> existingConversation
                    selectedContactPairs.isEmpty() -> Conversation.empty
                    selectedContactPairs.all { it.isMock } -> Conversation.empty(withUsers = users)
                    else -> Conversation.mock(withUsers = users)
                }
            ConversationSessionService.setCurrentConversation(conversation)
        }

    private fun sendEffect(text: String): Effect<Action> =
        Effect.run { send ->
            try {
                MessageDeliveryService.sendTextMessage(text)
                val conversationIDKey = ConversationSessionService.currentConversation?.id?.key
                if (conversationIDKey != null) {
                    send(Action.SendReturned(conversationIDKey))
                } else {
                    send(Action.SendFailed(Exception("No conversation resolved.", metadata = ExceptionMetadata(this)), text))
                }
            } catch (exception: Exception) {
                send(Action.SendFailed(exception, text))
            }
        }
}
