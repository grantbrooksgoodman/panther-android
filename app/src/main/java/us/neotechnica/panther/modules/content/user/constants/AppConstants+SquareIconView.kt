//
//  AppConstants+SquareIconView.kt
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

object SquareIconViewFloats {
    val cornerRadius: Dp = 30.dp

    val defaultFrameHeight: Dp = 150.dp
    val defaultFrameWidth: Dp = 150.dp

    const val OVERLAY_FRAME_HEIGHT_MULTIPLIER = 2f / 3f
    const val OVERLAY_FRAME_WIDTH_MULTIPLIER = 2f / 3f
    const val OVERLAY_TEXT_FONT_SCALE = 60f

    const val SHADOW_COLOR_OPACITY = 0.2f
    val shadowRadius: Dp = 10.dp
    val shadowYOffset: Dp = 5.dp
}

// MARK: - Color

object SquareIconViewColors {
    val overlaySymbolForeground = Color.White
    val shadow = Color.Black
}
