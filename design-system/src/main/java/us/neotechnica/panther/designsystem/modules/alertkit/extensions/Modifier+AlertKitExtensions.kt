//
//  Modifier+AlertKitExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import us.neotechnica.panther.designsystem.modules.alertkit.constants.AlertKitExtensionsFloats

// MARK: - Constants Accessors

private typealias ExtensionsFloats = AlertKitExtensionsFloats

// Draws a one-pixel separator along the top edge, or along the leading
// edge when `isLeading` is true.
internal fun Modifier.separator(
    color: Color,
    isLeading: Boolean = false,
): Modifier =
    drawBehind {
        val thickness = ExtensionsFloats.SEPARATOR_THICKNESS_PIXELS
        if (isLeading) {
            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - thickness else 0f
            drawRect(
                color = color,
                size = Size(thickness, size.height),
                topLeft = Offset(x, 0f),
            )
        } else {
            drawRect(color, size = Size(size.width, thickness))
        }
    }
