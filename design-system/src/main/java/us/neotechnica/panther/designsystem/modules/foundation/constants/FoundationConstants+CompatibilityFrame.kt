//
//  FoundationConstants+CompatibilityFrame.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The layout values of the compatibility frame. */
object CompatibilityFrameFloats {
    /** The ratio of the frame's width to its height. */
    const val FRAME_ASPECT_RATIO = 9f / 19.5f

    /** The minimum width of the frame. */
    const val FRAME_MIN_WIDTH = 360f

    /**
     * The width and height at or above which a window counts as
     * large.
     */
    const val LARGE_WINDOW_MIN_DIMENSION = 600f
}

// MARK: - Color

/** The colors of the compatibility frame. */
object CompatibilityFrameColors {
    /** The color of the window area outside the frame. */
    val LETTERBOX = Color.Black
}
