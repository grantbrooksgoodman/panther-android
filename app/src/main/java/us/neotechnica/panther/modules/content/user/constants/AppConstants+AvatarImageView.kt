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

object AvatarImageViewFloats {
    const val BADGE_VIEW_CORNER_RADIUS = 8f
    const val BADGE_VIEW_LABEL_SYSTEM_FONT_SIZE = 14f
    const val BADGE_VIEW_SHADOW_RADIUS = 20f

    const val BADGE_VIEW_HEIGHT = 20f
    const val BADGE_VIEW_WIDTH = 20f

    const val BADGE_VIEW_OFFSET_X = 15f
    const val BADGE_VIEW_OFFSET_Y = 15f

    const val CORNER_RADIUS = 10f

    const val FRAME_HEIGHT = 50f
    const val FRAME_WIDTH = 50f
}

// MARK: - Color

object AvatarImageViewColors {
    val BADGE_VIEW_DARK_FOREGROUND = Color(0xFF27252A)
    val BADGE_VIEW_LIGHT_FOREGROUND = Color(0xFFE5E5EA)

    val BADGE_VIEW_LABEL_SHADOW = Color.Black
    val IMAGE_FOREGROUND = Color.Gray
}

// MARK: - String

object AvatarImageViewStrings {
    const val BADGE_IMAGE_SYSTEM_NAME = "person.2.circle.fill"
    const val DEFAULT_IMAGE_SYSTEM_NAME = "person.crop.circle.fill"
}
