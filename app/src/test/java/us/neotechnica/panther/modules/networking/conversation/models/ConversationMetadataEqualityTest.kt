//
//  ConversationMetadataEqualityTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.parity.FixtureJson
import java.util.Date

/**
 * Exercises value equality for conversation metadata: image bytes
 * compare by content, the parity fixture decodes to equal values
 * regardless of key order, and the mutator clears the image only
 * when asked.
 */
class ConversationMetadataEqualityTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("conversation-metadata-equality-test")
    }

    // MARK: - Tests

    @Test
    fun `metadata with equal image bytes is equal and hashes equally`() {
        val date = Date(0)
        val left = metadata(imageData = byteArrayOf(1, 2, 3), date = date)
        val right = metadata(imageData = byteArrayOf(1, 2, 3), date = date)
        val different = metadata(imageData = byteArrayOf(3, 2, 1), date = date)

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertEquals(left.imageHash, right.imageHash)
        assertNotEquals(left, different)
        assertNotEquals(left.imageHash, different.imageHash)
    }

    @Test
    fun `the parity fixture decodes to equal values regardless of key order`() {
        val fixture = FixtureJson.loadObject("conversation_metadata_equality.json")

        @Suppress("UNCHECKED_CAST")
        val left = ConversationMetadata.decode(fixture["left"] as Map<String, Any?>)

        @Suppress("UNCHECKED_CAST")
        val right = ConversationMetadata.decode(fixture["right"] as Map<String, Any?>)

        @Suppress("UNCHECKED_CAST")
        val different = ConversationMetadata.decode(fixture["different"] as Map<String, Any?>)

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertEquals(left.encoded, right.encoded)
        assertNotEquals(left, different)
    }

    @Test
    fun `the mutator keeps the image unless cleared and returns itself without arguments`() {
        val original = metadata(imageData = byteArrayOf(9, 9), date = Date(0))

        assertSame(original, original.copyWith())
        assertEquals(original, original.copyWith(name = original.name))

        val cleared = original.copyWith(nilImageData = true)
        assertNull(cleared.imageData)
        assertNull(cleared.imageHash)
        assertNotEquals(original, cleared)
    }

    // MARK: - Auxiliary

    private fun metadata(
        imageData: ByteArray?,
        date: Date,
    ): ConversationMetadata =
        ConversationMetadata(
            name = "Group",
            imageData = imageData,
            isPenPalsConversation = false,
            lastModifiedDate = date,
            messageRecipientConsentAcknowledgementData = emptyList(),
            penPalsSharingData = emptyList(),
            requiresConsentFromInitiator = null,
        )
}
