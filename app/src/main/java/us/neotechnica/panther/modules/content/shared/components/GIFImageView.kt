//
//  GIFImageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 27/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import us.neotechnica.panther.R

/**
 * Displays an animated GIF that plays while [isActive] is `true`.
 *
 * The drawable is decoded once and primed while hidden, so it appears
 * instantly; playback is withheld until [isActive] becomes `true`.
 *
 * **Note:** Animated playback requires API 28 or later; on earlier
 * releases the static wordmark is shown instead.
 *
 * @param name The name of the GIF resource in `res/raw`.
 * @param isActive Whether the GIF is currently playing.
 * @param modifier The modifier for this view.
 */
@Composable
fun GIFImageView(
    name: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
            val drawable = imageView.drawable
            if (drawable is AnimatedImageDrawable) {
                if (isActive) drawable.start() else drawable.stop()
            }
        },
    )
}
