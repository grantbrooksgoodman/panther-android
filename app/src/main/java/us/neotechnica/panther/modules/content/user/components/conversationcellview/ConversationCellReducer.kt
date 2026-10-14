//
//  ConversationCellReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.foundation.extensions.darker
import us.neotechnica.panther.designsystem.modules.foundation.extensions.lighter
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewFloats
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewStrings
import us.neotechnica.panther.modules.content.user.extensions.empty
import us.neotechnica.panther.modules.content.user.extensions.isEmpty
import us.neotechnica.panther.modules.content.user.extensions.isRelevant
import us.neotechnica.panther.modules.content.user.extensions.textContains
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.services.ConversationCellViewService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
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
import kotlin.time.Duration.Companion.seconds

/**
 * The reducer that drives
 * [ConversationCellView][us.neotechnica.panther.modules.content.user.components.conversationcellview.ConversationCellView].
 *
 * The cell's behavior contract:
 *
 * - On first appearance, the cell loads its view data, retrying after
 *   a short delay if it is not yet available.
 * - Tapping the cell opens the chat page for the conversation; when a
 *   search query is active, the chat focuses the most recent matching
 *   message.
 * - Deleting the conversation requires confirmation through an action
 *   sheet; success logs an analytics event, and failure surfaces as a
 *   toast.
 * - The cell redacts its content while messages hydrate or the
 *   conversation reloads.
 * - When the session store reports a change affecting the
 *   conversation, its messages, or its participants, the cell reloads
 *   after a short delay; rapid successive changes coalesce into a
 *   single reload.
 */
class ConversationCellReducer : Reducer<ConversationCellReducer.State, ConversationCellReducer.Action> {
    // MARK: - Properties

    // Scopes reload-task cancellation to this reducer instance; the
    // cancellation registry is global, and sibling instances for the
    // same conversation must not cancel each other's reloads.
    private val instanceID = UUID.randomUUID()

    // MARK: - Action

    /** The actions the conversation cell can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared. Loads the cell's
         * view data, retrying after a short delay if it is not yet
         * available.
         */
        data object ViewAppeared : Action

        /**
         * An action that indicates the user tapped the block users
         * button. Begins the block users flow for the conversation,
         * logging any error.
         */
        data object BlockUsersButtonTapped : Action

        /**
         * An action that indicates the user tapped the cell. Opens the
         * chat page for the conversation, focusing the most recent
         * message matching the search query when one is active.
         */
        data object CellTapped : Action

        /**
         * An action that indicates the user tapped the delete button.
         * Presents the deletion confirmation action sheet.
         */
        data object DeleteConversationButtonTapped : Action

        /** An action that reloads the cell's view data, disregarding any cached value. */
        data object ReloadData : Action

        /**
         * An action that indicates the set of reloading conversations
         * changed, carrying the affected conversation ID keys. Updates
         * whether the cell shows redacted content.
         */
        data class ReloadingConversationsChanged(
            val conversationIDKeys: Set<String>,
        ) : Action

        /**
         * An action that indicates the user tapped the report users
         * button. Begins the report users flow for the conversation,
         * logging any error.
         */
        data object ReportUsersButtonTapped : Action

        /**
         * An action that indicates the search query changed, carrying
         * the new value. Reloads the cell's view data when the query
         * differs.
         */
        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        /**
         * An action that indicates the session store changed, carrying
         * the change. Reloads the cell after a short delay when the
         * change affects the conversation, its messages, or its
         * participants.
         */
        data class SessionStoreDidChange(
            val change: SessionStoreChange,
        ) : Action

        /**
         * An action that indicates the user tapped the user info badge.
         * Presents an alert with information about the other user.
         */
        data object UserInfoBadgeTapped : Action

        /**
         * An action that indicates conversation deletion finished,
         * carrying an [Exception] if it failed. Logs an analytics event
         * on success; otherwise, surfaces the error as a toast.
         */
        data class DeleteConversationReturned(
            val exception: Exception?,
        ) : Action

