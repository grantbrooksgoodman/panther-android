//
//  AudioMessagePlaybackServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.modules.networking.sources.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import java.util.Date

/**
 * Exercises auto-advance: playback moves on to the next audio message
 * only when it is visible and not showing its transcription.
 */
class AudioMessagePlaybackServiceTest {
    // MARK: - Setup

    private val first = audioMessage("m1")
    private val second = audioMessage("m2")
    private val text = SessionTestEnvironment.message(id = "m3", fromAccountID = "a", sentDate = Date())

    private var transcribingIDs = setOf<String>()
    private var visibleIDs = setOf("m1", "m2", "m3")

    @Before
    fun setUp() {
        AudioMessagePlaybackService.setDependencies(
            AudioMessagePlaybackService.Dependencies(
                displayedMessages = { listOf(first, second, text) },
                isDisplayingAudioTranscription = { it.id in transcribingIDs },
                visibleMessageIDs = { visibleIDs },
            ),
        )
    }

    @After
    fun tearDown() {
        AudioMessagePlaybackService.setDependencies(null)
    }

    // MARK: - Tests

    @Test
    fun `playback advances to the next visible audio message`() {
        assertEquals(second, AudioMessagePlaybackService.nextAudioMessage("m1"))
    }

    @Test
    fun `playback never advances to an offscreen message`() {
        visibleIDs = setOf("m1")

        assertNull(AudioMessagePlaybackService.nextAudioMessage("m1"))
    }

    @Test
    fun `playback never advances to a message showing its transcription`() {
        transcribingIDs = setOf("m2")

        assertNull(AudioMessagePlaybackService.nextAudioMessage("m1"))
    }

    @Test
    fun `playback stops after the last audio message`() {
        assertNull(AudioMessagePlaybackService.nextAudioMessage("m2"))
        assertNull(AudioMessagePlaybackService.nextAudioMessage("missing"))
    }

    // MARK: - Auxiliary

    private fun audioMessage(id: String): Message =
        SessionTestEnvironment
            .message(id = id, fromAccountID = "a", sentDate = Date())
            .copy(contentType = HostedContentType.Audio(AudioFileExtension.entries.first()))
}
