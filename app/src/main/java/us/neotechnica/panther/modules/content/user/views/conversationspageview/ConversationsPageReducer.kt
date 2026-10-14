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
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.user.dependencies.conversationsPageViewService
import us.neotechnica.panther.modules.content.user.extensions.filteredAndSorted
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.queried
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates
import java.util.UUID

/**
 * The reducer that drives the conversations page.
 *
 * This page is the app's home, listing the user's conversations. It
 * presents the conversations sorted by their most recent message and
 * filtered by the search query, and provides the entry points for
 * composing a new conversation and opening Settings.
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

    private val viewService get() = DependencyValues.current.conversationsPageViewService

    // MARK: - Action

    /** The actions the conversations page can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared. On each
         * appearance after the first, notifies observers so the
         * conversation cells refresh.
         */
        data object ViewAppeared : Action

        /** An action that indicates the view disappeared. */
        data object ViewDisappeared : Action

        /**
         * An action that indicates the view appeared for the first
         * time. Begins display string resolution and resets the shared
         * search query.
         */
        data object ViewFirstAppeared : Action

        /** An action that advances the compose button's pulsing animation. */
        data object AnimatedComposeToolbarButtonAppeared : Action

        /**
         * An action that indicates the user tapped the compose button.
         * Opens the new chat page.
         */
        data object ComposeToolbarButtonTapped : Action

        /**
         * An action that indicates the user tapped the settings button.
         * Opens Settings.
         */
        data object SettingsToolbarButtonTapped : Action

        /**
         * An action that indicates the user tapped the
         * delete-conversations developer button. Deletes the user's
         * conversations.
         */
        data object DeleteConversationsToolbarButtonTapped : Action

        /**
         * An action that reconciles the list with a pending change
         * originating from the chat page.
         */
        data object HandleChatPageStoreChange : Action

        /**
         * An action that indicates the user pulled to refresh. Reloads
         * the conversation data, unless a search is active.
         */
        data object PulledToRefresh : Action

        /**
         * An action that indicates the session store changed.
         * Refreshes the list and schedules handling of the change.
         */
        data object SessionStoreDidChange : Action

        /**
         * An action that indicates the trait collection changed.
         * Rebuilds the compose button.
         */
        data object TraitCollectionChanged : Action

        /**
         * An action that indicates whether the user is searching
         * changed, carrying the new value.
         */
        data class IsSearchingChanged(
            val isSearching: Boolean,
        ) : Action

        /**
         * An action that indicates the search query changed, carrying
         * the new value.
         */
        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        /**
         * An action that sets the compose button's animation amount,
         * carrying the new value.
         */
        data class ComposeToolbarButtonAnimationAmountSet(
            val animationAmount: Float,
        ) : Action

        /**
         * An action that indicates a data reload failed, carrying the
         * resulting [Exception].
         */
        data class ReloadDataFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates a data reload finished. */
        data object ReloadDataReturned : Action

        /**
         * An action that indicates display string resolution failed,
         * carrying the resulting [Exception].
         */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /**
         * An action that indicates display string resolution
         * succeeded, carrying the resolved strings.
         */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action
    }

    // MARK: - State

    /**
     * The state of the conversations page.
     *
     * @property animationAmount The current scale of the compose
     *   button's pulsing animation.
     * @property composeToolbarButtonViewID The identity of the compose
     *   button. Regenerated to rebuild it when the trait collection
     *   changes.
     * @property conversationsChangeToken A token that changes to
     *   signal the conversation list to refresh.
     * @property isRefreshing A Boolean value that indicates whether a
     *   pull-to-refresh reload is in progress.
     * @property isSearching A Boolean value that indicates whether the
     *   user is currently searching.
     * @property searchQuery The search query the user has entered.
     * @property strings The page's translated display strings.
     *   Contains the default, untranslated strings until resolution
     *   completes.
     * @property viewState The page's loading state. Remains loading
     *   until display string resolution completes.
     */
    data class State(
        val animationAmount: Float = 1f,
        val composeToolbarButtonViewID: UUID = UUID.randomUUID(),
        val conversationsChangeToken: UUID = UUID.randomUUID(),
        val isRefreshing: Boolean = false,
        val isSearching: Boolean = false,
        val searchQuery: String = "",
        val strings: List<TranslationOutputMap> = ConversationsPageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
        internal val didAppear: Boolean = false,
    ) {
        /**
         * The current user's conversations, filtered for visibility,
         * sorted by latest message sent date, and narrowed by the
         * active search query when one is present.
         *
         * Falls back to the current chat session's conversation during
         * the brief window between local conversation creation and
         * server-side conversation-ID sync.
         *
         * Memoized against [conversationsChangeToken] and the search
         * query, so its expensive resolution runs once per change
         * rather than on every view body evaluation.
         */
        val conversations: List<Conversation>
            get() {
                ConversationsListMemo.value(conversationsChangeToken, searchQuery)?.let { return it }

                val value = resolvedConversations
                ConversationsListMemo.store(value, conversationsChangeToken, searchQuery)
                return value
            }

        private val resolvedConversations: List<Conversation>
            get() {
                val allConversations =
                    (
                        DependencyValues.current.clientSession.entity.user.currentUser
                            ?.conversations ?: emptyList()
                    ).filteredAndSorted

                if (allConversations.isEmpty()) {
                    val currentConversation = DependencyValues.current.clientSession.entity.conversation.currentConversation
                    return if (currentConversation != null &&
                        !currentConversation.isMock &&
                        currentConversation.isVisibleForCurrentUser
                    ) {
                        listOf(currentConversation).filteredAndSorted
                    } else {
                        emptyList()
                    }
                }

                if (searchQuery.isEmpty()) return allConversations
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

            Action.ViewAppeared -> reduceViewAppeared(state)

            Action.AnimatedComposeToolbarButtonAppeared ->
                ReduceResult(state, animationAmountEffect(state.animationAmount))

            is Action.ComposeToolbarButtonAnimationAmountSet ->
                ReduceResult(state.copy(animationAmount = action.animationAmount))

            Action.ComposeToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePaths.NewChat)),
                )
                ReduceResult(state)
            }

            Action.DeleteConversationsToolbarButtonTapped -> {
                viewService.deleteConversationsToolbarButtonTapped()
                ReduceResult(state)
            }

            Action.HandleChatPageStoreChange ->
                ReduceResult(state, Effect.fireAndForget { viewService.handleChatPageStoreChange() })

            is Action.IsSearchingChanged ->
                ReduceResult(state.copy(isSearching = action.isSearching))

            Action.PulledToRefresh ->
                if (state.isSearching) ReduceResult(state) else ReduceResult(state.copy(isRefreshing = true), reloadDataEffect())

            is Action.ReloadDataFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(isRefreshing = false))
            }

            Action.ReloadDataReturned ->
                ReduceResult(state.copy(isRefreshing = false, conversationsChangeToken = UUID.randomUUID()))

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                viewService.viewLoaded()
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            is Action.ResolveReturned -> {
                viewService.viewLoaded()
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))
            }

            is Action.SearchQueryChanged ->
                if (state.searchQuery == action.searchQuery) {
                    ReduceResult(state)
                } else {
                    DependencyValues.current.sharedStates.conversationsSearchQuery.value = action.searchQuery
                    ReduceResult(state.copy(searchQuery = action.searchQuery))
                }

            Action.SessionStoreDidChange ->
                if (DependencyValues.current.navigation.state.value.modal != RootNavigatorState.ModalPaths.UserContent) {
                    ReduceResult(state, handleChatPageStoreChangeTask())
                } else {
                    viewService.showSecondsToLoadToastIfNeeded()
                    ReduceResult(state.copy(conversationsChangeToken = UUID.randomUUID()), handleChatPageStoreChangeTask())
                }

            Action.SettingsToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePaths.Settings)),
                )
                ReduceResult(state)
            }

            Action.TraitCollectionChanged -> {
                viewService.traitCollectionChanged()
                ReduceResult(state.copy(composeToolbarButtonViewID = UUID.randomUUID()))
            }

            Action.ViewDisappeared -> {
                viewService.viewDisappeared()
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    private fun reduceViewAppeared(state: State): ReduceResult<State, Action> {
        if (!state.didAppear) return ReduceResult(state.copy(didAppear = true))

        // Session store changes are skipped while a pushed page covers
        // the list, so refresh the memo on return.
        //
        // Backstop for cell reloads lost while the list was covered by
        // a pushed page; cells re-derive their view data upon
        // reappearance.
        val sharedEvents = DependencyValues.current.sharedEvents
        sharedEvents.conversationsPageReappeared.send(Unit)
        return ReduceResult(state.copy(conversationsChangeToken = UUID.randomUUID()))
    }
}
