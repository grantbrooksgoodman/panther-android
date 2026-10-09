//
//  ConversationCellReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewStrings
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.ConversationCellViewService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.session.entity.extensions.empty
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.cancellable
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer that drives
 * [ConversationCellView][us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellView].
 *
 * The cell's behavior contract:
 *
 * - On first appearance, the cell loads its view data.
 * - Tapping the cell opens the chat page for the conversation; when a
 *   search query is active, the chat focuses the most recent matching
 *   message.
 * - Blocking, reporting, and deleting begin their flows; deletion
 *   requires confirmation through an action sheet.
 * - The cell redacts its content while its data has not resolved or
 *   the conversation reloads.
 * - When the session store reports a change affecting the conversation,
 *   its messages, or its participants, the cell reloads after a short
 *   delay; rapid successive changes coalesce into a single reload.
 */
class ConversationCellReducer : Reducer<ConversationCellReducer.State, ConversationCellReducer.Action> {
    // MARK: - Properties

    // Scopes reload-task cancellation to this reducer instance; the
    // cancellation registry is global, and sibling instances for the
    // same conversation must not cancel each other's reloads.
    private val instanceID = UUID.randomUUID()

    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object BlockUsersButtonTapped : Action

        data object CellTapped : Action

        data object DeleteConversationButtonTapped : Action

        data object ReloadData : Action

        data class ReloadingConversationsChanged(
            val conversationIDKeys: Set<String>,
        ) : Action

        data object ReportUsersButtonTapped : Action

        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        data class SessionStoreDidChange(
            val change: SessionStoreChange,
        ) : Action

        data object UserInfoBadgeTapped : Action

        data class CellViewDataResolved(
            val cellViewData: ConversationCellViewData,
        ) : Action

        data class DeleteConversationReturned(
            val exception: Exception?,
        ) : Action

