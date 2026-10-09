//
//  FoundationConstants+ForcedUpdateModalPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

object ForcedUpdateModalPageViewFloats {
    const val APP_ICON_IMAGE_BOTTOM_PADDING = 10f
    const val APP_ICON_IMAGE_CORNER_RADIUS = 24f
    const val APP_ICON_IMAGE_MAX_HEIGHT = 120f
    const val APP_ICON_IMAGE_MAX_WIDTH = 120f
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_HEIGHT = 40f
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_WIDTH = 40f
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_X_OFFSET = 8f
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_Y_OFFSET = 10f

    const val SUBTITLE_LABEL_TEXT_BOTTOM_PADDING = 10f
    const val SUBTITLE_LABEL_TEXT_HORIZONTAL_PADDING = 20f
    const val SUBTITLE_LABEL_TEXT_SYSTEM_FONT_SCALE = 15f

    const val TITLE_LABEL_TEXT_BOTTOM_PADDING = 5f
    const val TITLE_LABEL_TEXT_HORIZONTAL_PADDING = 5f

    const val TRANSITION_ANIMATION_DURATION = 0.25f
}

// MARK: - Color

object ForcedUpdateModalPageViewColors {
    val appIconImageOverlayForeground = Color.White
    val appIconImageOverlaySecondaryForeground = Color(0xFFFFCC00)
    val versionLabelTextForeground = Color(0xFFAEAEB2)
}

// MARK: - String

object ForcedUpdateModalPageViewConstants {
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_NAME = "exclamationmark.triangle.fill"
}
