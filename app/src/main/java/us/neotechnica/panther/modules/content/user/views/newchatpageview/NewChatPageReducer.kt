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
import us.neotechnica.panther.networking.modules.session.services.MessageSessionService
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
 * resolves to its registered user). A first message creates the
 * conversation on send and navigates into it.
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

        data class RecipientQueryChanged(
            val query: String,
        ) : Action

        data object RecipientQuerySubmitted : Action

        data class AddRecipient(
            val userID: String,
            val displayName: String,
        ) : Action

        data class RemoveRecipient(
            val userID: String,
        ) : Action

        data object ShowContactSelector : Action

        data object DismissContactSelector : Action

        data class InputChanged(
            val text: String,
        ) : Action

        data object SendTapped : Action

        data class SendReturned(
            val conversationIDKey: String,
        ) : Action

        data class SendFailed(
            val exception: Exception,
        ) : Action

        data object BackTapped : Action
    }

    // MARK: - State

    data class State(
        val contacts: List<ContactMatch> = emptyList(),
        val recipients: List<Recipient> = emptyList(),
        val recipientQuery: String = "",
        val inputText: String = "",
        val isSending: Boolean = false,
        val isShowingContactSelector: Boolean = false,
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
            get() = recipients.isNotEmpty() && inputText.isNotBlank() && !isSending
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewFirstAppeared -> {
                AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.ACCESS_NEW_CHAT_PAGE)
                ReduceResult(state.copy(contacts = ContactService.matches()))
            }

            is Action.RecipientQueryChanged ->
                ReduceResult(state.copy(recipientQuery = action.query))

            Action.RecipientQuerySubmitted ->
                if (state.recipientQueryIsPhoneNumber) {
                    ReduceResult(state.copy(recipientQuery = ""), findByPhoneEffect(state.recipientQuery))
                } else {
                    ReduceResult(state)
                }

            is Action.AddRecipient -> {
                val alreadyAdded = state.recipients.any { it.userID == action.userID }
                val recipients =
                    if (alreadyAdded) state.recipients else state.recipients + Recipient(action.userID, action.displayName)
                ReduceResult(
                    state.copy(recipients = recipients, recipientQuery = "", isShowingContactSelector = false),
                )
            }

            is Action.RemoveRecipient ->
                ReduceResult(state.copy(recipients = state.recipients.filter { it.userID != action.userID }))

            Action.ShowContactSelector ->
                ReduceResult(state.copy(isShowingContactSelector = true))

            Action.DismissContactSelector ->
                ReduceResult(state.copy(isShowingContactSelector = false))

            is Action.InputChanged ->
                ReduceResult(state.copy(inputText = action.text))

            Action.SendTapped ->
                if (!state.canSend) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state.copy(isSending = true), sendEffect(state.inputText, state.recipients.map { it.userID }))
                }

            is Action.SendReturned -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Stack(listOf(UserContentNavigatorState.SeguePath.Chat(action.conversationIDKey))),
                    ),
                )
                ReduceResult(state.copy(isSending = false))
            }

            is Action.SendFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isSending = false))
            }

            Action.BackTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

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

    private fun sendEffect(
        text: String,
        recipientUserIDs: List<String>,
    ): Effect<Action> =
        Effect.run { send ->
            try {
                val users = UserService.getUsers(recipientUserIDs)
                if (users.isEmpty()) {
                    send(Action.SendFailed(Exception("No recipients resolved.", metadata = ExceptionMetadata(this))))
                    return@run
                }

                val conversation =
                    MessageSessionService.sendTextMessage(
                        text = text,
                        presetID = null,
                        users = users,
                        conversation = null,
                    )
                send(Action.SendReturned(conversation.id.key))
            } catch (exception: Exception) {
                send(Action.SendFailed(exception))
            }
        }
}
