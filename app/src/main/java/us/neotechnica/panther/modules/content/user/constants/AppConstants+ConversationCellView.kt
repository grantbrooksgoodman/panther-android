//
//  AppConstants+ConversationCellView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object ConversationCellViewFloats {
    /** The spacing between the avatar and the text column. */
    val avatarContentSpacing: Dp = 8.dp
    val avatarImageViewTopPadding: Dp = 10.dp

    val chevronImageAndDateLabelHStackSpacing: Dp = 0.dp
    const val CHEVRON_IMAGE_FOREGROUND_COLOR_ADJUSTMENT_PERCENTAGE = 75f
    val chevronImageFrameMaxWidth: Dp = 12.dp
    val chevronImageFrameMaxHeight: Dp = 12.dp

    /** The leading offset of the long-press menu, aligned with the avatar. */
    val contextMenuLeadingOffset: Dp = 20.dp

    val dateLabelPaddingTrailing: Dp = 6.dp
    const val DATE_LABEL_SYSTEM_FONT_SIZE = 14f

    val frameHeight: Dp = 62.dp

    const val REDACTED_TITLE_LABEL_SYSTEM_FONT_SIZE = 20f

    /** The list row's leading inset. */
    val rowStartPadding: Dp = 20.dp

    /** The list row's trailing inset. */
    val rowEndPadding: Dp = 16.dp

    /** The leading inset of the separator between rows. */
    val separatorInset: Dp = 78.dp

    const val SUBTITLE_LABEL_FOREGROUND_COLOR_ADJUSTMENT_PERCENTAGE = 3f
    const val SUBTITLE_LABEL_LINE_LIMIT = 2
    const val SUBTITLE_LABEL_SYSTEM_FONT_SIZE = 14f
    val subtitleLabelXOffset: Dp = 1.5.dp
    val subtitleLabelYOffset: Dp = (-3).dp

    /** The swipe distance that reveals one swipe action button. */
    val swipeActionButtonWidth: Dp = 74.dp

    val titleLabelBottomPadding: Dp = 0.01.dp
    const val TITLE_LABEL_MINIMUM_SCALE_FACTOR = 0.01f
    const val TITLE_LABEL_SYSTEM_FONT_SIZE = 20f

    val unreadIndicatorViewFrameHeight: Dp = 10.dp
    val unreadIndicatorViewFrameWidth: Dp = 10.dp
    val unreadIndicatorViewTrailingPadding: Dp = (-15).dp
    val unreadIndicatorViewXOffset: Dp = (-10).dp
    val unreadIndicatorViewYOffset: Dp = 7.dp
}

// MARK: - Color

object ConversationCellViewColors {
    val blockUsersButtonImageTint = Color(0xFF8E8E93)
    val deleteConversationButtonImageTint = Color(0xFFFF3B30)
    val reportUsersButtonImageTint = Color(0xFFFF9500)
    val unreadIndicatorViewForeground = Color(0xFF007AFF)
}

// MARK: - String

object ConversationCellViewStrings {
    const val BLOCK_USERS_BUTTON_IMAGE_SYSTEM_NAME = "flag"
    const val CHEVRON_IMAGE_SYSTEM_NAME = "chevron.forward"
    const val DELETE_CONVERSATION_BUTTON_IMAGE_SYSTEM_NAME = "trash"
    const val REDACTED_DATE_LABEL_TEXT = "0/00/00"
    const val REDACTED_SUBTITLE_LABEL_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
    const val REPORT_USERS_BUTTON_IMAGE_SYSTEM_NAME = "exclamationmark.bubble"
}
