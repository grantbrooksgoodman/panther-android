//
//  AppConstants+ChatPageHeaderView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Float

object ChatPageHeaderViewFloats {
    val avatarImageSize: Dp = 54.dp
    const val AVATAR_IMAGE_VIEW_BADGE_COUNT_COMPARATOR = 2
    val avatarViewNameLabelChevronSymbolFrameSize: Dp = 12.dp
    const val AVATAR_VIEW_NAME_LABEL_FONT_SIZE = 16f
    val avatarViewNameLabelGlassEffectPadding: Dp = 8.dp
    val avatarViewNameLabelSpacing: Dp = 2.dp
    val avatarViewNameLabelTextHorizontalPadding: Dp = 1.5.dp
    val avatarViewNameLabelViewHorizontalPadding: Dp = 30.dp
    val avatarViewSpacing: Dp = (-4).dp

    val backButtonFrameSize: Dp = 30.dp
    val backButtonGlassEffectPadding: Dp = 8.dp
    val backButtonViewHorizontalPadding: Dp = 16.dp
    val backgroundViewHeightIncrement: Dp = 28.dp

    val contentViewVerticalPadding: Dp = 4.dp

    /** The background blur opacity for the active theme. */
    fun backgroundViewBlurOpacity(isDarkModeActive: Boolean): Float =
        if (isDarkModeActive) DARK_BACKGROUND_VIEW_BLUR_OPACITY else LIGHT_BACKGROUND_VIEW_BLUR_OPACITY

    private const val DARK_BACKGROUND_VIEW_BLUR_OPACITY = 0.6f
    private const val LIGHT_BACKGROUND_VIEW_BLUR_OPACITY = 0.7f
}

// MARK: - Color

object ChatPageHeaderViewColors {
    val avatarViewNameLabelChevronSymbolForeground = Color(0xFFAEAEB2)
    val backgroundViewGradientColor = Color.Black

    /** The circle drawn behind the avatar. */
    val avatarImageViewBackground: Color
        @Composable get() = LocalPantherColors.current.background
}

// MARK: - String

object ChatPageHeaderViewStrings {
    const val AVATAR_VIEW_NAME_LABEL_CHEVRON_SYMBOL_IMAGE_SYSTEM_NAME = "chevron.compact.right"
}
