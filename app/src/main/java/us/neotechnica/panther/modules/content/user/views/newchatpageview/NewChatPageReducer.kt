//
//  NewChatPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.newchatpageview

import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.common.services.AnalyticsService
import us.neotechnica.panther.networking.modules.schema.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.conversations
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.extensions.empty
import us.neotechnica.panther.networking.modules.session.extensions.mock
import us.neotechnica.panther.networking.modules.session.extensions.sortedByLatestMessageSentDate
import us.neotechnica.panther.networking.modules.session.extensions.users
import us.neotechnica.panther.networking.modules.session.extensions.visibleForCurrentUser
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.MessageDeliveryService
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer for starting a new conversation.
 *
 * Recipients are added from the contact suggestions, the contact
 * selector, or by entering a phone number in the recipient bar (which
 * resolves to its registered user). The current conversation is kept
 * in sync with the recipients, and a first message sends through the
 * delivery service and navigates into the resulting conversation.
 */
class NewChatPageReducer : Reducer<NewChatPageReducer.State, NewChatPageReducer.Action> {
    // MARK: - Types

    /** A recipient added to the new conversation. */
    data class Recipient(
        val userID: String,
        val displayName: String,
    )

    // MARK: - Action

    sealed interface Action {
        data object ViewFirstAppeared : Action

        data object ViewDisappeared : Action

        data class RecipientQueryChanged(
            val query: String,
        ) : Action

        data object RecipientQuerySubmitted : Action

        data object RecipientBackspaced : Action

        data class AddRecipient(
            val userID: String,
            val displayName: String,
        ) : Action

        data class RemoveRecipient(
            val userID: String,
        ) : Action

        data class InputChanged(
            val text: String,
        ) : Action

        data object SendTapped : Action

        data object BackTapped : Action

        data class IsSendingMessageChanged(
            val isSendingMessage: Boolean,
        ) : Action

        data class MessageOutboxChanged(
            val hasSendingOutboxEntry: Boolean,
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
        val contacts: List<ContactMatch> = emptyList(),
        val recipients: List<Recipient> = emptyList(),
        val recipientQuery: String = "",
        val highlightedRecipientID: String? = null,
        val inputText: String = "",
        val isSendingMessage: Boolean = false,
        val hasSendingOutboxEntry: Boolean = false,
        val didNavigateToChat: Boolean = false,
    ) {
        /** The contact suggestions matching [recipientQuery], excluding already-added recipients. */
        val suggestions: List<ContactMatch>
            get() {
                if (recipientQuery.isBlank()) return emptyList()
                val query = recipientQuery.trim().lowercase()
                val addedIDs = recipients.map { it.userID }.toSet()
                return contacts.filter {
                    it.userID !in addedIDs &&
                        (it.fullName.lowercase().contains(query) || it.compiledNumberString.contains(query))
                }
            }

        /** Whether the entered recipient query is a phone number that can be looked up. */
        val recipientQueryIsPhoneNumber: Boolean
            get() {
                val digits = recipientQuery.digits
                if (digits.isEmpty()) return false
                return PhoneNumberService.numberIsValidLength(digits.length, PhoneNumberService.deviceCallingCode)
            }

        /** Whether the first message can be sent. */
        val canSend: Boolean
            get() = recipients.isNotEmpty() && inputText.isNotBlank() && !isSendingMessage
    }

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewFirstAppeared -> {
                AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.ACCESS_NEW_CHAT_PAGE)
                ReduceResult(state.copy(contacts = ContactService.matches().filter { it.userID != User.currentUserID }))
            }

            Action.ViewDisappeared -> {
                if (!state.didNavigateToChat) ConversationSessionService.setCurrentConversation(null)
                ReduceResult(state)
            }

            is Action.RecipientQueryChanged ->
                ReduceResult(state.copy(recipientQuery = action.query, highlightedRecipientID = null))

            Action.RecipientQuerySubmitted ->
                if (state.recipientQueryIsPhoneNumber) {
                    ReduceResult(state.copy(recipientQuery = ""), findByPhoneEffect(state.recipientQuery))
                } else {
                    ReduceResult(state)
                }

