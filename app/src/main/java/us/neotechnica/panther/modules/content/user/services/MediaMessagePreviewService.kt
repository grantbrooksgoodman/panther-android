//
//  MediaMessagePreviewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.modules.content.user.components.MediaImageLoader
import us.neotechnica.panther.modules.content.user.constants.MediaMessageBubbleFloats
import us.neotechnica.panther.modules.content.user.constants.MediaPreviewOverlayFloats
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * The service that manages media message previews.
 *
 * [MediaMessagePreviewService] presents a full-screen viewer for a
 * tapped media message, letting the user page through all of the
 * conversation's media. It also caches decoded images and thumbnails
 * for reuse.
 *
 * **Note:** While a preview is presented, [isPreviewingMedia] is `true`;
 * the chat page uses this to distinguish being covered by the preview
 * from being dismissed.
 */
object MediaMessagePreviewService {
    // MARK: - Types

    // MARK: - Properties

    /** The cache of full-size images, keyed by local file path. */
    val cachedImages = LockIsolated(mapOf<String, ImageBitmap>())

    /** The cache of thumbnail images, keyed by local file path. */
    val cachedThumbnails = LockIsolated(mapOf<String, ImageBitmap>())

    /** A Boolean value that indicates whether a media preview is currently presented. */
    var isPreviewingMedia by mutableStateOf(false)
        private set

    /** The identifier of the message whose media is being previewed, or `null`. */
    var previewingMessageID by mutableStateOf<String?>(null)
        private set

    // MARK: - Did Tap Image

    /**
     * Presents the media preview for the media in the given message.
     *
     * This method dismisses the keyboard and opens a full-screen viewer
     * showing all of the conversation's media, starting at the tapped
     * item.
     *
     * This method has no effect when the message holds no downloaded
     * media, when a preview is already presented, or when a context
     * menu is presented.
     *
     * @param message The message whose media to preview.
     * @param isPresentingContextMenu Whether a context menu is
     *   presented.
     */
    fun didTapImage(
        message: Message,
        isPresentingContextMenu: Boolean,
    ) {
        if (!message.contentType.isMedia) return
        val file = message.richContent?.mediaComponent?.localPathFile ?: return
        if (!file.exists() || isPreviewingMedia || isPresentingContextMenu) return

        KeyboardService.resignFirstResponders()
        previewingMessageID = message.id
        isPreviewingMedia = true
    }

    // MARK: - Dismiss Preview

    /** Dismisses the presented media preview, if any. */
    fun dismissPreview() {
        previewingMessageID = null
        isPreviewingMedia = false
    }

    // MARK: - Images

    /**
     * Returns the decoded full-size image for the given media file,
     * decoding and caching it on first access.
     *
     * @param mediaFile The media file to decode.
     *
     * @return The image, or `null` if the file cannot be decoded.
     */
    fun image(mediaFile: MediaFile): ImageBitmap? {
        val file = mediaFile.localPathFile ?: return null
        cachedImages.wrappedValue[file.path]?.let { return it }
        val image = MediaImageLoader.decode(file, MediaPreviewOverlayFloats.IMAGE_DECODE_MAX_DIMENSION) ?: return null
        cachedImages.withValue { it.value = it.value + (file.path to image) }
        return image
    }

    /**
     * Returns the decoded thumbnail for the given media file, decoding
     * and caching it on first access. Images without a thumbnail file
     * decode a reduced copy of the image itself.
     *
     * @param mediaFile The media file to decode.
     *
     * @return The thumbnail, or `null` if none can be decoded.
     */
    fun thumbnail(mediaFile: MediaFile): ImageBitmap? {
        val file = (if (mediaFile.fileExtension.isImage) mediaFile.localPathFile else mediaFile.thumbnailFile) ?: return null
        cachedThumbnails.wrappedValue[file.path]?.let { return it }
        val thumbnail = MediaImageLoader.decode(file, MediaMessageBubbleFloats.IMAGE_DECODE_MAX_DIMENSION) ?: return null
        cachedThumbnails.withValue { it.value = it.value + (file.path to thumbnail) }
        return thumbnail
    }

    // MARK: - Clear Cache

    /** Removes every cached image and thumbnail. */
    fun clearCache() {
        cachedImages.wrappedValue = emptyMap()
        cachedThumbnails.wrappedValue = emptyMap()
    }
}
