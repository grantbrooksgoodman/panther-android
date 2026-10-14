//
//  AppConstants+MediaMessageBubble.kt
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

/** The layout and timing values of the media message bubble. */
object MediaMessageBubbleFloats {
    /** The corner radius of the bubble. */
    val bubbleRadius: Dp = 18.dp

    /** The corner radius at the bubble's tail. */
    val bubbleTailRadius: Dp = 4.dp

    /** The maximum height of the image. */
    val imageMaxHeight: Dp = 280.dp

    /** The maximum width of the image. */
    val imageMaxWidth: Dp = 240.dp

    /** The size of the missing image glyph. */
    val missingImageGlyphSize: Dp = 100.dp

    /** The height of the placeholder. */
    val placeholderHeight: Dp = 150.dp

    /** The width of the placeholder. */
    val placeholderWidth: Dp = 210.dp

    /** The size of the play background. */
    val playBackgroundSize: Dp = 54.dp

    /** The size of the play glyph. */
    val playGlyphSize: Dp = 30.dp

    /** The maximum dimension, in pixels, at which an image is decoded. */
    const val IMAGE_DECODE_MAX_DIMENSION = 1080

    /** The default aspect ratio of a video. */
    const val VIDEO_DEFAULT_ASPECT_RATIO = 1.3333f
}

// MARK: - Color

/** The colors of the media message bubble. */
object MediaMessageBubbleColors {
    /** The color of the missing image glyph. */
    val missingImageGlyph = Color(0xFFC7C7CC)

    /** The background color of the play button. */
    val playBackground = Color(0x73000000)

    /** The color of the play glyph. */
    val playGlyph = Color(0xFFFFFFFF)

    /** The background color of the video placeholder. */
    val videoPlaceholderBackground = Color(0xFF1C1C1E)
}
