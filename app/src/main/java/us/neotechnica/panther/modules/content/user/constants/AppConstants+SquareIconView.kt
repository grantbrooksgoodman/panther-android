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

/** The layout and timing values of the square icon view. */
object SquareIconViewFloats {
    /** The corner radius of the icon. */
    val cornerRadius: Dp = 30.dp

    /** The default height of the icon. */
    val defaultFrameHeight: Dp = 150.dp

    /** The default width of the icon. */
    val defaultFrameWidth: Dp = 150.dp

    /** The fraction of the icon's height the overlay occupies. */
    const val OVERLAY_FRAME_HEIGHT_MULTIPLIER = 2f / 3f

    /** The fraction of the icon's width the overlay occupies. */
    const val OVERLAY_FRAME_WIDTH_MULTIPLIER = 2f / 3f

    /** The font size of a text overlay. */
    const val OVERLAY_TEXT_FONT_SCALE = 60f

    /** The opacity of the shadow. */
    const val SHADOW_COLOR_OPACITY = 0.2f

    /** The blur radius of the shadow. */
    val shadowRadius: Dp = 10.dp

    /** The vertical offset of the shadow. */
    val shadowYOffset: Dp = 5.dp
}

// MARK: - Color

/** The colors of the square icon view. */
object SquareIconViewColors {
    /** The foreground color of the overlay symbol. */
    val overlaySymbolForeground = Color.White

    /** The shadow color. */
    val shadow = Color.Black
}
