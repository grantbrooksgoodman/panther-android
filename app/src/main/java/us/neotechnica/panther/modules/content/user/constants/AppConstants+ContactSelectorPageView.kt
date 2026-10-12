//
//  AppConstants+ContactSelectorPageView.kt
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

object ContactSelectorPageViewFloats {
    val listViewDefaultMinListRowHeight: Dp = 44.dp
    val listViewRowHorizontalPadding: Dp = 20.dp
    val listViewSectionHeaderVerticalPadding: Dp = 6.dp
    val noResultsLabelHorizontalPadding: Dp = 10.dp

    const val CANCEL_TOOLBAR_BUTTON_SYSTEM_FONT_SIZE = 17f
}

// MARK: - Color

object ContactSelectorPageViewColors {
    val noResultsLabelAlternateForeground = Color(0xFF007AFF)
    val tintedGlassToolbarButtonForeground = Color.White

    val noResultsLabelForeground: Color
        @Composable get() = LocalPantherColors.current.subtitleText
}
