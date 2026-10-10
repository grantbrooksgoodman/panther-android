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

object SignInPageViewFloats {
    val backButtonTopPadding: Dp = 2.dp
    val continueButtonTopPadding: Dp = 5.dp

    val imageBottomPadding: Dp = 5.dp
    val imageFrameHeight: Dp = 70.dp
    val imageFrameWidth: Dp = 150.dp

    val instructionLabelHorizontalPadding: Dp = 30.dp
    val instructionLabelVerticalPadding: Dp = 5.dp

    val phoneNumberTextFieldTrailingPadding: Dp = 20.dp
    val phoneNumberTextFieldVerticalPadding: Dp = 2.dp

    val regionMenuLeadingPadding: Dp = 20.dp
    val regionMenuTrailingPadding: Dp = 5.dp

    val textFieldHorizontalPadding: Dp = 20.dp
    val textFieldVerticalPadding: Dp = 2.dp

    const val BACK_BUTTON_LABEL_FONT_SIZE = 15f
}

// MARK: - Color

object SignInPageViewColors {
    val imageDarkForeground = Color(0xFFF8F8F8)
}

// MARK: - String

object SignInPageViewStrings {
    const val TEXT_FIELD_PLACEHOLDER = "000000"
}
