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

/** The layout and timing values of the conversation cell view. */
object ConversationCellViewFloats {
    /** The top padding of the avatar image view. */
    val avatarImageViewTopPadding: Dp = 10.dp

    /** The spacing between the chevron image and the date label. */
    val chevronImageAndDateLabelHStackSpacing: Dp = 0.dp

    /** The percentage by which the chevron image's foreground color is adjusted. */
    @Suppress("VariableMaxLength")
    const val CHEVRON_IMAGE_FOREGROUND_COLOR_ADJUSTMENT_PERCENTAGE = 75f

    /** The maximum frame width of the chevron image. */
    val chevronImageFrameMaxWidth: Dp = 12.dp

    /** The maximum frame height of the chevron image. */
    val chevronImageFrameMaxHeight: Dp = 12.dp

    /** The leading offset of the long-press menu, aligned with the avatar. */
    val contextMenuLeadingOffset: Dp = 28.dp

    /** The trailing padding of the date label. */
    val dateLabelPaddingTrailing: Dp = 6.dp

    /** The font size of the date label. */
    const val DATE_LABEL_SYSTEM_FONT_SIZE = 14f

    /** The height of the cell. */
    val frameHeight: Dp = 62.dp

    /** The font size of the redacted title label. */
    const val REDACTED_TITLE_LABEL_SYSTEM_FONT_SIZE = 20f

    /** The list row's leading inset. */
    val rowStartPadding: Dp = 20.dp

    /** The list row's trailing inset. */
    val rowEndPadding: Dp = 20.dp

    /** The list row's top and bottom inset. */
    val rowVerticalPadding: Dp = 14.5.dp

    /** The leading inset of the separator between rows, aligned with the title. */
    val separatorInset: Dp = 86.dp

    /** The percentage by which the subtitle label's foreground color is adjusted. */
    @Suppress("VariableMaxLength")
    const val SUBTITLE_LABEL_FOREGROUND_COLOR_ADJUSTMENT_PERCENTAGE = 3f

    /** The line limit of the subtitle label. */
    const val SUBTITLE_LABEL_LINE_LIMIT = 2

    /** The font size of the subtitle label. */
    const val SUBTITLE_LABEL_SYSTEM_FONT_SIZE = 14f

    /** The horizontal offset of the subtitle label. */
    val subtitleLabelXOffset: Dp = 1.5.dp

    /** The vertical offset of the subtitle label. */
    val subtitleLabelYOffset: Dp = (-3).dp

    /** The swipe distance that reveals one swipe action button. */
    val swipeActionButtonWidth: Dp = 74.dp

    /** The bottom padding of the title label. */
    val titleLabelBottomPadding: Dp = 0.01.dp

    /** The minimum scale factor of the title label. */
    const val TITLE_LABEL_MINIMUM_SCALE_FACTOR = 0.01f

    /** The font size of the title label. */
    const val TITLE_LABEL_SYSTEM_FONT_SIZE = 17f

    /** The frame height of the unread indicator view. */
    val unreadIndicatorViewFrameHeight: Dp = 10.dp

    /** The frame width of the unread indicator view. */
    val unreadIndicatorViewFrameWidth: Dp = 10.dp

    /** The trailing padding of the unread indicator view. */
    val unreadIndicatorViewTrailingPadding: Dp = (-15).dp

    /** The horizontal offset of the unread indicator view. */
    val unreadIndicatorViewXOffset: Dp = (-10).dp

    /** The vertical offset of the unread indicator view. */
    val unreadIndicatorViewYOffset: Dp = 7.dp
}

// MARK: - Color

/** The colors of the conversation cell view. */
object ConversationCellViewColors {
    /** The tint color of the block users button image. */
    val blockUsersButtonImageTint = Color(0xFF8E8E93)

    /** The tint color of the delete conversation button image. */
    val deleteConversationButtonImageTint = Color(0xFFFF3B30)

    /** The tint color of the report users button image. */
    val reportUsersButtonImageTint = Color(0xFFFF9500)

    /** The foreground color of the unread indicator view. */
    val unreadIndicatorViewForeground = Color(0xFF007AFF)
}

// MARK: - String

/** The string constants of the conversation cell view. */
object ConversationCellViewStrings {
    /** The system name of the block users button image. */
    const val BLOCK_USERS_BUTTON_IMAGE_SYSTEM_NAME = "flag"

    /** The system name of the chevron image. */
    const val CHEVRON_IMAGE_SYSTEM_NAME = "chevron.forward"

    /** The system name of the delete conversation button image. */
    @Suppress("VariableMaxLength")
    const val DELETE_CONVERSATION_BUTTON_IMAGE_SYSTEM_NAME = "trash"

    /** The text of the redacted date label. */
    const val REDACTED_DATE_LABEL_TEXT = "0/00/00"

    /** The text of the redacted subtitle label. */
    const val REDACTED_SUBTITLE_LABEL_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

    /** The system name of the report users button image. */
    const val REPORT_USERS_BUTTON_IMAGE_SYSTEM_NAME = "exclamationmark.bubble"
}
