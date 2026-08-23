//
//  AppConstants+WheelPicker.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object WheelPickerFloats {
    val pillCornerRadius: Dp = 10.dp
    val rowHeight: Dp = 40.dp

    const val MIN_ROW_ALPHA = 0.12f
    const val VISIBLE_ROW_COUNT = 7
    const val HALF_ROW_COUNT = VISIBLE_ROW_COUNT / 2
}
