//
//  RegionMenu.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuFloats
import us.neotechnica.panther.modules.content.shared.constants.RegionMenuStrings
import androidx.compose.material3.Text as Material3Text

/**
 * A region picker, ported from the iOS `RegionMenu`.
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
    val colors = LocalPantherColors.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            modifier
                .shadow(RegionMenuFloats.buttonShadowElevation, RoundedCornerShape(RegionMenuFloats.buttonCornerRadius))
                .clip(RoundedCornerShape(RegionMenuFloats.buttonCornerRadius))
                .background(colors.background)
                .clickable { isExpanded = true }
                .widthIn(min = RegionMenuFloats.buttonMinWidth)
                .heightIn(min = RegionMenuFloats.buttonMinHeight)
                .padding(horizontal = RegionMenuFloats.buttonHorizontalPadding, vertical = RegionMenuFloats.buttonVerticalPadding),
    ) {
        Components.Text(
            RegionDetailService.emojiFlag(selectedRegionCode),
            color = colors.titleText,
            font = Font.system(FontScale.Custom(RegionMenuFloats.FLAG_FONT_SIZE)),
        )
        Components.Text(
            "+${RegionDetailService.callingCode(selectedRegionCode) ?: RegionMenuStrings.DEFAULT_CALLING_CODE}",
            color = colors.titleText,
            font = Font.system,
            modifier = Modifier.padding(top = RegionMenuFloats.callingCodeTopPadding),
        )
    }

    if (isExpanded) {
        ModalBottomSheet(onDismissRequest = { isExpanded = false }) {
            RegionList(
                onRegionCodeSelected = {
                    onRegionCodeSelected(it)
                    isExpanded = false
                },
            )
        }
    }
}

@Composable
private fun RegionList(onRegionCodeSelected: (String) -> Unit) {
    val colors = LocalPantherColors.current
    var searchQuery by remember { mutableStateOf("") }

    val regionCodes =
        remember(searchQuery) {
            RegionDetailService.allRegionCodes.filter { code ->
                searchQuery.isBlank() ||
                    RegionDetailService.regionTitle(code).contains(searchQuery, ignoreCase = true)
            }
        }

    Column(modifier = Modifier.padding(horizontal = RegionMenuFloats.searchHorizontalPadding)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Material3Text(RegionMenuStrings.SEARCH_REGIONS) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        LazyColumn(modifier = Modifier.heightIn(max = RegionMenuFloats.listMaxHeight)) {
            items(regionCodes) { code ->
                Components.Text(
                    RegionDetailService.regionTitle(code),
                    color = colors.titleText,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onRegionCodeSelected(code) }
                            .padding(vertical = RegionMenuFloats.listItemVerticalPadding),
                )
            }
        }
    }
}
