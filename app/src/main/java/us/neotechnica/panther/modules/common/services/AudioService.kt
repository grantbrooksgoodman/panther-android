//
//  AudioService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

/**
 * The umbrella service for audio functionality.
 *
 * Use [AudioService] to access the app's audio sub-services.
 */
object AudioService {
    /** The service that synthesizes speech from text. */
    val textToSpeech get() = TextToSpeechService
}
