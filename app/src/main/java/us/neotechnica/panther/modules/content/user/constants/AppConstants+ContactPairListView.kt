//
//  AppConstants+ContactPairListView.kt
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

/** The layout and timing values of the contact pair list view. */
object ContactPairListViewFloats {
    /** The default minimum height of a list row. */
    val defaultMinListRowHeight: Dp = 44.dp

    /** The horizontal padding of the row. */
    val rowHorizontalPadding: Dp = 20.dp

    /**
     * The height of a section header: the line height of its
     * 17-point label plus 4-point margins above and below.
     */
    val sectionHeaderFrameHeight: Dp = 28.33.dp

    /** The spacing above each section header. */
    val sectionHeaderTopPadding: Dp = 22.dp
}

// MARK: - Color

/** The colors of the contact pair list view. */
object ContactPairListViewColors {
    /** The divider color in dark mode. */
    val darkDivider = Color(0xFF3E3E41)

    /** The divider color in light mode. */
    val lightDivider = Color(0xFFC6C6C8)

    /** The highlight color of the row in dark mode. */
    val rowDarkHighlight = Color(0xFF3A3A3C)

    /** The highlight color of the row in light mode. */
    val rowLightHighlight = Color(0xFFD1D1D6)
}
