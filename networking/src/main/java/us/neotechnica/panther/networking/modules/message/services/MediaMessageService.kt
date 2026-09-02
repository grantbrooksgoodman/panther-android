//
//  MediaMessageService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.message.services

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.message.models.HostedContentType
import us.neotechnica.panther.networking.modules.schema.message.models.LocalMediaFilePath
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.io.File

/**
 * The service that uploads, downloads, and deletes media message content.
 *
 * **Note:** iOS LZFSE-compresses plain-text document payloads on the wire;
 * Android has no LZFSE, so plain-text documents are uploaded and stored
 * uncompressed (see `DEVIATIONS.md`). The Android `StorageDelegate` also
 * carries no content-type metadata, so uploads omit it.
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

    // MARK: - Upload Media Component

    /**
     * Uploads [mediaComponent] and its thumbnail for [message].
     *
     * The file and its thumbnail are independent Storage objects, uploaded
     * concurrently and each skipped when already present, and the local
     * files are moved into their permanent locations afterward.
     *
     * @param mediaComponent The media file to upload.
     * @param message The message the media belongs to.
     *
     * @throws Exception if an upload fails.
     */
    @Suppress("UnusedParameter")
    suspend fun uploadMediaComponent(
        mediaComponent: MediaFile,
        message: Message,
    ) {
        val storage = Networking.config.storageDelegate
        val pathPrefix = "${NetworkPath.media.rawValue}/${mediaComponent.encodedHash.take(SHORTENED_HASH_LENGTH)}"
        val relativePath = "$pathPrefix.${mediaComponent.fileExtension.rawValue}"
        val thumbnailRelativePath = "$pathPrefix${MediaFile.THUMBNAIL_IMAGE_NAME_SUFFIX}"

        coroutineScope {
            val primary = async { uploadPrimary(storage, mediaComponent, relativePath) }
            val thumbnail = async { uploadThumbnail(storage, mediaComponent, thumbnailRelativePath) }
            primary.await()
            thumbnail.await()
        }
    }

    // MARK: - Delete Media Component

    /**
     * Deletes the media content – and its thumbnail – for [messageID]
     * from remote storage.
     *
     * The content is preserved when it is still referenced by other
     * messages, or when the message is not a media message.
     *
     * @param messageID The identifier of the message.
     *
     * @throws Exception if the content type cannot be resolved or deletion
     *   fails.
     */
    suspend fun deleteMediaComponent(messageID: String) {
        val storage = Networking.config.storageDelegate
        val database = Networking.config.databaseDelegate
        val exceptions = mutableListOf<Exception>()

        try {
            val contentTypeValue: String? =
                database.getValues("${NetworkPath.messages.rawValue}/$messageID/$CONTENT_TYPE_KEY")
            val hostedContentType =
                contentTypeValue?.let { HostedContentType.from(it) }
                    ?: throw Exception("Failed to resolve hosted content type.", metadata = ExceptionMetadata(this))
            if (!hostedContentType.isMedia) return
            val mediaFilePath =
                hostedContentType.mediaFilePath
                    ?: throw Exception("Failed to resolve media file path.", metadata = ExceptionMetadata(this))

            if (multipleMessagesReference(mediaFilePath)) return

            try {
                storage.delete("${NetworkPath.media.rawValue}/$mediaFilePath")
            } catch (exception: Exception) {
                exceptions.add(exception)
            }

            // Best-effort; the thumbnail may be absent. The path mirrors
            // iOS, which appends the suffix to the extension-qualified path.
            runCatching { storage.delete("${NetworkPath.media.rawValue}/$mediaFilePath${MediaFile.THUMBNAIL_IMAGE_NAME_SUFFIX}") }
        } catch (exception: Exception) {
            exceptions.add(exception)
        }

        exceptions.firstOrNull()?.let { throw it }
    }

    // MARK: - Auxiliary

    private suspend fun uploadPrimary(
        storage: StorageDelegate,
        mediaComponent: MediaFile,
        relativePath: String,
    ) {
        val sourceFile =
            mediaComponent.localPathFile
                ?: throw Exception("Failed to resolve local media path.", metadata = ExceptionMetadata(this))
        if (!storage.itemExists(relativePath)) {
            storage.upload(sourceFile, relativePath)
        }
        moveIntoPlace(sourceFile, relativePath)
    }

    private suspend fun uploadThumbnail(
        storage: StorageDelegate,
        mediaComponent: MediaFile,
        thumbnailRelativePath: String,
    ) {
        val thumbnailFile = mediaComponent.thumbnailFile ?: return
        if (!storage.itemExists(thumbnailRelativePath)) {
            storage.upload(thumbnailFile, thumbnailRelativePath)
        }
        moveIntoPlace(thumbnailFile, thumbnailRelativePath)
    }

    private fun moveIntoPlace(
        source: File,
        destinationRelativePath: String,
    ) {
        val destination = FileStore.resolve(destinationRelativePath) ?: return
        if (source.absolutePath == destination.absolutePath) return
        destination.parentFile?.mkdirs()
        if (destination.exists()) destination.delete()
        if (!source.renameTo(destination)) {
            destination.writeBytes(source.readBytes())
            source.delete()
        }
    }

    private suspend fun multipleMessagesReference(mediaFilePath: String): Boolean {
        val database = Networking.config.databaseDelegate
        val allMessages: Map<String, Any?>? = database.getValues(NetworkPath.messages.rawValue)
        val referenceCount =
            allMessages
                ?.values
                ?.mapNotNull { (it as? Map<*, *>)?.get(CONTENT_TYPE_KEY) as? String }
                ?.mapNotNull { HostedContentType.from(it)?.mediaFilePath }
                ?.count { it == mediaFilePath }
                ?: 0
        return referenceCount > 1
    }

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

    // MARK: - Companion

    private const val SHORTENED_HASH_LENGTH = 32
    private const val CONTENT_TYPE_KEY = "contentType"
}
