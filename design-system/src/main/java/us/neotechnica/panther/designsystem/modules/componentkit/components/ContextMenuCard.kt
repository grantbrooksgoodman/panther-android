//
//  ContextMenuCard.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

@Composable
internal fun ContextMenuCard(
    actions: List<ContextMenuAction>,
    onSelect: (ContextMenuAction) -> Unit,
) {
    val colors = LocalPantherColors.current

    Column(
        Modifier
            .width(MENU_WIDTH)
            .clip(RoundedCornerShape(MENU_CORNER_RADIUS))
            .background(Color.White.copy(alpha = MENU_BACKGROUND_ALPHA)),
    ) {
        actions.forEachIndexed { index, action ->
            val contentColor = if (action.isDestructive) DESTRUCTIVE_COLOR else colors.titleText
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MENU_ROW_HEIGHT)
                        .clickable { onSelect(action) }
                        .padding(start = MENU_ROW_START_PADDING, end = 12.dp),
            ) {
                Components.Text(
                    action.title,
                    color = contentColor,
                    font = Font.system,
                    modifier = Modifier.weight(1f),
                )
                Components.Symbol(
                    action.systemImageName,
                    color = contentColor,
                    modifier = Modifier.size(MENU_ICON_SIZE),
                )
            }

            if (index != actions.lastIndex) {
                HorizontalDivider(thickness = MENU_DIVIDER_THICKNESS, color = colors.subtitleText.copy(alpha = 0.25f))
            }
        }
    }
}
