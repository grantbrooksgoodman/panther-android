//
//  ReactionChipView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatMessageCellFloats
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User

@Composable
internal fun ReactionChipView(chip: ReactionChip) {
    val colors = LocalPantherColors.current
    val background =
        if (chip.isOwn) {
            chip.style.squareIconColor.copy(alpha = ChatMessageCellFloats.REACTION_OWN_HIGHLIGHT_ALPHA)
        } else {
            colors.reactionButtonBackground
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(end = ChatMessageCellFloats.reactionChipSpacing)
                .clip(RoundedCornerShape(ChatMessageCellFloats.reactionChipCornerRadius))
                .background(background)
                .padding(
                    horizontal = ChatMessageCellFloats.reactionChipHorizontalPadding,
                    vertical = ChatMessageCellFloats.reactionChipVerticalPadding,
                ),
    ) {
        Components.Text(
            chip.style.emojiValue,
            foregroundColor = colors.titleText,
            font = Font.system(FontScale.Custom(ChatMessageCellFloats.REACTION_FONT_SIZE)),
        )
        if (chip.count > 1) {
            Components.Text(
                chip.count.toString(),
                foregroundColor = colors.subtitleText,
                font = Font.system(FontScale.Small),
                modifier = Modifier.padding(start = ChatMessageCellFloats.reactionChipCountStartPadding),
            )
        }
    }
}

internal data class ReactionChip(
    val style: Reaction.Style,
    val count: Int,
    val isOwn: Boolean,
)

internal fun reactionChips(reactions: List<Reaction>): List<ReactionChip> =
    reactions
        .groupBy { it.style }
        .entries
        .sortedBy { it.key.orderValue }
        .map { (style, styleReactions) ->
            ReactionChip(
                style = style,
                count = styleReactions.size,
                isOwn = styleReactions.any { it.userID == User.currentUserID },
            )
        }

internal fun reactionChoicesFor(
    row: ChatMessageRowData,
    onReact: (Message, Reaction.Style) -> Unit,
): List<ReactionChoice> {
    val ownStyles =
        row.reactions
            .filter { it.userID == User.currentUserID }
            .map { it.style }
            .toSet()
    return Reaction.Style.orderedCases.map { style ->
        ReactionChoice(
            emoji = style.emojiValue,
            selectedColor = style.squareIconColor,
            isSelected = style in ownStyles,
            isDoubleTapDefault = style == Reaction.Style.LOVE,
            onSelect = { onReact(row.message, style) },
        )
    }
}

// The background color of a reaction style's square icon.
internal val Reaction.Style.squareIconColor: Color
    get() =
        when (this) {
            Reaction.Style.DISLIKE -> Color(0xFFFF5252)
            Reaction.Style.EMPHASIS -> Color(0xFF0FB9B1)
            Reaction.Style.LAUGH -> Color(0xFFC56CF0)
            Reaction.Style.LIKE -> Color(0xFF27AE60)
            Reaction.Style.LOVE -> Color(0xFF30AAF2)
            Reaction.Style.QUESTION -> Color(0xFFFFB142)
        }
