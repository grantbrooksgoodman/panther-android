//
//  ConversationDecodeTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.conversation.models

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * Exercises the conversation decoders: empty collections fail,
 * image data must be padded Base64, and the archive decoder agrees
 * with the serialized decoder.
 */
class ConversationDecodeTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("conversation-decode-test")
    }

    // MARK: - Tests

    @Test
    fun `an empty activities list fails to decode`() {
        val fixture = FixtureJson.loadObject("conversation.json").toMutableMap()
        fixture["activities"] = emptyList<Map<String, Any?>>()

        assertTrue(Conversation.canDecode(fixture))
        assertEquals("Collection is empty.", decodeFailure(fixture).descriptor)
    }

    @Test
    fun `an empty reactions list fails to decode`() {
        val fixture = FixtureJson.loadObject("conversation.json").toMutableMap()
        fixture["reactionMetadata"] = listOf(mapOf("messageID" to "!", "reactions" to emptyList<Map<String, Any?>>()))

        assertTrue(Conversation.canDecode(fixture))
        assertEquals("Collection is empty.", decodeFailure(fixture).descriptor)
    }

    @Test
    fun `image data must be padded Base64`() {
        @Suppress("UNCHECKED_CAST")
        val metadata = (FixtureJson.loadObject("conversation.json")["metadata"] as Map<String, Any?>).toMutableMap()

        metadata["imageData"] = "AQID"
        assertTrue(ConversationMetadata.canDecode(metadata))
        assertArrayEquals(byteArrayOf(1, 2, 3), ConversationMetadata.decode(metadata).imageData)

        metadata["imageData"] = "AQI"
        assertFalse(ConversationMetadata.canDecode(metadata))
        assertEquals(
            "Decoding failed.",
            (runCatching { ConversationMetadata.decode(metadata) }.exceptionOrNull() as Exception).descriptor,
        )
    }

    @Test
    fun `the archive decoder agrees with the decoder and round-trips`() {
        val fixture = FixtureJson.loadObject("conversation.json")
        val conversation = Conversation.fromArchive(fixture)

        assertEquals(Conversation.decode(fixture), conversation)
        assertEquals(conversation, Conversation.fromArchive(conversation.encoded))
    }

    @Test
    fun `a style is found by its emoji`() {
        assertEquals(Reaction.Style.LOVE, Reaction.Style.fromEmojiValue("❤️"))
        assertEquals(null, Reaction.Style.fromEmojiValue("🙂"))
    }

    // MARK: - Auxiliary

    private fun decodeFailure(fixture: Map<String, Any?>): Exception =
        runCatching { Conversation.decode(fixture) }.exceptionOrNull() as Exception
}
