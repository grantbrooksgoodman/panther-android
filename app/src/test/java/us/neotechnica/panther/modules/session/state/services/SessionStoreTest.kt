//
//  SessionStoreTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.sessionStoreDidChange
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.serializable.decode
import us.neotechnica.panther.modules.networking.support.FakeHostedTranslationDelegate
import us.neotechnica.panther.modules.session.entity.extensions.empty
import us.neotechnica.panther.modules.session.state.extensions.archived
import us.neotechnica.panther.modules.session.state.extensions.fromArchive
import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.SessionStoreStorageKey
import us.neotechnica.panther.networking.modules.common.extensions.sessionStore
import us.neotechnica.panther.parity.FixtureJson
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.io.File

class SessionStoreTest {
    // MARK: - Setup

    private val conversationArchiveKey = PersistentStorageKey.sessionStore(SessionStoreStorageKey.CONVERSATION_ARCHIVE)
    private val messageArchiveKey = PersistentStorageKey.sessionStore(SessionStoreStorageKey.MESSAGE_ARCHIVE)

    private lateinit var validConversation: Conversation
    private lateinit var validMessage: Message

    @Before
    fun setUp() {
        val directory = File(System.getProperty("java.io.tmpdir"), "session-store-test-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()

        val hostedTranslation = FakeHostedTranslationDelegate()
        hostedTranslation.seed(
            inputValueEncodedHash = "-FixtureTranslation01",
            translation = Translation(TranslationInput("Hello"), "Hola", LanguagePair("en", "es")),
        )
        Networking.config.registerHostedTranslationDelegate(hostedTranslation)

        validConversation = runBlocking { Conversation.decode(FixtureJson.loadObject("conversation.json")) }
        validMessage = runBlocking { Message.decode(FixtureJson.loadObject("message.json")) }

        Persistent.setArchive(conversationArchiveKey, null)
        Persistent.setArchive(messageArchiveKey, null)
        SessionStore.reloadForTesting()
    }

    // MARK: - Tests

    @Test
    fun `load applies conversation filters`() {
        Persistent.setArchive(conversationArchiveKey, listOf(validConversation.encoded, Conversation.empty.encoded))
        SessionStore.reloadForTesting()

        assertEquals(setOf(validConversation.id.key), SessionStore.conversations.keys)
    }

    @Test
    fun `sweeps orphaned messages on load`() {
        // A message with no referencing conversation is an orphan.
        Persistent.setArchive(messageArchiveKey, listOf(validMessage.archived))
        SessionStore.reloadForTesting()

        assertTrue(SessionStore.messages.isEmpty())
    }

    @Test
    fun `message archive round-trips the decoded message`() {
        assertEquals(validMessage, Message.fromArchive(validMessage.archived))
    }

    @Test
    fun `message archive restores referenced messages on load`() {
        val referencedMessageID = validConversation.messageIDs.firstOrNull() ?: return
        val referencedMessage = validMessage.copy(id = referencedMessageID)
        Persistent.setArchive(conversationArchiveKey, listOf(validConversation.encoded))
        Persistent.setArchive(messageArchiveKey, listOf(referencedMessage.archived))
        SessionStore.reloadForTesting()

        assertEquals(referencedMessage, SessionStore.messages[referencedMessageID])
    }

    @Test
    fun `removeConversation removes orphaned messages`() {
        val referencedMessageID = validConversation.messageIDs.firstOrNull() ?: return
        val referencedMessage = validMessage.copy(id = referencedMessageID)

        SessionStore.upsertConversation(validConversation)
        SessionStore.upsertMessages(setOf(referencedMessage))
        assertTrue(SessionStore.messages.containsKey(referencedMessageID))

        SessionStore.removeConversation(validConversation.id.key)

        assertFalse(SessionStore.conversations.containsKey(validConversation.id.key))
        assertFalse(SessionStore.messages.containsKey(referencedMessageID))
    }

    @Test
    fun `upsertConversation emits only when changed`() =
        runTest(UnconfinedTestDispatcher()) {
            val received = mutableListOf<SessionStoreChange>()
            val sharedEvents = DependencyValues.current.sharedEvents
            val job = launch { sharedEvents.sessionStoreDidChange.events.collect { received.add(it) } }

            SessionStore.upsertConversation(validConversation)
            SessionStore.upsertConversation(validConversation)

            job.cancel()

            assertEquals(1, received.count { it is SessionStoreChange.Conversations })
        }
}
