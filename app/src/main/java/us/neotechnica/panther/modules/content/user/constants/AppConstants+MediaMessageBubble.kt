//
//  AppConstants+MediaMessageBubble.kt
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

object MediaMessageBubbleFloats {
    val bubbleRadius: Dp = 18.dp
    val bubbleTailRadius: Dp = 4.dp
    val documentIconSize: Dp = 30.dp
    val documentPadding: Dp = 12.dp
    val documentTextStartPadding: Dp = 10.dp
    val imageMaxHeight: Dp = 280.dp
    val imageMaxWidth: Dp = 240.dp
    val placeholderHeight: Dp = 150.dp
    val placeholderWidth: Dp = 210.dp
    val playBackgroundSize: Dp = 54.dp
    val playGlyphSize: Dp = 30.dp

    const val IMAGE_DECODE_MAX_DIMENSION = 1080
    const val VIDEO_DEFAULT_ASPECT_RATIO = 1.3333f
}

// MARK: - Color

object MediaMessageBubbleColors {
    val playBackground = Color(0x73000000)
    val playGlyph = Color(0xFFFFFFFF)
    val videoPlaceholderBackground = Color(0xFF1C1C1E)
}
