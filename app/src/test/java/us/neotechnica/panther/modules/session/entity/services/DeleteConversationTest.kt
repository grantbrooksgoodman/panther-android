//
//  DeleteConversationTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment

/**
 * Exercises forced conversation deletion: participant tokens, the
 * message node and its hosted media, and the conversation node are
 * all removed.
 */
class DeleteConversationTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("delete-conversation-test")
        environment.signIn(SessionTestEnvironment.user(id = "me"))
    }

    // MARK: - Tests

    @Test
    fun `a forced deletion removes tokens, messages, hosted media, and the node`() =
        runTest {
            val conversation = SessionTestEnvironment.conversation("-C", listOf("me", "them"), messageIDs = listOf("-M1"))
            val contentType = "image/jpeg – -MediaID – jpg"

            environment.database.getValuesResults["messages/-M1/contentType"] = contentType
            environment.database.getValuesResults["messages"] = mapOf("-M1" to mapOf("contentType" to contentType))

            ConversationSessionService.deleteConversation(conversation, forced = true)

            val tokenRemoval = environment.database.committedUpdates.first()
            assertEquals(
                mapOf<String, Any?>(
                    "users/me/openConversations/-C" to null,
                    "users/them/openConversations/-C" to null,
                ),
                tokenRemoval,
            )

            assertTrue(environment.storage.deletedPaths.contains("audioMessageInputs/-M1.m4a"))
            assertTrue(environment.storage.deletedPaths.contains("media/-MediaID.jpg"))
            assertTrue(environment.storage.deletedPaths.contains("media/-MediaID.jpg-thumbnail.jpeg"))

            assertTrue(environment.database.setValues.contains(null to "messages/-M1"))
            assertTrue(environment.database.committedUpdates.any { it.containsKey("conversations/-C/messages/-M1") })
            assertTrue(environment.database.committedUpdates.none { it.containsKey("conversations/-C/hash") })
            assertEquals(null to "conversations/-C", environment.database.setValues.last())
        }
}