        /**
         * An action that indicates the deletion action sheet was
         * dismissed, carrying whether the user canceled. Deletes the
         * conversation unless canceled.
         */
        data class DeletionActionSheetDismissed(
            val cancelled: Boolean,
        ) : Action
    }

    // MARK: - State

    /**
     * The state of the conversation cell.
     *
     * @property conversationIDKey The ID key of the conversation the
     *   cell describes.
     * @property searchQuery The search query active when the cell was
     *   created.
     * @property cellViewData The view data used to render the cell's
     *   content.
     * @property isConversationReloading A Boolean value that indicates
     *   whether the conversation is being reloaded.
     */
    data class State(
        val conversationIDKey: String,
        val searchQuery: String,
        val cellViewData: ConversationCellViewData = ConversationCellViewData.empty,
        val isConversationReloading: Boolean = false,
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
         * The text the date label displays, or a redaction placeholder
         * while content is redacted.
         */
        val dateLabelText: String
            get() {
                if (!isShowingRedactedContent || cellViewData.dateLabelText.isNotBlank()) {
                    return cellViewData.dateLabelText
                }

                return ConversationCellViewStrings.REDACTED_DATE_LABEL_TEXT
            }

        /**
         * The ID of the most recent message matching the search query,
         * or `null` if none match.
         */
        val focusedMessageID: String?
            get() = conversation.messages?.lastOrNull { it.textContains(searchQuery) }?.id

        /**
         * A Boolean value that indicates whether the cell redacts its
         * content – `true` while messages hydrate or the conversation
         * reloads.
         */
        val isShowingRedactedContent: Boolean
            get() = cellViewData.isAwaitingMessageHydration || isConversationReloading

        /**
         * The text the subtitle label displays, or a redaction
         * placeholder while content is redacted.
         */
        val subtitleLabelText: String
            get() {
                if (!isShowingRedactedContent || cellViewData.subtitleLabelText.isNotBlank()) {
                    return cellViewData.subtitleLabelText
                }

                return ConversationCellViewStrings.REDACTED_SUBTITLE_LABEL_TEXT
            }

        /**
         * Returns the foreground color of the cell's chevron symbol,
         * adjusted for the active theme.
         *
         * @param titleText The theme's title text color.
         * @param isDarkModeActive Whether dark mode is active.
         *
         * @return The adjusted chevron color.
         */
        fun chevronImageForegroundColor(
            titleText: Color,
            isDarkModeActive: Boolean,
        ): Color {
            if (!isDarkModeActive) {
                return titleText.lighter(by = ConversationCellViewFloats.CHEVRON_IMAGE_FOREGROUND_COLOR_ADJUSTMENT_PERCENTAGE)
            }

            return titleText.darker(by = ConversationCellViewFloats.CHEVRON_IMAGE_FOREGROUND_COLOR_ADJUSTMENT_PERCENTAGE)
        }

        /**
         * Returns the foreground color of the subtitle label.
         *
         * @param subtitleText The theme's subtitle text color.
         *
         * @return The adjusted subtitle color.
         */
        fun subtitleLabelTextForegroundColor(subtitleText: Color): Color =
            subtitleText.lighter(by = ConversationCellViewFloats.SUBTITLE_LABEL_FOREGROUND_COLOR_ADJUSTMENT_PERCENTAGE)
    }

    // MARK: - Reduce

    @Suppress("CyclomaticComplexMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared -> {
                val cellViewData = ConversationCellViewData(state.conversation, searchQuery = state.searchQuery)
                if (cellViewData == null) {
                    ReduceResult(state, reloadDataRetryTask(state.conversation))
                } else if (cellViewData.isAwaitingMessageHydration) {
                    // Backstop for provisional builds; message hydration may
                    // have completed before this cell subscribed to session
                    // store changes.
                    ReduceResult(
                        state.copy(cellViewData = cellViewData),
                        Effect.task(delay = HYDRATION_RELOAD_DELAY) { Action.ReloadData },
                    )
                } else {
                    ReduceResult(state.copy(cellViewData = cellViewData))
                }
            }

            Action.BlockUsersButtonTapped ->
                ReduceResult(state, blockUsersEffect(state.conversation))

            Action.CellTapped -> {
                val focusedMessageID = state.searchQuery.takeIf { it.isNotBlank() }?.let { state.focusedMessageID }
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Push(
                            UserContentNavigatorState.SeguePaths.Chat(state.conversationIDKey, focusedMessageID),
                        ),
                    ),
                )
                ReduceResult(state)
            }

            Action.DeleteConversationButtonTapped ->
                ReduceResult(state, deleteConversationButtonEffect(state.cellViewData.titleLabelText))

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

            Action.ReloadData -> reloadCellViewData(state, useCachedValue = false)

            is Action.ReloadingConversationsChanged ->
                ReduceResult(state.copy(isConversationReloading = action.conversationIDKeys.contains(state.conversationIDKey)))

            Action.ReportUsersButtonTapped ->
                ReduceResult(state, reportUsersEffect(state.conversation))

            is Action.SearchQueryChanged ->
                if (state.searchQuery == action.searchQuery) {
                    ReduceResult(state)
                } else {
                    reloadCellViewData(state.copy(searchQuery = action.searchQuery))
                }

            is Action.SessionStoreDidChange ->
                if (action.change.isRelevant(state.conversation)) {
                    ReduceResult(
                        state,
                        Effect
                            .task<Action>(delay = RELOAD_DELAY) { Action.ReloadData }
                            .cancellable(reloadDataTaskID, cancelInFlight = true),
                    )
                } else {
                    ReduceResult(state)
                }

            Action.UserInfoBadgeTapped -> {
                state.cellViewData.otherUser?.let { ConversationCellViewService.presentUserInfoAlert(it) }
                ReduceResult(state)
            }
        }

    // MARK: - Auxiliary

    private val reloadDataTaskID: String
        get() = "ConversationCellReducer/$instanceID/reloadData"

    private fun reloadCellViewData(
        state: State,
        useCachedValue: Boolean = true,
    ): ReduceResult<State, Action> {
        val cellViewData =
            ConversationCellViewData(
                state.conversation,
                searchQuery = state.searchQuery,
                useCachedValue = useCachedValue,
            ) ?: return ReduceResult(state, reloadDataRetryTask(state.conversation))

        if (cellViewData == state.cellViewData) return ReduceResult(state)
        return ReduceResult(state.copy(cellViewData = cellViewData))
    }

    // Retries the reload rather than consuming it; participants or
    // messages may still be hydrating into the session store.
    private fun reloadDataRetryTask(conversation: Conversation): Effect<Action> {
        if (conversation.isEmpty) return Effect.none()
        return Effect
            .task<Action>(delay = HYDRATION_RELOAD_DELAY) { Action.ReloadData }
            .cancellable(reloadDataTaskID, cancelInFlight = true)
    }

    private fun blockUsersEffect(conversation: Conversation): Effect<Action> =
        Effect.fireAndForget {
            try {
                ConversationCellViewService.blockUsersButtonTapped(conversation)
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

    private fun reportUsersEffect(conversation: Conversation): Effect<Action> =
        Effect.fireAndForget {
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
        private val HYDRATION_RELOAD_DELAY = 1.seconds
        private val RELOAD_DELAY = 250.milliseconds
    }
}
