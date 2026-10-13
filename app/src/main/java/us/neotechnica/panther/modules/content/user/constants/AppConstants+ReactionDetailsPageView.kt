//
//  AppConstants+ReactionDetailsPageView.kt
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

object ReactionDetailsPageViewFloats {
    const val ICON_FONT_SIZE = 22f
    val groupListViewHorizontalPadding: Dp = 20.dp
    val groupListViewTopPadding: Dp = 20.dp
    val listCornerRadius: Dp = 12.dp
    val rowHorizontalPadding: Dp = 16.dp
    val rowIconSize: Dp = 40.dp
    val rowIconSpacing: Dp = 12.dp
    val rowVerticalPadding: Dp = 12.dp
}

// MARK: - Color

object ReactionDetailsPageViewColors {
    val doneHeaderItemForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton
}
