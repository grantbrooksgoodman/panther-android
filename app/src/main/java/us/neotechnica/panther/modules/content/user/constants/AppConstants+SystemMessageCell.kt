//
//  AppConstants+SystemMessageCell.kt
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

object SystemMessageCellFloats {
    const val ACTIVITY_STRING_SYSTEM_FONT_SIZE = 12f
    const val LABEL_MINIMUM_SCALE_FACTOR = 0.9f
    const val LABEL_NUMBER_OF_LINES = 3

    val additionalVerticalPadding: Dp = 10.dp
    val defaultHeight: Dp = 44.dp
    val labelParagraphStyleLineSpacing: Dp = 4.dp
}

// MARK: - Color

object SystemMessageCellColors {
    val activityStringDarkForeground = Color.LightGray
    val activityStringLightForeground = Color(0xFF8E8E93)

    /**
     * The activity text color for the given appearance.
     *
     * @param isDarkModeActive Whether dark mode is active.
     */
    fun activityStringForeground(isDarkModeActive: Boolean): Color =
        if (isDarkModeActive) activityStringDarkForeground else activityStringLightForeground
}
