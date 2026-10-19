//
//  ConversationRemotelyUpdatableTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.conversation.remotelyupdatable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.currentUserID
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.conversation.models.copyWith
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.message.serializable.decode
import us.neotechnica.panther.modules.networking.sources.support.FakeDatabaseDelegate
import us.neotechnica.panther.modules.networking.sources.support.FakeHostedTranslationDelegate
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.ReactionSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationRemotelyUpdatableTest {
    // MARK: - Setup

    private lateinit var database: FakeDatabaseDelegate
    private lateinit var conversation: Conversation

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val directory = File(System.getProperty("java.io.tmpdir"), "conversation-updatable-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
        SessionStore.reloadForTesting()

        database = FakeDatabaseDelegate()
        Networking.config.registerDatabaseDelegate(database)

        val hostedTranslation = FakeHostedTranslationDelegate()
        hostedTranslation.seed(
            inputValueEncodedHash = "-FixtureTranslation01",
            translation = Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es")),
        )
        Networking.config.registerHostedTranslationDelegate(hostedTranslation)

        conversation = runBlocking { Conversation.decode(FixtureJson.loadObject("conversation.json")) }
    }

    @After
    fun tearDown() {
        // Clear the current-conversation pointer so later store
        // mutations in other test classes do not trigger the
        // conversation-removed presentation path.
        ConversationSessionService.setCurrentConversation(null)
        Dispatchers.resetMain()
    }

    // MARK: - Update Values

    @Test
    fun `updateValues writes changed field, hash, and participant tokens`() =
        runTest {
            val key = conversation.id.key
            val newMetadata = conversation.metadata.copyWith(name = "Renamed Conversation")

            val updated = conversation.updateValues(mapOf(ConversationUpdatableKey.METADATA to newMetadata))

            val updates = database.committedUpdates.single()
            assertTrue(updates.containsKey("conversations/$key/metadata"))
            assertEquals(updated.id.hash, updates["conversations/$key/hash"])
            for (participant in conversation.participants) {
                assertEquals(updated.id.hash, updates["users/${participant.userID}/openConversations/$key"])
            }

            // Fields that did not change are not written.
            assertFalse(updates.containsKey("conversations/$key/activities"))

            // The rehashed conversation is upserted into the store.
            assertEquals(updated, SessionStore.getConversation(updated.id))
        }

    @Test
    fun `updateValues rejects the identifier and hash keys as not updatable`() =
        runTest {
            for (key in listOf(ConversationUpdatableKey.ID, ConversationUpdatableKey.ENCODED_HASH)) {
                val failure = runCatching { conversation.updateValues(mapOf(key to "value")) }.exceptionOrNull()
                assertEquals("The specified serialization key is not updatable.", (failure as Exception).descriptor)
            }

            assertTrue(database.committedUpdates.isEmpty())
        }

    @Test
    fun `updateValues rejects a mistyped list`() =
        runTest {
            val failure =
                runCatching { conversation.updateValues(mapOf(ConversationUpdatableKey.ACTIVITIES to listOf("left"))) }
                    .exceptionOrNull()

            assertEquals("Type mismatch for serialization key.", (failure as Exception).descriptor)
            assertTrue(database.committedUpdates.isEmpty())
        }

    // MARK: - Reaction Transaction Round-Trip

    @Test
    fun `reaction applyingRaw strips the sentinel, preserves others, and adds the new reaction`() =
        runTest {
            val currentUserID = conversation.participants.first().userID
            Persistent.setString(PersistentStorageKey.currentUserID, currentUserID)

            val message = Message.decode(FixtureJson.loadObject("message.json"))
            SessionStore.upsertMessages(setOf(message))
            ConversationSessionService.setCurrentConversation(conversation)

            // The reaction write re-resolves the message from the network.
            database.getValuesResults["messages/${message.id}"] = FixtureJson.loadObject("message.json")

            // Seed the raw node: a sentinel entry (to strip) plus another
            // user's reaction on the same message (to preserve).
            database.transactionSeed =
                listOf(
                    mapOf<String, Any?>("messageID" to "!", "reactions" to listOf(mapOf("style" to "LOVE", "userID" to "!"))),
                    mapOf<String, Any?>(
                        "messageID" to message.id,
                        "reactions" to listOf(mapOf("style" to "LIKE", "userID" to "other-user")),
                    ),
                )

            ReactionSessionService.react(Reaction(Reaction.Style.LOVE, currentUserID), message)

            @Suppress("UNCHECKED_CAST")
            val result = database.lastTransactionResult as List<Map<String, Any?>>
            assertEquals(1, result.size)
            assertEquals(message.id, result.first()["messageID"])

            @Suppress("UNCHECKED_CAST")
            val reactions = result.first()["reactions"] as List<Map<String, Any?>>
            assertEquals(2, reactions.size)
            assertTrue(reactions.any { it["style"] == "LOVE" && it["userID"] == currentUserID })
            assertTrue(reactions.any { it["style"] == "LIKE" && it["userID"] == "other-user" })
        }
}
