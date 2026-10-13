//
//  AppConstants+SplashPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.constants

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Float

object SplashPageViewFloats {
    const val ACTIVITY_INDICATOR_SCALE_EFFECT = 0.8f
    const val PROGRESS_BAR_FADE_IN_DELAY_MILLISECONDS = 1750L
    const val PROGRESS_BAR_FADE_IN_DURATION_MILLISECONDS = 500

    val imageFrameHeight: Dp = 70.dp
    val imageFrameWidth: Dp = 150.dp

    val padding: Dp = 5.dp
    val progressBarHorizontalPadding: Dp = 130.dp
    val progressBarTopPadding: Dp = 10.dp
}

// MARK: - Color

object SplashPageViewColors {
    val imageDarkForeground = Color(0xFFF8F8F8)

    /** The progress bar's track color in dark mode. */
    val progressBarDarkTrackTint = Color(0xFF2C2C2E)

    /** The progress bar's track color in light mode. */
    val progressBarLightTrackTint = Color(0xFFE5E5EA)

    val progressBarTint: Color
        @Composable get() = LocalPantherColors.current.titleText
}

// MARK: - String

object SplashPageViewStrings {
    const val GIF_IMAGE_NAME = "animated_logotype"
}
