//
//  MessageContextReactionRowConstants.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

internal object MessageContextReactionRowFloats {
    val reactionButtonHeight: Dp = 35.dp
    val stackViewSpacing: Dp = 8.dp

    const val REACTION_BUTTON_TITLE_LABEL_SYSTEM_FONT_SIZE = 15f
    const val VIEW_ALPHA = 0.8f
}

// MARK: - Color

internal object MessageContextReactionRowColors {
    val reactionButtonDarkBackground = Color(0xFF48484A)
    val reactionButtonLightBackground = Color(0xFFE5E5EA)
}
