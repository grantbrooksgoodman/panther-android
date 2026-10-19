//
//  ImageBitmap+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

// MARK: - Methods

/**
 * Returns an image rendering the given initials centered on a colored
 * background.
 *
 * @param initials The initials to render.
 * @param backgroundColor The background color. The default is the
 *   system gray.
 * @param fontSize The text size, in scaled pixels.
 * @param textColor The text color. The default is white.
 * @param size The image's width and height, in density-independent
 *   pixels.
 *
 * @return The rendered image.
 */
fun ImageBitmap.Companion.fromInitials(
    initials: String,
    backgroundColor: Int = SYSTEM_GRAY,
    fontSize: Float = DEFAULT_FONT_SIZE,
    textColor: Int = Color.WHITE,
    size: Int = DEFAULT_SIZE,
): ImageBitmap {
    val density = Resources.getSystem().displayMetrics.density
    val sidePixels = (size * density).toInt().coerceAtLeast(1)
    val bitmap = createBitmap(sidePixels, sidePixels)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)

    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = fontSize * density
            typeface = Typeface.create(Typeface.DEFAULT, SEMIBOLD_WEIGHT, false)
        }

    // Centered on the text's own bounds, as a drawn string is.
    val baseline = sidePixels / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(initials, sidePixels / 2f, baseline, paint)
    return bitmap.asImageBitmap()
}

private const val DEFAULT_FONT_SIZE = 20f
private const val DEFAULT_SIZE = 50
private const val SEMIBOLD_WEIGHT = 600
private val SYSTEM_GRAY = Color.rgb(142, 142, 147)
