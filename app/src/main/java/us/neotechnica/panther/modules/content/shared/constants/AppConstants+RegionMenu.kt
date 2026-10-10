//
//  AppConstants+RegionMenu.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object RegionMenuFloats {
    val buttonLabelImageCornerRadius: Dp = 3.dp
    val buttonLabelImageFrameHeight: Dp = 25.dp
    val buttonLabelImageFrameWidth: Dp = 40.dp

    val buttonLabelVStackBackgroundRectangleCornerRadius: Dp = 6.dp
    val buttonLabelVStackShadowRadius: Dp = 2.dp

    val buttonLabelVStackFrameMinHeight: Dp = 80.dp
    val buttonLabelVStackFrameMinWidth: Dp = 45.dp

    val listViewCellLabelImageCornerRadius: Dp = 3.dp
    val listViewCellLabelImageFrameHeight: Dp = 25.dp
    val listViewCellLabelImageFrameWidth: Dp = 40.dp

    val listViewCellHorizontalPadding: Dp = 16.dp
    val listViewCellVerticalPadding: Dp = 12.dp

    val selectedCellImageLeadingPadding: Dp = 3.dp

    const val DELAY_MILLISECONDS = 500L
    const val SECONDARY_DELAY_MILLISECONDS = 200L
}

// MARK: - Color

object RegionMenuColors {
    val buttonLabelDarkForeground = Color(0xFF2A2A2C)
    val buttonLabelLightForeground = Color.White
    val buttonLabelTextForeground = Color(0xFF007AFF)

    val selectedCellImageForeground = Color.Green
}

// MARK: - String

object RegionMenuStrings {
    const val DEFAULT_CALLING_CODE = "1"
    const val SELECTED_CELL_IMAGE_SYSTEM_NAME = "checkmark.circle.fill"
}
