//
//  AppConstants+MediaPreviewOverlay.kt
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

/** The layout and timing values of the media preview overlay. */
object MediaPreviewOverlayFloats {
    /** The size of the close button glyph. */
    val closeButtonGlyphSize: Dp = 20.dp

    /** The padding of the close button. */
    val closeButtonPadding: Dp = 12.dp

    /** The size of the close button. */
    val closeButtonSize: Dp = 40.dp

    /** The size of the document glyph. */
    val documentGlyphSize: Dp = 72.dp

    /** The horizontal padding of the document name. */
    val documentNameHorizontalPadding: Dp = 32.dp

    /** The spacing between the document glyph and its name. */
    val documentSpacing: Dp = 16.dp

    /** The top padding of the title. */
    val titleTopPadding: Dp = 12.dp

    /** The maximum dimension, in pixels, at which an image is decoded. */
    const val IMAGE_DECODE_MAX_DIMENSION = 2048

    /** The maximum zoom scale. */
    const val MAX_ZOOM = 5f

    /** The minimum zoom scale. */
    const val MIN_ZOOM = 1f
}

// MARK: - Color

/** The colors of the media preview overlay. */
object MediaPreviewOverlayColors {
    /** The background color of the overlay. */
    val background = Color(0xFF000000)

    /** The background color of the close button. */
    val closeButtonBackground = Color(0x73000000)

    /** The foreground color of the overlay. */
    val foreground = Color(0xFFFFFFFF)
}
