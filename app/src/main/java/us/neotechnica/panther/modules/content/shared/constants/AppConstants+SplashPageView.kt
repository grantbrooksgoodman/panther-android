//
//  AppConstants+SplashPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 27/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object SplashPageViewFloats {
    const val ACTIVITY_INDICATOR_SCALE_EFFECT = 0.8f
    const val PROGRESS_BAR_FADE_IN_DELAY_MILLISECONDS = 1750L

    val imageFrameHeight: Dp = 70.dp
    val imageFrameWidth: Dp = 150.dp

    val padding: Dp = 5.dp
    val progressBarHorizontalPadding: Dp = 130.dp
    val progressBarTopPadding: Dp = 10.dp
}

// MARK: - Color

object SplashPageViewColors {
    val imageDarkForeground = Color(0xFFF8F8F8)
}

// MARK: - String

object SplashPageViewStrings {
    const val GIF_IMAGE_NAME = "animated_logotype"
}
