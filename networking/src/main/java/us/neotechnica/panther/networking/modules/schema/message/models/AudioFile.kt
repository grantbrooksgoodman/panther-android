//
//  AudioFile.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.networking.modules.common.models.AudioFileExtension
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.io.File

/**
 * An audio file stored in the app's documents directory, and its content
 * duration.
 *
 * **Note:** the iOS `AudioFile` locates its content by absolute URL and
 * loads its duration asynchronously on creation; this port locates it by
 * [relativePath] (like [MediaFile]) and leaves [contentDuration] `null`
 * until playback resolves it (Phase R4.2).
 */
data class AudioFile(
    /** The file's path, relative to the documents directory. */
    val relativePath: String,
    /** The file's name, without an extension. */
    val name: String,
    /** The file's extension. */
    val fileExtension: AudioFileExtension,
    /** The duration of the audio content in seconds, or `null` if undetermined. */
    val contentDuration: Float? = null,
) {
    // MARK: - Computed Properties

    /** The absolute file, resolved against the current documents directory. */
    val localPathFile: File?
        get() = FileStore.resolve(relativePath)

    // MARK: - Companion

    companion object {
        /**
         * Creates an audio file from the given relative path, deriving its
         * name and extension.
         *
         * @param relativePath The file's path, relative to the documents
         *   directory. The path's final component must consist of a name
         *   and a supported audio extension.
         *
         * @return An audio file, or `null` if no file exists at the path or
         *   its name and extension cannot be derived.
         */
        fun from(relativePath: String): AudioFile? {
            if ((FileStore.resolve(relativePath)?.length() ?: 0L) <= 0L) return null

            val fileName = relativePath.split("/").lastOrNull() ?: return null
            val components = fileName.split(".")
            if (components.size != 2) return null

            val fileExtension =
                AudioFileExtension.entries.firstOrNull { it.rawValue == components[1].lowercase() } ?: return null
            return AudioFile(
                relativePath = relativePath,
                name = components[0],
                fileExtension = fileExtension,
            )
        }
    }
}
