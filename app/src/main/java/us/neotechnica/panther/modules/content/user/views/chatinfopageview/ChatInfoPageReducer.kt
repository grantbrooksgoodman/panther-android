//
//  ChatInfoPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.services.ActivitySessionService
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.ModerationSessionService
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer for a conversation's info page.
 *
 * Shows the participants and offers group management (rename, add,
 * remove, leave) and moderation (block, report, delete).
 */
class ChatInfoPageReducer : Reducer<ChatInfoPageReducer.State, ChatInfoPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data class ViewFirstAppeared(
            val conversationIDKey: String,
        ) : Action

        data object Reload : Action

        data class RenameChanged(
            val text: String,
        ) : Action

        data object RenameSubmitted : Action

        data class AddParticipant(
            val userID: String,
        ) : Action

        data class RemoveParticipant(
            val userID: String,
        ) : Action

        data object BlockTapped : Action

        data object ReportTapped : Action

        data object LeaveTapped : Action

        data object DeleteTapped : Action

        data class Failed(
            val exception: Exception,
        ) : Action

        data object BackTapped : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val conversation: Conversation? = null,
        val renameText: String = "",
        val isBusy: Boolean = false,
    ) {
        val isGroup: Boolean
            get() = (conversation?.participants?.size ?: 0) > 2

        val otherParticipantIDs: List<String>
            get() = conversation?.participants?.map { it.userID }?.filter { it != User.currentUserID } ?: emptyList()

        val addableContacts: List<ContactMatch>
            get() {
                val existing = conversation?.participants?.map { it.userID }?.toSet() ?: emptySet()
                return ContactService.matches().filter { it.userID !in existing }
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            is Action.ViewFirstAppeared -> {
                val conversation = SessionStore.getConversation(action.conversationIDKey)
                ReduceResult(
                    state.copy(
                        conversationIDKey = action.conversationIDKey,
                        conversation = conversation,
                        renameText =
                            conversation
                                ?.metadata
                                ?.name
                                ?.takeUnless { it == "!" }
                                .orEmpty(),
                    ),
                )
            }

            Action.Reload ->
                ReduceResult(state.copy(conversation = SessionStore.getConversation(state.conversationIDKey)))

            is Action.RenameChanged ->
                ReduceResult(state.copy(renameText = action.text))

            Action.RenameSubmitted ->
                mutation(state) { conversation ->
                    ActivitySessionService.renameConversation(conversation, state.renameText)
                }

            is Action.AddParticipant ->
                mutation(state) { conversation ->
                    ActivitySessionService.addToConversation(action.userID, conversation)
                }

            is Action.RemoveParticipant ->
                mutation(state) { conversation ->
                    ActivitySessionService.removeFromConversation(action.userID, conversation)
                }

            Action.BlockTapped ->
                moderation(state) { ModerationSessionService.blockUsers(state.otherParticipantIDs) }

            Action.ReportTapped ->
                moderation(state) { ModerationSessionService.reportUsers(state.otherParticipantIDs) }

            Action.LeaveTapped ->
                leaveOrDelete(state) { conversation ->
                    val currentUserID = User.currentUserID ?: return@leaveOrDelete
                    ActivitySessionService.removeFromConversation(currentUserID, conversation)
                }

            Action.DeleteTapped ->
                leaveOrDelete(state) { conversation -> ConversationSessionService.deleteConversation(conversation) }

            is Action.Failed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(isBusy = false))
            }

            Action.BackTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    private fun mutation(
        state: State,
        operation: suspend (Conversation) -> Conversation,
    ): ReduceResult<State, Action> {
        val conversation = state.conversation ?: return ReduceResult(state)
        return ReduceResult(
            state.copy(isBusy = true),
            Effect.run { send ->
                try {
                    operation(conversation)
                    send(Action.Reload)
                } catch (exception: Exception) {
                    send(Action.Failed(exception))
                }
            },
        )
    }

    private fun moderation(
        state: State,
        operation: suspend () -> Unit,
    ): ReduceResult<State, Action> =
        ReduceResult(
            state.copy(isBusy = true),
            Effect.run { send ->
                try {
                    operation()
                    send(Action.Reload)
                } catch (exception: Exception) {
                    send(Action.Failed(exception))
                }
            },
        )

    private fun leaveOrDelete(
        state: State,
        operation: suspend (Conversation) -> Unit,
    ): ReduceResult<State, Action> {
        val conversation = state.conversation ?: return ReduceResult(state)
        return ReduceResult(
            state.copy(isBusy = true),
            Effect.run { send ->
                try {
                    operation(conversation)
                    DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
                } catch (exception: Exception) {
                    send(Action.Failed(exception))
                }
            },
        )
    }
}
