//
//  AppConstants+WelcomePageView.kt
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

/** The layout and timing values of the welcome page view. */
object WelcomePageViewFloats {
    /** The vertical padding of the continue button. */
    val continueButtonVerticalPadding: Dp = 5.dp

    /** The bottom padding of the image. */
    val imageBottomPadding: Dp = 5.dp

    /** The frame height of the image. */
    val imageFrameHeight: Dp = 70.dp

    /** The frame width of the image. */
    val imageFrameWidth: Dp = 150.dp

    /** The horizontal padding of the instruction label. */
    val instructionLabelHorizontalPadding: Dp = 30.dp

    /** The vertical padding of the instruction label. */
    val instructionLabelVerticalPadding: Dp = 5.dp

    /** The font size of the sign in button label. */
    const val SIGN_IN_BUTTON_LABEL_FONT_SIZE = 15f
}

// MARK: - Color

/** The colors of the welcome page view. */
object WelcomePageViewColors {
    /** The foreground color of the image in dark mode. */
    val imageDarkForeground = Color(0xFFF8F8F8)
}
