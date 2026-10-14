//
//  AppConstants+ContactDetailView.kt
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

/** The layout and timing values of the contact detail view. */
object ContactDetailViewFloats {
    /** The trailing padding of the avatar image view. */
    val avatarImageViewTrailingPadding: Dp = 2.dp

    /** The corner radius of the card. */
    val cornerRadius: Dp = 8.dp

    /** The padding of the card content. */
    val padding: Dp = 16.dp
}

// MARK: - Color

/** The colors of the contact detail view. */
object ContactDetailViewColors {
    /** The background color in dark mode. */
    val darkBackground = Color(0xFF2A2A2C)

    /** The background color in light mode. */
    val lightBackground = Color.White
}
