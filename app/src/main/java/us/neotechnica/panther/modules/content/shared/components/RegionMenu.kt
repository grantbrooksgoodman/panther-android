//
//  RegionMenu.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.RegionDetailService
import androidx.compose.material3.Text as Material3Text

/**
 * A searchable region picker, ported from the iOS `RegionMenu`.
 *
 * Tapping the current region opens a bottom sheet listing every region
 * with its emoji flag and calling code; selecting one reports its
 * region code.
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

    Components.Button(
        text = RegionDetailService.regionTitle(selectedRegionCode),
        color = colors.accent,
        onClick = { isExpanded = true },
        font = Font.systemMedium(FontScale.Small),
        modifier =
            modifier
                .clip(RoundedCornerShape(CORNER_RADIUS))
                .border(1.dp, colors.disabled, RoundedCornerShape(CORNER_RADIUS))
                .padding(horizontal = 14.dp, vertical = 10.dp),
    )

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

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Material3Text("Search regions") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        LazyColumn(modifier = Modifier.heightIn(max = LIST_MAX_HEIGHT)) {
            items(regionCodes) { code ->
                Components.Text(
                    RegionDetailService.regionTitle(code),
                    color = colors.titleText,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onRegionCodeSelected(code) }
                            .padding(vertical = 14.dp),
                )
            }
        }
    }
}

private val CORNER_RADIUS = 10.dp
private val LIST_MAX_HEIGHT = 420.dp
