//
//  TextToSpeechService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.Locale

/**
 * Synthesizes speech from text aloud.
 *
 * Voice lookups and language support checks are cached in memory
 * per language code; clear them through
 * [TextToSpeechServiceCache.clearCache].
 */
object TextToSpeechService {
    // MARK: - Properties

    private val audioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    private var audioManager: AudioManager? = null
    private var engine: TextToSpeech? = null
    private var focusRequest: AudioFocusRequest? = null
    private var isInitialized = false

    // Backed by observable snapshot state, driven by the utterance
    // callbacks, so the context menu's Speak/Stop-Speaking title rebuilds
    // when speech starts and ends.
    private var speaking by mutableStateOf(false)
    private var speakingMessageIDState by mutableStateOf<String?>(null)
    private var spokenRangeState by mutableStateOf<IntRange?>(null)

    // MARK: - Computed Properties

    /** A Boolean value that indicates whether a message is being spoken aloud. */
    val isSpeaking: Boolean
        get() = speaking

    /** The identifier of the message currently being spoken aloud, or `null`. */
    val speakingMessageID: String?
        get() = speakingMessageIDState

    /**
     * The character range of the spoken message currently being
     * enunciated, or `null`. Reported only on API 26 and later.
     */
    val spokenRange: IntRange?
        get() = spokenRangeState

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
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
                    endSpeaking()
                }

                override fun onRangeStart(
                    utteranceId: String?,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    spokenRangeState = start until end
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    endSpeaking()
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    endSpeaking()
                }

                override fun onStop(
                    utteranceId: String?,
                    interrupted: Boolean,
                ) {
                    endSpeaking()
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
     * @param messageID The identifier of the message being spoken.
     */
    fun speak(
        text: String,
        languageCode: String,
        messageID: String,
    ) {
        val engine = engine ?: return
        if (!isInitialized || text.isBlank()) return

        val locale = Locale.forLanguageTag(languageCode)
        val voice = highestQualityVoice(languageCode)
        if (voice != null) engine.voice = voice else engine.language = locale

        speakingMessageIDState = messageID
        spokenRangeState = null

        // Request transient audio focus before speaking.
        requestFocus()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    // MARK: - Stop

    /** Stops any in-progress utterance. */
    fun stop() {
        engine?.stop()
        endSpeaking()
    }

    // MARK: - Highest Quality Voice

    /**
     * Returns the highest quality voice available for the given
     * language code.
     *
     * A high-quality on-device voice is preferred; otherwise, the
     * best available on-device voice for the language is returned.
     * Results are cached in memory per language code.
     *
     * **Note:** While the speech engine is still initializing, this
     * method returns `null` without caching a result.
     *
     * @param languageCode The language code of the voice to find.
     *
     * @return The highest quality voice for the language code;
     *   otherwise, `null` if no voice is available.
     */
    fun highestQualityVoice(languageCode: String): Voice? {
        TextToSpeechServiceCache.voice(languageCode)?.let { return it }

        val voices = loadedVoices(languageCode) ?: return null
        val onDeviceVoices = voices.filter { !it.isNetworkConnectionRequired }
        val voice =
            onDeviceVoices
                .filter { it.quality >= Voice.QUALITY_HIGH }
                .maxByOrNull { it.quality }
                ?: onDeviceVoices.maxByOrNull { it.quality }
                ?: return null

        TextToSpeechServiceCache.setVoice(voice, languageCode)
        return voice
    }

    // MARK: - Capabilities

    /**
     * Returns a Boolean value that indicates whether text to speech
     * is supported for the given language code.
     *
     * Support is determined by whether any installed voice matches
     * the given code. Results are cached in memory per language
     * code.
     *
     * **Note:** While the speech engine is still initializing, this
     * method returns `false` without caching a result.
     *
     * @param languageCode The language code for which to check
     *   support.
     *
     * @return `true` if text to speech is supported for the given
     *   language code; otherwise, `false`.
     */
    fun isTextToSpeechSupported(languageCode: String): Boolean {
        TextToSpeechServiceCache.supportValue(languageCode)?.let { return it }

        val voices = loadedVoices(languageCode) ?: return false
        val isTextToSpeechSupported = voices.isNotEmpty()

        TextToSpeechServiceCache.setSupportValue(isTextToSpeechSupported, languageCode)
        return isTextToSpeechSupported
    }

    // MARK: - Auxiliary

    // The installed voices for the given language code, or null while
    // the speech engine is initializing.
    private fun loadedVoices(languageCode: String): List<Voice>? {
        val engine = engine ?: return null
        if (!isInitialized) return null

        val language = Locale.forLanguageTag(languageCode).language
        return runCatching {
            engine.voices
                ?.filter { it.locale.language.equals(language, ignoreCase = true) }
                .orEmpty()
        }.getOrNull()
    }

    private fun endSpeaking() {
        speaking = false
        speakingMessageIDState = null
        spokenRangeState = null
        abandonFocus()
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = audioManager ?: return
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { change -> if (change == AudioManager.AUDIOFOCUS_LOSS) stop() }
                .build()
        focusRequest = request
        manager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = audioManager ?: return
        focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    // MARK: - Companion

    private const val UTTERANCE_ID = "us.neotechnica.panther.speak"
}

/**
 * A namespace for managing the in-memory text-to-speech voice and
 * language support caches.
 */
object TextToSpeechServiceCache {
    // MARK: - Properties

    private val cachedSupportValuesForLanguageCodes = LockIsolated(mapOf<String, Boolean>())
    private val cachedVoicesForLanguageCodes = LockIsolated(mapOf<String, Voice>())

    // MARK: - Methods

    /** Removes every cached voice and language support value. */
    fun clearCache() {
        cachedSupportValuesForLanguageCodes.wrappedValue = emptyMap()
        cachedVoicesForLanguageCodes.wrappedValue = emptyMap()
    }

    internal fun setSupportValue(
        isSupported: Boolean,
        languageCode: String,
    ) {
        cachedSupportValuesForLanguageCodes.withValue { it.value = it.value + (languageCode to isSupported) }
    }

    internal fun setVoice(
        voice: Voice,
        languageCode: String,
    ) {
        cachedVoicesForLanguageCodes.withValue { it.value = it.value + (languageCode to voice) }
    }

    internal fun supportValue(languageCode: String): Boolean? = cachedSupportValuesForLanguageCodes.wrappedValue[languageCode]

    internal fun voice(languageCode: String): Voice? = cachedVoicesForLanguageCodes.wrappedValue[languageCode]
}
