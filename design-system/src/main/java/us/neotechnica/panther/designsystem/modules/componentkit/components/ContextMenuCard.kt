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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// The menu's elements – one row per action – with the element under a
// held long press highlighted. Each element reports its layout
// coordinates, including the divider below it, so a held long press
// can be hit-tested against contiguous elements.
@Composable
internal fun ContextMenuCard(
    actions: List<ContextMenuAction>,
    highlightedElementIndex: Int?,
    onElementPositioned: (Int, LayoutCoordinates) -> Unit,
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
            Column(modifier = Modifier.onGloballyPositioned { onElementPositioned(index, it) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(MENU_ROW_HEIGHT)
                            .background(if (index == highlightedElementIndex) MENU_ELEMENT_HIGHLIGHTED_BACKGROUND else Color.Transparent)
                            .clickable { onSelect(action) }
                            .padding(start = MENU_ROW_START_PADDING, end = 12.dp),
                ) {
                    Components.Text(
                        action.title,
                        foregroundColor = contentColor,
                        font = Font.system,
                        modifier = Modifier.weight(1f),
                    )
                    val imageResourceID = action.imageResourceID
                    if (imageResourceID != null) {
                        Icon(
                            painter = painterResource(imageResourceID),
                            contentDescription = null,
                            modifier = Modifier.size(MENU_ICON_SIZE),
                            tint = contentColor,
                        )
                    } else {
                        Components.Symbol(
                            action.systemImageName,
                            foregroundColor = contentColor,
                            modifier = Modifier.size(MENU_ICON_SIZE),
                        )
                    }
                }

                if (index != actions.lastIndex) {
                    HorizontalDivider(thickness = MENU_DIVIDER_THICKNESS, color = colors.subtitleText.copy(alpha = 0.25f))
                }
            }
        }
    }
}
