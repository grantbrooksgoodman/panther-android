//
//  GIFImage.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import us.neotechnica.panther.R
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService

/**
 * Displays an animated GIF that plays while [isActive] is `true`.
 *
 * The drawable is decoded once and primed while hidden, so it appears
 * instantly; playback is withheld until [isActive] becomes `true`.
 *
 * In dark mode, the image's opaque pixels are rendered white – the
 * equivalent of zeroing its brightness and inverting the result – so
 * dark artwork remains visible. The filter is re-applied whenever the
 * active theme changes.
 *
 * **Note:** Animated playback requires API 28 or later; on earlier
 * releases the static wordmark is shown instead.
 *
 * @param name The name of the GIF resource in `res/raw`.
 * @param isActive Whether the GIF is currently playing.
 * @param modifier The modifier for this view.
 */
@Composable
fun GIFImage(
    name: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val resourceID =
        remember(name) {
            context.resources.getIdentifier(name, "raw", context.packageName)
        }

    AndroidView(
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && resourceID != 0) {
                    runCatching {
                        val source = ImageDecoder.createSource(viewContext.resources, resourceID)
                        setImageDrawable(ImageDecoder.decodeDrawable(source))
                    }.onFailure { setImageResource(R.drawable.hello_wordmark) }
                } else {
                    setImageResource(R.drawable.hello_wordmark)
                }
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.colorFilter = if (isDarkModeActive) ColorMatrixColorFilter(DARK_MODE_COLOR_MATRIX) else null

            val drawable = imageView.drawable
            if (drawable is AnimatedImageDrawable) {
                if (isActive) drawable.start() else drawable.stop()
            }
        },
    )
}

// MARK: - Constants

/**
 * Maps every opaque pixel to white while preserving alpha: the red,
 * green, and blue rows carry only a full-brightness offset, and the
 * alpha row passes the source alpha through unchanged.
 */
private val DARK_MODE_COLOR_MATRIX =
    ColorMatrix(
        FloatArray(COLOR_MATRIX_SIZE) { index ->
            when {
                index < ALPHA_ROW_START && index % COLOR_MATRIX_COLUMNS == COLOR_MATRIX_COLUMNS - 1 -> MAXIMUM_CHANNEL_VALUE
                index == ALPHA_SCALE_INDEX -> 1f
                else -> 0f
            }
        },
    )

private const val ALPHA_ROW_START = 15
private const val ALPHA_SCALE_INDEX = 18
private const val COLOR_MATRIX_COLUMNS = 5
private const val COLOR_MATRIX_SIZE = 20
private const val MAXIMUM_CHANNEL_VALUE = 255f
