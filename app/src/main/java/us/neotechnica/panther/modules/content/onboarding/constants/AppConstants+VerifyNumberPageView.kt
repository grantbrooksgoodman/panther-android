//
//  AppConstants+VerifyNumberPageView.kt
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

/** The layout and timing values of the verify number page view. */
object VerifyNumberPageViewFloats {
    /** The top padding of the back button. */
    val backButtonTopPadding: Dp = 2.dp

    /** The bottom padding. */
    val bottomPadding: Dp = 30.dp

    /** The top padding of the continue button. */
    val continueButtonTopPadding: Dp = 5.dp

    /** The bottom padding of the inner stack. */
    val innerVStackBottomPadding: Dp = 50.dp

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

    /** The font size of the back button label. */
    const val BACK_BUTTON_LABEL_FONT_SIZE = 15f
}

// MARK: - Color

/** The colors of the verify number page view. */
object VerifyNumberPageViewColors {
    /** The foreground color of the instruction label. */
    val instructionLabelForeground = Color.Gray
}
