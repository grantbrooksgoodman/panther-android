//
//  MessageDeliveryServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore

/**
 * Exercises the delivery service's text send: the text reaches the
 * session untrimmed, and the sending flag is cleared once the send
 * completes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageDeliveryServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private lateinit var me: User
    private lateinit var them: User

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("message-delivery-service-test")

        me = SessionTestEnvironment.user(id = "me", languageCode = "en")
        // Blocking the sender keeps the recipient out of the push fan-out.
        them =
            SessionTestEnvironment.user(
                id = "them",
                languageCode = "en",
                nationalNumber = "5559876543",
                blockedUserIDs = listOf("me"),
            )

        environment.signIn(me)
        SessionStore.upsertUser(them)

        val conversation = SessionTestEnvironment.conversation("-C", listOf("me", "them"))
        SessionStore.upsertConversation(conversation)
        ConversationSessionService.setCurrentConversation(conversation)
    }

    @After
    fun tearDown() {
        ConversationSessionService.setCurrentConversation(null)
        // A send notifies recipients on a background scope that hops
        // to Main; let it settle before the Main dispatcher goes away.
        runBlocking { delay(SETTLE_MILLIS) }
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `the raw text reaches the session and the sending flag clears`() =
        runBlocking {
            MessageDeliveryService.sendTextMessage("Hello  ")

            val inputs = environment.hostedTranslation.translateRequests.map { it.first.value }
            assertEquals(listOf("Hello  "), inputs)
            assertFalse(MessageDeliveryService.isSendingMessage.value)
        }

    private companion object {
        const val SETTLE_MILLIS = 100L
    }
}
