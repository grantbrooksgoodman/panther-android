//
//  ConversationCell.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.user.constants.ConversationCellColors
import us.neotechnica.panther.modules.content.user.constants.ConversationCellFloats
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import androidx.compose.material3.Text as Material3Text

/**
 * A single conversation row: an unread dot, avatar, title with an
 * optional language chip, message preview, and timestamp.
 *
 * The cell data is resolved asynchronously (message previews may resolve
 * translations), keyed on the conversation and [changeToken] so the row
 * refreshes when the store updates.
 *
 * @param conversation The conversation to render.
 * @param languageCode The language to resolve previews into.
 * @param changeToken A token that changes when the store updates.
 * @param modifier The modifier for this cell.
 */
@Composable
fun ConversationCell(
    conversation: Conversation,
    languageCode: String,
    changeToken: Any,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val cellData by produceState<ConversationCellViewData?>(
        initialValue = null,
        conversation,
        changeToken,
    ) {
        value = ConversationCellViewData.build(conversation, languageCode)
    }
    val data = cellData ?: return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = ConversationCellFloats.rowStartPadding,
                    end = ConversationCellFloats.rowEndPadding,
                    top = ConversationCellFloats.rowTopPadding,
                    bottom = ConversationCellFloats.rowBottomPadding,
                ),
    ) {
        Box(modifier = Modifier.width(ConversationCellFloats.unreadSlotWidth), contentAlignment = Alignment.Center) {
            if (data.isShowingUnreadIndicator) {
                Box(
                    modifier =
                        Modifier
                            .size(ConversationCellFloats.unreadIndicatorSize)
                            .clip(CircleShape)
                            .background(colors.accent),
                )
            }
        }

        Avatar(data, conversation.metadata.imageData)

        Spacer(modifier = Modifier.width(ConversationCellFloats.titleAvatarSpacing))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ConversationCellFloats.subtitleSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Material3Text(
                        data.title,
                        color = colors.titleText,
                        style = Font.systemSemibold().textStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    data.otherLanguageCode?.let { LanguageChip(it, data.otherRegionCode) }
                }
                Spacer(modifier = Modifier.width(ConversationCellFloats.dateSpacerWidth))
                Components.Text(data.dateLabelText, color = colors.subtitleText, font = Font.system(FontScale.Small))
                Components.Symbol(
                    "chevron.right",
                    color = colors.subtitleText,
                    modifier =
                        Modifier
                            .padding(start = ConversationCellFloats.chevronStartPadding)
                            .size(ConversationCellFloats.chevronSize),
                )
            }
            Material3Text(
                data.subtitle.ifBlank { " " },
                color = colors.subtitleText,
                style = Font.system(FontScale.Small).textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Avatar(
    data: ConversationCellViewData,
    imageData: ByteArray?,
) {
    val colors = LocalPantherColors.current

    // The badge lives in an unclipped outer box; only the inner disc is
    // circle-clipped, so the count badge is never cut off at the corner.
    Box(modifier = Modifier.size(ConversationCellFloats.avatarSize)) {
        AvatarImageView(
            modifier = Modifier.fillMaxSize(),
            imageData = imageData,
            initials = if (!data.isGroup && data.hasContactName) data.initials else "",
            fallbackSymbol = if (data.isGroup) "person.2" else "person",
        )

        if (data.isGroup) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(ConversationCellFloats.badgeSize)
                        .clip(CircleShape)
                        .background(colors.background)
                        .border(ConversationCellFloats.badgeBorderWidth, ConversationCellColors.badgeBorder, CircleShape),
            ) {
                Components.Text(
                    data.participantCount.toString(),
                    color = colors.titleText,
                    font = Font.systemBold(FontScale.Small),
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(
    languageCode: String,
    regionCode: String?,
) {
    val colors = LocalPantherColors.current
    // Prefer the flag of the user's phone-number region (e.g. +1 → 🇺🇸), falling back to the
    // language code's flag, mirroring the iOS `UserInfoBadgeView`.
    val flag = regionCode?.let { RegionDetailService.emojiFlag(it) }?.ifBlank { null } ?: flagFor(languageCode)
    val label = languageCode.uppercase() + if (flag != null) " $flag" else ""
    Box(
        modifier =
            Modifier
                .padding(start = ConversationCellFloats.languageChipStartPadding)
                .clip(RoundedCornerShape(ConversationCellFloats.languageChipCornerRadius))
                .background(colors.groupedContentBackground)
                .padding(
                    horizontal = ConversationCellFloats.languageChipHorizontalPadding,
                    vertical = ConversationCellFloats.languageChipVerticalPadding,
                ),
    ) {
        Components.Text(label, color = colors.subtitleText, font = Font.systemMedium(FontScale.Small))
    }
}

private fun flagFor(languageCode: String): String? =
    when (languageCode.lowercase()) {
        "en" -> "🇺🇸"
        "es" -> "🇪🇸"
        "de" -> "🇩🇪"
        "fr" -> "🇫🇷"
        "it" -> "🇮🇹"
        "pt" -> "🇵🇹"
        "ru" -> "🇷🇺"
        "uk" -> "🇺🇦"
        "zh" -> "🇨🇳"
        "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"
        "nl" -> "🇳🇱"
        "pl" -> "🇵🇱"
        "tr" -> "🇹🇷"
        "ar" -> "🇸🇦"
        "hi" -> "🇮🇳"
        else -> null
    }
