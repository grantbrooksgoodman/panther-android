//
//  ReactionDetailsPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.reactiondetailspageview

import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.designsystem.modules.foundation.components.ListRowView
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.modules.content.user.extensions.alphabeticallySorted
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.uniquedByID
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.extensions.capitalized
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID

/**
 * The reducer that drives the reaction details page.
 *
 * This page lists the reactions on a message, grouped by
 * reaction, showing which participants reacted with each. The
 * message is the one whose context menu the reaction details were
 * opened from.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page dismisses the keyboard.
 * - The list shows one row per reaction style, each displaying
 *   the names of the participants who reacted with it.
 * - Tapping done dismisses the page.
 */
class ReactionDetailsPageReducer : Reducer<ReactionDetailsPageReducer.State, ReactionDetailsPageReducer.Action> {
    // MARK: - Actions

    /** The actions the reaction details page can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared. Dismisses
         * the keyboard.
         */
        data object ViewAppeared : Action

        /** An action that indicates the view disappeared. */
        data object ViewDisappeared : Action

        /**
         * An action that indicates the user tapped the done
         * header item. Dismisses the page.
         */
        data object DoneHeaderItemTapped : Action

        /**
         * An action that rebuilds the page's content from the
         * latest values.
         */
        data object UpdateViewID : Action
    }

    // MARK: - State

    /**
     * The state of the reaction details page.
     *
     * @property messageID The identifier of the message whose
     *   reactions are shown.
     * @property viewID The identity of the page's content.
     *   Regenerated to rebuild it from the latest values.
     */
    data class State(
        val messageID: String = "",
        val viewID: UUID = UUID.randomUUID(),
    ) {
        // MARK: - Dependencies

        private val clientSession: ClientSession by Dependency { it.clientSession }

        /**
         * The list rows describing the message's reactions, one per
         * reaction style, each showing the names of the participants
         * who reacted with it.
         */
        val listItems: List<ListRowView.Configuration>
            get() {
                val currentUser = clientSession.entity.user.currentUser ?: return emptyList()
                val reactions =
                    clientSession.entity.conversation.currentConversation
                        ?.messages
                        ?.firstOrNull { it.id == messageID }
                        ?.reactions ?: return emptyList()

                val storedUsers = clientSession.store.users.values
                val userMap = (storedUsers + currentUser).uniquedByID.associate { it.id to it.reactionDisplayName }

                return reactions
                    .groupBy { it.style }
                    .mapNotNull { (style, styleReactions) ->
                        val innerText =
                            styleReactions
                                .mapNotNull { userMap[it.userID] }
                                .alphabeticallySorted
                                .joinToString("\n")
                        if (innerText.isEmpty()) return@mapNotNull null
                        style to
                            ListRowView.Configuration(
                                ListRowView.Interaction.Button {},
                                innerText = innerText,
                                isEnabled = false,
                                imageView = { ListRowIcon(style) },
                            )
                    }.sortedBy { it.first.orderValue }
                    .map { it.second }
            }

        /** The page's navigation title. */
        val navigationTitle: String
            get() {
                val localizedString = LocalizedStringKey.ReactionDetails.localized().replace("…", "")
                if (RuntimeStorage.languageCode != "en") return localizedString
                return localizedString.capitalized
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                KeyboardService.resignFirstResponders()
                ReduceResult(state)
            }

            Action.ViewDisappeared -> ReduceResult(state)

            Action.DoneHeaderItemTapped -> {
                RootSheets.dismiss()
                ReduceResult(state)
            }

            Action.UpdateViewID -> ReduceResult(state.copy(viewID = UUID.randomUUID()))
        }
}

private val User.reactionDisplayName: String
    get() = if (id == User.currentUserID) LocalizedStringKey.You.localized() else displayName
