//
//  MessageContextMenuConstants.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Float

internal object MessageContextMenuFloats {
    val MENU_WIDTH = 250.dp
    val MENU_GAP = 8.dp
    val EDGE_MARGIN = 12.dp
    val MENU_CORNER_RADIUS = 12.dp
    val MENU_ROW_HEIGHT = 44.dp
    val MENU_ROW_START_PADDING = 16.dp
    val MENU_ICON_SIZE = 22.dp
    val MENU_DIVIDER_THICKNESS = 0.6.dp
    const val LIGHT_SCRIM_ALPHA = 0.2f
    const val LIFT_SCALE_BONUS = 0.08f
    const val MENU_MIN_SCALE = 0.0001f
    const val MENU_BACKGROUND_ALPHA = 0.5f
    val INTERACTION_DRAG_THRESHOLD = 10.dp
    val GESTURE_PRESS_DURATION = 220.milliseconds
    val REACTION_ROW_GAP = 8.dp
    val BLUR_RADIUS = 20.dp
}

// MARK: - Color

internal object MessageContextMenuColors {
    val MENU_ELEMENT_HIGHLIGHTED_BACKGROUND = Color.Black.copy(alpha = 0.2f)
    val DESTRUCTIVE_COLOR = Color(0xFFFF3B30)
}
