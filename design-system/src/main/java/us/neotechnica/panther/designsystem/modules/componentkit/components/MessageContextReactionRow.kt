//
//  MessageContextReactionRow.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/** The row of reaction options shown above a lifted message bubble. */
@Composable
internal fun ReactionRow(
    choices: List<ReactionChoice>,
    onSelect: (ReactionChoice) -> Unit,
) {
    val colors = LocalPantherColors.current

    val containerColor =
        (if (isSystemInDarkTheme()) SYSTEM_GRAY3_DARK else SYSTEM_GRAY3_LIGHT)
            .copy(alpha = REACTION_ROW_BACKGROUND_ALPHA)
    Row(
        Modifier
            .clip(RoundedCornerShape(REACTION_ROW_CORNER_RADIUS))
            .background(containerColor)
            .padding(start = REACTION_ROW_LEADING_INSET)
            .padding(REACTION_ROW_PADDING),
        horizontalArrangement = Arrangement.spacedBy(REACTION_ROW_SPACING),
    ) {
        choices.forEach { choice ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(REACTION_BUTTON_SIZE)
                        .clip(RoundedCornerShape(REACTION_BUTTON_CORNER_RADIUS))
                        .background(if (choice.isSelected) choice.selectedColor else Color.Transparent)
                        .clickable { onSelect(choice) },
            ) {
                Components.Text(
                    choice.emoji,
                    foregroundColor = colors.titleText,
                    font = Font.system(FontScale.Custom(REACTION_EMOJI_FONT_SIZE)),
                )
            }
        }
    }
}

private val REACTION_BUTTON_CORNER_RADIUS = 17.5.dp
private val REACTION_BUTTON_SIZE = 35.dp
private const val REACTION_EMOJI_FONT_SIZE = 15f
private const val REACTION_ROW_BACKGROUND_ALPHA = 0.8f
private val REACTION_ROW_CORNER_RADIUS = 5.dp
private val REACTION_ROW_LEADING_INSET = 10.dp
private val REACTION_ROW_PADDING = 6.dp
private val REACTION_ROW_SPACING = 8.dp
private val SYSTEM_GRAY3_DARK = Color(0xFF48484A)
private val SYSTEM_GRAY3_LIGHT = Color(0xFFC7C7CC)
