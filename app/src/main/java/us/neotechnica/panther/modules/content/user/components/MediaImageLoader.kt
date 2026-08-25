//
//  MediaImageLoader.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * Decodes on-disk media images to [ImageBitmap]s, downsampling large
 * files to a bounded dimension to keep memory in check.
 */
object MediaImageLoader {
    // MARK: - Methods

    /**
     * Decodes the image at [file], downsampled so neither dimension
     * greatly exceeds [maxDimension] pixels.
     *
     * @param file The image file, or `null`.
     * @param maxDimension The target maximum dimension, in pixels.
     *
     * @return The decoded image, or `null` if [file] is `null`, missing,
     *   or not a decodable image.
     */
    fun decode(
        file: File?,
        maxDimension: Int,
    ): ImageBitmap? {
        if (file == null || !file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                }
            BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
        }.getOrNull()
    }

    // MARK: - Auxiliary

    private fun sampleSize(
        width: Int,
        height: Int,
        maxDimension: Int,
    ): Int {
        var sampleSize = 1
        val largest = maxOf(width, height)
        while (largest / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
