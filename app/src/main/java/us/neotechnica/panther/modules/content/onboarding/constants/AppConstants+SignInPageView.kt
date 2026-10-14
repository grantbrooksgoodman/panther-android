//
//  AppConstants+SignInPageView.kt
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

/** The layout and timing values of the sign in page view. */
object SignInPageViewFloats {
    /** The top padding of the back button. */
    val backButtonTopPadding: Dp = 2.dp

    /** The top padding of the continue button. */
    val continueButtonTopPadding: Dp = 5.dp

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

    /** The trailing padding of the phone number text field. */
    val phoneNumberTextFieldTrailingPadding: Dp = 20.dp

    /** The vertical padding of the phone number text field. */
    val phoneNumberTextFieldVerticalPadding: Dp = 2.dp

    /** The leading padding of the region menu. */
    val regionMenuLeadingPadding: Dp = 20.dp

    /** The trailing padding of the region menu. */
    val regionMenuTrailingPadding: Dp = 5.dp

    /** The horizontal padding of the text field. */
    val textFieldHorizontalPadding: Dp = 20.dp

    /** The vertical padding of the text field. */
    val textFieldVerticalPadding: Dp = 2.dp

    /** The font size of the back button label. */
    const val BACK_BUTTON_LABEL_FONT_SIZE = 15f
}

// MARK: - Color

/** The colors of the sign in page view. */
object SignInPageViewColors {
    /** The foreground color of the image in dark mode. */
    val imageDarkForeground = Color(0xFFF8F8F8)
}

// MARK: - String

/** The string constants of the sign in page view. */
object SignInPageViewStrings {
    /** The placeholder of the text field. */
    const val TEXT_FIELD_PLACEHOLDER = "000000"
}
