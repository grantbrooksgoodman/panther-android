//
//  AvatarImageView.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

/**
 * A circular avatar rendering, in priority order: the decoded [imageData],
 * the contact's [initials], or a [fallbackSymbol] glyph, over a neutral
 * gray disc. Callers size it through [modifier] and add any badge or
 * overlap decorations around it.
 *
 * @param modifier The modifier for this avatar; callers set its size.
 * @param imageData The avatar image bytes, or `null`.
 * @param initials The contact's initials, shown when there is no image.
 * @param fallbackSymbol The SF Symbol shown when there is no image or
 *   initials (for example, `person` or `person.2`).
 * @param glyphSize The fallback glyph's size.
 * @param initialsFont The font used to render [initials].
 */
@Composable
@Suppress("LongParameterList")
fun AvatarImageView(
    modifier: Modifier = Modifier,
    imageData: ByteArray? = null,
    initials: String = "",
    fallbackSymbol: String = "person",
    glyphSize: Dp = DEFAULT_GLYPH_SIZE,
    initialsFont: Font = Font.systemSemibold(),
) {
    val colors = LocalPantherColors.current
    val image =
        remember(imageData) {
            imageData?.takeIf { it.isNotEmpty() }?.let { bytes ->
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
            }
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(CircleShape).background(AVATAR_BACKGROUND),
    ) {
        when {
            image != null ->
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

            initials.isNotBlank() -> Components.Text(initials, color = colors.background, font = initialsFont)
            else -> Components.Symbol(fallbackSymbol, color = colors.background, modifier = Modifier.size(glyphSize))
        }
    }
}

private val AVATAR_BACKGROUND = Color(0xFFC7C7CC)
private val DEFAULT_GLYPH_SIZE = 24.dp
