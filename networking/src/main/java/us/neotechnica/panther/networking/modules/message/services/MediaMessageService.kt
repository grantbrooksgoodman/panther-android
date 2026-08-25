//
//  MediaMessageService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.message.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.schema.message.models.LocalMediaFilePath
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * The service that downloads media message content for display.
 *
 * **Note:** This viewing-phase port provides download only; upload and
 * deletion land with the media-send phase. Hosted plain-text document
 * payloads are LZFSE-compressed on the wire; that decompression is not
 * yet ported, so plain-text documents are stored as downloaded.
 */
object MediaMessageService {
    // MARK: - Get Media Component

    /**
     * Returns the media file for the given message, using the local copy
     * when available and downloading it otherwise.
     *
     * @param messageID The identifier of the message.
     * @param localMediaFilePath The local file paths for the message's
     *   media.
     *
     * @return The media file.
     *
     * @throws Exception if the media cannot be resolved or downloaded.
     */
    suspend fun getMediaComponent(
        messageID: String,
        localMediaFilePath: LocalMediaFilePath,
    ): MediaFile =
        MediaFile.from(localMediaFilePath.relativePathString)
            ?: downloadMediaFile(messageID, localMediaFilePath)

    // MARK: - Auxiliary

    private suspend fun downloadMediaFile(
        messageID: String,
        localPath: LocalMediaFilePath,
    ): MediaFile {
        val storage = Networking.config.storageDelegate
        val destination =
            localPath.localPathFile
                ?: throw Exception(
                    "Failed to resolve local media path.",
                    metadata = ExceptionMetadata(this),
                )

        storage.download(localPath.relativePathString, destination)

        // The thumbnail is a best-effort companion object; a missing one
        // does not prevent the primary media from resolving.
        val thumbnailPath = localPath.relativeThumbnailPathString
        val thumbnailFile = localPath.localThumbnailPathFile
        if (thumbnailPath != null && thumbnailFile != null) {
            runCatching { storage.download(thumbnailPath, thumbnailFile) }
        }

        return MediaFile.from(localPath.relativePathString)
            ?: throw Exception(
                "Failed to generate media file.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo = mapOf("MessageID" to messageID))
    }
}
