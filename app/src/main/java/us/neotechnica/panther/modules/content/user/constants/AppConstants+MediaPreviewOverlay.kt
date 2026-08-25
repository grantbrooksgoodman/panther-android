//
//  AppConstants+MediaPreviewOverlay.kt
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

object MediaPreviewOverlayFloats {
    val closeButtonGlyphSize: Dp = 20.dp
    val closeButtonPadding: Dp = 12.dp
    val closeButtonSize: Dp = 40.dp
    val documentGlyphSize: Dp = 72.dp
    val documentNameHorizontalPadding: Dp = 32.dp
    val documentSpacing: Dp = 16.dp

    const val IMAGE_DECODE_MAX_DIMENSION = 2048
    const val MAX_ZOOM = 5f
    const val MIN_ZOOM = 1f
}

// MARK: - Color

object MediaPreviewOverlayColors {
    val background = Color(0xFF000000)
    val closeButtonBackground = Color(0x73000000)
    val foreground = Color(0xFFFFFFFF)
}

// MARK: - String

object MediaPreviewOverlayStrings {
    const val OPEN = "Open"
}
