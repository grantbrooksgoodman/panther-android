//
//  OutboxDisplayMessageTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import java.util.Date

/**
 * Exercises an outbox entry's display message: staged text is
 * shown without its trailing whitespace.
 */
class OutboxDisplayMessageTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("outbox-display-message-test")
        environment.signIn(SessionTestEnvironment.user(id = "me"))
    }

    // MARK: - Tests

    @Test
    fun `staged text displays without its trailing whitespace`() {
        val entry =
            OutboxEntry(
                conversationIDKey = "-C",
                createdDate = Date(1_000),
                fromAccountID = "me",
                id = "outbox-1",
                isPenPalsConversation = false,
                payload = OutboxEntry.Payload.Text("Hello  \n"),
                recipientUserIDs = listOf("them"),
                attemptCount = 1,
                lastAttemptDate = null,
                reservedRemoteID = null,
                state = OutboxEntry.State.SENDING,
                transcription = null,
            )

        val translation = entry.asDisplayMessage.translations?.single()
        assertEquals("Hello", translation?.input?.original)
        assertEquals("Hello", translation?.output)
    }
}
