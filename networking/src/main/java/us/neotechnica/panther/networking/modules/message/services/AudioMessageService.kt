//
//  AudioMessageService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.message.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.schema.message.models.AudioFile
import us.neotechnica.panther.networking.modules.schema.message.models.AudioMessageReference
import us.neotechnica.panther.networking.modules.schema.message.models.LocalAudioFilePath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.Translation

/**
 * The service that downloads audio message content for playback.
 *
 * **Note:** the iOS original also uploads and deletes audio components;
 * audio message *sending* is cut, so this port provides retrieval only.
 */
object AudioMessageService {
    // MARK: - Get Audio Component

    /**
     * Returns the audio component for the given message, using the local
     * copy when available and downloading it otherwise.
     *
     * @param messageID The identifier of the message.
     * @param isFromCurrentUser Whether the message was sent by the current
     *   user.
     * @param localAudioFilePath The local file paths for the message's audio.
     * @param translation The translation associated with the audio.
     *
     * @return The audio component.
     *
     * @throws Exception if the audio cannot be resolved or downloaded.
     */
    suspend fun getAudioComponent(
        messageID: String,
        isFromCurrentUser: Boolean,
        localAudioFilePath: LocalAudioFilePath,
        translation: Translation,
    ): AudioMessageReference =
        cachedAudioMessageReference(localAudioFilePath, translation)
            ?: downloadAudioMessageReference(messageID, isFromCurrentUser, localAudioFilePath, translation)

    // MARK: - Auxiliary

    private fun cachedAudioMessageReference(
        localAudioFilePath: LocalAudioFilePath,
        translation: Translation,
    ): AudioMessageReference? {
        val input = AudioFile.from(localAudioFilePath.inputFilePathString) ?: return null
        val output = AudioFile.from(localAudioFilePath.outputFilePathString) ?: return null
        return AudioMessageReference(translation, input, output, localAudioFilePath.outputDirectoryPathString)
    }

    private suspend fun downloadAudioMessageReference(
        messageID: String,
        isFromCurrentUser: Boolean,
        localAudioFilePath: LocalAudioFilePath,
        translation: Translation,
    ): AudioMessageReference {
        val storage = Networking.config.storageDelegate
        val userInfo = mapOf("MessageID" to messageID)

        val sourcePathString =
            if (isFromCurrentUser) localAudioFilePath.inputFilePathString else localAudioFilePath.outputFilePathString
        val sourceFile =
            (if (isFromCurrentUser) localAudioFilePath.inputFilePathFile else localAudioFilePath.outputFilePathFile)
                ?: throw failure("Failed to resolve local audio path.").appending(userInfo = userInfo)
        val destinationFile =
            (if (isFromCurrentUser) localAudioFilePath.outputFilePathFile else localAudioFilePath.inputFilePathFile)
                ?: throw failure("Failed to resolve local audio path.").appending(userInfo = userInfo)

        try {
            storage.download(sourcePathString, sourceFile)
        } catch (exception: Exception) {
            throw exception.appending(userInfo = userInfo)
        }

        // Mirror the downloaded audio into the counterpart slot (unless the
        // translation is idempotent, when both slots are the same file).
        if (sourceFile.absolutePath != destinationFile.absolutePath) {
            destinationFile.parentFile?.mkdirs()
            destinationFile.writeBytes(sourceFile.readBytes())
        }

        val input = AudioFile.from(localAudioFilePath.inputFilePathString)
        val output = AudioFile.from(localAudioFilePath.outputFilePathString)
        if (input == null || output == null) {
            throw failure("Failed to generate audio files.").appending(userInfo = userInfo)
        }

        return AudioMessageReference(translation, input, output, localAudioFilePath.outputDirectoryPathString)
    }

    private fun failure(message: String): Exception = Exception(message, metadata = ExceptionMetadata(this))
}
