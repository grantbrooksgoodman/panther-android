//
//  FoundationConstants+ToastView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

object ToastViewFloats {
    const val BANNER_CORNER_RADIUS = 8f
    const val BANNER_DISMISS_BUTTON_FOREGROUND_COLOR_OPACITY = 0.7f
    const val BANNER_DISMISS_BUTTON_MIN_SIZE = 30f
    const val BANNER_HORIZONTAL_PADDING = 16f
    const val BANNER_MESSAGE_LABEL_FONT_SIZE = 12f
    const val BANNER_MESSAGE_LABEL_FOREGROUND_COLOR_OPACITY = 0.6f
    const val BANNER_OVERLAY_FRAME_WIDTH = 6f
    const val BANNER_SHADOW_COLOR_OPACITY = 0.25f
    const val BANNER_SHADOW_RADIUS = 4f
    const val BANNER_SHADOW_X = 0f
    const val BANNER_SHADOW_Y = 1f
    const val BANNER_SPACER_MIN_LENGTH = 10f
    const val BANNER_TITLE_LABEL_FONT_SIZE = 14f
    const val BANNER_TITLE_LABEL_FOREGROUND_COLOR_OPACITY = 0.6f
    const val BOTTOM_APPEARANCE_EDGE_PADDING = 30f
    const val BOTTOM_APPEARANCE_EDGE_Y_OFFSET = -20f
    const val CAPSULE_IMAGE_FRAME_MAX_HEIGHT = 20f
    const val CAPSULE_IMAGE_FRAME_MAX_WIDTH = 20f
    const val CAPSULE_MESSAGE_LABEL_FONT_SIZE = 12f
    const val CAPSULE_MESSAGE_LABEL_HORIZONTAL_PADDING = 5f
    const val CAPSULE_MESSAGE_LABEL_VERTICAL_PADDING = 5f
    const val CAPSULE_OVERLAY_STROKE_COLOR_OPACITY = 0.2f
    const val CAPSULE_OVERLAY_STROKE_LINE_WIDTH = 1f
    const val CAPSULE_PRIMARY_HORIZONTAL_PADDING = 20f
    const val CAPSULE_SECONDARY_HORIZONTAL_PADDING = 16f
    const val CAPSULE_SHADOW_COLOR_OPACITY = 0.1f
    const val CAPSULE_SHADOW_RADIUS = 5f
    const val CAPSULE_SHADOW_X = 0f
    const val CAPSULE_SHADOW_Y = 6f
    const val CAPSULE_TITLE_LABEL_FONT_SIZE = 14f
    const val CAPSULE_VERTICAL_PADDING = 10f
    const val TOP_APPEARANCE_EDGE_PADDING = 30f
    const val TOP_APPEARANCE_EDGE_Y_OFFSET = 20f
}

// MARK: - Color

object ToastViewColors {
    val BANNER_SHADOW = Color.Black
    val CAPSULE_MESSAGE_LABEL_FOREGROUND = Color.Gray
    val CAPSULE_OVERLAY_STROKE = Color.Gray
    val CAPSULE_SHADOW = Color.Black
    val DEFAULT_ERROR = Color(0xFFFF3B30)
    val DEFAULT_INFO = Color(0xFF007AFF)
    val DEFAULT_SUCCESS = Color(0xFF34C759)
    val DEFAULT_WARNING = Color(0xFFFF9500)
}

// MARK: - String

object ToastViewStrings {
    const val BANNER_DISMISS_BUTTON_IMAGE_SYSTEM_NAME = "xmark"
    const val BANNER_ERROR_ICON_IMAGE_SYSTEM_NAME = "xmark.circle.fill"
    const val BANNER_INFO_ICON_IMAGE_SYSTEM_NAME = "info.circle.fill"
    const val BANNER_SUCCESS_ICON_IMAGE_SYSTEM_NAME = "checkmark.circle.fill"
    const val BANNER_WARNING_ICON_IMAGE_SYSTEM_NAME = "exclamationmark.triangle.fill"
    const val CAPSULE_ERROR_ICON_IMAGE_SYSTEM_NAME = "xmark"
    const val CAPSULE_INFO_ICON_IMAGE_SYSTEM_NAME = "info"
    const val CAPSULE_SUCCESS_ICON_IMAGE_SYSTEM_NAME = "checkmark"
    const val CAPSULE_WARNING_ICON_IMAGE_SYSTEM_NAME = "exclamationmark.triangle.fill"
}
