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

object MediaItemViewFloats {
    val imageCornerRadius: Dp = 8.dp
    val imageFrameHeight: Dp = 60.dp
    val imageFrameWidth: Dp = 60.dp

    /** The longest side, in pixels, a thumbnail is decoded at. */
    const val THUMBNAIL_DECODE_MAX_DIMENSION = 240
}

// MARK: - Color

object MediaItemViewColors {
    val missingImageForeground = Color(0xFFC7C7CC)
    val senderLabelForeground = Color(0xFF8E8E93)
    val timestampLabelForeground = Color(0xFF8E8E93)
}

// MARK: - String

object MediaItemViewStrings {
    const val SAVE_ACTION_IMAGE_SYSTEM_NAME = "square.and.arrow.down"
}
