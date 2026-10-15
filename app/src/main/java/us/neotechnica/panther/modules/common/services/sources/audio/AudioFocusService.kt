//
//  AudioFocusService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Holds transient audio focus on behalf of a service that plays or
 * speaks audio.
 *
 * Request focus before playback or speech begins and abandon it once
 * they end. Focus is requested for speech played as media, with
 * [audioAttributes]; when another app takes focus for good, the
 * service is told to stop.
 *
 * @property onFocusLost The action performed when the service loses
 *   audio focus to another app.
 */
class AudioFocusService(
    private val onFocusLost: () -> Unit,
) {
    // MARK: - Properties

    /** The attributes of the audio the service plays: speech, played as media. */
    val audioAttributes: AudioAttributes by lazy {
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }

    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    // MARK: - Methods

    /** Abandons the audio focus held, if any. */
    fun abandonFocus() {
        val manager = audioManager ?: return
        focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    /** Requests transient audio focus. */
    fun requestFocus() {
        val manager = audioManager ?: return
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { change -> if (change == AudioManager.AUDIOFOCUS_LOSS) onFocusLost() }
                .build()
        focusRequest = request
        manager.requestAudioFocus(request)
    }
}
