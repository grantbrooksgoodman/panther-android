//
//  ChatPageHeaderReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.chatpageheaderview

import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.extensions.empty
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer that drives
 * [ChatPageHeaderView][us.neotechnica.panther.modules.content.user.components.chatpageheaderview.ChatPageHeaderView].
 *
 * The header's behavior contract:
 *
 * - Tapping the back button pops the user content navigation stack.
 * - Tapping the chat info button opens the chat info page.
 * - When the session store reports a change affecting the current
 *   conversation, its messages, or its participants, the header
 *   reloads after a short delay; rapid successive changes coalesce
 *   into a single reload.
 */
class ChatPageHeaderReducer : Reducer<ChatPageHeaderReducer.State, ChatPageHeaderReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object BackButtonTapped : Action

        data object ChatInfoButtonTapped : Action

        data object ReloadData : Action

        data class SessionStoreDidChange(
            val change: SessionStoreChange,
        ) : Action

        data class CellViewDataResolved(
            val cellViewData: ConversationCellViewData,
        ) : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val viewID: UUID = UUID.randomUUID(),
        val cellViewData: ConversationCellViewData? = null,
    ) {
        /**
         * The conversation the header describes, resolved from the
         * session store – an empty placeholder if it no longer exists.
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
            Action.BackButtonTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.ChatInfoButtonTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Push(UserContentNavigatorState.SeguePath.ChatInfo(state.conversationIDKey)),
                    ),
                )
                ReduceResult(state)
            }

            Action.ReloadData ->
                ReduceResult(state.copy(viewID = UUID.randomUUID()), resolveCellViewDataEffect(state.conversation))

            is Action.SessionStoreDidChange ->
                if (isRelevantChange(action.change, state.conversation)) {
                    ReduceResult(
                        state,
                        Effect
                            .task<Action>(delay = RELOAD_DELAY) { Action.ReloadData }
                            .cancellable("$TASK_ID_RELOAD_DATA/${state.conversation.id.key}", cancelInFlight = true),
                    )
                } else {
                    ReduceResult(state)
                }

            is Action.CellViewDataResolved ->
                ReduceResult(state.copy(cellViewData = action.cellViewData))
        }

    // MARK: - Auxiliary

    private fun resolveCellViewDataEffect(conversation: Conversation): Effect<Action> =
        Effect.run { send ->
            send(Action.CellViewDataResolved(ConversationCellViewData.build(conversation, RuntimeStorage.languageCode)))
        }

    private fun isRelevantChange(
        change: SessionStoreChange,
        conversation: Conversation,
    ): Boolean =
        when (change) {
            is SessionStoreChange.Conversations ->
                conversation.id.key in change.upsertedIDKeys || conversation.id.key in change.removedIDKeys

            is SessionStoreChange.Messages ->
                conversation.messageIDs
                    .toSet()
                    .intersect(change.upsertedIDs + change.removedIDs)
                    .isNotEmpty()

            is SessionStoreChange.Users ->
                conversation.participants
                    .map { it.userID }
                    .toSet()
                    .intersect(change.upsertedIDs + change.removedIDs)
                    .isNotEmpty()
        }

    // MARK: - Companion

    companion object {
        private val RELOAD_DELAY = 250.milliseconds
        private const val TASK_ID_RELOAD_DATA = "ChatPageHeaderReducer/reloadData"
    }
}
