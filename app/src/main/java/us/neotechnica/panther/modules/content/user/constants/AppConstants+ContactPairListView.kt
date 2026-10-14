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

object ContactPairListViewFloats {
    val defaultMinListRowHeight: Dp = 44.dp
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

object ContactPairListViewColors {
    val darkDivider = Color(0xFF3E3E41)
    val lightDivider = Color(0xFFC6C6C8)
    val rowDarkHighlight = Color(0xFF3A3A3C)
    val rowLightHighlight = Color(0xFFD1D1D6)
}
