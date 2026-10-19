//
//  PantherMessagingServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.notificationextension

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

class PantherMessagingServiceTest {
    // MARK: - Tests

    @Test
    fun `a payload missing a required value throws a reportable exception`() {
        val failure =
            runCatching { ReceivedNotification.from(mapOf("body" to "Hi", "conversationIDKey" to "-C")) }
                .exceptionOrNull()

        assertEquals("Failed to resolve required values.", (failure as Exception).descriptor)
        assertTrue(failure.isReportable)
    }

    @Test
    fun `a complete payload resolves its values`() {
        val received =
            ReceivedNotification.from(
                mapOf(
                    "body" to "Hi",
                    "conversationIDKey" to "-C",
                    "reactionMessageID" to "!",
                    "recipientUserID" to "me",
                ),
            )

        assertEquals("Hi", received.body)
        assertEquals("-C", received.conversationIDKey)
        assertEquals("me", received.recipientUserID)
        assertNull(received.title)
        assertFalse(received.isReaction)
        assertTrue(received.copy(reactionMessageID = "-M").isReaction)
    }
}
