//
//  AppConstants+WheelPicker.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/** The layout and timing values of the wheel picker. */
object WheelPickerFloats {
    /** The corner radius of the pill. */
    val pillCornerRadius: Dp = 10.dp

    /** The height of the row. */
    val rowHeight: Dp = 40.dp

    /** The minimum opacity of a row, reached farthest from the selection. */
    const val MIN_ROW_ALPHA = 0.12f

    /** The opacity of the selection pill. */
    const val SELECTION_PILL_ALPHA = 0.4f

    /** The number of visible rows. */
    const val VISIBLE_ROW_COUNT = 7

    /** The number of visible rows above or below the selection. */
    const val HALF_ROW_COUNT = VISIBLE_ROW_COUNT / 2
}
