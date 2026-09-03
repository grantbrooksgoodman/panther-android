//
//  LocalAudioFilePath.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.networking.modules.common.models.AudioFileExtension
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.translator.models.Translation
import java.io.File

/**
 * The local file paths for an audio message's input recording and its
 * translated output audio.
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

    /** The absolute file of the input recording. */
    val inputFilePathFile: File?
        get() = FileStore.resolve(inputFilePathString)

    /** The absolute file of the translated output audio. */
    val outputFilePathFile: File?
        get() = FileStore.resolve(outputFilePathString)

    // MARK: - Companion

    companion object {
        /**
         * Creates an audio file path for the given message and translation.
         *
         * For an idempotent translation, the output path matches the input
         * path.
         *
         * @param messageID The identifier of the message.
         * @param translation The translation to derive the output paths from.
         * @param hostingKey The translation's hosting key, naming the
         *   translated-audio directory.
         */
        fun from(
            messageID: String,
            translation: Translation,
            hostingKey: String,
        ): LocalAudioFilePath {
            val inputFilePathString = "${NetworkPath.audioMessageInputs.rawValue}/$messageID.${AudioFileExtension.M4A.rawValue}"
            val outputDirectoryPathString = "${NetworkPath.audioTranslations.rawValue}/$hostingKey"
            val outputFilePathString =
                if (translation.languagePair.isIdempotent) {
                    inputFilePathString
                } else {
                    "$outputDirectoryPathString/${translation.languagePair.to}-$OUTPUT_M4A"
                }

            return LocalAudioFilePath(inputFilePathString, outputDirectoryPathString, outputFilePathString)
        }

        /**
         * Creates an audio file path from the given message and its resolved
         * translation, or `null` if the message is not an audio message or
         * carries no translation reference.
         */
        fun from(
            message: Message,
            translation: Translation,
        ): LocalAudioFilePath? {
            if (message.contentType !is HostedContentType.Audio) return null
            val hostingKey = message.translationReferences?.firstOrNull()?.hostingKey ?: return null
            return from(message.id, translation, hostingKey)
        }

        private const val OUTPUT_M4A = "output.m4a"
    }
}
