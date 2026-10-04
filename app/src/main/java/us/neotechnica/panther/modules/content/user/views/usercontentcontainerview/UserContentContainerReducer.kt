//
//  UserContentContainerReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.usercontentcontainerview

import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID

/**
 * The reducer that drives the user content container.
 *
 * This container hosts the signed-in user's content. It opens the
 * chat info page and rebuilds its content when the current
 * conversation's metadata changes.
 */
class UserContentContainerReducer : Reducer<UserContentContainerReducer.State, UserContentContainerReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        /** An action that indicates the user tapped the chat info button. Opens the chat info page. */
        data object ChatInfoToolbarButtonTapped : Action

        /**
         * An action that indicates the current conversation's metadata
         * changed. Rebuilds the container from the latest values.
         */
        data object ConversationMetadataChanged : Action
    }

    // MARK: - State

    data class State(
        /** The identity of the container's content. Regenerated to rebuild it from the latest values. */
        val objectID: UUID = UUID.randomUUID(),
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ChatInfoToolbarButtonTapped -> {
                val conversationIDKey = ConversationSessionService.currentConversation?.id?.key
                if (conversationIDKey != null) {
                    DependencyValues.current.navigation.navigate(
                        Route.UserContent(
                            UserContentRoute.Push(UserContentNavigatorState.SeguePath.ChatInfo(conversationIDKey)),
                        ),
                    )
                }
                ReduceResult(state)
            }

            Action.ConversationMetadataChanged ->
                ReduceResult(state.copy(objectID = UUID.randomUUID()))
        }
}
