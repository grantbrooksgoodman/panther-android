//
//  TextToSpeechService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import us.neotechnica.panther.subsystem.modules.foundation.extensions.englishLanguageName
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.util.Locale

/**
 * Synthesizes speech from text aloud.
 *
 * Speech uses only voices installed on the device; use
 * [downloadVoiceIfNeeded] to install a language's voice ahead of
 * time. Voice lookups and positive language support checks are cached
 * in memory per language code; clear them through
 * [TextToSpeechServiceCache.clearCache].
 */
object TextToSpeechService {
    // MARK: - Properties

    private val audioFocus = AudioFocusService { stop() }

    private var engine: TextToSpeech? = null
    private var isInitialized = false
    private var pendingVoiceDownloadLanguageCode: String? = null

    // Backed by observable snapshot state, driven by the utterance
    // callbacks, so the context menu's Speak/Stop-Speaking title rebuilds
    // when speech starts and ends.
    private var speaking by mutableStateOf(false)
    private var speakingMessageIDState by mutableStateOf<String?>(null)
    private var spokenRangeState by mutableStateOf<IntRange?>(null)

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether a message is being
     * spoken aloud.
     */
    val isSpeaking: Boolean
        get() = speaking

    /**
     * The identifier of the message currently being spoken aloud,
     * or `null`.
     */
    val speakingMessageID: String?
        get() = speakingMessageIDState

