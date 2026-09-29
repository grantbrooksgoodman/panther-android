//
//  ReactionDetailsPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 30/03/2025.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.reactiondetailspageview

import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID

/**
 * The reducer that drives the reaction details page.
 *
 * This page lists the reactions on a message, grouped by reaction,
 * showing which participants reacted with each. The message is the one
 * whose context menu the reaction details were opened from.
 */
class ReactionDetailsPageReducer : Reducer<ReactionDetailsPageReducer.State, ReactionDetailsPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        /** An action that indicates the user tapped the done header item. Dismisses the page. */
        data object DoneHeaderItemTapped : Action

        /** An action that rebuilds the page's content from the latest values. */
        data object StoreChanged : Action
    }

    // MARK: - State

    data class State(
        /** The identifier of the message whose reactions are shown. */
        val messageID: String = "",
        /** The identity of the page's content. Regenerated to rebuild it from the latest values. */
        val changeToken: UUID = UUID.randomUUID(),
    )

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.DoneHeaderItemTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.StoreChanged ->
                ReduceResult(state.copy(changeToken = UUID.randomUUID()))
        }
}
