//
//  InviteLanguagePickerReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.invitelanguagepickerview

import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.seconds

/**
 * The reducer that drives the invite language picker.
 *
 * The picker's behavior contract:
 *
 * - On appearance, the picker clears its search query and selection and
 *   disables the done button.
 * - The list displays the languages whose names match the search query,
 *   or all supported languages while the query is empty.
 * - Selecting a language enables the done button.
 * - Tapping done dismisses the picker and, after a short delay, composes
 *   the invitation in the selected language, surfacing any error as a
 *   toast. Tapping cancel dismisses the picker without composing.
 */
class InviteLanguagePickerReducer : Reducer<InviteLanguagePickerReducer.State, InviteLanguagePickerReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object ViewDisappeared : Action

        data object CancelHeaderItemTapped : Action

        data object DoneHeaderItemTapped : Action

        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        data class SelectedLanguageCodeChanged(
            val selectedLanguageCode: String,
        ) : Action
    }

    // MARK: - State

    data class State(
        val cancelHeaderItemText: String = LocalizedStringKey.Cancel.localized(),
        val doneHeaderItemText: String = LocalizedStringKey.Done.localized(LocalizationSource.SUBSYSTEM),
        val isDoneHeaderItemEnabled: Boolean = false,
        val noResultsLabelText: String = LocalizedStringKey.NoResults.localized(),
        val searchQuery: String = "",
        val selectedLanguageCode: String = "",
    ) {
        /** The selectable languages, keyed by language code, with display names as values. */
        val localizedLanguageNames: Map<String, String>
            get() = LocalizedStringResolver.languageDisplayNames()

        /** The picker's navigation title. */
        val navigationTitle: String
            get() {
                val title = LocalizedStringKey.SelectLanguage.localized(LocalizationSource.SUBSYSTEM)
                return if (RuntimeStorage.languageCode == "en") title.replaceFirstChar { it.uppercase() } else title
            }

        /** The languages whose display names match the search query, keyed by language code. */
        val queriedLanguageNames: Map<String, String>
            get() {
                val query = searchQuery.trim().lowercase()
                return localizedLanguageNames.filter { (_, name) -> name.trim().lowercase().contains(query) }
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(state.copy(isDoneHeaderItemEnabled = false, searchQuery = "", selectedLanguageCode = ""))

            Action.ViewDisappeared -> ReduceResult(state)

            Action.CancelHeaderItemTapped -> {
                RootSheets.dismiss()
                ReduceResult(state)
            }

            Action.DoneHeaderItemTapped -> {
                if (state.isDoneHeaderItemEnabled) {
                    RootSheets.dismiss()
                    val languageCode = state.selectedLanguageCode
                    Task.delayed(by = COMPOSE_INVITATION_DELAY_SECONDS.seconds) {
                        runCatching { InviteService.composeInvitation(languageCode) }
                            .onFailure { Logger.log(it.toException(), with = AlertType.toast) }
                    }
                }
                ReduceResult(state)
            }

            is Action.SearchQueryChanged ->
                ReduceResult(state.copy(searchQuery = action.searchQuery))

            is Action.SelectedLanguageCodeChanged ->
                ReduceResult(state.copy(selectedLanguageCode = action.selectedLanguageCode, isDoneHeaderItemEnabled = true))
        }

    // MARK: - Auxiliary

    private fun Throwable.toException(): Exception = this as? Exception ?: Exception.from(this, ExceptionMetadata(this))

    // MARK: - Companion

    private companion object {
        const val COMPOSE_INVITATION_DELAY_SECONDS = 2
    }
}
