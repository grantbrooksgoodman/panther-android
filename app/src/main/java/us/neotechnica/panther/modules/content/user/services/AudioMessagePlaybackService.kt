//
//  AudioMessagePlaybackService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 03/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.networking.modules.schema.message.models.AudioMessageReference
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.session.extensions.isAudioMessage
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.resolvedAudioReference
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/**
 * Manages audio message playback: tap-to-play/pause, playback-progress,
 * and auto-advance to the next audio message when one finishes.
 *
 * **Note:** the iOS original plays through an `AVAudioPlayer` and
 * activates an `AVAudioSession`; this port uses Android `MediaPlayer` with
 * an `AudioFocusRequest` (see `DEVIATIONS.md`) rather than Media3/ExoPlayer,
 * which would add a dependency without benefit for single-file playback.
 */
object AudioMessagePlaybackService {
    // MARK: - Properties

    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var progressJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    private val audioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    // Backed by observable snapshot state so audio bubbles reflect the
    // playing message and its progress.
    private var mutablePlayingMessageID by mutableStateOf<String?>(null)
    private var mutableProgress by mutableFloatStateOf(0f)

    // MARK: - Computed Properties

    /** The identifier of the message currently playing, or `null`. */
    val playingMessageID: String?
        get() = mutablePlayingMessageID

    /** The playback progress of the current message, in `0.0...1.0`. */
    val progress: Float
        get() = mutableProgress

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    // MARK: - Did Tap Play Button

    /**
     * Handles a tap on an audio message's play button, starting or
     * stopping playback.
     *
     * Tapping a message that is not playing starts its playback; tapping
     * the playing message stops it.
     *
     * @param message The tapped message.
     * @param audioReference The message's resolved audio.
     */
    fun didTapPlayButton(
        message: Message,
        audioReference: AudioMessageReference,
    ) {
        if (mutablePlayingMessageID == message.id) return stopPlayback()

        TextToSpeechService.stop()

        val audioFile = if (message.isFromCurrentUser) audioReference.original else audioReference.translated
        val file = audioFile.localPathFile ?: return
        play(message.id, file.absolutePath)
    }

    // MARK: - Stop Playback

    /** Stops audio playback and resets the playing state. */
    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        abandonFocus()
        mutablePlayingMessageID = null
        mutableProgress = 0f
    }

    // MARK: - Auxiliary

    private fun play(
        messageID: String,
        filePath: String,
    ) {
        stopPlayback()
        requestFocus()

        player =
            MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(filePath)
                setOnPreparedListener { start(); startProgressPolling() }
                setOnCompletionListener { onFinished(messageID) }
                setOnErrorListener { _, _, _ -> stopPlayback(); true }
                runCatching { prepareAsync() }.onFailure { stopPlayback() }
            }
        mutablePlayingMessageID = messageID
    }

    private fun onFinished(finishedMessageID: String) {
        val next = nextAudioMessage(finishedMessageID)
        stopPlayback()

        next ?: return
        scope.launch {
            delay(PLAY_NEXT_MESSAGE_DELAY_MILLISECONDS)
            val reference =
                withContext(Dispatchers.IO) { next.resolvedAudioReference(RuntimeStorage.languageCode) } ?: return@launch
            didTapPlayButton(next, reference)
        }
    }

    private fun nextAudioMessage(afterMessageID: String): Message? {
        val messages = ConversationSessionService.displayedMessages.value
        val index = messages.indexOfFirst { it.id == afterMessageID }
        if (index < 0) return null
        return messages.getOrNull(index + 1)?.takeIf { it.isAudioMessage }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob =
            scope.launch {
                while (isActive) {
                    val currentPlayer = player ?: break
                    val duration = runCatching { currentPlayer.duration }.getOrDefault(0)
                    if (duration > 0) {
                        mutableProgress =
                            runCatching { currentPlayer.currentPosition.toFloat() / duration }.getOrDefault(0f).coerceIn(0f, 1f)
                    }
                    delay(PROGRESS_POLL_INTERVAL_MILLISECONDS)
                }
            }
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = audioManager ?: return
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { change -> if (change == AudioManager.AUDIOFOCUS_LOSS) stopPlayback() }
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

    private const val PROGRESS_POLL_INTERVAL_MILLISECONDS = 50L
    private const val PLAY_NEXT_MESSAGE_DELAY_MILLISECONDS = 500L
}
