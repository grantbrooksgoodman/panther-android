//
//  LocalAudioFilePath.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.models

import us.neotechnica.panther.bundle.audioMessageInputs
import us.neotechnica.panther.bundle.audioTranslations
import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.common.services.AudioService
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.translator.models.Translation
import java.io.File

/**
 * The local file paths for an audio message's input and translated
 * output audio.
 */
data class LocalAudioFilePath(
    /** The input recording's path, relative to the documents directory. */
    val inputFilePathString: String,
    /** The path to the directory containing the translated output audio. */
    val outputDirectoryPathString: String,
    /** The translated output audio's path, relative to the documents directory. */
    val outputFilePathString: String,
) {
    // MARK: - Computed Properties

    /**
     * The absolute file of the input recording, or `null` before the
     * file store is initialized.
     */
    val inputFilePathURL: File?
        get() = FileStore.resolve(inputFilePathString)

    /**
     * The absolute file of the translated output audio, or `null`
     * before the file store is initialized.
     */
    val outputFilePathURL: File?
        get() = FileStore.resolve(outputFilePathString)

    // MARK: - Companion

    companion object {
        /**
         * Creates an audio file path for the given message and
         * translation.
         *
         * For an idempotent translation, the output path matches the
         * input path.
         *
         * @param messageID The identifier of the message.
         * @param translation The translation to derive the output
         *   paths from.
         *
         * @return The audio file path.
         */
        fun from(
            messageID: String,
            translation: Translation,
        ): LocalAudioFilePath {
            val inputFileExtension = MediaFileExtension.Audio(AudioFileExtension.M4A).rawValue
            val inputFilePath = "${NetworkPath.audioMessageInputs.rawValue}/$messageID.$inputFileExtension"
            val outputDirectoryPath =
                listOf(
                    NetworkPath.audioTranslations.rawValue,
                    translation.reference.hostingKey,
                ).joinToString("/")

            var outputFilePath = "$outputDirectoryPath/${translation.languagePair.to}-${AudioService.FileNames.OUTPUT_M4A}"
            if (translation.languagePair.isIdempotent) {
                outputFilePath = inputFilePath
            }

            return LocalAudioFilePath(
                inputFilePathString = inputFilePath,
                outputDirectoryPathString = outputDirectoryPath,
                outputFilePathString = outputFilePath,
            )
        }

        /**
         * Creates an audio file path from the given message.
         *
         * @param message The message to derive the paths from.
         *
         * @return An audio file path, or `null` if the message is not
         *   an audio message or has no translation.
         */
        fun from(message: Message): LocalAudioFilePath? {
            if (!message.contentType.isAudio) return null
            val translation = message.translation ?: return null
            return from(message.id, translation)
        }
    }
}
