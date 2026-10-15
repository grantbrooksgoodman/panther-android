//
//  FoundationConstants+HeaderView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The layout and timing values of the header view. */
object HeaderViewFloats {
    /** The height of the back button image. */
    const val BACK_BUTTON_IMAGE_SIZE_HEIGHT = 20.5f

    /** The width of the back button image. */
    const val BACK_BUTTON_IMAGE_SIZE_WIDTH = 12f

    /** The minimum height of a full-screen-cover header. */
    @Suppress("VariableMaxLength")
    const val FULL_SCREEN_COVER_SIZE_CLASS_FRAME_MIN_HEIGHT = 44f

    /** The horizontal padding. */
    const val HORIZONTAL_PADDING = 16f

    /** The maximum height of the image. */
    const val IMAGE_MAX_HEIGHT = 30f

    /** The character count at which the center text counts as long. */
    @Suppress("VariableMaxLength")
    const val LONG_CENTER_ITEM_TEXT_CHARACTER_COUNT_THRESHOLD = 20f

    /** The line limit of long center text. */
    const val LONG_CENTER_ITEM_TEXT_LINE_LIMIT = 2f

    /** The divisor applied to the window width to size the center item. */
    const val MAIN_WINDOW_SIZE_WIDTH_DIVISOR = 3f

    /** The maximum height of the separator. */
    const val SEPARATOR_MAX_HEIGHT = 0.3f

    /** The minimum height of a sheet header. */
    const val SHEET_SIZE_CLASS_FRAME_MIN_HEIGHT = 54f

    /** The minimum scale factor of the text. */
    const val TEXT_MINIMUM_SCALE_FACTOR = 0.5f
}

// MARK: - Color

/** The colors of the header view. */
object HeaderViewColors {
    /** The foreground color of the separator in dark mode. */
    val SEPARATOR_DARK_FOREGROUND = Color(0xFF48484A)

    /** The foreground color of the separator in light mode. */
    val SEPARATOR_LIGHT_FOREGROUND = Color(0xFFA3A3A3)
}

// MARK: - String

/** The string constants of the header view. */
object HeaderViewStrings {
    /** The system name of the back button image. */
    const val BACK_BUTTON_IMAGE_SYSTEM_NAME = "chevron.backward"
}
