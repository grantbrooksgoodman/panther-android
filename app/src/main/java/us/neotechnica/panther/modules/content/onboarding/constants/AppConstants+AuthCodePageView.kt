//
//  AppConstants+AuthCodePageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object AuthCodePageViewFloats {
    val backButtonTopPadding: Dp = 8.dp
    val continueButtonVerticalPadding: Dp = 5.dp
    val innerVStackBottomPadding: Dp = 50.dp
    val instructionLabelVerticalPadding: Dp = 5.dp
    val textFieldBottomPadding: Dp = 8.dp
    val textFieldHorizontalPadding: Dp = 20.dp
    val textFieldTopPadding: Dp = 2.dp

    const val BACK_BUTTON_LABEL_FONT_SIZE = 15f
}

// MARK: - Color

object AuthCodePageViewColors {
    val debugForeground = Color(0xFFFF9500)
}

// MARK: - String

object AuthCodePageViewStrings {
    const val FORCE_CONTINUE_DEBUG = "Force Continue (Debug)"
    const val TEXT_FIELD_PLACEHOLDER = "000000"
}
