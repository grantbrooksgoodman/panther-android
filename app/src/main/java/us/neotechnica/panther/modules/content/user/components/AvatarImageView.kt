//
//  AvatarImageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
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
 */
@Composable
fun AvatarImageView(
    image: ImageBitmap?,
    badgeCount: Int = 0,
    size: DpSize? = null,
    modifier: Modifier = Modifier,
) {
    val resolvedSize = size ?: DpSize(AvatarFloats.FRAME_WIDTH.dp, AvatarFloats.FRAME_HEIGHT.dp)
    Box(
        modifier =
            modifier
                .size(resolvedSize)
                .clip(RoundedCornerShape(AvatarFloats.CORNER_RADIUS.dp)),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Components.Symbol(
                if (badgeCount < 2 && badgeCount != -1) {
                    AvatarStrings.DEFAULT_IMAGE_SYSTEM_NAME
                } else {
                    AvatarStrings.BADGE_IMAGE_SYSTEM_NAME
                },
                foregroundColor = AvatarColors.IMAGE_FOREGROUND,
                usesIntrinsicSize = false,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }

        if (badgeCount > 1) BadgeView(badgeCount)
    }
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
