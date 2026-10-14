//
//  SquareIconViewConfiguration.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    // MARK: - Companion

    companion object {
        /**
         * Returns a configuration whose overlay is the symbol with the
         * given system name.
         *
         * @param backgroundColor The icon's background color.
         * @param systemName The system name of the overlaid symbol.
         * @param framePercentOfTotalSize The fraction of the icon's size
         *   the symbol's frame occupies.
         * @param weight The symbol's font weight, or `null` for the
         *   regular weight.
         *
         * @return The icon configuration.
         */
        fun symbol(
            backgroundColor: Color,
            systemName: String,
            framePercentOfTotalSize: Float = SquareIconViewFloats.OVERLAY_FRAME_HEIGHT_MULTIPLIER,
            weight: FontWeight? = null,
        ): SquareIconViewConfiguration =
            SquareIconViewConfiguration(
                backgroundColor = backgroundColor,
                overlay =
                    OverlayConfiguration.Symbol(
                        name = systemName,
                        framePercentOfTotalSize = framePercentOfTotalSize,
                        weight = weight,
                    ),
            )
    }

    // MARK: - Types

    /** The content overlaid on a square icon. */
    sealed interface OverlayConfiguration {
        /**
         * An image resource, with its foreground color, frame size
         * relative to the icon, and font weight.
         *
         * The resource is drawn as a template, tinted with its
         * foreground color. A resource has no weight variants, so
         * [weight] has no effect on it.
         *
         * @property resourceID The drawable resource to draw.
         * @property foregroundColor The color the resource is tinted
         *   with.
         * @property framePercentOfTotalSize The fraction of the icon's
         *   size the resource's frame occupies.
         * @property weight The font weight, which has no effect on a
         *   resource.
         */
        data class Resource(
            @DrawableRes val resourceID: Int,
            val foregroundColor: Color = SquareIconViewColors.overlaySymbolForeground,
            val framePercentOfTotalSize: Float = SquareIconViewFloats.OVERLAY_FRAME_HEIGHT_MULTIPLIER,
            val weight: FontWeight? = null,
        ) : OverlayConfiguration

        /**
         * A symbol, with its foreground color, frame size relative to
         * the icon, and font weight.
         *
         * @property name The system name of the symbol.
         * @property foregroundColor The symbol's foreground color.
         * @property framePercentOfTotalSize The fraction of the icon's
         *   size the symbol's frame occupies.
         * @property weight The symbol's font weight, or `null` for the
         *   regular weight.
         */
        data class Symbol(
            val name: String,
            val foregroundColor: Color = SquareIconViewColors.overlaySymbolForeground,
            val framePercentOfTotalSize: Float = SquareIconViewFloats.OVERLAY_FRAME_HEIGHT_MULTIPLIER,
            val weight: FontWeight? = null,
        ) : OverlayConfiguration

        /**
         * A text string, with its font and foreground color.
         *
         * @property string The text to draw.
         * @property font The text's font.
         * @property foregroundColor The text's foreground color.
         */
        data class Text(
            val string: String,
            val font: Font = Font.system(FontScale.Custom(SquareIconViewFloats.OVERLAY_TEXT_FONT_SCALE)),
            val foregroundColor: Color = SquareIconViewColors.overlaySymbolForeground,
        ) : OverlayConfiguration
    }
}
