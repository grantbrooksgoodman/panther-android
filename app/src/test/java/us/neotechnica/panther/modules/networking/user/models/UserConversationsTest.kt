//
//  UserConversationsTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.services.SessionStore

/**
 * Exercises conversation resolution on a user: the list resolves only
 * when every identifier is in the session store.
 */
class UserConversationsTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("user-conversations-test")
    }

    // MARK: - Tests

    @Test
    fun `conversations resolve only when every identifier is stored`() {
        val first = SessionTestEnvironment.conversation("-A", listOf("me", "them"))
        val second = SessionTestEnvironment.conversation("-B", listOf("me", "them"))
        val user =
            SessionTestEnvironment.user(
                id = "me",
                conversationIDs = listOf(first.id, second.id),
            )

        assertNull(user.conversations)

        SessionStore.upsertConversation(first)
        assertNull(user.conversations)

        SessionStore.upsertConversation(second)
        assertEquals(listOf("-A", "-B"), user.conversations?.map { it.id.key })

        assertNull(user.copy(conversationIDs = null).conversations)
        assertNull(user.copy(conversationIDs = listOf(ConversationID(key = "-Z", hash = "z"))).conversations)
    }
}
