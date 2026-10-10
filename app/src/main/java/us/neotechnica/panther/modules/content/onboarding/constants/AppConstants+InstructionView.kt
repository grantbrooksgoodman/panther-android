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

object InstructionViewFloats {
    val frameMaxHeight: Dp = 220.dp
    val leadingPadding: Dp = 20.dp

    val titleLabelBottomPadding: Dp = 2.dp

    val topPadding: Dp = 15.dp

    const val SCREEN_WIDTH_DIVISOR = 2
    const val SUBTITLE_LABEL_FONT_SIZE = 14f
    const val SUBTITLE_LABEL_MINIMUM_SCALE_FACTOR = 0.01f
    const val TITLE_LABEL_MINIMUM_SCALE_FACTOR = 0.01f
}

// MARK: - Color

object InstructionViewColors {
    val subtitleLabelForeground = Color.Gray
}
