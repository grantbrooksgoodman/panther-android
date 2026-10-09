//
//  AudioMessageService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.services

import us.neotechnica.panther.bundle.audioMessageInputs
import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.modules.networking.message.models.AudioFile
import us.neotechnica.panther.modules.networking.message.models.AudioMessageReference
import us.neotechnica.panther.modules.networking.message.models.LocalAudioFilePath
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.Translation

/**
 * The service that downloads and deletes audio message content.
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

    // MARK: - Delete Input Audio Component

    /**
     * Deletes the input recording for the given message from remote
     * storage.
     *
     * A missing recording is not treated as an error.
     *
     * @param messageID The identifier of the message.
     *
     * @throws Exception if deletion fails.
     */
    suspend fun deleteInputAudioComponent(messageID: String) {
        val storage = Networking.config.storageDelegate

        try {
            storage.deleteItem(
                listOf(
                    NetworkPath.audioMessageInputs.rawValue,
                    "$messageID.${AudioFileExtension.M4A.rawValue}",
                ).joinToString("/"),
            )
        } catch (exception: Exception) {
            if (exception.isEqual(to = AppException.Networking.Storage.storageItemDoesNotExist)) return
            throw exception
        }
    }

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
            (if (isFromCurrentUser) localAudioFilePath.inputFilePathURL else localAudioFilePath.outputFilePathURL)
                ?: throw failure("Failed to resolve local audio path.").appending(userInfo = userInfo)
        val destinationFile =
            (if (isFromCurrentUser) localAudioFilePath.outputFilePathURL else localAudioFilePath.inputFilePathURL)
                ?: throw failure("Failed to resolve local audio path.").appending(userInfo = userInfo)

        try {
            storage.downloadItem(sourcePathString, sourceFile)
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
