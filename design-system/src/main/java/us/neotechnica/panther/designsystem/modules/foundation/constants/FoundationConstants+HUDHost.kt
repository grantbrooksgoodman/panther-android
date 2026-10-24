//
//  FoundationConstants+HUDHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Color

/** The colors of the heads-up display. */
object HUDHostColors {
    /** The background color of the display in dark mode. */
    val DARK_BACKGROUND = Color(0xFF393939)

    /** The color of the status label in dark mode. */
    val DARK_STATUS_LABEL_FOREGROUND = Color.White

    /** The background color of the display in light mode. */
    val LIGHT_BACKGROUND = Color(0xFFFDFDFF)

    /** The color of the status label in light mode. */
    val LIGHT_STATUS_LABEL_FOREGROUND = Color.Black

    /** The color of the display's indicator or image. */
    val MEDIA_FOREGROUND = Color(0xFFAAAAAA)
}

// MARK: - Float

/** The layout and timing values of the heads-up display. */
object HUDHostFloats {
    /** The duration, in milliseconds, of the display's appearance and dismissal. */
    const val ANIMATION_DURATION_MILLISECONDS = 150

    /**
     * The base interval, in milliseconds, before a success or flash
     * display dismisses itself.
     */
    const val AUTO_DISMISS_BASE_MILLISECONDS = 1250L

    /**
     * The interval, in milliseconds, that each character of the status
     * text adds before a success or flash display dismisses itself.
     */
    const val AUTO_DISMISS_PER_CHARACTER_MILLISECONDS = 30L

    /**
     * The delay, in milliseconds, before the checkmark begins drawing
     * when it appears with the display.
     */
    const val CHECKMARK_APPEARANCE_DELAY_MILLISECONDS = 250

    /** The horizontal position of the checkmark's end, as a fraction of the media size. */
    const val CHECKMARK_END_X = 1f

    /** The vertical position of the checkmark's end, as a fraction of the media size. */
    const val CHECKMARK_END_Y = 0.25f

    /** The line width of the checkmark. */
    const val CHECKMARK_LINE_WIDTH = 9f

    /** The horizontal position of the checkmark's start, as a fraction of the media size. */
    const val CHECKMARK_START_X = 0.15f

    /** The vertical position of the checkmark's start, as a fraction of the media size. */
    const val CHECKMARK_START_Y = 0.5f

    /** The duration, in milliseconds, of the checkmark's stroke. */
    const val CHECKMARK_STROKE_DURATION_MILLISECONDS = 250

    /** The horizontal position of the checkmark's vertex, as a fraction of the media size. */
    const val CHECKMARK_VERTEX_X = 0.5f

    /** The vertical position of the checkmark's vertex, as a fraction of the media size. */
    const val CHECKMARK_VERTEX_Y = 0.8f

    /** The corner radius of the display. */
    const val CORNER_RADIUS = 10f

    /** The scale to which the display shrinks as it dismisses. */
    const val DISMISSAL_SCALE = 0.3f

    /**
     * The scale from which the display shrinks as it appears. The
     * display settles at the reciprocal of this scale.
     */
    const val DISPLAY_SCALE = 1.4f

    /** The width of the exclamation image. */
    const val EXCLAMATION_IMAGE_WIDTH = 59f

    /**
     * The number of ticks over which a spoke of the activity indicator
     * fades from highlighted to resting.
     */
    const val INDICATOR_FADE_TICK_COUNT = 8

    /** The opacity of the highlighted spoke of the activity indicator. */
    const val INDICATOR_HIGHLIGHTED_SPOKE_OPACITY = 0.85f

    /** The opacity of a resting spoke of the activity indicator. */
    const val INDICATOR_RESTING_SPOKE_OPACITY = 0.265f

    /** The number of spokes in the activity indicator. */
    const val INDICATOR_SPOKE_COUNT = 8

    /** The distance from the activity indicator's center to the inner end of each spoke. */
    const val INDICATOR_SPOKE_INNER_RADIUS = 10.5f

    /** The distance from the activity indicator's center to the outer end of each spoke. */
    const val INDICATOR_SPOKE_OUTER_RADIUS = 33f

    /** The width of each spoke of the activity indicator. */
    const val INDICATOR_SPOKE_WIDTH = 9.5f

    /** The duration, in milliseconds, of each tick of the activity indicator. */
    const val INDICATOR_TICK_DURATION_MILLISECONDS = 50

    /** The number of ticks the activity indicator's highlight remains on each spoke. */
    const val INDICATOR_TICKS_PER_SPOKE = 2

    /** The margin between the display's edges and its content. */
    const val MARGIN_SIZE = 30f

    /** The width and height of the display's indicator or image. */
    const val MEDIA_SIZE = 70f

    /** The font size of the status label. */
    const val STATUS_LABEL_FONT_SIZE = 24f

    /** The maximum width of the status label. */
    const val STATUS_LABEL_MAXIMUM_WIDTH = 250f
}

// MARK: - String

/** The string values of the heads-up display. */
object HUDHostStrings {
    /** The SF Symbol name of the exclamation image. */
    const val EXCLAMATION_IMAGE_SYSTEM_NAME = "exclamationmark.triangle.fill"
}
