//
//  MessageContextMenuConstants.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

internal val MENU_WIDTH = 250.dp
internal val MENU_GAP = 8.dp
internal val EDGE_MARGIN = 12.dp
internal val MENU_CORNER_RADIUS = 12.dp
internal val MENU_ROW_HEIGHT = 44.dp
internal val MENU_ROW_START_PADDING = 16.dp
internal val MENU_ICON_SIZE = 22.dp
internal val MENU_DIVIDER_THICKNESS = 0.6.dp
internal const val LIGHT_SCRIM_ALPHA = 0.2f
internal const val LIFT_SCALE_BONUS = 0.08f
internal const val MENU_MIN_SCALE = 0.0001f
internal const val MENU_BACKGROUND_ALPHA = 0.5f
internal val DESTRUCTIVE_COLOR = Color(0xFFFF3B30)
internal val REACTION_ROW_GAP = 8.dp
internal val BLUR_RADIUS = 20.dp

// A blur separates the content on every API level (hardware on 31+,
// a software-blurred capture below), so the light tint applies
// throughout.
internal fun contextMenuScrimAlpha(): Float = LIGHT_SCRIM_ALPHA

internal fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())
