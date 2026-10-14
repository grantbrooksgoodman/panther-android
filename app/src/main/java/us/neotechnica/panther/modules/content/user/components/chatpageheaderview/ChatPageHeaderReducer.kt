//
//  ChatPageHeaderReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.chatpageheaderview

import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.modules.content.user.extensions.empty
import us.neotechnica.panther.modules.content.user.extensions.isRelevant
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
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

    /** The actions the chat page header can process. */
    sealed interface Action {
        /** An action that indicates the user tapped the back button. Pops the user content navigation stack. */
        data object BackButtonTapped : Action

        /** An action that indicates the user tapped the chat info button. Opens the chat info page. */
        data object ChatInfoButtonTapped : Action

        /** An action that rebuilds the header from the latest values. */
        data object ReloadData : Action

        /**
         * An action that indicates the session store changed, carrying
         * the change. Schedules a reload when the change affects the
         * current conversation, its messages, or its participants.
         */
        data class SessionStoreDidChange(
            val change: SessionStoreChange,
        ) : Action
    }

    // MARK: - State

    /**
     * The state of the chat page header.
     *
     * @property conversationIDKey The ID key of the conversation the
     *   header describes.
     * @property viewID The identity of the header's content.
     *   Regenerated to rebuild it from the latest values.
     */
    data class State(
        val conversationIDKey: String = "",
        val viewID: UUID = UUID.randomUUID(),
    ) {
        /** The conversation cell view data used to render the header's avatar and title. */
        val cellViewData: ConversationCellViewData
            get() = ConversationCellViewData(conversation) ?: ConversationCellViewData.empty

        /**
         * The conversation the header describes, resolved from the
         * session store – an empty placeholder if it no longer exists.
         */
        val conversation: Conversation
            get() =
                DependencyValues.current.clientSession.store
                    .getConversation(conversationIDKey) ?: Conversation.empty
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
                        UserContentRoute.Push(UserContentNavigatorState.SeguePaths.ChatInfo(state.conversationIDKey)),
                    ),
                )
                ReduceResult(state)
            }

            Action.ReloadData ->
                ReduceResult(state.copy(viewID = UUID.randomUUID()))

            is Action.SessionStoreDidChange ->
                if (action.change.isRelevant(state.conversation)) {
                    ReduceResult(
                        state,
                        Effect
                            .task<Action>(delay = RELOAD_DELAY) { Action.ReloadData }
                            .cancellable("$TASK_ID_RELOAD_DATA/${state.conversation.id.key}", cancelInFlight = true),
                    )
                } else {
                    ReduceResult(state)
                }
        }

    // MARK: - Companion

    companion object {
        private val RELOAD_DELAY = 250.milliseconds
        private const val TASK_ID_RELOAD_DATA = "ChatPageHeaderReducer/reloadData"
    }
}
