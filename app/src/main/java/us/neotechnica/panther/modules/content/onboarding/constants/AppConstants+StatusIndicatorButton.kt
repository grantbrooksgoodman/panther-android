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

object StatusIndicatorButtonFloats {
    val imageFrameHeight: Dp = 30.dp
    val imageFrameWidth: Dp = 30.dp

    val imageTrailingPadding: Dp = 3.dp

    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 10.dp

    const val DISABLED_ALPHA = 0.5f
    const val LABEL_FONT_SIZE = 15f
}

// MARK: - Color

object StatusIndicatorButtonColors {
    val deniedStatusImageSecondaryForeground = Color.Red
    val grantedStatusImageSecondaryForeground = Color.Green
    val undeterminedStatusImageSecondaryForeground = Color(0xFFFF9500)

    val determinedStatusLabelForeground = Color.Gray
    val undeterminedStatusLabelForeground = Color.White

    val foreground = Color.Blue
}

// MARK: - String

object StatusIndicatorButtonStrings {
    const val DENIED_STATUS_IMAGE_SYSTEM_NAME = "x.circle.fill"
    const val GRANTED_STATUS_IMAGE_SYSTEM_NAME = "checkmark.circle.fill"
    const val UNDETERMINED_STATUS_IMAGE_SYSTEM_NAME = "questionmark.circle.fill"
}
