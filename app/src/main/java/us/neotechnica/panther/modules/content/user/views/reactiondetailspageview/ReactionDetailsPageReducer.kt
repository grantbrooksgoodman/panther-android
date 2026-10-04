//
//  ReactionDetailsPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/03/2025.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.reactiondetailspageview

import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID

/**
 * The reducer that drives the reaction details page.
 *
 * This page lists the reactions on a message, grouped by reaction,
 * showing which participants reacted with each. The message is the one
 * whose context menu the reaction details were opened from.
 *
 * The page's behavior contract:
 *
 * - The list shows one row per reaction style, each displaying the
 *   names of the participants who reacted with it.
 * - Tapping done dismisses the page.
 */
class ReactionDetailsPageReducer : Reducer<ReactionDetailsPageReducer.State, ReactionDetailsPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        /** An action that indicates the view appeared. */
        data object ViewAppeared : Action

        /** An action that indicates the view disappeared. */
        data object ViewDisappeared : Action

        /** An action that indicates the user tapped the done header item. Dismisses the page. */
        data object DoneHeaderItemTapped : Action

        /** An action that rebuilds the page's content from the latest values. */
        data object UpdateViewID : Action
    }

    // MARK: - State

    data class State(
        /** The identifier of the message whose reactions are shown. */
        val messageID: String = "",
        /** The identity of the page's content. Regenerated to rebuild it from the latest values. */
        val viewID: UUID = UUID.randomUUID(),
    ) {
        /** The page's navigation title. */
        val navigationTitle: String
            get() {
                val base = LocalizedStringKey.ReactionDetails.localized().replace("…", "")
                if (RuntimeStorage.languageCode != "en") return base
                return base.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> ReduceResult(state)

            Action.ViewDisappeared -> ReduceResult(state)

            Action.DoneHeaderItemTapped -> {
                RootSheets.dismiss()
                ReduceResult(state)
            }

            Action.UpdateViewID ->
                ReduceResult(state.copy(viewID = UUID.randomUUID()))
        }
}
