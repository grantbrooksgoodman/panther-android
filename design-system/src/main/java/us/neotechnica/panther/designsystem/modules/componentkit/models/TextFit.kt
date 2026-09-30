//
//  TextFit.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Describes how a text component fits its bounds, bundling the line,
 * shrink, overflow, and alignment behavior that iOS expresses through
 * `lineLimit`, `minimumScaleFactor`, `truncationMode`, and
 * `multilineTextAlignment`.
 *
 * The default value imposes no constraints: the text spans as many
 * lines as it needs, never shrinks, clips anything that overflows, and
 * uses the default alignment.
 *
 * @param maxLines The maximum number of lines the text may span
 *   before it overflows.
 * @param minimumScaleFactor The smallest fraction of the font's point
 *   size the text may shrink to in order to fit its space. A value of
 *   `1` disables shrinking.
 * @param overflow The handling for text that overflows its space once
 *   shrinking and wrapping are exhausted.
 * @param textAlign The horizontal alignment of the text, or `null` to
 *   use the default.
 */
data class TextFit(
    val maxLines: Int = Int.MAX_VALUE,
    val minimumScaleFactor: Float = 1f,
    val overflow: TextOverflow = TextOverflow.Clip,
    val textAlign: TextAlign? = null,
)
