//
//  PlatformColors.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.models

import androidx.compose.ui.graphics.Color

/**
 * Compose-color approximations of the iOS system colors referenced by
 * the ported themes, so the Android palette matches iOS within the
 * ~80% visual-parity budget.
 */
internal object PlatformColors {
    val black = Color.Black
    val gray = rgb(0x808080)
    val lightGray = rgb(0xAAAAAA)
    val systemBlue = rgb(0x007AFF)
    val systemGray3 = rgb(0xC7C7CC)
    val white = Color.White
}

/**
 * Returns an opaque [Color] from a `0xRRGGBB` value, mirroring the iOS
 * `UIColor(hex:)` helper.
 *
 * @param value The 24-bit red-green-blue value.
 *
 * @return The corresponding fully opaque color.
 */
internal fun rgb(value: Long): Color = Color(OPAQUE_ALPHA or value)

/** The alpha channel of a fully opaque `0xAARRGGBB` color. */
private const val OPAQUE_ALPHA = 0xFF000000L
