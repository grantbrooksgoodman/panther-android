//
//  AppConstants+ChatMessageCell.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object ChatMessageCellFloats {
    val bottomLabelEndPadding: Dp = 4.dp
    val bottomLabelStartPadding: Dp = 4.dp
    val bottomLabelTopPadding: Dp = 2.dp
    val bubbleHorizontalPadding: Dp = 14.dp
    val bubbleMaxWidth: Dp = 280.dp
    val bubbleRadius: Dp = 18.dp
    val bubbleTailRadius: Dp = 4.dp
    val bubbleVerticalPadding: Dp = 9.dp
    val rowHorizontalPadding: Dp = 12.dp
    val rowVerticalPadding: Dp = 2.dp
    val senderAvatarGlyphSize: Dp = 18.dp
    val senderAvatarSize: Dp = 30.dp
    val senderAvatarSpacing: Dp = 6.dp
    val senderNameBottomPadding: Dp = 2.dp
    val senderNameStartPadding: Dp = 12.dp
    val separatorVerticalPadding: Dp = 8.dp

    const val DAYS_IN_WEEK = 7
    const val DAY_SEPARATOR_GAP_MILLIS = 5_400_000L
    const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    const val REACTION_FONT_SIZE = 14f
}

// MARK: - Color

object ChatMessageCellColors {
    val error = Color(0xFFFF3B30)
}

// MARK: - String

object ChatMessageCellStrings {
    const val DAY_OF_WEEK_FORMAT = "EEEE"
    const val FULL_DATE_FORMAT = "MMM d, yyyy"
    const val STATUS_SEPARATOR = " | "
    const val TIME_FORMAT = "h:mm a"
}
