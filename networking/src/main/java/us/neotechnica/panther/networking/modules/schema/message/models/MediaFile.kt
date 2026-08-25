//
//  MediaFile.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.networking.modules.common.models.MediaFileExtension
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.io.File

/**
 * A media file stored in the app's documents directory.
 *
 * A media file locates its content by [relativePath], resolved against
 * the documents directory, so values remain valid across app launches
 * even though the directory's absolute location can change.
 *
 * **Note:** This viewing-phase port omits the iOS content-hash
 * identity used for upload deduplication; downloaded media is
 * identified by its server-assigned path.
 */
data class MediaFile(
    /** The file's path, relative to the documents directory. */
    val relativePath: String,
    /** The file's name, without an extension. */
    val name: String,
    /** The file's extension. */
    val fileExtension: MediaFileExtension,
) {
    // MARK: - Computed Properties

    /** The absolute file, resolved against the current documents directory. */
    val localPathFile: File?
        get() = FileStore.resolve(relativePath)

    /**
     * The absolute file of the media's thumbnail, if one has been
     * downloaded (videos and documents), or `null` otherwise.
     */
    val thumbnailFile: File?
        get() {
            val thumbnailRelativePath = relativePath.substringBeforeLast(".") + THUMBNAIL_IMAGE_NAME_SUFFIX
            return FileStore.resolve(thumbnailRelativePath)?.takeIf { it.exists() }
        }

    // MARK: - Companion

    companion object {
        /** The suffix appended to a media file's path prefix to form its thumbnail path. */
        const val THUMBNAIL_IMAGE_NAME_SUFFIX = "-thumbnail.jpeg"

        /**
         * Creates a media file from the given relative path, deriving its
         * name and extension.
         *
         * @param relativePath The file's path, relative to the documents
         *   directory. The path's final component must consist of a name
         *   and a supported extension.
         *
         * @return A media file, or `null` if no file exists at the path or
         *   its name and extension cannot be derived.
         */
        fun from(relativePath: String): MediaFile? {
            // Require a non-empty file so a 0-byte remnant of a failed
            // download is treated as a cache miss and re-fetched.
            if ((FileStore.resolve(relativePath)?.length() ?: 0L) <= 0L) return null

            val fileName = relativePath.split("/").lastOrNull() ?: return null
            val components = fileName.split(".")
            if (components.size != 2) return null

            val fileExtension = MediaFileExtension.from(components[1]) ?: return null
            return MediaFile(
                relativePath = relativePath,
                name = components[0],
                fileExtension = fileExtension,
            )
        }
    }
}
