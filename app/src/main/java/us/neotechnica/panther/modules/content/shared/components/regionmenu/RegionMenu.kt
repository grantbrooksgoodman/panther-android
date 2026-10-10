//
//  RegionMenu.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.regionmenu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.designsystem.modules.theming.views.ThemedView
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.shared.components.SearchBarInView
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuColors
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuFloats
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuStrings
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Colors = RegionMenuColors
private typealias Floats = RegionMenuFloats
private typealias Strings = RegionMenuStrings

/**
 * A button that displays the selected region's flag and calling code,
 * and presents a region picker when tapped.
 *
 * Use `RegionMenu` alongside a phone number field to let the user
 * choose the region their phone number belongs to. Tapping the button
 * presents a searchable list of regions in a sheet; selecting one
 * reports the region's code through [onSelectedRegionCodeChange] and
 * dismisses the picker.
 *
 * @param selectedRegionCode The code of the selected region.
 * @param onSelectedRegionCodeChange Called with the code of the newly
 *   selected region.
 * @param modifier The modifier for this component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionMenu(
    selectedRegionCode: String,
    onSelectedRegionCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { ViewModel(RegionMenuReducer.State(selectedRegionCode = selectedRegionCode), RegionMenuReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())

    LaunchedEffect(state.selectedRegionCode) {
        if (state.selectedRegionCode.isNotBlank() && state.selectedRegionCode != selectedRegionCode) {
            onSelectedRegionCodeChange(state.selectedRegionCode)
        }
    }

    ThemedView {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                modifier
                    .shadow(
                        Floats.buttonLabelVStackShadowRadius,
                        RoundedCornerShape(Floats.buttonLabelVStackBackgroundRectangleCornerRadius),
                    ).clip(RoundedCornerShape(Floats.buttonLabelVStackBackgroundRectangleCornerRadius))
                    .background(if (isDarkModeActive) Colors.buttonLabelDarkForeground else Colors.buttonLabelLightForeground)
                    .clickable { viewModel.send(RegionMenuReducer.Action.RunIsPresentedEffect(true)) }
                    .widthIn(min = Floats.buttonLabelVStackFrameMinWidth)
                    .heightIn(min = Floats.buttonLabelVStackFrameMinHeight),
        ) {
            RegionDetailService.image(RegionDetailService.QueryStrategy.RegionCode(state.selectedRegionCode))?.let {
                Image(
                    painter = painterResource(it.resourceID),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .size(width = Floats.buttonLabelImageFrameWidth, height = Floats.buttonLabelImageFrameHeight)
                            .clip(RoundedCornerShape(Floats.buttonLabelImageCornerRadius)),
                )
            }

            Components.Text(
                "+${RegionDetailService.callingCode(state.selectedRegionCode) ?: Strings.DEFAULT_CALLING_CODE}",
                foregroundColor = Colors.buttonLabelTextForeground,
            )
        }
    }

    if (state.isPresented) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.send(RegionMenuReducer.Action.RunIsPresentedEffect(false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.navigationBarBackground,
        ) {
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

    LaunchedEffect(Unit) { HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM) }

    HeaderView(
        centerItem = HeaderView.CenterItemType.Text(HeaderView.TextAttributes(state.headerLabelText)),
        attributes = HeaderView.Attributes(showsDivider = false, sizeClass = HeaderView.SizeClass.Sheet),
    ) {
        SearchBarInView(query = state.searchQuery, onQueryChange = onSearchQueryChange) {
            Box(modifier = Modifier.fillMaxSize().background(colors.groupedContentBackground)) {
                val regionTitles = state.queriedRegionTitles
                if (regionTitles == null) {
                    Components.Text(
                        state.noResultsLabelText,
                        foregroundColor = colors.subtitleText,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    ListView(
                        regionTitles = regionTitles,
                        selectedRegionTitle = state.selectedRegionTitle,
                        onRegionTitleSelected = onRegionTitleSelected,
                        onListAppeared = onListAppeared,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListView(
    regionTitles: List<String>,
    selectedRegionTitle: String?,
    onRegionTitleSelected: (String) -> Unit,
    onListAppeared: (LazyListState) -> Unit,
) {
    val colors = LocalPantherColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { onListAppeared(listState) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(regionTitles) { regionTitle ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.groupedRowBackground)
                        .clickable { onRegionTitleSelected(regionTitle) }
                        .padding(
                            horizontal = Floats.listViewCellHorizontalPadding,
                            vertical = Floats.listViewCellVerticalPadding,
                        ),
            ) {
                RegionDetailService.image(RegionDetailService.QueryStrategy.RegionTitle(regionTitle))?.let {
                    Image(
                        painter = painterResource(it.resourceID),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier
                                .padding(end = Floats.listViewCellHorizontalPadding / 2)
                                .size(
                                    width = Floats.listViewCellLabelImageFrameWidth,
                                    height = Floats.listViewCellLabelImageFrameHeight,
                                ).clip(RoundedCornerShape(Floats.listViewCellLabelImageCornerRadius)),
                    )
                }

                Components.Text(regionTitle, foregroundColor = colors.titleText)

                if (regionTitle == selectedRegionTitle) {
                    Components.Symbol(
                        Strings.SELECTED_CELL_IMAGE_SYSTEM_NAME,
                        foregroundColor = Colors.selectedCellImageForeground,
                        modifier = Modifier.padding(start = Floats.selectedCellImageLeadingPadding),
                    )
                }
            }
        }
    }
}
