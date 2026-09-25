//
//  AppConstants+DeliveryProgressIndicator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object DeliveryProgressIndicatorFloats {
    const val ANIMATION_DELAY = 1f
    const val ANIMATION_DURATION = 0.2f
    const val APPEARANCE_TIMER_TIME_INTERVAL = 5f
    const val HIDDEN_TIMER_TIME_INTERVAL = 0.05f
    const val TIMER_PROGRESS_INCREMENT = 0.001f
    const val TIMER_PROGRESS_INCREMENT_THRESHOLD = 0.9f
    const val VISIBLE_TIMER_TIME_INTERVAL = 0.01f
    val viewFrameHeight: Dp = 2.dp
}

// MARK: - Color

object DeliveryProgressIndicatorColors {
    val progressBarTint = Color(0xFF007AFF)
}
