//
//  AudioMessagePlaybackService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources

import android.content.Context
import android.media.MediaPlayer
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
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.HapticsService
import us.neotechnica.panther.modules.common.services.sources.audio.AudioFocusService
import us.neotechnica.panther.modules.common.services.sources.audio.TextToSpeechService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.networking.sources.message.models.AudioMessageReference
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency

// MARK: - Constants Accessors

private typealias PlaybackFloats = ChatPageViewServiceFloats.AudioMessagePlayback

/**
 * Manages audio message playback: tap-to-play/pause,
 * playback-progress, and auto-advance to the next audio message
 * when one finishes.
 *
 * **Note:** Playback uses `MediaPlayer` with an `AudioFocusRequest`.
 */
object AudioMessagePlaybackService {
    // MARK: - Types

    /** The message list the service reads when advancing playback. */
    internal class Dependencies(
        val displayedMessages: () -> List<Message> = { conversationSession.displayedMessages.value },
        val isDisplayingAudioTranscription: (Message) -> Boolean = AlternateMessageService::isDisplayingAudioTranscription,
        val visibleMessageIDs: () -> Set<String> = { visibleMessageIDsProvider?.invoke() ?: emptySet() },
    )

    // MARK: - Dependencies

    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val hapticsService: HapticsService by Dependency { it.commonServices.haptics }
    private val textToSpeechService: TextToSpeechService by Dependency { it.commonServices.audio.textToSpeech }

    // MARK: - Properties

    private val audioFocus = AudioFocusService { stopPlayback() }
    private val scope = CoroutineScope(Dispatchers.Main)

    private var appContext: Context? = null
    private var dependencies: Dependencies? = null
    private var visibleMessageIDsProvider: (() -> Set<String>)? = null
    private var player: MediaPlayer? = null
    private var progressJob: Job? = null

    // Backed by observable snapshot state so audio bubbles reflect the
    // playing message and its progress.
    private var mutablePlayingMessageID by mutableStateOf<String?>(null)
    private var mutableProgress by mutableFloatStateOf(0f)

    // MARK: - Computed Properties

    /** The identifier of the message currently playing, or `null`. */
    val playingMessageID: String?
        get() = mutablePlayingMessageID

    /** The message currently playing, or `null`. */
    val playingMessage: Message?
        get() = mutablePlayingMessageID?.let { id -> conversationSession.displayedMessages.value.firstOrNull { it.id == id } }

    /**
     * The playback progress of the current message, in
     * `0.0...1.0`.
     */
    val progress: Float
        get() = mutableProgress

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        audioFocus.initialize(context)
    }

    /** Replaces the message list the service reads. */
    internal fun setDependencies(dependencies: Dependencies?) {
        this.dependencies = dependencies
    }

    // MARK: - Set Visible Message IDs

    /**
     * Registers the provider of the identifiers of the messages the
     * chat page currently shows, or clears it.
     *
     * Playback advances only to a next audio message that is visible.
     *
     * @param provider Returns the visible messages' identifiers, or
     *   `null` to clear the registration.
     */
    fun setVisibleMessageIDs(provider: (() -> Set<String>)?) {
        visibleMessageIDsProvider = provider
    }

    // MARK: - Did Tap Play Button

    /**
     * Handles a tap on an audio message's play button, starting
     * or stopping playback.
     *
     * Tapping a message that is not playing starts its playback;
     * tapping the playing message stops it.
     *
     * @param message The tapped message.
     * @param audioReference The message's resolved audio.
     */
    fun didTapPlayButton(
        message: Message,
        audioReference: AudioMessageReference,
    ) {
        val audioFile = if (message.isFromCurrentUser) audioReference.original else audioReference.translated
        val file = audioFile.localPathFile ?: return

        // Both the play and the stop tap give feedback.
        hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)
        if (mutablePlayingMessageID == message.id) return stopPlayback()

        textToSpeechService.stop()
        play(message.id, file.absolutePath)
    }

    // MARK: - Stop Playback

    /** Stops audio playback and resets the playing state. */
    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        audioFocus.abandonFocus()
        mutablePlayingMessageID = null
        mutableProgress = 0f
    }

    // MARK: - Auxiliary

    private fun play(
        messageID: String,
        filePath: String,
    ) {
        stopPlayback()
        audioFocus.requestFocus()

        player =
            MediaPlayer().apply {
                setAudioAttributes(audioFocus.audioAttributes)
                setDataSource(filePath)
                setOnPreparedListener {
                    start()
                    startProgressPolling()
                }
                setOnCompletionListener { onFinished(messageID) }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
                runCatching { prepareAsync() }.onFailure { stopPlayback() }
            }
        mutablePlayingMessageID = messageID
    }

    private fun onFinished(finishedMessageID: String) {
        val next = nextAudioMessage(finishedMessageID)
        stopPlayback()

        next ?: return
        scope.launch {
            delay(PlaybackFloats.PLAY_NEXT_MESSAGE_DELAY_MILLISECONDS)
            val reference = next.audioComponent ?: return@launch
            didTapPlayButton(next, reference)
        }
    }

    /**
     * Returns the audio message that follows the given one,
     * provided it is visible and not showing its transcription;
     * otherwise, `null`.
     */
    internal fun nextAudioMessage(afterMessageID: String): Message? {
        val dependencies = resolvedDependencies()
        val messages = dependencies.displayedMessages()
        val index = messages.indexOfFirst { it.id == afterMessageID }
        if (index < 0) return null
        return messages.getOrNull(index + 1)?.takeIf { next ->
            next.contentType.isAudio &&
                next.id in dependencies.visibleMessageIDs() &&
                !dependencies.isDisplayingAudioTranscription(next)
        }
    }

    private fun resolvedDependencies(): Dependencies = dependencies ?: Dependencies().also { dependencies = it }

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
                    delay((PlaybackFloats.PLAYBACK_TIMER_TIME_INTERVAL * MILLISECONDS_PER_SECOND).toLong())
                }
            }
    }

    // MARK: - Companion

    private const val MILLISECONDS_PER_SECOND = 1000f
}
