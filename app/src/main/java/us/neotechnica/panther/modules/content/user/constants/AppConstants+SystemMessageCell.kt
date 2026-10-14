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

/** The layout and timing values of the system message cell. */
object SystemMessageCellFloats {
    /** The font size of the activity text. */
    const val ACTIVITY_STRING_SYSTEM_FONT_SIZE = 12f

    /** The minimum scale factor of the label. */
    const val LABEL_MINIMUM_SCALE_FACTOR = 0.9f

    /** The maximum number of lines of the label. */
    const val LABEL_NUMBER_OF_LINES = 3

    /** The additional vertical padding. */
    val additionalVerticalPadding: Dp = 10.dp

    /** The default height of the cell. */
    val defaultHeight: Dp = 44.dp

    /** The line spacing of the label. */
    val labelParagraphStyleLineSpacing: Dp = 4.dp
}

// MARK: - Color

/** The colors of the system message cell. */
object SystemMessageCellColors {
    /** The foreground color of the activity text in dark mode. */
    val activityStringDarkForeground = Color.LightGray

    /** The foreground color of the activity text in light mode. */
    val activityStringLightForeground = Color(0xFF8E8E93)

    /**
     * The activity text color for the given appearance.
     *
     * @param isDarkModeActive Whether dark mode is active.
     */
    fun activityStringForeground(isDarkModeActive: Boolean): Color =
        if (isDarkModeActive) activityStringDarkForeground else activityStringLightForeground
}
