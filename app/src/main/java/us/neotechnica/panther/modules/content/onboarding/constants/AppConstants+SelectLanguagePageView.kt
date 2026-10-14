//
//  AppConstants+SelectLanguagePageView.kt
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

/** The layout and timing values of the select language page view. */
object SelectLanguagePageViewFloats {
    /** The top padding of the back button. */
    val backButtonTopPadding: Dp = 2.dp

    /** The top padding of the continue button. */
    val continueButtonTopPadding: Dp = 5.dp

    /** The bottom padding of the inner stack. */
    val innerVStackBottomPadding: Dp = 50.dp

    /** The vertical padding of the instruction label. */
    val instructionLabelVerticalPadding: Dp = 5.dp

    /** The horizontal padding of the picker. */
    val pickerHorizontalPadding: Dp = 30.dp

    /** The font size of the back button label. */
    const val BACK_BUTTON_LABEL_FONT_SIZE = 15f
}

// MARK: - Color

/** The colors of the select language page view. */
object SelectLanguagePageViewColors {
    /** The foreground color of the instruction label. */
    val instructionLabelForeground = Color.Gray
}
