//
//  FoundationConstants+BuildInfoOverlayView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The layout and timing values of the build info overlay view. */
object BuildInfoOverlayViewFloats {
    /** The frame height of the build info button. */
    const val BUILD_INFO_BUTTON_FRAME_HEIGHT = 15f

    /** The frame height of the developer mode indicator. */
    const val DEVELOPER_MODE_INDICATOR_FRAME_HEIGHT = 8f

    /** The frame width of the developer mode indicator. */
    const val DEVELOPER_MODE_INDICATOR_FRAME_WIDTH = 8f

    /** The trailing padding of the developer mode indicator. */
    const val DEVELOPER_MODE_INDICATOR_TRAILING_PADDING = -6f

    /** The frame height of the send feedback button. */
    const val SEND_FEEDBACK_BUTTON_FRAME_HEIGHT = 20f

    /** The font size of the send feedback button label. */
    const val SEND_FEEDBACK_BUTTON_LABEL_FONT_SIZE = 12f

    /** The frame height of the stats view. */
    const val STATS_VIEW_FRAME_HEIGHT = 15f

    /** The speed of the translucency animation. */
    const val TRANSLUCENCY_ANIMATION_SPEED = 2f

    /** The horizontal offset of the overlay. */
    const val X_OFFSET = -20f
}

// MARK: - Color

/** The colors of the build info overlay view. */
object BuildInfoOverlayViewColors {
    /** The foreground color of the build info button label. */
    val buildInfoButtonLabelForeground = Color.White

    /** The foreground color of the send feedback button label. */
    val sendFeedbackButtonLabelForeground = Color.White

    /** The foreground color of the stats label. */
    val statsLabelForeground = Color.White
}

// MARK: - String

/** The string constants of the build info overlay view. */
object BuildInfoOverlayViewStrings {
    /** The font name of the send feedback button's label. */
    const val SEND_FEEDBACK_BUTTON_LABEL_FONT_NAME = "Arial"
}
