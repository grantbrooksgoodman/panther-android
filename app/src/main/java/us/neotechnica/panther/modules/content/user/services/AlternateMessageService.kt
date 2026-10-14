//
//  AlternateMessageService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency

/**
 * The service that manages messages displaying alternate content, such
 * as original text or audio transcriptions.
 *
 * Use [AlternateMessageService] to track which messages currently show
 * alternate content in place of their default content, and to toggle
 * that state per message. The tracked sets are observable snapshot
 * state, so message cells rebuild as they change.
 */
object AlternateMessageService {
    // MARK: - Dependencies

    private val analyticsService: AnalyticsService by Dependency { it.commonServices.analytics }

    // MARK: - Types

    /** The kinds of alternate content a message can display. */
    enum class AlternateMessageType {
        /** The message's original, untranslated text. */
        ALTERNATE_TEXT,

        /** The transcription of the message's audio. */
        AUDIO_TRANSCRIPTION,
    }

    // MARK: - Properties

    private var alternateTextMessageIDs by mutableStateOf(emptySet<String>())
    private var audioTranscriptionMessageIDs by mutableStateOf(emptySet<String>())

    // MARK: - Is Displaying

    /**
     * Returns a Boolean value that indicates whether the given message
     * currently displays its alternate text.
     *
     * @param message The message to query.
     *
     * @return `true` if the message displays its alternate text;
     *   otherwise, `false`.
     */
    fun isDisplayingAlternateText(message: Message): Boolean = message.id in alternateTextMessageIDs

    /**
     * Returns a Boolean value that indicates whether the given message
     * currently displays its audio transcription.
     *
     * @param message The message to query.
     *
     * @return `true` if the message displays its audio transcription;
     *   otherwise, `false`.
     */
    fun isDisplayingAudioTranscription(message: Message): Boolean = message.id in audioTranscriptionMessageIDs

    // MARK: - Restore All

    /** Restores every message displaying alternate text to its default content. */
    fun restoreAllAlternateTextMessageIDs() {
        alternateTextMessageIDs = emptySet()
    }

    /** Restores every message displaying an audio transcription to its default content. */
    fun restoreAllAudioTranscriptionMessageIDs() {
        audioTranscriptionMessageIDs = emptySet()
    }

    // MARK: - Toggle

    /**
     * Toggles the given kind of alternate content for the given
     * message.
     *
     * @param type The kind of alternate content to toggle.
     * @param message The message to toggle.
     */
    fun toggle(
        type: AlternateMessageType,
        message: Message,
    ) {
        val isDisplaying =
            when (type) {
                AlternateMessageType.ALTERNATE_TEXT -> message.id in alternateTextMessageIDs
                AlternateMessageType.AUDIO_TRANSCRIPTION -> message.id in audioTranscriptionMessageIDs
            }

        if (isDisplaying) {
            remove(type, message)
            return
        }

        append(type, message)
    }

    // MARK: - Auxiliary

    private fun append(
        type: AlternateMessageType,
        message: Message,
    ) {
        analyticsService.logEvent(AnalyticsService.AnalyticsEvent.VIEW_ALTERNATE)
        when (type) {
            AlternateMessageType.ALTERNATE_TEXT -> alternateTextMessageIDs = alternateTextMessageIDs + message.id
            AlternateMessageType.AUDIO_TRANSCRIPTION -> audioTranscriptionMessageIDs = audioTranscriptionMessageIDs + message.id
        }
    }

    private fun remove(
        type: AlternateMessageType,
        message: Message,
    ) {
        when (type) {
            AlternateMessageType.ALTERNATE_TEXT -> alternateTextMessageIDs = alternateTextMessageIDs - message.id
            AlternateMessageType.AUDIO_TRANSCRIPTION -> audioTranscriptionMessageIDs = audioTranscriptionMessageIDs - message.id
        }
    }
}