        data class DeletionActionSheetDismissed(
            val cancelled: Boolean,
        ) : Action
    }

    // MARK: - State

    data class State(
        val conversationIDKey: String = "",
        val searchQuery: String = "",
        val cellViewData: ConversationCellViewData = ConversationCellViewData.empty,
        val isConversationReloading: Boolean = false,
        val hasResolvedCellViewData: Boolean = false,
    ) {
        /** The localized text the block users button displays. */
        val blockUsersButtonText: String get() = LocalizedStringKey.BlockUser.localized()

        /** The localized text the delete button displays. */
        val deleteConversationButtonText: String get() = LocalizedStringKey.Delete.localized()

        /** The localized text the report users button displays. */
        val reportUsersButtonText: String get() = LocalizedStringKey.ReportUser.localized()

        /**
         * The conversation the cell describes, resolved from the session
         * store – an empty placeholder if it no longer exists.
         */
        val conversation: Conversation
            get() = SessionStore.getConversation(conversationIDKey) ?: Conversation.empty

        /**
         * The identifier of the most recent message matching the search
         * query, or `null` if none match.
         */
        val focusedMessageID: String?
            get() = ConversationCellViewData.focusedMessageID(conversation, searchQuery)

        /**
         * A Boolean value that indicates whether the cell redacts its
         * content – `true` until its data resolves or while the
         * conversation reloads.
         */
        val isShowingRedactedContent: Boolean
            get() = !hasResolvedCellViewData || isConversationReloading

        /**
         * The text the date label displays, or a redaction placeholder
         * while content is redacted.
         */
        val dateLabelText: String
            get() =
                if (isShowingRedactedContent && cellViewData.dateLabelText.isBlank()) {
                    ConversationCellViewStrings.REDACTED_DATE_LABEL_TEXT
                } else {
                    cellViewData.dateLabelText
                }

        /**
         * The text the subtitle label displays, or a redaction
         * placeholder while content is redacted.
         */
        val subtitleLabelText: String
            get() =
                if (isShowingRedactedContent && cellViewData.subtitle.isBlank()) {
                    ConversationCellViewStrings.REDACTED_SUBTITLE_LABEL_TEXT
                } else {
                    cellViewData.subtitle
                }
    }

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(state, reloadCellViewDataEffect(state, useCachedValue = true))

            Action.BlockUsersButtonTapped ->
                ReduceResult(state, blockUsersEffect(state.conversation))

            Action.CellTapped -> {
                val focusedMessageID = state.searchQuery.takeIf { it.isNotBlank() }?.let { state.focusedMessageID }
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Push(
                            UserContentNavigatorState.SeguePath.Chat(state.conversationIDKey, focusedMessageID),
                        ),
                    ),
                )
                ReduceResult(state)
            }

            Action.DeleteConversationButtonTapped ->
                ReduceResult(state, deleteConversationButtonEffect(state.cellViewData.title))

            Action.ReloadData ->
                ReduceResult(state, reloadCellViewDataEffect(state, useCachedValue = false))

            is Action.ReloadingConversationsChanged ->
                ReduceResult(state.copy(isConversationReloading = action.conversationIDKeys.contains(state.conversationIDKey)))

            Action.ReportUsersButtonTapped ->
                ReduceResult(state, reportUsersEffect(state.conversation))

            is Action.SearchQueryChanged ->
                if (state.searchQuery == action.searchQuery) {
                    ReduceResult(state)
                } else {
                    val updated = state.copy(searchQuery = action.searchQuery)
                    ReduceResult(updated, reloadCellViewDataEffect(updated, useCachedValue = true))
                }

            is Action.SessionStoreDidChange ->
                if (isRelevantChange(action.change, state.conversation)) {
                    ReduceResult(
                        state,
                        Effect
                            .task<Action>(delay = RELOAD_DELAY) { Action.ReloadData }
                            .cancellable(reloadDataTaskID(state), cancelInFlight = true),
                    )
                } else {
                    ReduceResult(state)
                }

            Action.UserInfoBadgeTapped -> {
                state.cellViewData.otherUser?.let { ConversationCellViewService.presentUserInfoAlert(it) }
                ReduceResult(state)
            }

            is Action.CellViewDataResolved ->
                ReduceResult(state.copy(cellViewData = action.cellViewData, hasResolvedCellViewData = true))

            is Action.DeleteConversationReturned -> {
                val exception = action.exception
                if (exception == null) {
                    AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.DELETE_CONVERSATION)
                } else {
                    Logger.log(exception, with = AlertType.toast)
                }
                ReduceResult(state)
            }

            is Action.DeletionActionSheetDismissed ->
                if (action.cancelled) {
                    ReduceResult(state)
                } else {
                    ReduceResult(state, deleteConversationEffect(state.conversation))
                }
        }

    // MARK: - Auxiliary

    private fun reloadDataTaskID(state: State): String = "ConversationCellReducer/$instanceID/${state.conversation.id.key}/reloadData"

    internal fun isRelevantChange(
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

    private fun reloadCellViewDataEffect(
        state: State,
        useCachedValue: Boolean,
    ): Effect<Action> =
        Effect.run { send ->
            val cellViewData =
                ConversationCellViewData.build(
                    state.conversation,
                    state.searchQuery,
                    useCachedValue = useCachedValue,
                )
            send(Action.CellViewDataResolved(cellViewData))
        }

    private fun blockUsersEffect(conversation: Conversation): Effect<Action> =
        Effect.run {
            try {
                ConversationCellViewService.blockUsersButtonTapped(conversation)
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

    private fun reportUsersEffect(conversation: Conversation): Effect<Action> =
        Effect.run {
            try {
                ConversationCellViewService.reportUsersButtonTapped(conversation)
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

    private fun deleteConversationButtonEffect(title: String): Effect<Action> =
        Effect.run { send ->
            val cancelled = ConversationCellViewService.presentDeletionActionSheet(title)
            send(Action.DeletionActionSheetDismissed(cancelled))
        }

    private fun deleteConversationEffect(conversation: Conversation): Effect<Action> =
        Effect.run { send ->
            try {
                ConversationSessionService.deleteConversation(conversation)
                send(Action.DeleteConversationReturned(null))
            } catch (exception: Exception) {
                send(Action.DeleteConversationReturned(exception))
            }
        }

    // MARK: - Companion

    companion object {
        private val RELOAD_DELAY = 250.milliseconds
    }
}
