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

/** The layout and timing values of the contact selector page view. */
object ContactSelectorPageViewFloats {
    /** The horizontal padding of the no results label. */
    val noResultsLabelHorizontalPadding: Dp = 10.dp

    /** The font size of the cancel toolbar button. */
    const val CANCEL_TOOLBAR_BUTTON_SYSTEM_FONT_SIZE = 17f
}

// MARK: - Color

/** The colors of the contact selector page view. */
object ContactSelectorPageViewColors {
    /** The background color of the list view in dark mode. */
    val listViewDarkBackground = Color(0xFF1C1C1E)

    /** The background color of the list view in light mode. */
    val listViewLightBackground = Color.White

    /** The alternate foreground color of the no results label. */
    val noResultsLabelAlternateForeground = Color(0xFF007AFF)

    /** The foreground color of a tinted toolbar button. */
    val tintedGlassToolbarButtonForeground = Color.White

    /** The foreground color of the no results label. */
    val noResultsLabelForeground: Color
        @Composable get() = LocalPantherColors.current.subtitleText
}
