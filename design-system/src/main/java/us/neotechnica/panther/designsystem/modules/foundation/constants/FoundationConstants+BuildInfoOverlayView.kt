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

object BuildInfoOverlayViewFloats {
    const val BUILD_INFO_BUTTON_FRAME_HEIGHT = 15f

    const val DEVELOPER_MODE_INDICATOR_FRAME_HEIGHT = 8f
    const val DEVELOPER_MODE_INDICATOR_FRAME_WIDTH = 8f
    const val DEVELOPER_MODE_INDICATOR_TRAILING_PADDING = -6f

    const val SEND_FEEDBACK_BUTTON_FRAME_HEIGHT = 20f
    const val SEND_FEEDBACK_BUTTON_LABEL_FONT_SIZE = 12f

    const val STATS_VIEW_FRAME_HEIGHT = 15f
    const val TRANSLUCENCY_ANIMATION_SPEED = 2f
    const val X_OFFSET = -20f
}

// MARK: - Color

object BuildInfoOverlayViewColors {
    val buildInfoButtonLabelForeground = Color.White
    val sendFeedbackButtonLabelForeground = Color.White
    val statsLabelForeground = Color.White
}

// MARK: - String

object BuildInfoOverlayViewStrings {
    const val SEND_FEEDBACK_BUTTON_LABEL_FONT_NAME = "Arial"
}
