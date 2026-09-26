//
//  ConversationCellReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.ConversationCellViewService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.extensions.empty
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer that drives the conversation cell's context menu.
 *
 * Tapping the cell opens the chat page; the block and report actions
 * begin their moderation flows, logging any error; and deletion
 * requires confirmation through an action sheet before removing the
 * conversation and logging an analytics event.
 */
class ConversationCellReducer : Reducer<ConversationCellReducer.State, ConversationCellReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object BlockUsersButtonTapped : Action

        data object CellTapped : Action

        data object DeleteConversationButtonTapped : Action

        data object ReportUsersButtonTapped : Action

        data class DeleteConversationReturned(
            val exception: Exception?,
        ) : Action

        data class DeletionActionSheetDismissed(
            val cancelled: Boolean,
        ) : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
    ) {
        /** The localized text the block users button displays. */
        val blockUsersButtonText: String get() = LocalizedStringKey.BlockUser.localized()

        /** The localized text the delete button displays. */
        val deleteConversationButtonText: String get() = LocalizedStringKey.Delete.localized()

        /** The localized text the report users button displays. */
        val reportUsersButtonText: String get() = LocalizedStringKey.ReportUser.localized()

        /**
         * The conversation the cell describes, resolved from the session
         * store – an empty placeholder if it no longer exists.
         */
        val conversation: Conversation
            get() = SessionStore.getConversation(conversationIDKey) ?: Conversation.empty
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.BlockUsersButtonTapped ->
                ReduceResult(state, blockUsersEffect(state.conversation))

            Action.CellTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Push(
                            UserContentNavigatorState.SeguePath.Chat(state.conversationIDKey),
                        ),
                    ),
                )
                ReduceResult(state)
            }

            Action.DeleteConversationButtonTapped ->
                ReduceResult(state, deleteConversationButtonEffect(state.conversation))

            Action.ReportUsersButtonTapped ->
                ReduceResult(state, reportUsersEffect(state.conversation))

            is Action.DeleteConversationReturned -> {
                val exception = action.exception
                if (exception == null) {
                    AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.DELETE_CONVERSATION)
                } else {
                    Logger.log(exception, with = AlertType.toast)
                }
                ReduceResult(state)
            }

            is Action.DeletionActionSheetDismissed ->
                if (action.cancelled) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state, deleteConversationEffect(state.conversation))
                }
        }

    // MARK: - Auxiliary

    private fun blockUsersEffect(conversation: Conversation): Effect<Action> =
        Effect.run {
            try {
                ConversationCellViewService.blockUsersButtonTapped(conversation)
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

    private fun reportUsersEffect(conversation: Conversation): Effect<Action> =
        Effect.run {
            try {
                ConversationCellViewService.reportUsersButtonTapped(conversation)
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

    private fun deleteConversationButtonEffect(conversation: Conversation): Effect<Action> =
        Effect.run { send ->
            val title = ConversationCellViewData.build(conversation, RuntimeStorage.languageCode).title
            val cancelled = ConversationCellViewService.presentDeletionActionSheet(title)
            send(Action.DeletionActionSheetDismissed(cancelled))
        }

    private fun deleteConversationEffect(conversation: Conversation): Effect<Action> =
        Effect.run { send ->
            try {
                ConversationSessionService.deleteConversation(conversation)
                send(Action.DeleteConversationReturned(null))
            } catch (exception: Exception) {
                send(Action.DeleteConversationReturned(exception))
            }
        }
}
