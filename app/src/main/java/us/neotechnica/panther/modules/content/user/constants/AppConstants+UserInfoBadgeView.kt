//
//  AppConstants+UserInfoBadgeView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object UserInfoBadgeViewFloats {
    val bodyCornerRadius: Dp = 3.dp
    val bodyMaxHeight: Dp = 20.dp
    val bodyMaxWidth: Dp = 56.dp
    val labelViewHorizontalPadding: Dp = 5.dp
    val labelViewHStackSpacing: Dp = 2.dp
    val labelViewImageCornerRadius: Dp = 2.dp
    val labelViewImageFrameHeight: Dp = 10.dp
    val labelViewImageFrameWidth: Dp = 20.dp
    val labelViewVerticalPadding: Dp = 2.dp
}

// MARK: - Color

object UserInfoBadgeViewColors {
    val bodyDarkForeground = Color(0xFF27252A)
    val bodyLightForeground = Color(0xFFE5E5EA)
}
