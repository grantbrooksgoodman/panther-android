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

/** The layout and timing values of the toast view. */
object ToastViewFloats {
    /** The corner radius of the banner. */
    const val BANNER_CORNER_RADIUS = 8f

    /** The opacity of the banner dismiss button's foreground. */
    @Suppress("VariableMaxLength")
    const val BANNER_DISMISS_BUTTON_FOREGROUND_COLOR_OPACITY = 0.7f

    /** The minimum size of the banner dismiss button. */
    const val BANNER_DISMISS_BUTTON_MIN_SIZE = 30f

    /** The horizontal padding of the banner. */
    const val BANNER_HORIZONTAL_PADDING = 16f

    /** The font size of the banner message label. */
    const val BANNER_MESSAGE_LABEL_FONT_SIZE = 12f

    /** The opacity of the banner message label's foreground. */
    @Suppress("VariableMaxLength")
    const val BANNER_MESSAGE_LABEL_FOREGROUND_COLOR_OPACITY = 0.6f

    /** The width of the banner's accent strip. */
    const val BANNER_OVERLAY_FRAME_WIDTH = 6f

    /** The opacity of the banner's shadow. */
    const val BANNER_SHADOW_COLOR_OPACITY = 0.25f

    /** The blur radius of the banner's shadow. */
    const val BANNER_SHADOW_RADIUS = 4f

    /** The horizontal offset of the banner's shadow. */
    const val BANNER_SHADOW_X = 0f

    /** The vertical offset of the banner's shadow. */
    const val BANNER_SHADOW_Y = 1f

    /** The spacing between the banner's elements. */
    const val BANNER_SPACER_MIN_LENGTH = 10f

    /** The font size of the banner title label. */
    const val BANNER_TITLE_LABEL_FONT_SIZE = 14f

    /** The opacity of the banner title label's foreground. */
    @Suppress("VariableMaxLength")
    const val BANNER_TITLE_LABEL_FOREGROUND_COLOR_OPACITY = 0.6f

    /** The padding of a toast from the bottom edge. */
    const val BOTTOM_APPEARANCE_EDGE_PADDING = 30f

    /** The vertical offset of a toast appearing from the bottom edge. */
    const val BOTTOM_APPEARANCE_EDGE_Y_OFFSET = -20f

    /** The maximum frame height of the capsule image. */
    const val CAPSULE_IMAGE_FRAME_MAX_HEIGHT = 20f

    /** The maximum frame width of the capsule image. */
    const val CAPSULE_IMAGE_FRAME_MAX_WIDTH = 20f

    /** The font size of the capsule message label. */
    const val CAPSULE_MESSAGE_LABEL_FONT_SIZE = 12f

    /** The horizontal padding of the capsule message label. */
    const val CAPSULE_MESSAGE_LABEL_HORIZONTAL_PADDING = 5f

    /** The vertical padding of the capsule message label. */
    const val CAPSULE_MESSAGE_LABEL_VERTICAL_PADDING = 5f

    /** The opacity of the capsule's stroke. */
    const val CAPSULE_OVERLAY_STROKE_COLOR_OPACITY = 0.2f

    /** The line width of the capsule's stroke. */
    const val CAPSULE_OVERLAY_STROKE_LINE_WIDTH = 1f

    /** The primary horizontal padding of the capsule. */
    const val CAPSULE_PRIMARY_HORIZONTAL_PADDING = 20f

    /** The secondary horizontal padding of the capsule. */
    const val CAPSULE_SECONDARY_HORIZONTAL_PADDING = 16f

    /** The opacity of the capsule's shadow. */
    const val CAPSULE_SHADOW_COLOR_OPACITY = 0.1f

    /** The blur radius of the capsule's shadow. */
    const val CAPSULE_SHADOW_RADIUS = 5f

    /** The horizontal offset of the capsule's shadow. */
    const val CAPSULE_SHADOW_X = 0f

    /** The vertical offset of the capsule's shadow. */
    const val CAPSULE_SHADOW_Y = 6f

    /** The font size of the capsule title label. */
    const val CAPSULE_TITLE_LABEL_FONT_SIZE = 14f

    /** The vertical padding of the capsule. */
    const val CAPSULE_VERTICAL_PADDING = 10f

    /** The padding of a toast from the top edge. */
    const val TOP_APPEARANCE_EDGE_PADDING = 30f

    /** The vertical offset of a toast appearing from the top edge. */
    const val TOP_APPEARANCE_EDGE_Y_OFFSET = 20f
}

// MARK: - Color

/** The colors of the toast view. */
object ToastViewColors {
    /** The foreground color of the capsule message label. */
    val CAPSULE_MESSAGE_LABEL_FOREGROUND = Color.Gray

    /** The stroke color of the capsule. */
    val CAPSULE_OVERLAY_STROKE = Color.Gray

    /** The default color of an error toast. */
    val DEFAULT_ERROR = Color(0xFFFF3B30)

    /** The default color of an info toast. */
    val DEFAULT_INFO = Color(0xFF007AFF)

    /** The default color of a success toast. */
    val DEFAULT_SUCCESS = Color(0xFF34C759)

    /** The default color of a warning toast. */
    val DEFAULT_WARNING = Color(0xFFFF9500)
}

// MARK: - String

/** The string constants of the toast view. */
object ToastViewStrings {
    /** The system name of the banner dismiss button image. */
    const val BANNER_DISMISS_BUTTON_IMAGE_SYSTEM_NAME = "xmark"

    /** The system name of the banner error icon image. */
    const val BANNER_ERROR_ICON_IMAGE_SYSTEM_NAME = "xmark.circle.fill"

    /** The system name of the banner info icon image. */
    const val BANNER_INFO_ICON_IMAGE_SYSTEM_NAME = "info.circle.fill"

    /** The system name of the banner success icon image. */
    const val BANNER_SUCCESS_ICON_IMAGE_SYSTEM_NAME = "checkmark.circle.fill"

    /** The system name of the banner warning icon image. */
    const val BANNER_WARNING_ICON_IMAGE_SYSTEM_NAME = "exclamationmark.triangle.fill"

    /** The system name of the capsule error icon image. */
    const val CAPSULE_ERROR_ICON_IMAGE_SYSTEM_NAME = "xmark"

    /** The system name of the capsule info icon image. */
    const val CAPSULE_INFO_ICON_IMAGE_SYSTEM_NAME = "info"

    /** The system name of the capsule success icon image. */
    const val CAPSULE_SUCCESS_ICON_IMAGE_SYSTEM_NAME = "checkmark"

    /** The system name of the capsule warning icon image. */
    const val CAPSULE_WARNING_ICON_IMAGE_SYSTEM_NAME = "exclamationmark.triangle.fill"
}
