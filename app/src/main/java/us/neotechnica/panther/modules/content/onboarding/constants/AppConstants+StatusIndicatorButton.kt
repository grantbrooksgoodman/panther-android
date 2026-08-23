//
//  AppConstants+StatusIndicatorButton.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object StatusIndicatorButtonFloats {
    val circleSize: Dp = 30.dp
    val glyphSize: Dp = 18.dp
    val horizontalPadding: Dp = 16.dp
    val iconTrailingPadding: Dp = 3.dp
    val verticalPadding: Dp = 10.dp

    const val LABEL_FONT_SIZE = 15f
}

// MARK: - Color

object StatusIndicatorButtonColors {
    val deniedStatusForeground = Color(0xFFFF3B30)
    val foreground = Color(0xFF007AFF)
    val grantedStatusForeground = Color(0xFF34C759)
    val undeterminedStatusForeground = Color(0xFFFF9500)
}
