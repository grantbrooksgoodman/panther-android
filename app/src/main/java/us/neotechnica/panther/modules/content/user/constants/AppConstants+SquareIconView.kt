//
//  AppConstants+SquareIconView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object SquareIconViewFloats {
    val defaultFrameHeight: Dp = 150.dp
    val defaultFrameWidth: Dp = 150.dp

    val shadowRadius: Dp = 10.dp

    const val CORNER_RADIUS_RATIO = 30f / 150f
    const val OVERLAY_FRAME_HEIGHT_MULTIPLIER = 2f / 3f
    const val OVERLAY_TEXT_FONT_SCALE = 60f
}

// MARK: - Color

object SquareIconViewColors {
    val overlaySymbolForeground = Color.White
}
