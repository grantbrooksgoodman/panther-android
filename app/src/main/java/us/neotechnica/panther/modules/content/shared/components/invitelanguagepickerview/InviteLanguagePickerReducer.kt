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
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.seconds

/**
 * The reducer that drives [InviteLanguagePickerView].
 *
 * The picker's behavior contract:
 *
 * - On appearance, the picker clears its search query and selection and
 *   disables the done button.
 * - The list displays the languages whose names match the search query,
 *   or a no results message when none match. While the query is empty,
 *   all supported languages are shown.
 * - Selecting a language enables the done button.
 * - Tapping done dismisses the sheet and, after a short delay, composes
 *   the invitation in the selected language, surfacing any error as a
 *   toast. Tapping cancel dismisses the sheet without composing an
 *   invitation.
 */
class InviteLanguagePickerReducer : Reducer<InviteLanguagePickerReducer.State, InviteLanguagePickerReducer.Action> {
    // MARK: - Actions

    /** The actions the invite language picker can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Resets the picker's selection and search query. */
        data object ViewAppeared : Action

        /** An action that indicates the view disappeared. */
        data object ViewDisappeared : Action

        /** An action that indicates the user tapped the cancel button. Dismisses the sheet. */
        data object CancelHeaderItemTapped : Action

        /** An action that indicates the user tapped the done button. Dismisses the sheet and begins composing the invitation. */
        data object DoneHeaderItemTapped : Action

        /** An action that indicates the search query changed, carrying the new value. */
        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        /** An action that indicates the user selected a language, carrying its language code. Enables the done button. */
        data class SelectedLanguageCodeChanged(
            val selectedLanguageCode: String,
        ) : Action
    }

    // MARK: - State

    /** The state of the invite language picker. */
    data class State(
        /** The localized text the cancel button displays. */
        val cancelHeaderItemText: String = LocalizedStringKey.Cancel.localized(),
        /** The localized text the done button displays. */
        val doneHeaderItemText: String = LocalizedStringKey.Done.localized(LocalizationSource.SUBSYSTEM),
        /** A Boolean value that indicates whether the done button is enabled. Enabled once a language has been selected. */
        val isDoneHeaderItemEnabled: Boolean = false,
        /** The localized text the no results label displays. */
        val noResultsLabelText: String = LocalizedStringKey.NoResults.localized(),
        /** The search query the user has entered. */
        val searchQuery: String = "",
        /** The language code of the selected language. */
        val selectedLanguageCode: String = "",
    ) {
        /** The selectable languages, keyed by language code, with localized display names as values. */
        val localizedLanguageNames: Map<String, String>
            get() =
                CoreUtilities.localizedLanguageCodeDictionary(RuntimeStorage.languageCode)
                    ?: RuntimeStorage.languageCodeDictionary
                    ?: emptyMap()

        /** The localized text the picker's header displays. */
        val navigationTitle: String
            get() {
                val localizedString = LocalizedStringKey.SelectLanguage.localized()
                if (RuntimeStorage.languageCode != "en") return localizedString
                return localizedString.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
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
                    Task.delayed(by = COMPOSE_INVITATION_DELAY) {
                        try {
                            InviteService.composeInvitation(languageCode)
                        } catch (exception: Exception) {
                            Logger.log(exception, with = AlertType.toast)
                        }
                    }
                }
                ReduceResult(state)
            }

            is Action.SearchQueryChanged ->
                ReduceResult(state.copy(searchQuery = action.searchQuery))

            is Action.SelectedLanguageCodeChanged ->
                ReduceResult(state.copy(selectedLanguageCode = action.selectedLanguageCode, isDoneHeaderItemEnabled = true))
        }

    // MARK: - Companion

    private companion object {
        val COMPOSE_INVITATION_DELAY = 2.seconds
    }
}
