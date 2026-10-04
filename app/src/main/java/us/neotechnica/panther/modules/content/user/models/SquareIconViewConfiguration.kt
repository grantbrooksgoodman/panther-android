//
//  SquareIconViewConfiguration.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.modules.content.user.constants.SquareIconViewColors
import us.neotechnica.panther.modules.content.user.constants.SquareIconViewFloats

/**
 * The appearance of a square icon.
 *
 * A configuration describes the icon's size, background color,
 * overlay, and shadow.
 *
 * @property size The size of the icon.
 * @property backgroundColor The icon's background color.
 * @property overlay The content overlaid on the icon.
 * @property includesShadow A Boolean value that indicates whether the
 *   icon casts a shadow.
 */
data class SquareIconViewConfiguration(
    val size: DpSize =
        DpSize(
            SquareIconViewFloats.defaultFrameWidth,
            SquareIconViewFloats.defaultFrameHeight,
        ),
    val backgroundColor: Color,
    val overlay: OverlayConfiguration,
    val includesShadow: Boolean = false,
) {
    /** The content overlaid on a square icon. */
    sealed interface OverlayConfiguration {
        /** A symbol, with its foreground color and frame size relative to the icon. */
        data class Symbol(
            val name: String,
            val foregroundColor: Color = SquareIconViewColors.overlaySymbolForeground,
            val framePercentOfTotalSize: Float = SquareIconViewFloats.OVERLAY_FRAME_HEIGHT_MULTIPLIER,
        ) : OverlayConfiguration

        /** A text string, with its font and foreground color. */
        data class Text(
            val string: String,
            val font: Font = Font.system(FontScale.Custom(SquareIconViewFloats.OVERLAY_TEXT_FONT_SCALE)),
            val foregroundColor: Color = SquareIconViewColors.overlaySymbolForeground,
        ) : OverlayConfiguration
    }
}
