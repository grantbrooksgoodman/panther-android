//
//  RegionMenuReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.regionmenu

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.delay
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer that drives the region picker presented by [RegionMenu].
 *
 * The picker displays a searchable list of regions and reports its
 * results back to the presenting view through the selection it holds in
 * its state.
 *
 * The picker's behavior contract:
 *
 * - On appearance, the list scrolls to the currently selected region
 *   after a brief delay.
 * - The list displays the regions whose titles match the search query,
 *   or a no results message when none match.
 * - Selecting a region records its code, then dismisses the picker after
 *   a brief delay.
 */
class RegionMenuReducer : Reducer<RegionMenuReducer.State, RegionMenuReducer.Action> {
    // MARK: - Action

    sealed interface Action {
        data class IsPresentedChanged(
            val isPresented: Boolean,
        ) : Action

        data class ListViewAppeared(
            val listState: LazyListState,
        ) : Action

        data class RunIsPresentedEffect(
            val isPresented: Boolean,
        ) : Action

        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        data class SelectedRegionTitleChanged(
            val selectedRegionTitle: String,
        ) : Action
    }

    // MARK: - State

    data class State(
        val isPresented: Boolean = false,
        val searchQuery: String = "",
        val selectedRegionCode: String = "",
    ) {
        val headerLabelText: String
            get() {
                val localizedString = LocalizedStringKey.SelectCallingCode.localized()
                if (RuntimeStorage.languageCode != "en") return localizedString
                return localizedString.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
            }

        val noResultsLabelText: String
            get() = LocalizedStringKey.NoResults.localized()

        val queriedRegionTitles: List<String>?
            get() {
                val titles =
                    RegionDetailService.allRegionCodes
                        .filter { searchQuery.isBlank() || RegionDetailService.regionTitle(it).contains(searchQuery, ignoreCase = true) }
                        .map { RegionDetailService.regionTitle(it) }
                return titles.ifEmpty { null }
            }

        val selectedRegionTitle: String?
            get() = if (selectedRegionCode.isBlank()) null else RegionDetailService.regionTitle(selectedRegionCode)
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            is Action.IsPresentedChanged -> {
                val isPresented = action.isPresented
                ReduceResult(
                    state,
                    Effect.task<Action>(delay = DELAY_MILLISECONDS.milliseconds) { Action.RunIsPresentedEffect(isPresented) },
                )
            }

            is Action.ListViewAppeared -> {
                val index = state.selectedRegionTitle?.let { state.queriedRegionTitles?.indexOf(it) } ?: -1
                val listState = action.listState
                ReduceResult(
                    state,
                    Effect.run {
                        delay(DELAY_MILLISECONDS)
                        if (index >= 0) listState.animateScrollToItem(index)
                    },
                )
            }

            is Action.RunIsPresentedEffect ->
                ReduceResult(state.copy(isPresented = action.isPresented))

            is Action.SearchQueryChanged ->
                ReduceResult(state.copy(searchQuery = action.searchQuery))

            is Action.SelectedRegionTitleChanged -> {
                val selectedRegionCode =
                    RegionDetailService.allRegionCodes
                        .firstOrNull { RegionDetailService.regionTitle(it) == action.selectedRegionTitle }
                        ?: ""
                ReduceResult(
                    state.copy(selectedRegionCode = selectedRegionCode),
                    Effect.task<Action>(delay = DELAY_MILLISECONDS.milliseconds) { Action.IsPresentedChanged(false) },
                )
            }
        }

    // MARK: - Companion

    private companion object {
        const val DELAY_MILLISECONDS = 500L
    }
}
