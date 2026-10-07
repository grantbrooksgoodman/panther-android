//
//  FoundationConstants+HeaderView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

object HeaderViewFloats {
    const val BACK_BUTTON_IMAGE_SIZE_HEIGHT = 20f
    const val BACK_BUTTON_IMAGE_SIZE_WIDTH = 20f

    const val FULL_SCREEN_COVER_SIZE_CLASS_FRAME_MIN_HEIGHT = 44f

    const val HORIZONTAL_PADDING = 16f

    const val IMAGE_MAX_HEIGHT = 30f

    const val LONG_CENTER_ITEM_TEXT_CHARACTER_COUNT_THRESHOLD = 20f
    const val LONG_CENTER_ITEM_TEXT_LINE_LIMIT = 2f

    const val MAIN_WINDOW_SIZE_WIDTH_DIVISOR = 3f

    const val SEPARATOR_MAX_HEIGHT = 0.3f

    const val SHEET_SIZE_CLASS_FRAME_MIN_HEIGHT = 54f

    const val TEXT_MINIMUM_SCALE_FACTOR = 0.5f
}

// MARK: - Color

object HeaderViewColors {
    val SEPARATOR_DARK_FOREGROUND = Color(0xFF48484A)
    val SEPARATOR_LIGHT_FOREGROUND = Color(0xFFA3A3A3)
}

// MARK: - String

object HeaderViewStrings {
    const val BACK_BUTTON_IMAGE_SYSTEM_NAME = "chevron.backward"
}
