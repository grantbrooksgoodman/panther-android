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

/** The layout and timing values of the region menu. */
object RegionMenuFloats {
    /** The corner radius of the button label image. */
    val buttonLabelImageCornerRadius: Dp = 3.dp

    /** The frame height of the button label image. */
    val buttonLabelImageFrameHeight: Dp = 25.dp

    /** The frame width of the button label image. */
    val buttonLabelImageFrameWidth: Dp = 40.dp

    /** The corner radius of the button label stack's background. */
    @Suppress("VariableMaxLength")
    val buttonLabelVStackBackgroundRectangleCornerRadius: Dp = 6.dp

    /** The blur radius of the button label stack's shadow. */
    val buttonLabelVStackShadowRadius: Dp = 2.dp

    /** The minimum frame height of the button label stack. */
    val buttonLabelVStackFrameMinHeight: Dp = 80.dp

    /** The minimum frame width of the button label stack. */
    val buttonLabelVStackFrameMinWidth: Dp = 45.dp

    /** The corner radius of the list view cell label image. */
    val listViewCellLabelImageCornerRadius: Dp = 3.dp

    /** The frame height of the list view cell label image. */
    val listViewCellLabelImageFrameHeight: Dp = 25.dp

    /** The frame width of the list view cell label image. */
    val listViewCellLabelImageFrameWidth: Dp = 40.dp

    /** The horizontal padding of the list view cell. */
    val listViewCellHorizontalPadding: Dp = 16.dp

    /** The leading padding of the selected cell image. */
    val selectedCellImageLeadingPadding: Dp = 3.dp

    /** The delay, in milliseconds, before the menu reacts to a presentation change. */
    const val DELAY_MILLISECONDS = 500L

    /** The secondary delay, in milliseconds. */
    const val SECONDARY_DELAY_MILLISECONDS = 200L
}

// MARK: - Color

/** The colors of the region menu. */
object RegionMenuColors {
    /** The foreground color of the button label in dark mode. */
    val buttonLabelDarkForeground = Color(0xFF2A2A2C)

    /** The foreground color of the button label in light mode. */
    val buttonLabelLightForeground = Color.White

    /** The foreground color of the button label's text. */
    val buttonLabelTextForeground = Color(0xFF007AFF)

    /** The foreground color of the selected cell image. */
    val selectedCellImageForeground = Color.Green
}

// MARK: - String

/** The string constants of the region menu. */
object RegionMenuStrings {
    /** The default calling code. */
    const val DEFAULT_CALLING_CODE = "1"

    /** The system name of the selected cell image. */
    const val SELECTED_CELL_IMAGE_SYSTEM_NAME = "checkmark.circle.fill"
}
