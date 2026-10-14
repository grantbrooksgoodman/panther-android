//
//  SquareIconView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.modules.content.user.constants.SquareIconViewColors
import us.neotechnica.panther.modules.content.user.constants.SquareIconViewFloats
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration

// MARK: - Constants Accessors

private typealias SquareIconColors = SquareIconViewColors
private typealias SquareIconFloats = SquareIconViewFloats

/**
 * A rounded square icon with a configurable overlay.
 *
 * Use [SquareIconView] to display a colored, rounded square
 * containing an image resource, a symbol, or text, as described by
 * its configuration.
 *
 * @param configuration The configuration that describes the icon.
 * @param modifier The modifier for this view.
 */
@Composable
fun SquareIconView(
    configuration: SquareIconViewConfiguration,
    modifier: Modifier = Modifier,
) {
    // The corner radius is fixed for the default size and scales with
    // an icon drawn smaller or larger than it.
    val cornerRadius = configuration.cornerRadius
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(configuration.size)
                .then(if (configuration.includesShadow) Modifier.shadow(cornerRadius) else Modifier)
                .clip(shape)
                .background(configuration.backgroundColor),
    ) {
        OverlayView(configuration)
    }
}

// MARK: - Overlay

@Composable
private fun OverlayView(configuration: SquareIconViewConfiguration) {
    when (val overlay = configuration.overlay) {
        is SquareIconViewConfiguration.OverlayConfiguration.Resource ->
            Icon(
                painter = painterResource(overlay.resourceID),
                contentDescription = null,
                modifier =
                    Modifier.size(
                        width = (configuration.size.width * overlay.framePercentOfTotalSize).roundedToNearestOrEven(),
                        height = (configuration.size.height * overlay.framePercentOfTotalSize).roundedToNearestOrEven(),
                    ),
                tint = overlay.foregroundColor,
            )

        is SquareIconViewConfiguration.OverlayConfiguration.Symbol ->
            Components.Symbol(
                overlay.name,
                foregroundColor = overlay.foregroundColor,
                weight = overlay.weight,
                usesIntrinsicSize = false,
                modifier =
                    Modifier.size(
                        width = (configuration.size.width * overlay.framePercentOfTotalSize).roundedToNearestOrEven(),
                        height = (configuration.size.height * overlay.framePercentOfTotalSize).roundedToNearestOrEven(),
                    ),
            )

        is SquareIconViewConfiguration.OverlayConfiguration.Text ->
            Components.Text(
                overlay.string,
                foregroundColor = overlay.foregroundColor,
                font = overlay.font,
            )
    }
}

// MARK: - Auxiliary

// The icon's shadow: a blurred copy of its rounded square, offset
// downward, drawn behind it.
private fun Modifier.shadow(cornerRadius: Dp): Modifier =
    drawBehind {
        val cornerRadius = cornerRadius.toPx()
        val yOffset = SquareIconFloats.shadowYOffset.toPx()
        val paint =
            Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = SquareIconColors.shadow.copy(alpha = SquareIconFloats.SHADOW_COLOR_OPACITY).toArgb()
                maskFilter = BlurMaskFilter(SquareIconFloats.shadowRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
            }

        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(0f, yOffset, size.width, size.height + yOffset, cornerRadius, cornerRadius, paint)
        }
    }

private val SquareIconViewConfiguration.cornerRadius: Dp
    get() = SquareIconFloats.cornerRadius * (size.width / SquareIconFloats.defaultFrameWidth)

private fun Dp.roundedToNearestOrEven(): Dp = Math.rint(value.toDouble()).toFloat().dp
