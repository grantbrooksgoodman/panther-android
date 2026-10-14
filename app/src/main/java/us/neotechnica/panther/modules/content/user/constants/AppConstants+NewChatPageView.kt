//
//  AppConstants+NewChatPageView.kt
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

/** The layout and timing values of the new chat page view. */
object NewChatPageViewFloats {
    /** The frame height of the done toolbar button. */
    val doneToolbarButtonFrameHeight: Dp = 30.dp

    /** The frame width of the done toolbar button. */
    val doneToolbarButtonFrameWidth: Dp = 30.dp

    /** The amount added to the navigation bar's height. */
    val navigationBarHeightIncrement: Dp = 20.dp
}

// MARK: - Color

/** The colors of the new chat page view. */
object NewChatPageViewColors {
    /** The background color in dark mode. */
    val darkBackground = Color(0xFF1C1C1E)
}

// MARK: - String

/** The string constants of the new chat page view. */
object NewChatPageViewConstants {
    /** The system name of the cancel toolbar button image. */
    const val CANCEL_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "xmark"

    /** The system name of the done toolbar button image. */
    const val DONE_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "checkmark"
}
