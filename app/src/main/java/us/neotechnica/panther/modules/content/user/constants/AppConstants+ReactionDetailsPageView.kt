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
    val groupListViewHorizontalPadding: Dp = 20.dp
    val groupListViewTopPadding: Dp = 20.dp

    /** The font scale of the emoji in a list row's square icon. */
    const val LIST_ROW_ICON_TEXT_FONT_SCALE = 90f
}

// MARK: - Color

object ReactionDetailsPageViewColors {
    val doneHeaderItemForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton

    val navigationBarItemGlassTint: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton
}
