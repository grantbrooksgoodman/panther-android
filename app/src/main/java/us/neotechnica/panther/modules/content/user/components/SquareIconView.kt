//
//  SquareIconView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.modules.content.user.constants.SquareIconViewFloats
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration

/**
 * A rounded square icon with a configurable overlay.
 *
 * Use [SquareIconView] to display a colored, rounded square
 * containing a symbol or text, as described by its configuration.
 *
 * @param configuration The configuration that describes the icon.
 * @param modifier The modifier for this view.
 */
@Composable
fun SquareIconView(
    configuration: SquareIconViewConfiguration,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(configuration.size.width * SquareIconViewFloats.CORNER_RADIUS_RATIO)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(configuration.size)
                .then(if (configuration.includesShadow) Modifier.shadow(SquareIconViewFloats.shadowRadius, shape) else Modifier)
                .clip(shape)
                .background(configuration.backgroundColor),
    ) {
        when (val overlay = configuration.overlay) {
            is SquareIconViewConfiguration.OverlayConfiguration.Symbol ->
                Components.Symbol(
                    overlay.name,
                    foregroundColor = overlay.foregroundColor,
                    modifier = Modifier.size(configuration.size.width * overlay.framePercentOfTotalSize),
                )

            is SquareIconViewConfiguration.OverlayConfiguration.Text ->
                Components.Text(
                    overlay.string,
                    foregroundColor = overlay.foregroundColor,
                    font = overlay.font,
                )
        }
    }
}
