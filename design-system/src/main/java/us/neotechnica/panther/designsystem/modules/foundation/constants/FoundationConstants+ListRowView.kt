//
//  FoundationConstants+ListRowView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The layout and timing values of the list row view. */
object ListRowViewFloats {
    /** The maximum frame height of the chevron image. */
    const val CHEVRON_IMAGE_FRAME_MAX_HEIGHT = 14f

    /** The maximum frame width of the chevron image. */
    const val CHEVRON_IMAGE_FRAME_MAX_WIDTH = 14f

    /** The horizontal padding of the footer label. */
    const val FOOTER_LABEL_HORIZONTAL_PADDING = 16f

    /** The font size of the footer label. */
    const val FOOTER_LABEL_SYSTEM_FONT_SCALE = 13.5f

    /** The minimum height of the frame. */
    const val FRAME_MIN_HEIGHT = 44f

    /** The horizontal padding of the header label. */
    const val HEADER_LABEL_HORIZONTAL_PADDING = 16f

    /** The font size of the header label. */
    const val HEADER_LABEL_SYSTEM_FONT_SCALE = 13.5f

    /** The frame height of the image. */
    const val IMAGE_FRAME_HEIGHT = 30f

    /** The frame width of the image. */
    const val IMAGE_FRAME_WIDTH = 30f

    /** The leading padding of the image. */
    const val IMAGE_LEADING_PADDING = 3f

    /** The leading padding of the title label. */
    const val TITLE_LABEL_LEADING_PADDING = 5f

    /** The vertical padding. */
    const val VERTICAL_PADDING = 8f
}

// MARK: - Color

/** The colors of the list row view. */
object ListRowViewColors {
    /** The background color of a button row in dark mode. */
    val BUTTON_STYLE_DARK_NOT_PRESSED_BACKGROUND = Color(0xFF2A2A2C)

    /** The background color of a pressed button row in dark mode. */
    val BUTTON_STYLE_DARK_PRESSED_BACKGROUND = Color(0xFF3A3A3C)

    /** The background color of a button row in light mode. */
    @Suppress("VariableMaxLength")
    val BUTTON_STYLE_LIGHT_NOT_PRESSED_BACKGROUND = Color.White

    /** The background color of a pressed button row in light mode. */
    val BUTTON_STYLE_LIGHT_PRESSED_BACKGROUND = Color(0xFFD1D1D6)

    /** The background color in dark mode. */
    val DARK_BACKGROUND = Color(0xFF2A2A2C)

    /** The background color in light mode. */
    val LIGHT_BACKGROUND = Color.White

    /** The foreground color of the title label while disabled. */
    val TITLE_LABEL_DISABLED_FOREGROUND = Color.Gray
}

// MARK: - String

/** The string constants of the list row view. */
object ListRowViewStrings {
    /** The system name of the chevron image. */
    const val CHEVRON_IMAGE_SYSTEM_NAME = "chevron.forward"
}
