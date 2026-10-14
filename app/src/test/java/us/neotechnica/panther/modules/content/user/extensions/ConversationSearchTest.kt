//
//  ConversationSearchTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.content.user.models.QueriedConversationCache
import us.neotechnica.panther.modules.networking.sources.conversation.models.Activity
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.Date

class ConversationSearchTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("conversation-search-test")
        environment.signIn(SessionTestEnvironment.user(id = "me"))
        SessionStore.upsertUser(SessionTestEnvironment.user(id = "them", languageCode = "es", nationalNumber = "5559876543"))
        QueriedConversationCache.clearCache()
    }

    @After
    fun tearDown() {
        QueriedConversationCache.clearCache()
    }

    // MARK: - Tests

    @Test
    fun `messages sent before the current user was added never match`() {
        val before = textMessage("-M1", from = "them", input = "Secreto", output = "Secret", sentDate = Date(1_000))
        val after = textMessage("-M2", from = "them", input = "Hola", output = "Hello", sentDate = Date(3_000))
        val conversation = conversation(before, after, currentUserAddedDate = Date(2_000))

        assertTrue(listOf(conversation).queried("secret").isEmpty())
        assertEquals(listOf(conversation.id.key), listOf(conversation).queried("hello").map { it.id.key })
    }

    @Test
    fun `received messages match their translated text, not the original`() {
        val received = textMessage("-M1", from = "them", input = "Hola", output = "Hello", sentDate = Date(1_000))
        val conversation = conversation(received)

        assertTrue(listOf(conversation).queried("hola").isEmpty())
        assertEquals(listOf(conversation.id.key), listOf(conversation).queried("hello").map { it.id.key })
    }

    @Test
    fun `own messages match the text the current user wrote, not its translation`() {
        val own = textMessage("-M1", from = "me", input = "Goodbye", output = "Adiós", sentDate = Date(1_000))
        val conversation = conversation(own)

        assertTrue(listOf(conversation).queried("adiós").isEmpty())
        assertEquals(listOf(conversation.id.key), listOf(conversation).queried("goodbye").map { it.id.key })
    }

    // MARK: - Auxiliary

    private fun conversation(
        vararg messages: Message,
        currentUserAddedDate: Date? = null,
    ): Conversation {
        SessionStore.upsertMessages(messages.toSet())
        return SessionTestEnvironment
            .conversation("-C", listOf("me", "them"), messageIDs = messages.map { it.id })
            .copy(
                activities =
                    currentUserAddedDate?.let {
                        listOf(Activity(Activity.Action.AddedToConversation("me"), it, "them"))
                    },
            )
    }

    private fun textMessage(
        id: String,
        from: String,
        input: String,
        output: String,
        sentDate: Date,
    ): Message {
        val languagePair = if (from == "me") LanguagePair("en", "es") else LanguagePair("es", "en")
        return SessionTestEnvironment
            .message(id = id, fromAccountID = from, sentDate = sentDate)
            .copy(translations = listOf(Translation(TranslationInput(input), output, languagePair)))
    }
}
