//
//  MediaItemView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.MediaItemViewColors
import us.neotechnica.panther.modules.content.user.constants.MediaItemViewFloats
import us.neotechnica.panther.modules.content.user.models.MediaItemViewData
import us.neotechnica.panther.networking.modules.common.models.MediaFileExtension

/**
 * A row describing a media file in a conversation's shared-media list: a
 * thumbnail, the media type, the sender, and the timestamp. Tapping the
 * row opens the media preview. Mirrors the iOS `MediaItemView`.
 *
 * @param data The display inputs for the row.
 * @param onClick The action performed when the row is tapped.
 */
@Composable
fun MediaItemView(
    data: MediaItemViewData,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = MediaItemViewFloats.rowHorizontalPadding,
                    vertical = MediaItemViewFloats.rowVerticalPadding,
                ),
    ) {
        Thumbnail(data)
        Column(modifier = Modifier.weight(1f).padding(start = MediaItemViewFloats.textStartPadding)) {
            Components.Text(data.mediaTypeLabelText, color = colors.titleText, font = Font.systemSemibold())
            Components.Text(
                data.senderLabelText,
                color = MediaItemViewColors.senderLabelForeground,
                font = Font.system(FontScale.Small),
                modifier = Modifier.padding(top = MediaItemViewFloats.senderTopPadding),
            )
        }
        Spacer(modifier = Modifier.width(MediaItemViewFloats.textStartPadding))
        Components.Text(
            data.timestampLabelText,
            color = MediaItemViewColors.timestampLabelForeground,
            font = Font.system(FontScale.Small),
        )
    }
}

// MARK: - Thumbnail

@Composable
private fun Thumbnail(data: MediaItemViewData) {
    val file = data.file
    val bitmap =
        remember(file.relativePath) {
            val source = if (file.fileExtension.isImage) file.localPathFile else file.thumbnailFile
            MediaImageLoader.decode(source, MediaItemViewFloats.THUMBNAIL_DECODE_MAX_DIMENSION)
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(MediaItemViewFloats.imageSize)
                .clip(RoundedCornerShape(MediaItemViewFloats.imageCornerRadius))
                .background(MediaItemViewColors.thumbnailBackground),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = data.mediaTypeLabelText,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Components.Symbol(
                glyphFor(file.fileExtension),
                color = MediaItemViewColors.thumbnailGlyph,
                modifier = Modifier.size(MediaItemViewFloats.glyphSize),
            )
        }
    }
}

private fun glyphFor(fileExtension: MediaFileExtension): String =
    when {
        fileExtension.isVideo -> "film.fill"
        fileExtension.isImage -> "photo.fill"
        else -> "doc.fill"
    }
