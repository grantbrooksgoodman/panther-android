//
//  SearchBar.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * A compact, capsule-shaped search field: a magnifying-glass glyph, a
 * placeholder, and a single-line input. Shared by the conversations list
 * and the contact selector.
 *
 * @param value The current query text.
 * @param placeholder The placeholder shown when [value] is empty.
 * @param onValueChange Invoked as the query changes.
 * @param modifier The modifier for this bar (for outer positioning).
 * @param containerColor The capsule's fill color.
 */
@Composable
fun SearchBar(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalPantherColors.current.groupedContentBackground,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .height(SEARCH_BAR_HEIGHT)
                .clip(CircleShape)
                .background(containerColor)
                .padding(horizontal = 10.dp),
    ) {
        Components.Symbol("magnifyingglass", color = colors.subtitleText, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
            if (value.isEmpty()) {
                Components.Text(placeholder, color = colors.subtitleText)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Font.system.textStyle.copy(color = colors.titleText),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val SEARCH_BAR_HEIGHT = 38.dp
