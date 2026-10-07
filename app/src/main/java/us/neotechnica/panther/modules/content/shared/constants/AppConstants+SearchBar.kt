//
//  AppConstants+SearchBar.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

object SearchBarFloats {
    const val CLEAR_BUTTON_IMAGE_OPACITY = 1f

    const val DEFAULT_BOTTOM_PADDING = 8f

    const val INNER_ROW_CORNER_RADIUS = 10f
    const val INNER_ROW_HORIZONTAL_PADDING = 8f

    const val TEXT_FIELD_FRAME_HEIGHT = 36f
    const val TEXT_FIELD_MINIMUM_SCALE_FACTOR = 0.5f
}

// MARK: - Color

object SearchBarColors {
    val INNER_ROW_DARK_BACKGROUND = Color(0xFF3B3A3F)
    val INNER_ROW_LIGHT_BACKGROUND = Color(0xFFE7E7E9)
}

// MARK: - String

object SearchBarStrings {
    const val CLEAR_BUTTON_IMAGE_SYSTEM_NAME = "xmark.circle.fill"
    const val SEARCH_IMAGE_SYSTEM_NAME = "magnifyingglass"
}
