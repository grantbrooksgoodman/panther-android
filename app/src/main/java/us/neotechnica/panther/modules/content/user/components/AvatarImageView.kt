//
//  AvatarImageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.AvatarImageViewColors
import us.neotechnica.panther.modules.content.user.constants.AvatarImageViewFloats
import us.neotechnica.panther.modules.content.user.constants.AvatarImageViewStrings

// MARK: - Constants Accessors

private typealias AvatarColors = AvatarImageViewColors
private typealias AvatarFloats = AvatarImageViewFloats
private typealias AvatarStrings = AvatarImageViewStrings

/**
 * A circular avatar rendering the given image, or a gray placeholder
 * glyph when there is none.
 *
 * When [badgeCount] is greater than one, a numeric badge overlays
 * the avatar's corner. When it is greater than one or `-1`, the
 * placeholder glyph is the group-conversation symbol.
 *
 * @param image The avatar image, or `null` for the placeholder.
 * @param badgeCount The number shown in the corner badge. Pass `0`
 *   to omit the badge, or `-1` for the group glyph without a badge.
 * @param size The avatar's size, or `null` for the 50-point default.
 * @param modifier The modifier for this avatar (for outer
 *   positioning).
 * @param isRedacted Whether the view is redacted, showing the
 *   placeholder glyph regardless of [image].
 */
@Composable
fun AvatarImageView(
    image: ImageBitmap?,
    badgeCount: Int = 0,
    size: DpSize? = null,
    modifier: Modifier = Modifier,
    isRedacted: Boolean = false,
) {
    val resolvedSize = size ?: DpSize(AvatarFloats.FRAME_WIDTH.dp, AvatarFloats.FRAME_HEIGHT.dp)

    // The badge is offset from the avatar's center and drawn outside
    // the avatar's corner clip.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(resolvedSize),
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(AvatarFloats.CORNER_RADIUS.dp))) {
            AvatarContent(image, badgeCount, isRedacted)
        }

        if (badgeCount > 1) BadgeView(badgeCount)
    }
}

// The avatar's image, or its placeholder symbol.
@Composable
private fun AvatarContent(
    image: ImageBitmap?,
    badgeCount: Int,
    isRedacted: Boolean,
) {
    when {
        isRedacted -> DefaultPlaceholderView()

        image != null ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )

        badgeCount < 2 && badgeCount != -1 -> DefaultPlaceholderView()

        else -> BadgePlaceholderView()
    }
}

// The group placeholder: a circle filling the frame with the symbol's
// glyph cut out of it, so the background shows through the glyph. The
// SF Symbol has its glyph cut out already; for a Material symbol, the
// cut-out is composed.
@Composable
private fun BadgePlaceholderView() {
    if (SFSymbol.resolvesToSFSymbol(AvatarStrings.BADGE_IMAGE_SYSTEM_NAME)) {
        return Components.Symbol(
            AvatarStrings.BADGE_IMAGE_SYSTEM_NAME,
            foregroundColor = AvatarColors.IMAGE_FOREGROUND,
            usesIntrinsicSize = false,
            modifier = Modifier.fillMaxSize(),
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clip(CircleShape)
                .background(AvatarColors.IMAGE_FOREGROUND),
    ) {
        Components.Symbol(
            AvatarStrings.BADGE_IMAGE_SYSTEM_NAME.removeSuffix(CIRCLE_FILL_SUFFIX),
            foregroundColor = Color.Black,
            usesIntrinsicSize = false,
            modifier =
                Modifier
                    .fillMaxSize(AvatarFloats.BADGE_IMAGE_GLYPH_FRACTION)
                    .drawWithContent {
                        drawContext.canvas.saveLayer(size.toRect(), Paint().apply { blendMode = BlendMode.DstOut })
                        drawContent()
                        drawContext.canvas.restore()
                    },
        )
    }
}

// The single-person placeholder. A Material symbol's circle spans only
// part of its glyph box, so it is scaled for the circle to fill the
// frame; an SF Symbol's circle already fills it.
@Composable
private fun DefaultPlaceholderView() {
    val scale =
        if (SFSymbol.resolvesToSFSymbol(AvatarStrings.DEFAULT_IMAGE_SYSTEM_NAME)) 1f else AvatarFloats.DEFAULT_IMAGE_SCALE
    Components.Symbol(
        AvatarStrings.DEFAULT_IMAGE_SYSTEM_NAME,
        foregroundColor = AvatarColors.IMAGE_FOREGROUND,
        usesIntrinsicSize = false,
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
    )
}

// MARK: - Badge

@Composable
private fun BadgeView(badgeCount: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .offset(x = AvatarFloats.BADGE_VIEW_OFFSET_X.dp, y = AvatarFloats.BADGE_VIEW_OFFSET_Y.dp)
                .size(AvatarFloats.BADGE_VIEW_WIDTH.dp, AvatarFloats.BADGE_VIEW_HEIGHT.dp)
                .shadow(AvatarFloats.BADGE_VIEW_SHADOW_RADIUS.dp / BADGE_SHADOW_ELEVATION_DIVISOR, CircleShape)
                .clip(RoundedCornerShape(AvatarFloats.BADGE_VIEW_CORNER_RADIUS.dp))
                .background(
                    if (isSystemInDarkTheme()) {
                        AvatarColors.BADGE_VIEW_DARK_FOREGROUND
                    } else {
                        AvatarColors.BADGE_VIEW_LIGHT_FOREGROUND
                    },
                ),
    ) {
        Components.Text(
            "$badgeCount",
            foregroundColor = LocalPantherColors.current.titleText,
            font = Font.systemSemibold(FontScale.Custom(AvatarFloats.BADGE_VIEW_LABEL_SYSTEM_FONT_SIZE)),
        )
    }
}

private const val BADGE_SHADOW_ELEVATION_DIVISOR = 4
private const val CIRCLE_FILL_SUFFIX = ".circle.fill"
