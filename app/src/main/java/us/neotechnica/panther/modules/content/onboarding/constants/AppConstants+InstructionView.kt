//
//  AppConstants+InstructionView.kt
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

/** The layout and timing values of the instruction view. */
object InstructionViewFloats {
    /** The maximum height of the frame. */
    val frameMaxHeight: Dp = 220.dp

    /** The leading padding. */
    val leadingPadding: Dp = 20.dp

    /** The bottom padding of the title label. */
    val titleLabelBottomPadding: Dp = 2.dp

    /** The top padding. */
    val topPadding: Dp = 15.dp

    /** The divisor applied to the screen width to size the view. */
    const val SCREEN_WIDTH_DIVISOR = 2

    /** The font size of the subtitle label. */
    const val SUBTITLE_LABEL_FONT_SIZE = 14f

    /** The minimum scale factor of the subtitle label. */
    const val SUBTITLE_LABEL_MINIMUM_SCALE_FACTOR = 0.01f

    /** The minimum scale factor of the title label. */
    const val TITLE_LABEL_MINIMUM_SCALE_FACTOR = 0.01f
}

// MARK: - Color

/** The colors of the instruction view. */
object InstructionViewColors {
    /** The foreground color of the subtitle label. */
    val subtitleLabelForeground = Color.Gray
}
