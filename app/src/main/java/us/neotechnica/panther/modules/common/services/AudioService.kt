//
//  AudioService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.modules.common.models.MediaFileExtension

/**
 * The umbrella service for audio functionality.
 *
 * Use [AudioService] to access the app's audio sub-services.
 */
object AudioService {
    // MARK: - Types

    /** The names of the audio files the service reads and writes. */
    object FileNames {
        /** The name of a translated output recording. */
        val OUTPUT_M4A = "output.${MediaFileExtension.Audio(AudioFileExtension.M4A).rawValue}"
    }

    // MARK: - Properties

    /** The service that synthesizes speech from text. */
    val textToSpeech get() = TextToSpeechService
}
