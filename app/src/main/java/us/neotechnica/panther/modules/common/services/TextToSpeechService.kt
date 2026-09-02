//
//  TextToSpeechService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Synthesizes speech from text aloud, standing in for the iOS
 * `TextToSpeechService` speak path.
 *
 * **Note:** the iOS original also renders speech to audio files for audio
 * messages; that path arrives with audio messages (Phase R4). Android
 * `TextToSpeech` exposes a narrower voice inventory than iOS's
 * `AVSpeechSynthesisVoice`, so [highestQualityVoice] approximates the iOS
 * enhanced- and premium-quality preference with the best on-device voice.
 */
object TextToSpeechService {
    // MARK: - Properties

    private var engine: TextToSpeech? = null
    private var isInitialized = false

    // Backed by observable snapshot state, driven by the utterance
    // callbacks, so the context menu's Speak/Stop-Speaking title rebuilds
    // when speech starts and ends.
    private var speaking by mutableStateOf(false)

    // MARK: - Computed Properties

    /** A Boolean value that indicates whether a message is being spoken aloud. */
    val isSpeaking: Boolean
        get() = speaking

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        val engine =
            TextToSpeech(context.applicationContext) { status ->
                isInitialized = status == TextToSpeech.SUCCESS
            }
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    speaking = true
                }

                override fun onDone(utteranceId: String?) {
                    speaking = false
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    speaking = false
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    speaking = false
                }

                override fun onStop(
                    utteranceId: String?,
                    interrupted: Boolean,
                ) {
                    speaking = false
                }
            },
        )
        this.engine = engine
    }

    // MARK: - Speak

    /**
     * Speaks [text] aloud with the highest quality voice available for
     * [languageCode], replacing any in-progress utterance.
     *
     * @param text The text to speak.
     * @param languageCode The language code of the voice with which to speak the text.
     */
    fun speak(
        text: String,
        languageCode: String,
    ) {
        val engine = engine ?: return
        if (!isInitialized || text.isBlank()) return

        val locale = Locale.forLanguageTag(languageCode)
        val voice = highestQualityVoice(engine, languageCode)
        if (voice != null) engine.voice = voice else engine.language = locale

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    // MARK: - Stop

    /** Stops any in-progress utterance. */
    fun stop() {
        engine?.stop()
        speaking = false
    }

    // MARK: - Highest Quality Voice

    /**
     * Returns the highest quality on-device voice available for
     * [languageCode], or `null` if none is available.
     */
    private fun highestQualityVoice(
        engine: TextToSpeech,
        languageCode: String,
    ): Voice? {
        val language = Locale.forLanguageTag(languageCode).language
        return runCatching {
            engine.voices
                ?.filter { it.locale.language.equals(language, ignoreCase = true) && !it.isNetworkConnectionRequired }
                ?.maxByOrNull { it.quality }
        }.getOrNull()
    }

    // MARK: - Companion

    private const val UTTERANCE_ID = "us.neotechnica.panther.speak"
}
