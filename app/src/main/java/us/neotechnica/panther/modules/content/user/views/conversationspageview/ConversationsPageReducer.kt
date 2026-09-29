//
//  ConversationsPageReducer.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.filteredAndSorted
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

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

        data object DeleteConversationsToolbarButtonTapped : Action

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
                return all.filter { ConversationCellViewData.matches(it, searchQuery) }
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

            Action.DeleteConversationsToolbarButtonTapped ->
                ReduceResult(state, deleteConversationsEffect())

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
                Logger.log(action.exception, with = AlertType.toast)
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

    /**
     * Resolves the current user's conversations and messages, then
     * deletes all of the current user's conversations. Mirrors the iOS
     * developer-mode delete-conversations danger-zone action.
     */
    private fun deleteConversationsEffect(): Effect<Action> =
        Effect.run {
            try {
                UserSessionService.resolveCurrentUser(
                    setOf(
                        UserSessionService.DataType.CONVERSATIONS,
                        UserSessionService.DataType.MESSAGES,
                    ),
                )
                deleteCurrentUserConversations()
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }

    private suspend fun deleteCurrentUserConversations() {
        val didConfirm =
            ConfirmationAlert(
                title = DELETE_CONVERSATIONS_ALERT_TITLE,
                message = DELETE_CONVERSATIONS_ALERT_MESSAGE,
                confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
            ).present(translating = emptyList())
        if (!didConfirm) return

        val conversations = UserSessionService.currentUser?.conversations ?: return
        for (conversation in conversations) {
            ConversationSessionService.deleteConversation(conversation, forced = true)
        }

        HUD.showSuccess()
        Task.delayed(by = 1.seconds) {
            Application.reset(
                preserveCurrentUserID = true,
                onCompletion = Application.ResetCompletionProcedure.NAVIGATE_TO_SPLASH,
            )
        }
    }

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

private const val DELETE_CONVERSATIONS_ALERT_TITLE = "Delete Current User Conversations"
private const val DELETE_CONVERSATIONS_ALERT_MESSAGE =
    "This will delete all conversations for the current user.\n\nThis operation cannot be undone."
