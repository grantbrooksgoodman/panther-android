//
//  CompatibilityFrame.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import us.neotechnica.panther.designsystem.modules.foundation.constants.CompatibilityFrameColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.CompatibilityFrameFloats
import kotlin.math.roundToInt

// MARK: - Constants Accessors

private typealias CompatibilityColors = CompatibilityFrameColors
private typealias CompatibilityFloats = CompatibilityFrameFloats

// MARK: - View

/**
 * A container that keeps its content at a phone's proportions on
 * a large window.
 *
 * On a window whose width and height are both at least 600 dp –
 * such as a tablet's – [CompatibilityFrame] centers its content
 * in a full-height frame with a phone's proportions, and fills
 * the rest of the window with black. On any other window, the
 * content fills the window.
 *
 * Wrap the root content of each window in a compatibility frame,
 * including the content of a dialog that fills the window, so
 * that every window frames its content identically. Within the
 * frame, text renders at the size its font specifies regardless
 * of the font size the user sets for the device, and
 * [LocalConfiguration] reports the frame's width as the screen
 * width, so content that sizes itself to a fraction of the screen
 * sizes itself to the frame.
 *
 * @param content The framed content.
 */
@Composable
fun CompatibilityFrame(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val frameWidth = compatibilityFrameWidth(LocalWindowInfo.current.containerDpSize)
    val frameConfiguration =
        remember(configuration, frameWidth) {
            frameWidth?.let { configuration.framed(it) } ?: configuration
        }

    // A unit font scale keeps each point size constant.
    val frameDensity = remember(density) { Density(density.density, fontScale = 1f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind { frameWidth?.let { drawLetterbox(it.roundToPx()) } },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .then(frameWidth?.let { Modifier.width(it) } ?: Modifier.fillMaxWidth())
                    .clipToBounds(),
        ) {
            CompositionLocalProvider(
                LocalConfiguration provides frameConfiguration,
                LocalDensity provides frameDensity,
                content = content,
            )
        }
    }
}

// MARK: - Methods

/**
 * Returns the width of the frame in which a compatibility frame
 * lays out its content on a window of the given size.
 *
 * Use this function to size content to the frame where no
 * composition is available to read [LocalConfiguration] from.
 *
 * @param windowSize The size of the window.
 *
 * @return The width of the frame, or `null` if the window isn't
 *   large enough for its content to be framed.
 */
fun compatibilityFrameWidth(windowSize: DpSize): Dp? {
    if (windowSize.isUnspecified) return null
    val isLargeWindow =
        windowSize.width >= CompatibilityFloats.LARGE_WINDOW_MIN_DIMENSION.dp &&
            windowSize.height >= CompatibilityFloats.LARGE_WINDOW_MIN_DIMENSION.dp
    if (!isLargeWindow) return null

    return (windowSize.height * CompatibilityFloats.FRAME_ASPECT_RATIO).coerceIn(
        minimumValue = CompatibilityFloats.FRAME_MIN_WIDTH.dp,
        maximumValue = windowSize.width,
    )
}

// MARK: - Auxiliary

// Fills the areas beside a frame of the given width, which content
// that isn't opaque – such as a dialog's dimming – never covers. The
// frame's left edge is where centering places it.
private fun DrawScope.drawLetterbox(frameWidth: Int) {
    val frameLeft = ((size.width - frameWidth) / 2).roundToInt().toFloat()
    drawRect(
        color = CompatibilityColors.LETTERBOX,
        size = Size(frameLeft, size.height),
    )

    drawRect(
        color = CompatibilityColors.LETTERBOX,
        size = Size(size.width - frameLeft - frameWidth, size.height),
        topLeft = Offset(frameLeft + frameWidth, 0f),
    )
}

// The configuration of a portrait screen as wide as the frame.
private fun Configuration.framed(frameWidth: Dp): Configuration =
    Configuration(this).apply {
        orientation = Configuration.ORIENTATION_PORTRAIT
        screenWidthDp = frameWidth.value.toInt()
        smallestScreenWidthDp = minOf(screenWidthDp, screenHeightDp)
    }
