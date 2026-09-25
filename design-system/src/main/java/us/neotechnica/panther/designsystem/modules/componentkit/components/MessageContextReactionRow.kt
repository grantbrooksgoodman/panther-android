//
//  MessageContextReactionRow.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

    Row(
        Modifier
            .clip(RoundedCornerShape(REACTION_ROW_CORNER_RADIUS))
            .background(colors.reactionButtonBackground)
            .padding(REACTION_ROW_PADDING),
        horizontalArrangement = Arrangement.spacedBy(REACTION_ROW_SPACING),
    ) {
        choices.forEach { choice ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(REACTION_BUTTON_SIZE)
                        .clip(CircleShape)
                        .background(if (choice.isSelected) choice.selectedColor else Color.Transparent)
                        .clickable { onSelect(choice) },
            ) {
                Components.Text(choice.emoji, color = colors.titleText, font = Font.system(FontScale.Custom(REACTION_EMOJI_FONT_SIZE)))
            }
        }
    }
}

private val REACTION_ROW_CORNER_RADIUS = 26.dp
private val REACTION_ROW_PADDING = 6.dp
private val REACTION_ROW_SPACING = 2.dp
private val REACTION_BUTTON_SIZE = 40.dp
private const val REACTION_EMOJI_FONT_SIZE = 22f
