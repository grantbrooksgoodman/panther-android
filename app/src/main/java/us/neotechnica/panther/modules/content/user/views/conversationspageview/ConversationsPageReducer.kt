//
//  ConversationsPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.session.extensions.conversations
import us.neotechnica.panther.networking.modules.session.extensions.filteredAndSorted
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.UUID

/**
 * The reducer for the conversations list.
 *
 * Renders the current user's conversations from the session store,
 * observing store changes to stay live, and supports search and
 * pull-to-refresh.
 */
class ConversationsPageReducer : Reducer<ConversationsPageReducer.State, ConversationsPageReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewFirstAppeared : Action

        data object SessionStoreDidChange : Action

        data object PulledToRefresh : Action

        data object ReloadReturned : Action

        data class ReloadFailed(
            val exception: Exception,
        ) : Action

        data class SearchQueryChanged(
            val query: String,
        ) : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action
    }

    // MARK: - State

    data class State(
        val changeToken: UUID = UUID.randomUUID(),
        val isRefreshing: Boolean = false,
        val searchQuery: String = "",
        val strings: List<TranslationOutputMap> = ConversationsPageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
    ) {
        /** The conversations to display, filtered by [searchQuery]. */
        val conversations: List<Conversation>
            get() {
                val all = UserSessionService.currentUser?.conversations?.filteredAndSorted ?: emptyList()
                if (searchQuery.isBlank()) return all
                val query = searchQuery.trim().lowercase()
                return all.filter {
                    it.metadata.name
                        .lowercase()
                        .contains(query)
                }
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewFirstAppeared -> {
                UserSessionService.startObservingCurrentUserChanges()
                ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())
            }

            Action.SessionStoreDidChange ->
                ReduceResult(state.copy(changeToken = UUID.randomUUID()))

            Action.PulledToRefresh ->
                if (state.isRefreshing) {
                    ReduceResult(state)
                } else {
                    ReduceResult(
                        state.copy(isRefreshing = true),
                        Effect.run { send ->
                            try {
                                UserSessionService.resolveCurrentUser(UserSessionService.DataType.entries.toSet())
                                send(Action.ReloadReturned)
                            } catch (exception: Exception) {
                                send(Action.ReloadFailed(exception))
                            }
                        },
                    )
                }

            Action.ReloadReturned ->
                ReduceResult(state.copy(isRefreshing = false, changeToken = UUID.randomUUID()))

            is Action.ReloadFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(isRefreshing = false))
            }

            is Action.SearchQueryChanged ->
                ReduceResult(state.copy(searchQuery = action.query))

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }
        }

    // MARK: - Auxiliary

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(
                    Action.ResolveReturned(
                        Networking.config.hostedTranslationDelegate.resolve(ConversationsPageViewStrings),
                    ),
                )
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }
}

/** The translated label strings for the conversations page. */
object ConversationsPageViewStrings : TranslatedLabelStrings {
    val navigationBarTitle = TranslatedLabelStringCollection("conversationsPageView.navigationBarTitle")
    val noConversationsLabelText = TranslatedLabelStringCollection("conversationsPageView.noConversationsLabelText")
    val searchBarPlaceholder = TranslatedLabelStringCollection("conversationsPageView.searchBarPlaceholder")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(navigationBarTitle, TranslationInput("Messages")),
            TranslationInputMap(noConversationsLabelText, TranslationInput("No conversations yet.")),
            TranslationInputMap(searchBarPlaceholder, TranslationInput("Search")),
        )
}
