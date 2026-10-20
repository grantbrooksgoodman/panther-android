//
//  AppConstants+AvatarImageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color

// MARK: - Float

/** The layout and timing values of the avatar image view. */
object AvatarImageViewFloats {
    /** The fraction of the frame spanned by the group placeholder's cut-out glyph. */
    const val BADGE_IMAGE_GLYPH_FRACTION = 0.73f

    /** The corner radius of the badge view. */
    const val BADGE_VIEW_CORNER_RADIUS = 8f

    /** The font size of the badge view label. */
    const val BADGE_VIEW_LABEL_SYSTEM_FONT_SIZE = 14f

    /** The shadow radius of the badge view. */
    const val BADGE_VIEW_SHADOW_RADIUS = 20f

    /** The height of the badge view. */
    const val BADGE_VIEW_HEIGHT = 20f

    /** The width of the badge view. */
    const val BADGE_VIEW_WIDTH = 20f

    /** The horizontal offset of the badge view. */
    const val BADGE_VIEW_OFFSET_X = 15f

    /** The vertical offset of the badge view. */
    const val BADGE_VIEW_OFFSET_Y = 15f

    /** The corner radius. */
    const val CORNER_RADIUS = 10f

    /** The scale at which the default placeholder's Material symbol's circle fills the frame. */
    const val DEFAULT_IMAGE_SCALE = 1.2f

    /** The frame height. */
    const val FRAME_HEIGHT = 50f

    /** The frame width. */
    const val FRAME_WIDTH = 50f
}

// MARK: - Color

/** The colors of the avatar image view. */
object AvatarImageViewColors {
    /** The foreground color of the badge view in dark mode. */
    val BADGE_VIEW_DARK_FOREGROUND = Color(0xFF27252A)

    /** The foreground color of the badge view in light mode. */
    val BADGE_VIEW_LIGHT_FOREGROUND = Color(0xFFE5E5EA)

    /** The shadow color of the badge view label. */
    val BADGE_VIEW_LABEL_SHADOW = Color.Black

    /** The foreground color of the image. */
    val IMAGE_FOREGROUND = Color(0xFF8E8E93)
}

// MARK: - String

/** The string constants of the avatar image view. */
object AvatarImageViewStrings {
    /** The system name of the badge image. */
    const val BADGE_IMAGE_SYSTEM_NAME = "person.2.circle.fill"

    /** The system name of the default image. */
    const val DEFAULT_IMAGE_SYSTEM_NAME = "person.crop.circle.fill"
}
