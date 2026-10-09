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
    val avatarSize: Dp = 48.dp
    val badgeBorderWidth: Dp = 1.dp
    val badgeSize: Dp = 18.dp
    val chevronSize: Dp = 14.dp
    val chevronStartPadding: Dp = 4.dp
    val dateSpacerWidth: Dp = 8.dp
    val languageChipStartPadding: Dp = 6.dp
    val rowBottomPadding: Dp = 14.dp
    val rowEndPadding: Dp = 16.dp
    val rowStartPadding: Dp = 12.dp
    val rowTopPadding: Dp = 14.dp
    val subtitleSpacing: Dp = 3.dp
    val textInset: Dp = 92.dp
    val titleAvatarSpacing: Dp = 12.dp
    val unreadIndicatorSize: Dp = 9.dp
    val unreadSlotWidth: Dp = 20.dp
}

// MARK: - Color

object ConversationCellViewColors {
    val badgeBorder = Color(0xFFC7C7CC)
}

// MARK: - String

object ConversationCellViewStrings {
    const val BLOCK_USERS_BUTTON_IMAGE_SYSTEM_NAME = "flag"
    const val DELETE_CONVERSATION_BUTTON_IMAGE_SYSTEM_NAME = "trash"
    const val REDACTED_DATE_LABEL_TEXT = "0/00/00"
    const val REDACTED_SUBTITLE_LABEL_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
    const val REPORT_USERS_BUTTON_IMAGE_SYSTEM_NAME = "exclamationmark.bubble"
}
