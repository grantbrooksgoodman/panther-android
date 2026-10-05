//
//  ConversationsPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.conversationsPageReappeared
import us.neotechnica.panther.bundle.conversationsSearchQuery
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.services.KeyboardService
import us.neotechnica.panther.modules.content.user.services.ConversationsPageViewService
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.filteredAndSorted
import us.neotechnica.panther.modules.session.entity.extensions.isMock
import us.neotechnica.panther.modules.session.entity.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.queried
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer for the conversations list – the app's home.
 *
 * Lists the current user's conversations sorted by their most recent
 * message and filtered by the search query, and provides the entry
 * points for composing a new conversation and opening Settings.
 *
 * The page's behavior contract:
 *
 * - On first appearance, the page resolves its translated display
 *   strings, remaining in the loading state until resolution
 *   completes.
 * - Pulling to refresh reloads the conversation data, unless a search
 *   is active.
 * - Session store changes refresh the list and reconcile it with
 *   changes originating from the chat page.
 * - Tapping the compose button opens the new chat page; tapping the
 *   settings button opens Settings.
 */
class ConversationsPageReducer : Reducer<ConversationsPageReducer.State, ConversationsPageReducer.Action> {
    // MARK: - Dependencies

    private val viewService = ConversationsPageViewService

    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object ViewDisappeared : Action

        data object ViewFirstAppeared : Action

        data object ComposeToolbarButtonTapped : Action

        data object DeleteConversationsToolbarButtonTapped : Action

        data object HandleChatPageStoreChange : Action

        data object PulledToRefresh : Action

        data object SessionStoreDidChange : Action

        data object SettingsToolbarButtonTapped : Action

        data object TraitCollectionChanged : Action

        data class IsSearchingChanged(
            val isSearching: Boolean,
        ) : Action

        data class SearchQueryChanged(
            val query: String,
        ) : Action

        data class ReloadDataFailed(
            val exception: Exception,
        ) : Action

        data object ReloadDataReturned : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action
    }

    // MARK: - State

    data class State(
        val conversationsChangeToken: UUID = UUID.randomUUID(),
        val isRefreshing: Boolean = false,
        val isSearching: Boolean = false,
        val searchQuery: String = "",
        val strings: List<TranslationOutputMap> = ConversationsPageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
        val didAppear: Boolean = false,
    ) {
        /**
         * The current user's conversations, filtered for visibility,
         * sorted by latest message sent date, and narrowed by the
         * active search query when one is present.
         *
         * Falls back to the current chat session's conversation during
         * the brief window between local conversation creation and
         * server-side conversation-ID sync.
         */
        val conversations: List<Conversation>
            get() {
                val allConversations = (UserSessionService.currentUser?.conversations ?: emptyList()).filteredAndSorted

                if (allConversations.isEmpty()) {
                    val currentConversation = ConversationSessionService.currentConversation
                    return if (currentConversation != null &&
                        !currentConversation.isMock &&
                        currentConversation.isVisibleForCurrentUser
                    ) {
                        listOf(currentConversation).filteredAndSorted
                    } else {
                        emptyList()
                    }
                }

                if (searchQuery.isBlank()) return allConversations
                return allConversations.queried(searchQuery).filteredAndSorted
            }

        /**
         * A Boolean value that indicates whether the extra developer
         * toolbar buttons are shown. Shown only when Developer Mode is
         * enabled and the app is connected to the staging environment,
         * outside staging builds.
         */
        val shouldShowExtraToolbarButtons: Boolean
            get() =
                !Application.isInStagingMode &&
                    Build.isDeveloperModeEnabled &&
                    Networking.config.environment == NetworkEnvironment.STAGING
    }

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewFirstAppeared -> {
                // Clears any query left in the shared stream by a previous page
                // instance; cells subscribe against this instance's fresh state.
                DependencyValues.current.sharedStates.conversationsSearchQuery.value = state.searchQuery
                viewService.viewAppeared()
                viewService.showSecondsToLoadToastIfNeeded()
                ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())
            }

            Action.ViewAppeared ->
                if (!state.didAppear) {
                    ReduceResult(state.copy(didAppear = true))
                } else {
                    // Session store changes are skipped while a pushed page covers
                    // the list, so refresh the memo and the cells on return.
                    val sharedEvents = DependencyValues.current.sharedEvents
                    sharedEvents.conversationsPageReappeared.send(Unit)
                    ReduceResult(state.copy(conversationsChangeToken = UUID.randomUUID()))
                }

            Action.ViewDisappeared -> {
                viewService.viewDisappeared()
                ReduceResult(state)
            }

            Action.ComposeToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePath.NewChat)),
                )
                ReduceResult(state)
            }

            Action.SettingsToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePath.Settings)),
                )
                ReduceResult(state)
            }

            Action.DeleteConversationsToolbarButtonTapped -> {
                viewService.deleteConversationsToolbarButtonTapped()
                ReduceResult(state)
            }

            Action.HandleChatPageStoreChange ->
                ReduceResult(state, Effect.fireAndForget { viewService.handleChatPageStoreChange() })

            is Action.IsSearchingChanged -> {
                // Dismiss the keyboard when search is cleared.
                if (state.isSearching && !action.isSearching) KeyboardService.resignFirstResponders()
                ReduceResult(state.copy(isSearching = action.isSearching))
            }

            Action.PulledToRefresh ->
                if (state.isSearching) {
                    ReduceResult(state)
                } else {
                    ReduceResult(
                        state.copy(isRefreshing = true),
                        Effect.run { send ->
                            try {
                                viewService.reloadData()
                                send(Action.ReloadDataReturned)
                            } catch (exception: Exception) {
                                send(Action.ReloadDataFailed(exception))
                            }
                        },
                    )
                }

            is Action.ReloadDataFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isRefreshing = false))
            }

            Action.ReloadDataReturned ->
                ReduceResult(state.copy(isRefreshing = false, conversationsChangeToken = UUID.randomUUID()))

            is Action.SearchQueryChanged ->
                if (state.searchQuery == action.query) {
                    ReduceResult(state)
                } else {
                    DependencyValues.current.sharedStates.conversationsSearchQuery.value = action.query
                    ReduceResult(state.copy(searchQuery = action.query))
                }

            Action.SessionStoreDidChange -> {
                viewService.showSecondsToLoadToastIfNeeded()
                ReduceResult(state.copy(conversationsChangeToken = UUID.randomUUID()), handleChatPageStoreChangeTask())
            }

            Action.TraitCollectionChanged -> {
                viewService.traitCollectionChanged()
                ReduceResult(state)
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                viewService.viewLoaded()
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            is Action.ResolveReturned -> {
                viewService.viewLoaded()
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))
            }
        }

    // MARK: - Auxiliary

    private fun handleChatPageStoreChangeTask(): Effect<Action> =
        Effect
            .task<Action>(delay = HANDLE_CHAT_PAGE_STORE_CHANGE_DELAY) { Action.HandleChatPageStoreChange }
            .cancellable(TASK_ID_HANDLE_CHAT_PAGE_STORE_CHANGE, cancelInFlight = true)

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(ConversationsPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    // MARK: - Companion

    companion object {
        private val HANDLE_CHAT_PAGE_STORE_CHANGE_DELAY = 250.milliseconds
        private const val TASK_ID_HANDLE_CHAT_PAGE_STORE_CHANGE = "ConversationsPageReducer/handleChatPageStoreChange"
    }
}
