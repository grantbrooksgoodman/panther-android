//
//  SchemaRoundTripTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.parity

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash

class SchemaRoundTripTest {
    // MARK: - Round-Trip Tests

    @Test
    fun `user decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("user.json")
            assertEquals(fixture, User.decode(fixture).encoded)
        }

    @Test
    fun `conversation decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("conversation.json")
            assertEquals(fixture, Conversation.decode(fixture).encoded)
        }

    @Test
    fun `message decodes and re-encodes structurally identically`() =
        runTest {
            val fixture = FixtureJson.loadObject("message.json")
            assertEquals(fixture, Message.decode(fixture).encoded)
        }

    // MARK: - Identity-Hash Tests

    @Test
    fun `decoded type hashes match iOS`() =
        runTest {
            val expected = FixtureJson.loadObject("type_hashes.json")

            assertEquals(
                expected["userHash"],
                User.decode(FixtureJson.loadObject("user.json")).encodedHash,
            )

            assertEquals(
                expected["conversationHash"],
                Conversation.decode(FixtureJson.loadObject("conversation.json")).encodedHash,
            )

            assertEquals(
                expected["messageHash"],
                Message.decode(FixtureJson.loadObject("message.json")).encodedHash,
            )
        }
}
