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

/** The layout and timing values of the splash page view. */
object SplashPageViewFloats {
    /** The scale factor of the activity indicator. */
    const val ACTIVITY_INDICATOR_SCALE_EFFECT = 0.8f

    /** The delay, in milliseconds, of the progress bar fade in. */
    const val PROGRESS_BAR_FADE_IN_DELAY_MILLISECONDS = 1750L

    /** The duration, in milliseconds, of the progress bar fade in. */
    @Suppress("VariableMaxLength")
    const val PROGRESS_BAR_FADE_IN_DURATION_MILLISECONDS = 500

    /** The frame height of the image. */
    val imageFrameHeight: Dp = 70.dp

    /** The frame width of the image. */
    val imageFrameWidth: Dp = 150.dp

    /** The padding of the page content. */
    val padding: Dp = 5.dp

    /** The horizontal padding of the progress bar. */
    val progressBarHorizontalPadding: Dp = 130.dp

    /** The top padding of the progress bar. */
    val progressBarTopPadding: Dp = 10.dp
}

// MARK: - Color

/** The colors of the splash page view. */
object SplashPageViewColors {
    /** The foreground color of the image in dark mode. */
    val imageDarkForeground = Color(0xFFF8F8F8)

    /** The progress bar's track color in dark mode. */
    val progressBarDarkTrackTint = Color(0xFF2C2C2E)

    /** The progress bar's track color in light mode. */
    val progressBarLightTrackTint = Color(0xFFE5E5EA)

    /** The tint color of the progress bar. */
    val progressBarTint: Color
        @Composable get() = LocalPantherColors.current.titleText
}

// MARK: - String

/** The string constants of the splash page view. */
object SplashPageViewStrings {
    /** The name of the GIF image resource. */
    const val GIF_IMAGE_NAME = "animated_logotype"
}
