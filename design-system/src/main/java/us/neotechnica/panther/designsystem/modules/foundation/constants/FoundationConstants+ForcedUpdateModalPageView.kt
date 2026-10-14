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

/** The layout and timing values of the forced update modal page view. */
object ForcedUpdateModalPageViewFloats {
    /** The bottom padding of the app icon image. */
    const val APP_ICON_IMAGE_BOTTOM_PADDING = 10f

    /** The corner radius of the app icon image. */
    const val APP_ICON_IMAGE_CORNER_RADIUS = 24f

    /** The maximum height of the app icon image. */
    const val APP_ICON_IMAGE_MAX_HEIGHT = 120f

    /** The maximum width of the app icon image. */
    const val APP_ICON_IMAGE_MAX_WIDTH = 120f

    /** The maximum frame height of the app icon image overlay symbol. */
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_HEIGHT = 40f

    /** The maximum frame width of the app icon image overlay symbol. */
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_FRAME_MAX_WIDTH = 40f

    /** The horizontal offset of the app icon image overlay symbol. */
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_X_OFFSET = 8f

    /** The vertical offset of the app icon image overlay symbol. */
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_Y_OFFSET = 10f

    /** The bottom padding of the subtitle label text. */
    const val SUBTITLE_LABEL_TEXT_BOTTOM_PADDING = 10f

    /** The horizontal padding of the subtitle label text. */
    const val SUBTITLE_LABEL_TEXT_HORIZONTAL_PADDING = 20f

    /** The font size of the subtitle label text. */
    const val SUBTITLE_LABEL_TEXT_SYSTEM_FONT_SCALE = 15f

    /** The bottom padding of the title label text. */
    const val TITLE_LABEL_TEXT_BOTTOM_PADDING = 5f

    /** The horizontal padding of the title label text. */
    const val TITLE_LABEL_TEXT_HORIZONTAL_PADDING = 5f

    /** The duration, in seconds, of the transition animation. */
    const val TRANSITION_ANIMATION_DURATION = 0.25f
}

// MARK: - Color

/** The colors of the forced update modal page view. */
object ForcedUpdateModalPageViewColors {
    /** The foreground color of the app icon image overlay. */
    val appIconImageOverlayForeground = Color.White

    /** The secondary foreground color of the app icon's overlay. */
    val appIconImageOverlaySecondaryForeground = Color(0xFFFFCC00)

    /** The foreground color of the version label text. */
    val versionLabelTextForeground = Color(0xFFAEAEB2)
}

// MARK: - String

/** The string constants of the forced update modal page view. */
object ForcedUpdateModalPageViewConstants {
    /** The system name of the app icon image overlay symbol. */
    const val APP_ICON_IMAGE_OVERLAY_SYMBOL_NAME = "exclamationmark.triangle.fill"
}
