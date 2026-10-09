//
//  ConversationsDidChangeTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.modules.session.state.services.SessionStore

/**
 * Exercises the current-user observer's conversation diff: removed
 * conversations leave the store, already-known versions skip the
 * resolve, and unknown versions trigger a full resolve.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsDidChangeTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private lateinit var first: Conversation
    private lateinit var second: Conversation
    private lateinit var me: User

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("conversations-did-change-test")
        UserService.clearCache()

        first = SessionTestEnvironment.conversation("-A", listOf("me", "them"))
        second = SessionTestEnvironment.conversation("-B", listOf("me", "them"))
        me = SessionTestEnvironment.user(id = "me", conversationIDs = listOf(first.id, second.id))

        environment.signIn(me)
        SessionStore.upsertUser(SessionTestEnvironment.user(id = "them", nationalNumber = "5559876543"))
        SessionStore.upsertConversations(setOf(first, second))

        environment.database.getValuesResults["users/me"] = me.encoded
        environment.database.getValuesResults["conversations/-A"] = first.encoded
        environment.database.getValuesResults["conversations/-B"] = second.encoded
    }

    @After
    fun tearDown() {
        UserSessionService.stopObservingCurrentUserChanges()
    }

    // MARK: - Tests

    @Test
    fun `a conversation missing from the user node is removed and a resolve follows`() =
        runBlocking {
            // The follow-up resolve reads the user node without the removed conversation.
            environment.database.getValuesResults["users/me"] = me.copy(conversationIDs = listOf(first.id)).encoded

            UserSessionService.startObservingCurrentUserChanges()
            environment.database.emit("users/me", snapshot(mapOf("-A" to first.id.hash)))

            awaitUntil { SessionStore.getConversation("-B") == null }
            assertNotNull(SessionStore.getConversation("-A"))
            awaitUntil { "users/me" in environment.database.getValuesPaths }
        }

    @Test
    fun `an already-known conversation version does not trigger a resolve`() =
        runBlocking {
            val newerSecond = second.copy(id = ConversationID(key = "-B", hash = "hash-B-2"))
            SessionStore.upsertConversation(newerSecond)

            UserSessionService.startObservingCurrentUserChanges()
            environment.database.emit(
                "users/me",
                snapshot(mapOf("-A" to first.id.hash, "-B" to "hash-B-2")),
            )

            delay(SETTLE_MILLIS)
            assertTrue(environment.database.getValuesPaths.isEmpty())
            assertEquals("hash-B-2", SessionStore.getConversation("-B")?.id?.hash)
        }

    @Test
    fun `an unrecognized conversation version triggers a full resolve`() =
        runBlocking {
            UserSessionService.startObservingCurrentUserChanges()
            environment.database.emit(
                "users/me",
                snapshot(mapOf("-A" to first.id.hash, "-B" to "hash-B-remote")),
            )

            awaitUntil { "users/me" in environment.database.getValuesPaths }
        }

    @Test
    fun `a user without a local conversation map always resolves`() =
        runBlocking {
            environment.signIn(me.copy(conversationIDs = null))

            UserSessionService.startObservingCurrentUserChanges()
            environment.database.emit("users/me", snapshot(mapOf("-A" to first.id.hash)))

            awaitUntil { "users/me" in environment.database.getValuesPaths }
            assertNull(SessionStore.getConversation("-Z"))
        }

    // MARK: - Auxiliary

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(TIMEOUT_MILLIS) {
            while (!condition()) delay(POLL_MILLIS)
        }
    }

    private fun snapshot(openConversations: Map<String, String>): Map<String, Any?> =
        mapOf(
            User.SerializableKey.CONVERSATION_IDS.rawValue to openConversations,
            User.SerializableKey.DEVICE_ID.rawValue to "device",
        )

    // MARK: - Companion

    private companion object {
        const val POLL_MILLIS = 10L
        const val SETTLE_MILLIS = 300L
        const val TIMEOUT_MILLIS = 3_000L
    }
}
