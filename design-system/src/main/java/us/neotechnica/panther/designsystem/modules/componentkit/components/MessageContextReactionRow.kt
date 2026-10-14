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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageContextReactionRowColors
import us.neotechnica.panther.designsystem.modules.componentkit.constants.MessageContextReactionRowFloats
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias ReactionRowColors = MessageContextReactionRowColors
private typealias ReactionRowFloats = MessageContextReactionRowFloats

/**
 * The row of reaction options shown above a lifted message bubble: a
 * capsule as tall as its reaction buttons, so that a selected
 * reaction's circle fills the capsule's height.
 */
@Composable
internal fun ReactionRow(
    choices: List<ReactionChoice>,
    onSelect: (ReactionChoice) -> Unit,
) {
    val colors = LocalPantherColors.current

    val containerColor =
        (if (isSystemInDarkTheme()) ReactionRowColors.reactionButtonDarkBackground else ReactionRowColors.reactionButtonLightBackground)
            .copy(alpha = ReactionRowFloats.VIEW_ALPHA)
    Row(
        Modifier
            .clip(CircleShape)
            .background(containerColor),
        horizontalArrangement = Arrangement.spacedBy(ReactionRowFloats.stackViewSpacing),
    ) {
        choices.forEach { choice ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(ReactionRowFloats.reactionButtonHeight)
                        .clip(CircleShape)
                        .background(if (choice.isSelected) choice.selectedColor else Color.Transparent)
                        .clickable { onSelect(choice) },
            ) {
                Components.Text(
                    choice.emoji,
                    foregroundColor = colors.titleText,
                    font = Font.system(FontScale.Custom(ReactionRowFloats.REACTION_BUTTON_TITLE_LABEL_SYSTEM_FONT_SIZE)),
                )
            }
        }
    }
}
