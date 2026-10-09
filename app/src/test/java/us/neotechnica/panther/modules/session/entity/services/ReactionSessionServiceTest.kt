//
//  ReactionSessionServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.serializable.decode
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment.Companion.user
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Exercises reactions: applying one writes the reaction and clears
 * the in-flight flag, applying the same one again removes it, and a
 * message that is not displayed is rejected.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReactionSessionServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private lateinit var conversation: Conversation
    private lateinit var message: Message

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("reaction-session-service-test")
        environment.hostedTranslation.seed(
            inputValueEncodedHash = "-FixtureTranslation01",
            translation = Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es")),
        )

        conversation = runBlocking { Conversation.decode(FixtureJson.loadObject("conversation.json")) }
        environment.signIn(user(id = conversation.participants.first().userID, languageCode = "en"))

        message = runBlocking { Message.decode(FixtureJson.loadObject("message.json")) }
        SessionStore.upsertMessages(setOf(message))
        ConversationSessionService.setCurrentConversation(conversation)

        // The reaction write re-resolves the message from the network.
        environment.database.getValuesResults["messages/${message.id}"] = FixtureJson.loadObject("message.json")
    }

    @After
    fun tearDown() {
        ConversationSessionService.setCurrentConversation(null)
    }

    // MARK: - Tests

    @Test
    fun `reacting writes the reaction and reacting again removes it`() =
        runBlocking {
            val reaction = checkNotNull(Reaction.from(Reaction.Style.LOVE))

            ReactionSessionService.react(reaction, message)

            assertFalse(ReactionSessionService.isReactingToMessage)
            val stored = checkNotNull(SessionStore.getConversation(conversation.id.key))
            assertEquals(listOf(reaction), stored.reactionMetadata?.single()?.reactions)

            environment.database.transactionSeed = environment.database.lastTransactionResult
            ReactionSessionService.react(reaction, message)

            assertFalse(ReactionSessionService.isReactingToMessage)
            assertNull(SessionStore.getConversation(conversation.id.key)?.reactionMetadata)
        }

    @Test
    fun `reacting to a message that is not displayed is rejected`() =
        runBlocking {
            val undisplayed = message.copy(id = "-Undisplayed")

            try {
                ReactionSessionService.react(checkNotNull(Reaction.from(Reaction.Style.LIKE)), undisplayed)
                fail("expected the reaction to be rejected")
            } catch (exception: Exception) {
                assertEquals("Failed to resolve required values.", exception.descriptor)
            }

            assertTrue(environment.database.committedUpdates.isEmpty())
        }
}
