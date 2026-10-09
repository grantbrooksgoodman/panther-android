//
//  AudioFile.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.models

import android.media.MediaMetadataRetriever
import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.io.File

/**
 * An audio file stored in the app's documents directory, and its
 * content duration.
 *
 * **Note:** This locates its content by [relativePath] (like
 * [MediaFile]). The duration is read from the file's metadata when
 * the file is created from a path, and is `null` when it cannot be
 * determined. Durations are cached in memory by path; use
 * [AudioFileDurationCache.clearCache] to release them.
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
        private const val MILLISECONDS_PER_SECOND = 1000f

        /**
         * Creates an audio file from the given relative path, deriving
         * its name, extension, and duration.
         *
         * @param relativePath The file's path, relative to the documents
         *   directory. The path's final component must consist of a name
         *   and a supported audio extension.
         *
         * @return An audio file, or `null` if no file exists at the path
         *   or its name and extension cannot be derived.
         */
        fun from(relativePath: String): AudioFile? {
            val file = FileStore.resolve(relativePath)?.takeIf { it.exists() } ?: return null

            val fileName = relativePath.split("/").lastOrNull() ?: return null
            val components = fileName.split(".")
            if (components.size != 2) return null

            val fileExtension =
                AudioFileExtension.entries.firstOrNull { it.rawValue == components[1].lowercase() } ?: return null
            return AudioFile(
                relativePath = relativePath,
                name = components[0],
                fileExtension = fileExtension,
                contentDuration = contentDuration(file),
            )
        }

        private fun contentDuration(file: File): Float? {
            AudioFileDurationCache.duration(file.absolutePath)?.let { return it }

            val retriever = runCatching { MediaMetadataRetriever() }.getOrNull() ?: return null
            val duration =
                try {
                    runCatching {
                        retriever.setDataSource(file.absolutePath)
                        retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toFloatOrNull()
                            ?.let { it / MILLISECONDS_PER_SECOND }
                    }.getOrNull()
                } finally {
                    runCatching { retriever.release() }
                }

            if (duration != null && duration > 0f) {
                AudioFileDurationCache.setDuration(duration, file.absolutePath)
            }

            return duration
        }
    }
}

// MARK: - Audio File Duration Cache

/**
 * An in-memory cache of audio file durations, keyed by absolute path.
 *
 * Durations are cached when an audio file is created from a path;
 * use [clearCache] to release them.
 */
object AudioFileDurationCache {
    // MARK: - Properties

    private val durations = LockIsolated(mapOf<String, Float>())

    // MARK: - Methods

    /**
     * Returns the cached duration for the file at the given path.
     *
     * @param forPath The file's absolute path.
     *
     * @return The cached duration in seconds, or `null` if none is
     *   cached.
     */
    fun duration(forPath: String): Float? = durations.wrappedValue[forPath]

    /**
     * Caches the given duration for the file at the given path.
     *
     * @param duration The duration in seconds.
     * @param forPath The file's absolute path.
     */
    fun setDuration(
        duration: Float,
        forPath: String,
    ) {
        durations.withValue { it.value = it.value + (forPath to duration) }
    }

    /** Removes every cached duration. */
    fun clearCache() {
        durations.wrappedValue = emptyMap()
    }
}
