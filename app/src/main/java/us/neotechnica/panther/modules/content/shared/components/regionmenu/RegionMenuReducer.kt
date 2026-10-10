//
//  RegionMenuReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.regionmenu

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.AndroidUiDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuFloats
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import kotlin.time.Duration.Companion.milliseconds

/**
 * The reducer that drives the region picker presented by [RegionMenu].
 *
 * The picker displays a searchable list of regions and reports its
 * results back to the presenting view through the presentation and
 * selection values it holds in its state.
 *
 * The picker's behavior contract:
 *
 * - On appearance, the list scrolls to the currently selected region
 *   after a brief delay.
 * - The list displays the regions whose titles match the search query,
 *   or a no results message when none match.
 * - Selecting a region records its code, then dismisses the picker
 *   after a brief delay.
 */
class RegionMenuReducer : Reducer<RegionMenuReducer.State, RegionMenuReducer.Action> {
    // MARK: - Actions

    /** The actions the region picker can process. */
    sealed interface Action {
        /**
         * An action that requests a presentation change, carrying whether
         * the picker should be presented. Triggers [RunIsPresentedEffect]
         * after a brief delay.
         */
        data class IsPresentedChanged(
            val isPresented: Boolean,
        ) : Action

        /** An action that indicates the region list appeared, carrying the list state used to scroll to the current selection. */
        data class ListViewAppeared(
            val listState: LazyListState,
        ) : Action

        /** An action that applies the given presentation state. */
        data class RunIsPresentedEffect(
            val isPresented: Boolean,
        ) : Action

        /** An action that indicates the search query changed, carrying the new value. */
        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        /**
         * An action that indicates the user selected a region, carrying its
         * display title. Records the region's code and dismisses the picker
         * after a brief delay.
         */
        data class SelectedRegionTitleChanged(
            val selectedRegionTitle: String,
        ) : Action
    }

    // MARK: - State

    /** The state of the region picker. */
    data class State(
        /** A Boolean value that indicates whether the picker is presented. */
        val isPresented: Boolean = false,
        /** The search query the user has entered. */
        val searchQuery: String = "",
        /** The code of the selected region. */
        val selectedRegionCode: String = "",
    ) {
        /** The localized text the picker's header displays. */
        val headerLabelText: String
            get() {
                val localizedString = LocalizedStringKey.SelectCallingCode.localized()
                if (RuntimeStorage.languageCode != "en") return localizedString
                return localizedString.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
            }

        /** The localized text the no results label displays. */
        val noResultsLabelText: String
            get() = LocalizedStringKey.NoResults.localized()

        /** The display titles of the regions matching the search query, or `null` if none match. */
        val queriedRegionTitles: List<String>?
            get() = RegionDetailService.regionTitles(RegionDetailService.QueryStrategy.SearchTerm(searchQuery))

        /** The display title of the selected region, or `null` if it cannot be determined. */
        val selectedRegionTitle: String?
            get() =
                RegionDetailService
                    .regionTitles(
                        RegionDetailService.QueryStrategy.RegionCode(selectedRegionCode),
                        titleFormat = RegionDetailService.RegionTitleFormat.REGION_NAME_FIRST,
                    )?.firstOrNull()
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
                    Effect.task<Action>(delay = RegionMenuFloats.DELAY_MILLISECONDS.milliseconds) {
                        Action.RunIsPresentedEffect(isPresented)
                    },
                )
            }

            is Action.ListViewAppeared -> {
                val index = state.selectedRegionTitle?.let { state.queriedRegionTitles?.indexOf(it) } ?: -1
                val listState = action.listState
                ReduceResult(
                    state,
                    Effect.run {
                        delay(RegionMenuFloats.DELAY_MILLISECONDS)
                        // Scroll animations require the UI frame clock.
                        if (index >= 0) withContext(AndroidUiDispatcher.Main) { listState.animateScrollToItem(index) }
                    },
                )
            }

            is Action.RunIsPresentedEffect ->
                ReduceResult(state.copy(isPresented = action.isPresented))

            is Action.SearchQueryChanged ->
                ReduceResult(state.copy(searchQuery = action.searchQuery))

            is Action.SelectedRegionTitleChanged -> {
                val selectedRegionCode =
                    RegionDetailService.regionCode(
                        RegionDetailService.QueryStrategy.RegionTitle(action.selectedRegionTitle),
                    ) ?: ""
                ReduceResult(
                    state.copy(selectedRegionCode = selectedRegionCode),
                    Effect.task<Action>(delay = RegionMenuFloats.DELAY_MILLISECONDS.milliseconds) { Action.IsPresentedChanged(false) },
                )
            }
        }
}
