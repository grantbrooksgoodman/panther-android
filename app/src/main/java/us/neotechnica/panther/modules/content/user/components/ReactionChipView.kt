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
import us.neotechnica.panther.R
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.foundation.services.SoundPlayer
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.HapticsService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Constants Accessors

private typealias ChipFloats = ChatPageViewFloats.MessagesDisplayDelegate

/**
 * A reaction chip: the style's emoji and, for more than one reaction of
 * that style, its count. The current user's own reaction is highlighted
 * in the style's color.
 *
 * @param chip The chip to render.
 */
@Composable
internal fun ReactionChipView(chip: ReactionChip) {
    val colors = LocalPantherColors.current
    val background =
        if (chip.isOwn) {
            chip.style.squareIconBackgroundColor.copy(alpha = ChipFloats.REACTION_CHIP_OWN_HIGHLIGHT_ALPHA)
        } else {
            colors.reactionButtonBackground
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(RoundedCornerShape(ChipFloats.reactionChipCornerRadius))
                .background(background)
                .padding(
                    horizontal = ChipFloats.reactionChipHorizontalPadding,
                    vertical = ChipFloats.reactionChipVerticalPadding,
                ),
    ) {
        Components.Text(
            chip.style.emojiValue,
            foregroundColor = colors.titleText,
            font = Font.system(FontScale.Custom(ChipFloats.REACTION_CHIP_FONT_SIZE)),
        )
        if (chip.count > 1) {
            Components.Text(
                chip.count.toString(),
                foregroundColor = colors.subtitleText,
                font = Font.system(FontScale.Small),
                modifier = Modifier.padding(start = ChipFloats.reactionChipCountStartPadding),
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

/**
 * The reaction row choices for a message: every style in display
 * order, with the current user's own styles selected and the love style
 * as the double-tap default.
 *
 * Choosing a reaction plays the reaction sound with medium haptic
 * feedback; the double tap gives the haptic feedback alone.
 *
 * @param message The message to react to.
 * @param reactions The message's reactions.
 * @param onReact Applies the given style to the given message.
 */
internal fun reactionChoicesFor(
    message: Message,
    reactions: List<Reaction>,
    onReact: (Message, Reaction.Style) -> Unit,
): List<ReactionChoice> {
    val hapticsService = DependencyValues.current.commonServices.haptics
    val ownStyles =
        reactions
            .filter { it.userID == User.currentUserID }
            .map { it.style }
            .toSet()
    return Reaction.Style.orderedCases.map { style ->
        ReactionChoice(
            emoji = style.emojiValue,
            selectedColor = style.squareIconBackgroundColor,
            isSelected = style in ownStyles,
            isDoubleTapDefault = style == Reaction.Style.LOVE,
            onSelect = {
                SoundPlayer.play(R.raw.reaction)
                hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)
                onReact(message, style)
            },
            onDoubleTap = {
                hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)
                onReact(message, style)
            },
        )
    }
}
