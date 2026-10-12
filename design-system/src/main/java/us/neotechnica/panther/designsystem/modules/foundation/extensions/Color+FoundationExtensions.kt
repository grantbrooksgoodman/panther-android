//
//  Color+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.extensions

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

// MARK: - Methods

/**
 * Returns a darker variant of the color, adjusted by the given
 * percentage.
 *
 * @param by The percentage to darken by. The default is 30.
 *
 * @return The adjusted color, with the alpha component preserved.
 */
fun Color.darker(by: Float = DEFAULT_ADJUSTMENT_PERCENTAGE): Color = adjust(by = -1 * abs(by))

/**
 * Returns a lighter variant of the color, adjusted by the given
 * percentage.
 *
 * @param by The percentage to lighten by. The default is 30.
 *
 * @return The adjusted color, with the alpha component preserved.
 */
fun Color.lighter(by: Float = DEFAULT_ADJUSTMENT_PERCENTAGE): Color = adjust(by = abs(by))

// MARK: - Auxiliary

private fun Color.adjust(by: Float): Color {
    val delta = by / PERCENTAGE_DIVISOR
    return Color(
        red = (red + delta).coerceIn(0f, 1f),
        green = (green + delta).coerceIn(0f, 1f),
        blue = (blue + delta).coerceIn(0f, 1f),
        alpha = alpha,
    )
}

private const val DEFAULT_ADJUSTMENT_PERCENTAGE = 30f
private const val PERCENTAGE_DIVISOR = 100f
