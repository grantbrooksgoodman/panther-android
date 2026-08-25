//
//  MediaMessageBubble.kt
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.MediaMessageBubbleColors
import us.neotechnica.panther.modules.content.user.constants.MediaMessageBubbleFloats
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import androidx.compose.material3.Text as Material3Text

/**
 * A message bubble carrying media: an inline image, a video thumbnail
 * with a play affordance, or a document card. While the media is still
 * downloading ([mediaFile] `null`), a loading placeholder is shown.
 * Tapping resolved media invokes [onTap] to open the full-screen
 * preview.
 *
 * @param mediaFile The resolved media file, or `null` while downloading.
 * @param isOwn Whether the message is from the current user.
 * @param onTap The action performed when the bubble is tapped.
 */
@Composable
fun MediaMessageBubble(
    mediaFile: MediaFile?,
    isOwn: Boolean,
    onTap: () -> Unit,
) {
    val shape =
        RoundedCornerShape(
            topStart = MediaMessageBubbleFloats.bubbleRadius,
            topEnd = MediaMessageBubbleFloats.bubbleRadius,
            bottomStart = if (isOwn) MediaMessageBubbleFloats.bubbleRadius else MediaMessageBubbleFloats.bubbleTailRadius,
            bottomEnd = if (isOwn) MediaMessageBubbleFloats.bubbleTailRadius else MediaMessageBubbleFloats.bubbleRadius,
        )

    val fileExtension = mediaFile?.fileExtension
    when {
        mediaFile == null -> Placeholder(shape, isOwn)
        fileExtension?.isImage == true -> ImageContent(mediaFile, shape, onTap)
        fileExtension?.isVideo == true -> VideoContent(mediaFile, shape, onTap)
        else -> DocumentContent(mediaFile, isOwn, shape, onTap)
    }
}

// MARK: - Image

@Composable
private fun ImageContent(
    mediaFile: MediaFile,
    shape: Shape,
    onTap: () -> Unit,
) {
    val image =
        remember(mediaFile.relativePath) {
            MediaImageLoader.decode(mediaFile.localPathFile, MediaMessageBubbleFloats.IMAGE_DECODE_MAX_DIMENSION)
        }
    if (image == null) {
        Placeholder(shape, isOwn = false)
        return
    }

    val displayHeight =
        (MediaMessageBubbleFloats.imageMaxWidth / (image.width.toFloat() / image.height))
            .coerceAtMost(MediaMessageBubbleFloats.imageMaxHeight)

    Image(
        bitmap = image,
        contentDescription = LocalizedStringKey.Image.localized(),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .width(MediaMessageBubbleFloats.imageMaxWidth)
                .height(displayHeight)
                .clip(shape)
                .clickable(onClick = onTap),
    )
}

// MARK: - Video

@Composable
private fun VideoContent(
    mediaFile: MediaFile,
    shape: Shape,
    onTap: () -> Unit,
) {
    val thumbnail =
        remember(mediaFile.relativePath) {
            MediaImageLoader.decode(mediaFile.thumbnailFile, MediaMessageBubbleFloats.IMAGE_DECODE_MAX_DIMENSION)
        }
    val displayHeight =
        if (thumbnail != null) {
            (MediaMessageBubbleFloats.imageMaxWidth / (thumbnail.width.toFloat() / thumbnail.height))
                .coerceAtMost(MediaMessageBubbleFloats.imageMaxHeight)
        } else {
            MediaMessageBubbleFloats.imageMaxWidth / MediaMessageBubbleFloats.VIDEO_DEFAULT_ASPECT_RATIO
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(MediaMessageBubbleFloats.imageMaxWidth)
                .height(displayHeight)
                .clip(shape)
                .background(MediaMessageBubbleColors.videoPlaceholderBackground)
                .clickable(onClick = onTap),
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = LocalizedStringKey.Video.localized(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(MediaMessageBubbleFloats.imageMaxWidth).height(displayHeight),
            )
        }
        PlayBadge()
    }
}

@Composable
private fun PlayBadge() {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(MediaMessageBubbleFloats.playBackgroundSize)
                .clip(CircleShape)
                .background(MediaMessageBubbleColors.playBackground),
    ) {
        Components.Symbol(
            "play.fill",
            color = MediaMessageBubbleColors.playGlyph,
            modifier = Modifier.size(MediaMessageBubbleFloats.playGlyphSize),
        )
    }
}

// MARK: - Document

@Composable
private fun DocumentContent(
    mediaFile: MediaFile,
    isOwn: Boolean,
    shape: Shape,
    onTap: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val foreground = if (isOwn) MediaMessageBubbleColors.playGlyph else colors.titleText
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(shape)
                .background(if (isOwn) colors.senderBubble else colors.receiverBubble)
                .clickable(onClick = onTap)
                .padding(MediaMessageBubbleFloats.documentPadding)
                .widthIn(max = MediaMessageBubbleFloats.imageMaxWidth),
    ) {
        Components.Symbol(
            "doc.fill",
            color = foreground,
            modifier = Modifier.size(MediaMessageBubbleFloats.documentIconSize),
        )
        Material3Text(
            "${mediaFile.name}.${mediaFile.fileExtension.rawValue}",
            color = foreground,
            style = Font.system.textStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = MediaMessageBubbleFloats.documentTextStartPadding),
        )
    }
}

// MARK: - Placeholder

@Composable
private fun Placeholder(
    shape: Shape,
    isOwn: Boolean,
) {
    val colors = LocalPantherColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(MediaMessageBubbleFloats.placeholderWidth)
                .height(MediaMessageBubbleFloats.placeholderHeight)
                .clip(shape)
                .background(if (isOwn) colors.senderBubble else colors.receiverBubble),
    ) {
        CircularProgressIndicator(color = colors.subtitleText)
    }
}
