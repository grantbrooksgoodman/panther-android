//
//  ReadReceiptServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import java.util.Date

/**
 * Exercises read receipts: unread incoming messages are marked read,
 * and nothing is marked once the most recent incoming message has
 * been read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReadReceiptServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("read-receipt-service-test")
        environment.signIn(SessionTestEnvironment.user(id = "me"))
        SessionStore.upsertUser(SessionTestEnvironment.user(id = "a", nationalNumber = "5559876543"))
    }

    @After
    fun tearDown() {
        ConversationSessionService.setCurrentConversation(null)
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `unread incoming messages are marked read`() =
        runBlocking {
            setCurrentConversation(incoming("m1"), incoming("m2"))

            assertTrue(ReadReceiptService.updateReadDateForUnreadMessages())
            assertTrue(environment.database.committedUpdates.any { update -> update.keys.any { it.contains("readReceipts") } })
        }

    @Test
    fun `nothing is marked once the latest incoming message has been read`() =
        runBlocking {
            setCurrentConversation(incoming("m1"), incoming("m2", isRead = true))

            assertFalse(ReadReceiptService.updateReadDateForUnreadMessages())
            assertTrue(environment.database.committedUpdates.isEmpty())
        }

    // MARK: - Auxiliary

    private fun incoming(
        id: String,
        isRead: Boolean = false,
    ): Message =
        SessionTestEnvironment
            .message(id = id, fromAccountID = "a", sentDate = Date())
            .copy(readReceipts = if (isRead) listOf(ReadReceipt(userID = "me", readDate = Date())) else null)

    private fun setCurrentConversation(vararg messages: Message) {
        SessionStore.upsertMessages(messages.toSet())
        val conversation = SessionTestEnvironment.conversation("-C", listOf("me", "a")).copy(messageIDs = messages.map { it.id })
        SessionStore.upsertConversation(conversation)
        ConversationSessionService.setCurrentConversation(conversation)
    }
}
