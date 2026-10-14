//
//  AppConstants+StatusIndicatorButton.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/** The layout and timing values of the status indicator button. */
object StatusIndicatorButtonFloats {
    /** The frame height of the image. */
    val imageFrameHeight: Dp = 30.dp

    /** The frame width of the image. */
    val imageFrameWidth: Dp = 30.dp

    /** The trailing padding of the image. */
    val imageTrailingPadding: Dp = 3.dp

    /** The horizontal padding. */
    val horizontalPadding: Dp = 16.dp

    /** The vertical padding. */
    val verticalPadding: Dp = 10.dp

    /** The opacity of the button while disabled. */
    const val DISABLED_ALPHA = 0.5f

    /** The font size of the label. */
    const val LABEL_FONT_SIZE = 15f
}

// MARK: - Color

/** The colors of the status indicator button. */
object StatusIndicatorButtonColors {
    /** The secondary foreground color of the denied status image. */
    val deniedStatusImageSecondaryForeground = Color.Red

    /** The secondary foreground color of the granted status image. */
    val grantedStatusImageSecondaryForeground = Color.Green

    /** The secondary foreground color of the undetermined status image. */
    val undeterminedStatusImageSecondaryForeground = Color(0xFFFF9500)

    /** The foreground color of the determined status label. */
    val determinedStatusLabelForeground = Color.Gray

    /** The foreground color of the undetermined status label. */
    val undeterminedStatusLabelForeground = Color.White

    /** The foreground color of the button. */
    val foreground = Color.Blue
}

// MARK: - String

/** The string constants of the status indicator button. */
object StatusIndicatorButtonStrings {
    /** The system name of the denied status image. */
    const val DENIED_STATUS_IMAGE_SYSTEM_NAME = "x.circle.fill"

    /** The system name of the granted status image. */
    const val GRANTED_STATUS_IMAGE_SYSTEM_NAME = "checkmark.circle.fill"

    /** The system name of the undetermined status image. */
    const val UNDETERMINED_STATUS_IMAGE_SYSTEM_NAME = "questionmark.circle.fill"
}
