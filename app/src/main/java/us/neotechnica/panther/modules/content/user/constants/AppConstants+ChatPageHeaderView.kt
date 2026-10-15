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

/** The layout and timing values of the chat page header view. */
object ChatPageHeaderViewFloats {
    /** The size of the avatar image. */
    val avatarImageSize: Dp = 54.dp

    /** The participant count above which the avatar shows its group badge. */
    const val AVATAR_IMAGE_VIEW_BADGE_COUNT_COMPARATOR = 2

    /** The frame size of the avatar view name label chevron symbol. */
    @Suppress("VariableMaxLength")
    val avatarViewNameLabelChevronSymbolFrameSize: Dp = 12.dp

    /** The font size of the avatar view name label. */
    const val AVATAR_VIEW_NAME_LABEL_FONT_SIZE = 16f

    /** The padding of the avatar view name label glass effect. */
    val avatarViewNameLabelGlassEffectPadding: Dp = 8.dp

    /** The spacing of the avatar view name label. */
    val avatarViewNameLabelSpacing: Dp = 2.dp

    /** The horizontal padding of the avatar view name label text. */
    val avatarViewNameLabelTextHorizontalPadding: Dp = 1.5.dp

    /** The horizontal padding of the avatar view name label view. */
    val avatarViewNameLabelViewHorizontalPadding: Dp = 30.dp

    /** The spacing of the avatar view. */
    val avatarViewSpacing: Dp = (-4).dp

    /** The frame size of the back button. */
    val backButtonFrameSize: Dp = 30.dp

    /** The padding of the back button glass effect. */
    val backButtonGlassEffectPadding: Dp = 8.dp

    /** The horizontal padding of the back button view. */
    val backButtonViewHorizontalPadding: Dp = 16.dp

    /** The amount added to the background view's height. */
    val backgroundViewHeightIncrement: Dp = 28.dp

    /** The vertical padding of the content view. */
    val contentViewVerticalPadding: Dp = 4.dp
}

// MARK: - Color

/** The colors of the chat page header view. */
object ChatPageHeaderViewColors {
    /** The foreground color of the avatar view name label chevron symbol. */
    @Suppress("VariableMaxLength")
    val avatarViewNameLabelChevronSymbolForeground = Color(0xFFAEAEB2)

    /** The color of the background view gradient. */
    val backgroundViewGradientColor = Color.Black

    /** The circle drawn behind the avatar. */
    val avatarImageViewBackground: Color
        @Composable get() = LocalPantherColors.current.background
}

// MARK: - String

/** The string constants of the chat page header view. */
object ChatPageHeaderViewStrings {
    /** The system name of the avatar view name label chevron image. */
    @Suppress("VariableMaxLength")
    const val AVATAR_VIEW_NAME_LABEL_CHEVRON_SYMBOL_IMAGE_SYSTEM_NAME = "chevron.compact.right"
}
