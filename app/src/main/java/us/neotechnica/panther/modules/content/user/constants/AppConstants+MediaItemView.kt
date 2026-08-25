//
//  AppConstants+MediaItemView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object MediaItemViewFloats {
    val glyphSize: Dp = 28.dp
    val imageCornerRadius: Dp = 8.dp
    val imageSize: Dp = 60.dp
    val rowHorizontalPadding: Dp = 16.dp
    val rowVerticalPadding: Dp = 8.dp
    val senderTopPadding: Dp = 1.dp
    val textStartPadding: Dp = 12.dp

    const val THUMBNAIL_DECODE_MAX_DIMENSION = 240
}

// MARK: - Color

object MediaItemViewColors {
    val senderLabelForeground = Color(0xFF8E8E93)
    val thumbnailBackground = Color(0xFFC7C7CC)
    val thumbnailGlyph = Color(0xFFFFFFFF)
    val timestampLabelForeground = Color(0xFF8E8E93)
}
