//
//  MediaMessageBubble.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.MediaMessageBubbleColors
import us.neotechnica.panther.modules.content.user.constants.MediaMessageBubbleFloats
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsStrings
import us.neotechnica.panther.modules.content.user.services.MediaMessagePreviewService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.subsystem.modules.localization.models.localized

/**
 * A message bubble carrying media: an inline image, a video thumbnail
 * with a play affordance, or a document rendered as a photo through its
 * thumbnail inside a gray outlined bubble.
 *
 * A placeholder spinner shows only while the file is absent; a file
 * that cannot be decoded, or a document without a thumbnail, shows the
 * missing-image glyph instead.
 *
 * The bubble carries no tap handler of its own: the enclosing
 * `MessageContextMenu` owns the tap (to open the preview) alongside the
 * long-press and double-tap. Pinching the bubble also opens the
 * preview.
 *
 * @param mediaFile The media file.
 * @param isOwn Whether the message is from the current user.
 * @param onPinch Invoked when the bubble is pinched outward.
 */
@Composable
fun MediaMessageBubble(
    mediaFile: MediaFile,
    isOwn: Boolean,
    onPinch: () -> Unit = {},
) {
    val shape = bubbleShape(isOwn)
    val pinchModifier =
        Modifier.pointerInput(mediaFile.relativePath) {
            detectTransformGestures { _, _, zoom, _ -> if (zoom > PINCH_OPEN_ZOOM_THRESHOLD) onPinch() }
        }

    val fileExtension = mediaFile.fileExtension
    val isFilePresent = mediaFile.localPathFile?.exists() == true
    Box(modifier = pinchModifier) {
        when {
            !isFilePresent -> Placeholder(shape, isOwn)
            fileExtension.isImage -> ImageContent(mediaFile, shape)
            fileExtension.isVideo -> VideoContent(mediaFile, shape)
            else -> DocumentContent(mediaFile, shape)
        }
    }
}

// MARK: - Image

@Composable
private fun ImageContent(
    mediaFile: MediaFile,
    shape: Shape,
) {
    val image = remember(mediaFile.relativePath) { MediaMessagePreviewService.thumbnail(mediaFile) }
    if (image == null) {
        MissingImage(shape)
        return
    }

    Image(
        bitmap = image,
        contentDescription = LocalizedStringKey.Image.localized(),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .width(MediaMessageBubbleFloats.imageMaxWidth)
                .height(displayHeight(image))
                .clip(shape),
    )
}

// MARK: - Video

@Composable
private fun VideoContent(
    mediaFile: MediaFile,
    shape: Shape,
) {
    val thumbnail = remember(mediaFile.relativePath) { MediaMessagePreviewService.thumbnail(mediaFile) }
    val height =
        thumbnail?.let { displayHeight(it) }
            ?: (MediaMessageBubbleFloats.imageMaxWidth / MediaMessageBubbleFloats.VIDEO_DEFAULT_ASPECT_RATIO)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(MediaMessageBubbleFloats.imageMaxWidth)
                .height(height)
                .clip(shape)
                .background(MediaMessageBubbleColors.videoPlaceholderBackground),
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = LocalizedStringKey.Video.localized(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(MediaMessageBubbleFloats.imageMaxWidth).height(height),
            )
        } else {
            MissingImageGlyph()
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
            foregroundColor = MediaMessageBubbleColors.playGlyph,
            modifier = Modifier.size(MediaMessageBubbleFloats.playGlyphSize),
        )
    }
}

// MARK: - Document

// A document renders as a photo through its thumbnail inside a gray
// outlined bubble; without a thumbnail, the missing-image glyph shows.
@Composable
private fun DocumentContent(
    mediaFile: MediaFile,
    shape: Shape,
) {
    val thumbnail = remember(mediaFile.relativePath) { MediaMessagePreviewService.thumbnail(mediaFile) }
    val colors = LocalPantherColors.current
    val outline =
        Modifier.border(
            ChatPageViewFloats.MessagesDisplayDelegate.bubbleOutlineWidth,
            ChatPageViewColors.MessagesDisplayDelegate.documentBubbleOutline,
            shape,
        )

    if (thumbnail == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .width(MediaMessageBubbleFloats.placeholderWidth)
                    .height(MediaMessageBubbleFloats.placeholderHeight)
                    .clip(shape)
                    .background(colors.receiverBubble)
                    .then(outline),
        ) {
            MissingImageGlyph()
        }
        return
    }

    Image(
        bitmap = thumbnail,
        contentDescription = LocalizedStringKey.Document.localized(),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .width(MediaMessageBubbleFloats.imageMaxWidth)
                .height(displayHeight(thumbnail))
                .clip(shape)
                .then(outline),
    )
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

@Composable
private fun MissingImage(shape: Shape) {
    val colors = LocalPantherColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(MediaMessageBubbleFloats.placeholderWidth)
                .height(MediaMessageBubbleFloats.placeholderHeight)
                .clip(shape)
                .background(colors.receiverBubble),
    ) {
        MissingImageGlyph()
    }
}

@Composable
private fun MissingImageGlyph() {
    Components.Symbol(
        UserContentExtensionsStrings.UIImage.MISSING_IMAGE_SYSTEM_NAME,
        foregroundColor = MediaMessageBubbleColors.missingImageGlyph,
        modifier = Modifier.size(MediaMessageBubbleFloats.missingImageGlyphSize),
    )
}

// MARK: - Auxiliary

private fun displayHeight(image: ImageBitmap): Dp =
    (MediaMessageBubbleFloats.imageMaxWidth / (image.width.toFloat() / image.height))
        .coerceAtMost(MediaMessageBubbleFloats.imageMaxHeight)

private const val PINCH_OPEN_ZOOM_THRESHOLD = 1.05f
