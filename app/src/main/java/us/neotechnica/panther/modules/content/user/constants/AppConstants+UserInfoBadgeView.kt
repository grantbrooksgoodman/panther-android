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

/** The layout and timing values of the user info badge view. */
object UserInfoBadgeViewFloats {
    /** The corner radius of the body. */
    val bodyCornerRadius: Dp = 3.dp

    /** The maximum height of the body. */
    val bodyMaxHeight: Dp = 20.dp

    /** The maximum width of the body. */
    val bodyMaxWidth: Dp = 50.dp

    /** The spacing of the label view's row. */
    val labelViewHStackSpacing: Dp = 2.dp

    /** The corner radius of the label view image. */
    val labelViewImageCornerRadius: Dp = 2.dp

    /** The frame height of the label view image. */
    val labelViewImageFrameHeight: Dp = 10.dp

    /** The frame width of the label view image. */
    val labelViewImageFrameWidth: Dp = 20.dp

    /** The frame height of the label view text. */
    val labelViewTextFrameHeight: Dp = 10.dp

    /** The frame width of the label view text. */
    val labelViewTextFrameWidth: Dp = 20.dp

    /** The opacity of the label view text. */
    const val LABEL_VIEW_TEXT_OPACITY = 0.8f

    /** The shadow radius of the label view text. */
    val labelViewTextShadowRadius: Dp = 20.dp
}

// MARK: - Color

/** The colors of the user info badge view. */
object UserInfoBadgeViewColors {
    /** The foreground color of the body in dark mode. */
    val bodyDarkForeground = Color(0xFF27252A)

    /** The foreground color of the body in light mode. */
    val bodyLightForeground = Color(0xFFE5E5EA)

    /** The shadow color of the label view text. */
    val labelViewTextShadow = Color.Black
}