            Action.RecipientBackspaced -> {
                val updated = state.backspacingRecipient()
                if (updated.recipients != state.recipients) {
                    ReduceResult(updated, resolveConversationEffect(updated.recipients.map { it.userID }))
                } else {
                    ReduceResult(updated)
                }
            }

            is Action.AddRecipient -> {
                if (action.userID == User.currentUserID) {
                    ReduceResult(state)
                } else {
                    val alreadyAdded = state.recipients.any { it.userID == action.userID }
                    val recipients =
                        if (alreadyAdded) state.recipients else state.recipients + Recipient(action.userID, action.displayName)
                    ReduceResult(
                        state.copy(
                            recipients = recipients,
                            recipientQuery = "",
                            highlightedRecipientID = null,
                        ),
                        resolveConversationEffect(recipients.map { it.userID }),
                    )
                }
            }

            is Action.RemoveRecipient -> {
                val recipients = state.recipients.filter { it.userID != action.userID }
                ReduceResult(
                    state.copy(
                        recipients = recipients,
                        highlightedRecipientID = state.highlightedRecipientID.takeIf { it != action.userID },
                    ),
                    resolveConversationEffect(recipients.map { it.userID }),
                )
            }

            is Action.InputChanged ->
                ReduceResult(state.copy(inputText = action.text))

            Action.SendTapped ->
                if (!state.canSend) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state.copy(inputText = ""), sendEffect(state.inputText))
                }

            Action.BackTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            is Action.IsSendingMessageChanged ->
                ReduceResult(state.copy(isSendingMessage = action.isSendingMessage))

            is Action.MessageOutboxChanged ->
                ReduceResult(state.copy(hasSendingOutboxEntry = action.hasSendingOutboxEntry))

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

    /**
     * The state after a backspace on the empty field: the first backspace
     * highlights the trailing recipient, and a second removes it, mirroring
     * the iOS `onSuperflousBackspace`.
     */
    private fun State.backspacingRecipient(): State =
        when {
            recipientQuery.isNotEmpty() || recipients.isEmpty() -> this
            highlightedRecipientID == null -> copy(highlightedRecipientID = recipients.last().userID)
            else ->
                copy(
                    recipients = recipients.filter { it.userID != highlightedRecipientID },
                    highlightedRecipientID = null,
                )
        }

    private fun findByPhoneEffect(query: String): Effect<Action> =
        Effect.run { send ->
            val regionCode = RegionDetailService.deviceRegionCode
            val phoneNumber =
                PhoneNumber(
                    callingCode = RegionDetailService.callingCode(regionCode) ?: PhoneNumberService.deviceCallingCode,
                    nationalNumberString = query.digits,
                    regionCode = regionCode,
                    label = null,
                    internalFormattedString = null,
                )
            try {
                if (UserService.accountExists(phoneNumber)) {
                    val user = UserService.getUser(phoneNumber)
                    val name = ContactService.match(user.id)?.fullName ?: user.phoneNumber.formattedString()
                    send(Action.AddRecipient(user.id, name))
                }
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

    private fun resolveConversationEffect(recipientUserIDs: List<String>): Effect<Action> =
        Effect.run {
            if (recipientUserIDs.isEmpty()) {
                ConversationSessionService.setCurrentConversation(Conversation.empty)
                return@run
            }

            val users =
                try {
                    UserService.getUsers(recipientUserIDs)
                } catch (exception: Exception) {
                    Logger.log(exception)
                    return@run
                }

            val sortedIDs = recipientUserIDs.sorted()
            val existingConversation =
                UserSessionService.currentUser
                    ?.conversations
                    ?.visibleForCurrentUser
                    ?.filter { it.users != null }
                    ?.sortedByLatestMessageSentDate
                    ?.firstOrNull { it.users?.map { user -> user.id }?.sorted() == sortedIDs }

            if (existingConversation != null) {
                ConversationSessionService.setCurrentConversation(existingConversation)
            } else {
                ConversationSessionService.setCurrentConversation(Conversation.mock(withUsers = users))
            }
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
