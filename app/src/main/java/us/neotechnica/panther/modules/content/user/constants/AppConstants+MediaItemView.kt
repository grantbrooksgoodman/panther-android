//
//  AppConstants+MediaItemView.kt
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

/** The layout and timing values of the media item view. */
object MediaItemViewFloats {
    /** The corner radius of the image. */
    val imageCornerRadius: Dp = 8.dp

    /** The frame height of the image. */
    val imageFrameHeight: Dp = 60.dp

    /** The frame width of the image. */
    val imageFrameWidth: Dp = 60.dp

    /** The longest side, in pixels, a thumbnail is decoded at. */
    const val THUMBNAIL_DECODE_MAX_DIMENSION = 240
}

// MARK: - Color

/** The colors of the media item view. */
object MediaItemViewColors {
    /** The foreground color of the missing image. */
    val missingImageForeground = Color(0xFFC7C7CC)

    /** The foreground color of the sender label. */
    val senderLabelForeground = Color(0xFF8E8E93)

    /** The foreground color of the timestamp label. */
    val timestampLabelForeground = Color(0xFF8E8E93)
}

// MARK: - String

/** The string constants of the media item view. */
object MediaItemViewStrings {
    /** The system name of the save action image. */
    const val SAVE_ACTION_IMAGE_SYSTEM_NAME = "square.and.arrow.down"
}
