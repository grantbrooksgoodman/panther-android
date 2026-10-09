//
//  AppConstants+NetworkActivityIndicator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

internal object NetworkActivityIndicatorFloats {
    const val FRAME_HEIGHT = 40f
    const val FRAME_WIDTH = 40f

    const val HIDDEN_Y_OFFSET = -1000f
    const val HIDE_IF_INACTIVE_TASK_DELAY_SECONDS = 2.0
    const val HIDE_INDICATOR_TASK_DELAY_SECONDS = 1.25

    const val PADDING = 5f
}

// MARK: - Color

internal object NetworkActivityIndicatorColors {
    val GLASS_EFFECT_TINT = Color(0xFF007AFF)
}
