//
//  LocalMediaFilePath.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.io.File

/**
 * The local file paths for a media message's content.
 */
data class LocalMediaFilePath(
    /** The media file's path, relative to the documents directory. */
    val relativePathString: String,
    /**
     * The thumbnail's path, relative to the documents directory, or
     * `null` if there is no thumbnail.
     */
    val relativeThumbnailPathString: String? = null,
) {
    // MARK: - Computed Properties

    /** The absolute file of the media file. */
    val localPathFile: File?
        get() = FileStore.resolve(relativePathString)

    /** The absolute file of the thumbnail, or `null` if there is no thumbnail. */
    val localThumbnailPathFile: File?
        get() = relativeThumbnailPathString?.let { FileStore.resolve(it) }

    // MARK: - Companion

    companion object {
        /**
         * Creates a media file path from the given content type.
         *
         * @param contentType The content type to derive the paths from.
         *
         * @return A media file path, or `null` if the content type is not
         *   media.
         */
        fun from(contentType: HostedContentType): LocalMediaFilePath? {
            val media = contentType as? HostedContentType.Media ?: return null
            val pathPrefix = "${NetworkPath.media.rawValue}/${media.id}"
            val filePath = "$pathPrefix.${media.fileExtension.rawValue}"
            val thumbnailPath = "$pathPrefix${MediaFile.THUMBNAIL_IMAGE_NAME_SUFFIX}"

            return LocalMediaFilePath(
                relativePathString = filePath,
                relativeThumbnailPathString =
                    if (media.fileExtension.isDocument || media.fileExtension.isVideo) thumbnailPath else null,
            )
        }

        /**
         * Creates a media file path from the given message.
         *
         * @param message The message to derive the paths from.
         *
         * @return A media file path, or `null` if the message is not a
         *   media message.
         */
        fun from(message: Message): LocalMediaFilePath? = from(message.contentType)
    }
}
