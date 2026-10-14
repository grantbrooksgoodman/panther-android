//
//  FoundationConstants+GroupedListView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The layout and timing values of the grouped list view. */
object GroupedListViewFloats {
    /** The horizontal margin of the card. */
    const val CARD_HORIZONTAL_MARGIN = 16f

    /** The corner radius. */
    const val CORNER_RADIUS = 10f

    /** The alternate leading padding of a divider. */
    const val DIVIDER_ALTERNATE_LEADING_PADDING = 60f

    /** The leading padding of the divider. */
    const val DIVIDER_LEADING_PADDING = 20f

    /** The horizontal padding of the footer label. */
    const val FOOTER_LABEL_HORIZONTAL_PADDING = 16f

    /** The font size of the footer label. */
    const val FOOTER_LABEL_SYSTEM_FONT_SCALE = 13.5f

    /** The horizontal padding of the header label. */
    const val HEADER_LABEL_HORIZONTAL_PADDING = 16f

    /** The font size of the header label. */
    const val HEADER_LABEL_SYSTEM_FONT_SCALE = 13.5f

    /** The vertical padding of the list. */
    const val LIST_VERTICAL_PADDING = 35f

    /** The horizontal padding of the row. */
    const val ROW_HORIZONTAL_PADDING = 16f

    /** The minimum height of the row. */
    const val ROW_MIN_HEIGHT = 44f

    /** The vertical padding of the row. */
    const val ROW_VERTICAL_PADDING = 8f
}

// MARK: - Color

/** The colors of the grouped list view. */
object GroupedListViewColors {
    /** The divider color in dark mode. */
    val DARK_DIVIDER = Color(0xFF47474A)

    /** The divider color in light mode. */
    val LIGHT_DIVIDER = Color(0xFFC6C6C8)
}
