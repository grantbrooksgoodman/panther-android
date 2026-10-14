//
//  AppConstants+SearchBar.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The layout and timing values of the search bar. */
object SearchBarFloats {
    /** The spacing between the field and the Cancel button. */
    const val CANCEL_BUTTON_LEADING_PADDING = 11f

    /** The opacity of the clear button image. */
    const val CLEAR_BUTTON_IMAGE_OPACITY = 1f

    /** The default bottom padding. */
    const val DEFAULT_BOTTOM_PADDING = 8f

    /** The default horizontal padding. */
    const val DEFAULT_HORIZONTAL_PADDING = 16f

    /** The corner radius of the inner row. */
    const val INNER_ROW_CORNER_RADIUS = 10f

    /** The horizontal padding of the inner row. */
    const val INNER_ROW_HORIZONTAL_PADDING = 8f

    /** The frame height of the text field. */
    const val TEXT_FIELD_FRAME_HEIGHT = 36f

    /** The minimum scale factor of the text field. */
    const val TEXT_FIELD_MINIMUM_SCALE_FACTOR = 0.5f
}

// MARK: - Color

/** The colors of the search bar. */
object SearchBarColors {
    /** The background color of the inner row in dark mode. */
    val INNER_ROW_DARK_BACKGROUND = Color(0xFF3B3A3F)

    /** The background color of the inner row in light mode. */
    val INNER_ROW_LIGHT_BACKGROUND = Color(0xFFE7E7E9)
}

// MARK: - String

/** The string constants of the search bar. */
object SearchBarStrings {
    /** The system name of the clear button image. */
    const val CLEAR_BUTTON_IMAGE_SYSTEM_NAME = "xmark.circle.fill"

    /** The system name of the search image. */
    const val SEARCH_IMAGE_SYSTEM_NAME = "magnifyingglass"
}
