//
//  RegionMenu.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.regionmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuFloats
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuStrings
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import androidx.compose.material3.Text as Material3Text

/**
 * A region picker.
 *
 * The button is a white, rounded, shadowed pill stacking the selected
 * region's emoji flag over its calling code. Tapping it opens a
 * searchable bottom sheet listing every region; selecting one reports
 * its region code.
 *
 * @param selectedRegionCode The currently selected region code.
 * @param onRegionCodeSelected Called with the newly selected region code.
 * @param modifier The modifier for this component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionMenu(
    selectedRegionCode: String,
    onRegionCodeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { ViewModel(RegionMenuReducer.State(selectedRegionCode = selectedRegionCode), RegionMenuReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    LaunchedEffect(state.selectedRegionCode) {
        if (state.selectedRegionCode.isNotBlank() && state.selectedRegionCode != selectedRegionCode) {
            onRegionCodeSelected(state.selectedRegionCode)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            modifier
                .shadow(RegionMenuFloats.buttonShadowElevation, RoundedCornerShape(RegionMenuFloats.buttonCornerRadius))
                .clip(RoundedCornerShape(RegionMenuFloats.buttonCornerRadius))
                .background(colors.background)
                .clickable { viewModel.send(RegionMenuReducer.Action.RunIsPresentedEffect(true)) }
                .widthIn(min = RegionMenuFloats.buttonMinWidth)
                .heightIn(min = RegionMenuFloats.buttonMinHeight)
                .padding(horizontal = RegionMenuFloats.buttonHorizontalPadding, vertical = RegionMenuFloats.buttonVerticalPadding),
    ) {
        Components.Text(
            RegionDetailService.emojiFlag(state.selectedRegionCode),
            foregroundColor = colors.titleText,
            font = Font.system(FontScale.Custom(RegionMenuFloats.FLAG_FONT_SIZE)),
        )
        Components.Text(
            "+${RegionDetailService.callingCode(state.selectedRegionCode) ?: RegionMenuStrings.DEFAULT_CALLING_CODE}",
            foregroundColor = colors.titleText,
            font = Font.system,
            modifier = Modifier.padding(top = RegionMenuFloats.callingCodeTopPadding),
        )
    }

    if (state.isPresented) {
        ModalBottomSheet(onDismissRequest = { viewModel.send(RegionMenuReducer.Action.RunIsPresentedEffect(false)) }) {
            RegionPickerView(
                state = state,
                onSearchQueryChange = { viewModel.send(RegionMenuReducer.Action.SearchQueryChanged(it)) },
                onRegionTitleSelected = { viewModel.send(RegionMenuReducer.Action.SelectedRegionTitleChanged(it)) },
                onListAppeared = { viewModel.send(RegionMenuReducer.Action.ListViewAppeared(it)) },
            )
        }
    }
}

// MARK: - Region Picker

@Composable
private fun RegionPickerView(
    state: RegionMenuReducer.State,
    onSearchQueryChange: (String) -> Unit,
    onRegionTitleSelected: (String) -> Unit,
    onListAppeared: (LazyListState) -> Unit,
) {
    val colors = LocalPantherColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM) }

    Column(modifier = Modifier.padding(horizontal = RegionMenuFloats.searchHorizontalPadding)) {
        Components.Text(
            state.headerLabelText,
            foregroundColor = colors.titleText,
            font = Font.systemBold(),
            modifier = Modifier.fillMaxWidth().padding(vertical = RegionMenuFloats.listItemVerticalPadding),
        )

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Material3Text(RegionMenuStrings.SEARCH_REGIONS) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        val regionTitles = state.queriedRegionTitles
        if (regionTitles == null) {
            Components.Text(
                state.noResultsLabelText,
                foregroundColor = colors.subtitleText,
                modifier = Modifier.fillMaxWidth().padding(vertical = RegionMenuFloats.listItemVerticalPadding),
            )
        } else {
            LaunchedEffect(Unit) { onListAppeared(listState) }
            LazyColumn(state = listState, modifier = Modifier.heightIn(max = RegionMenuFloats.listMaxHeight)) {
                items(regionTitles) { regionTitle ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onRegionTitleSelected(regionTitle) }
                                .padding(vertical = RegionMenuFloats.listItemVerticalPadding),
                    ) {
                        Components.Text(regionTitle, foregroundColor = colors.titleText)
                        if (regionTitle == state.selectedRegionTitle) {
                            Components.Symbol(
                                systemName = "checkmark.circle.fill",
                                foregroundColor = Color.Green,
                                modifier = Modifier.padding(start = RegionMenuFloats.callingCodeTopPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}