    /**
     * The character range of the spoken message currently being
     * enunciated, or `null`.
     */
    val spokenRange: IntRange?
        get() = spokenRangeState

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        audioFocus.initialize(context)
        val engine =
            TextToSpeech(context.applicationContext) { status ->
                isInitialized = status == TextToSpeech.SUCCESS

                // A download requested during initialization begins now.
                val languageCode = pendingVoiceDownloadLanguageCode ?: return@TextToSpeech
                pendingVoiceDownloadLanguageCode = null
                if (isInitialized) downloadVoiceIfNeeded(languageCode)
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
     * Speaks [text] aloud with the highest quality voice
     * available for [languageCode], replacing any in-progress
     * utterance.
     *
     * Nothing is spoken if no voice for the language is installed
     * on the device; use [isTextToSpeechSupported] to check
     * beforehand.
     *
     * @param text The text to speak.
     * @param languageCode The language code of the voice with
     *   which to speak the text.
     * @param messageID The identifier of the message being
     *   spoken.
     *
     * @return `true` if the utterance was queued for speaking;
     *   otherwise, `false`.
     */
    fun speak(
        text: String,
        languageCode: String,
        messageID: String,
    ): Boolean {
        val engine = engine ?: return false
        if (!isInitialized || text.isBlank()) return false

        val voice = highestQualityVoice(languageCode) ?: return false
        if (engine.setVoice(voice) != TextToSpeech.SUCCESS) return false

        speakingMessageIDState = messageID
        spokenRangeState = null

        // Request transient audio focus before speaking.
        audioFocus.requestFocus()
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) == TextToSpeech.SUCCESS) return true
        endSpeaking()
        return false
    }

    // MARK: - Stop

    /** Stops any in-progress utterance. */
    fun stop() {
        engine?.stop()
        endSpeaking()
    }

    /** Stops the in-progress utterance, if any. */
    fun stopSpeakingIfNeeded() {
        if (!speaking) return
        stop()
    }

    // MARK: - Download Voice

    /**
     * Downloads a voice for the given language code if none is
     * installed on the device.
     *
     * The speech engine downloads the voice's data in the background,
     * after which [isTextToSpeechSupported] returns `true` for the
     * language and text in it can be spoken. A voice for the device's
     * region is preferred, then the highest quality voice. This method
     * has no effect when a voice for the language is already installed
     * or when the speech engine offers none to download.
     *
     * **Note:** While the speech engine is still initializing, the
     * download begins once initialization completes.
     *
     * @param languageCode The language code of the voice to download.
     */
    fun downloadVoiceIfNeeded(languageCode: String) {
        val engine = engine ?: return
        if (!isInitialized) {
            pendingVoiceDownloadLanguageCode = languageCode
            return
        }

        if (isTextToSpeechSupported(languageCode)) return
        val country = Locale.getDefault().country
        val voice =
            offlineVoices(languageCode)
                ?.filter { it.isNotInstalled }
                ?.maxWithOrNull(
                    compareBy(
                        { it.locale.country == country },
                        { it.quality },
                    ),
                ) ?: return

        // Selecting a voice whose data is not installed starts its
        // download.
        Logger.log("Downloading ${languageCode.englishLanguageName ?: languageCode.uppercase()} voice \"${voice.name}\".")
        engine.setVoice(voice)
    }

    // MARK: - Highest Quality Voice

    /**
     * Returns the highest quality voice available for the given
     * language code.
     *
     * Only voices installed on the device are considered: a voice
     * the speech engine lists but has not downloaded, or one that
     * requires a network connection, is skipped. A high-quality
     * voice is preferred; otherwise, the best installed voice for
     * the language is returned. Results are cached in memory per
     * language code.
     *
     * **Note:** While the speech engine is still initializing,
     * this method returns `null` without caching a result.
     *
     * @param languageCode The language code of the voice to find.
     *
     * @return The highest quality voice for the language code;
     *   otherwise, `null` if no voice is available.
     */
    fun highestQualityVoice(languageCode: String): Voice? {
        TextToSpeechServiceCache.voice(languageCode)?.let { return it }

        val voices = installedVoices(languageCode) ?: return null
        val voice =
            voices
                .filter { it.quality >= Voice.QUALITY_HIGH }
                .maxByOrNull { it.quality }
                ?: voices.maxByOrNull { it.quality }
                ?: return null

        TextToSpeechServiceCache.setVoice(voice, languageCode)
        return voice
    }

    // MARK: - Capabilities

    /**
     * Returns a Boolean value that indicates whether text to speech
     * is supported for the given language code.
     *
     * Text to speech is supported for a language when a voice for it
     * is installed on the device and requires no network connection;
     * a voice the speech engine lists but has not downloaded doesn't
     * count. Only positive results are cached in memory, so a voice
     * downloaded while the app is running is found on a later call.
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

        val voices = installedVoices(languageCode) ?: return false
        if (voices.isEmpty()) return false

        TextToSpeechServiceCache.setSupportValue(true, languageCode)
        return true
    }

    // MARK: - Auxiliary

    // The voices for the given language code that can speak without
    // downloading data or connecting to the network, or null while the
    // speech engine is initializing.
    private fun installedVoices(languageCode: String): List<Voice>? = offlineVoices(languageCode)?.filterNot { it.isNotInstalled }

    // The voices for the given language code that speak without
    // connecting to the network, whether or not their data is
    // installed, or null while the speech engine is initializing.
    private fun offlineVoices(languageCode: String): List<Voice>? {
        val engine = engine ?: return null
        if (!isInitialized) return null

        // A voice matches on the raw code prefix of its language tag; the
        // locale's language is the fallback for the installed-voice rule.
        val language = Locale.forLanguageTag(languageCode).language
        return runCatching {
            engine.voices
                ?.filter { voice ->
                    (
                        voice.locale.toLanguageTag().startsWith(languageCode, ignoreCase = true) ||
                            voice.locale.language.equals(language, ignoreCase = true)
                    ) &&
                        !voice.isNetworkConnectionRequired
                }.orEmpty()
        }.getOrNull()
    }

    // Whether the voice's data must be downloaded before it can speak.
    private val Voice.isNotInstalled: Boolean
        get() = features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true

    private fun endSpeaking() {
        speaking = false
        speakingMessageIDState = null
        spokenRangeState = null
        audioFocus.abandonFocus()
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
