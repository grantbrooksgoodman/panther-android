//
//  AudioMessageReference.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.translator.models.Translation

/**
 * A reference to an audio message's original and translated audio.
 *
 * @property translation The translation associated with the audio.
 * @property original The original audio recorded by the sender.
 * @property translated The audio translated into the recipient's language.
 * @property translatedDirectoryPath The path to the directory containing
 *   the translated audio.
 */
data class AudioMessageReference(
    val translation: Translation,
    val original: AudioFile,
    val translated: AudioFile,
    val translatedDirectoryPath: String,
)
