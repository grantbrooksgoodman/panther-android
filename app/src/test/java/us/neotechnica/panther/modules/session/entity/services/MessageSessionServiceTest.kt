//
//  MessageSessionServiceTest.kt
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.translator.models.LanguagePair

/**
 * Exercises text sends: a recipient's language is translated into
 * and archived, an existing conversation receives the message through
 * the atomic fan-out, and a missing conversation is created around
 * the first message.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageSessionServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private lateinit var me: User
    private lateinit var them: User

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("message-session-service-test")

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
    }

    // MARK: - Tests

    @Test
    fun `sending into an existing conversation translates, appends, and fans out`() =
        runBlocking {
            val conversation = SessionTestEnvironment.conversation("-C", listOf("me", "them"))
            SessionStore.upsertConversation(conversation)

            val updated =
                MessageSessionService.sendTextMessage(
                    text = "Hello",
                    toUsers = listOf(them),
                    inConversation = conversation,
                    isPenPalsConversation = false,
                )

            val requests = environment.hostedTranslation.translateRequests.map { it.first.value to it.second }
            assertEquals(listOf("Hello" to LanguagePair("en", "en")), requests)
            assertTrue(updated.messageIDs.contains("-fakeGeneratedKey"))
            assertTrue(
                environment.database.committedUpdates.any { update ->
                    update.keys.any { it == "conversations/-C/messages/-fakeGeneratedKey" }
                },
            )
            assertEquals(updated, SessionStore.getConversation("-C"))
        }

    @Test
    fun `sending without a conversation creates one around the first message`() =
        runBlocking {
            val created =
                MessageSessionService.sendTextMessage(
                    text = "Hello",
                    toUsers = listOf(them),
                    inConversation = null,
                    isPenPalsConversation = false,
                )

            assertEquals("-fakeGeneratedKey", created.id.key)
            assertEquals(listOf("me", "them"), created.participants.map { it.userID })

            val fanOut = environment.database.committedUpdates.single()
            assertTrue(fanOut.containsKey("conversations/-fakeGeneratedKey/participants"))
            assertTrue(fanOut.containsKey("messages/-fakeGeneratedKey"))
            assertEquals(created.id.hash, fanOut["users/me/openConversations/-fakeGeneratedKey"])
            assertEquals(created.id.hash, fanOut["users/them/openConversations/-fakeGeneratedKey"])
        }
}
