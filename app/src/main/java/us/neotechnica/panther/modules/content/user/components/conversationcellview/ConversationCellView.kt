//
//  ConversationCellView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components.conversationcellview

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.UserInfoBadgeView
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewColors
import us.neotechnica.panther.modules.content.user.constants.ConversationCellViewFloats
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import androidx.compose.material3.Text as Material3Text

/**
 * A single conversation row: an unread dot, avatar, title with an
 * optional user-info badge, message preview, and timestamp.
 *
 * The content renders from the reducer's resolved cell view data and
 * redacts its preview and timestamp while the data has not resolved
 * or the conversation reloads.
 *
 * @param state The conversation cell's state.
 * @param onUserInfoBadgeTapped The action performed when the user
 *   taps the user-info badge.
 * @param modifier The modifier for this cell.
 */
@Composable
fun ConversationCellView(
    state: ConversationCellReducer.State,
    onUserInfoBadgeTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val data = state.cellViewData
    val contentAlpha = if (state.isShowingRedactedContent) REDACTED_CONTENT_ALPHA else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = ConversationCellViewFloats.rowStartPadding,
                    end = ConversationCellViewFloats.rowEndPadding,
                    top = ConversationCellViewFloats.rowTopPadding,
                    bottom = ConversationCellViewFloats.rowBottomPadding,
                ),
    ) {
        Box(modifier = Modifier.width(ConversationCellViewFloats.unreadSlotWidth), contentAlignment = Alignment.Center) {
            if (data.isShowingUnreadIndicator) {
                Box(
                    modifier =
                        Modifier
                            .size(ConversationCellViewFloats.unreadIndicatorSize)
                            .clip(CircleShape)
                            .background(colors.accent),
                )
            }
        }

        Avatar(data, state.conversation.metadata.imageData)

        Spacer(modifier = Modifier.width(ConversationCellViewFloats.titleAvatarSpacing))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ConversationCellViewFloats.subtitleSpacing),
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
                    data.otherUser?.let { user ->
                        Box(modifier = Modifier.padding(start = ConversationCellViewFloats.languageChipStartPadding)) {
                            UserInfoBadgeView(
                                user = user,
                                action = if (Build.isDeveloperModeEnabled) onUserInfoBadgeTapped else null,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(ConversationCellViewFloats.dateSpacerWidth))
                Components.Text(
                    state.dateLabelText,
                    color = colors.subtitleText,
                    font = Font.system(FontScale.Small),
                    modifier = Modifier.alpha(contentAlpha),
                )
                Components.Symbol(
                    "chevron.right",
                    color = colors.subtitleText,
                    modifier =
                        Modifier
                            .padding(start = ConversationCellViewFloats.chevronStartPadding)
                            .size(ConversationCellViewFloats.chevronSize),
                )
            }
            Material3Text(
                state.subtitleLabelText.ifBlank { " " },
                color = colors.subtitleText,
                style = Font.system(FontScale.Small).textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(contentAlpha),
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
    Box(modifier = Modifier.size(ConversationCellViewFloats.avatarSize)) {
        AvatarImageView(
            modifier = Modifier.fillMaxSize(),
            imageData = imageData,
            initials = if (!data.isGroup && data.hasContactName) data.initials else "",
            fallbackSymbol = if (data.isGroup) "person.2" else "person.crop.circle.fill",
        )

        if (data.isGroup) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(ConversationCellViewFloats.badgeSize)
                        .clip(CircleShape)
                        .background(colors.background)
                        .border(ConversationCellViewFloats.badgeBorderWidth, ConversationCellViewColors.badgeBorder, CircleShape),
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

private const val REDACTED_CONTENT_ALPHA = 0.5f
