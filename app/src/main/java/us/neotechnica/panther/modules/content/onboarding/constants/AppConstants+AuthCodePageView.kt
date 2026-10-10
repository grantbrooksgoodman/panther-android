//
//  AppConstants+AuthCodePageView.kt
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

object AuthCodePageViewFloats {
    val backButtonTopPadding: Dp = 2.dp

    val continueButtonTopPadding: Dp = 10.dp

    val innerVStackBottomPadding: Dp = 50.dp
    val instructionLabelVerticalPadding: Dp = 5.dp

    val textFieldHorizontalPadding: Dp = 20.dp
    val textFieldVerticalPadding: Dp = 2.dp

    const val BACK_BUTTON_LABEL_FONT_SIZE = 15f
}

// MARK: - Color

object AuthCodePageViewColors {
    val instructionLabelForeground = Color.Gray
}

// MARK: - String

object AuthCodePageViewStrings {
    const val TEXT_FIELD_PLACEHOLDER = "000000"
}
