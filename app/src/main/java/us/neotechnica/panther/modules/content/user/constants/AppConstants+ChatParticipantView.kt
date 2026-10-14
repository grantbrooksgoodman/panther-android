//
//  AppConstants+ChatParticipantView.kt
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

object ChatParticipantViewFloats {
    val avatarImageViewSizeHeight: Dp = 40.dp
    val avatarImageViewSizeWidth: Dp = 40.dp
    val avatarImageViewTrailingPadding: Dp = 2.dp

    /** The width of the delete button a trailing swipe reveals. */
    val deleteButtonWidth: Dp = 74.dp
}

// MARK: - Color

object ChatParticipantViewColors {
    val deleteButtonTint = Color(0xFFFF3B30)
}

// MARK: - String

object ChatParticipantViewStrings {
    const val DELETE_BUTTON_IMAGE_SYSTEM_NAME = "trash"
}
