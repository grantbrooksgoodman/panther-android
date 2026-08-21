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
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.session.services.MessageSessionService
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer for starting a new conversation.
 *
 * Lists contact matches (multi-select for group chats), composes a first
 * message, and creates the conversation on send, navigating into it.
 *
 * **Note:** find-a-user-by-number and invites are deferred; the selector
 * shows registered users found in the device's contacts.
 */
class NewChatPageReducer : Reducer<NewChatPageReducer.State, NewChatPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewFirstAppeared : Action

        data class ContactsLoaded(
            val contacts: List<ContactMatch>,
        ) : Action

        data class SearchChanged(
            val query: String,
        ) : Action

        data class ToggleSelected(
            val userID: String,
        ) : Action

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
        val searchQuery: String = "",
        val selectedUserIDs: Set<String> = emptySet(),
        val inputText: String = "",
        val isSending: Boolean = false,
    ) {
        /** The contacts matching [searchQuery]. */
        val filteredContacts: List<ContactMatch>
            get() {
                if (searchQuery.isBlank()) return contacts
                val query = searchQuery.trim().lowercase()
                return contacts.filter {
                    it.fullName.lowercase().contains(query) || it.compiledNumberString.contains(query)
                }
            }

        /** Whether the first message can be sent. */
        val canSend: Boolean
            get() = selectedUserIDs.isNotEmpty() && inputText.isNotBlank() && !isSending
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewFirstAppeared ->
                ReduceResult(state.copy(contacts = ContactService.matches()))

            is Action.ContactsLoaded ->
                ReduceResult(state.copy(contacts = action.contacts))

            is Action.SearchChanged ->
                ReduceResult(state.copy(searchQuery = action.query))

            is Action.ToggleSelected -> {
                val updated =
                    if (action.userID in state.selectedUserIDs) {
                        state.selectedUserIDs - action.userID
                    } else {
                        state.selectedUserIDs + action.userID
                    }
                ReduceResult(state.copy(selectedUserIDs = updated))
            }

            is Action.InputChanged ->
                ReduceResult(state.copy(inputText = action.text))

            Action.SendTapped ->
                if (!state.canSend) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state.copy(isSending = true), sendEffect(state.inputText, state.selectedUserIDs))
                }

            is Action.SendReturned -> {
                val navigation = DependencyValues.current.navigation
                navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Stack(listOf(UserContentNavigatorState.SeguePath.Chat(action.conversationIDKey))),
                    ),
                )
                ReduceResult(state.copy(isSending = false))
            }

            is Action.SendFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(isSending = false))
            }

            Action.BackTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    private fun sendEffect(
        text: String,
        selectedUserIDs: Set<String>,
    ): Effect<Action> =
        Effect.run { send ->
            try {
                val users = UserService.getUsers(selectedUserIDs.toList())
                if (users.isEmpty()) {
                    send(Action.SendFailed(Exception("No recipients resolved.", metadata = exceptionMetadata())))
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

    private fun exceptionMetadata() =
        us.neotechnica.panther.subsystem.modules.foundation.models
            .ExceptionMetadata(this)
}
