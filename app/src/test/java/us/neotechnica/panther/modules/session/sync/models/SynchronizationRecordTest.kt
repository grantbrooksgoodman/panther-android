//
//  SynchronizationRecordTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.sync.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SynchronizationRecordTest {
    // MARK: - Cooldown Ladder

    @Test
    fun `first attempt expires after three seconds`() {
        assertFalse(record(attempt = 1, secondsAgo = 2).isExpired)
        assertTrue(record(attempt = 1, secondsAgo = 4).isExpired)
    }

    @Test
    fun `second attempt expires after fifteen seconds`() {
        assertFalse(record(attempt = 2, secondsAgo = 10).isExpired)
        assertTrue(record(attempt = 2, secondsAgo = 16).isExpired)
    }

    @Test
    fun `third attempt expires after sixty seconds`() {
        assertFalse(record(attempt = 3, secondsAgo = 45).isExpired)
        assertTrue(record(attempt = 3, secondsAgo = 61).isExpired)
    }

    @Test
    fun `attempts beyond the ladder clamp to the longest cooldown`() {
        assertFalse(record(attempt = 9, secondsAgo = 45).isExpired)
        assertTrue(record(attempt = 9, secondsAgo = 61).isExpired)
    }

    // MARK: - Equality

    @Test
    fun `equality and hashing are keyed only by conversation ID key`() {
        val first = SynchronizationRecord("conversation-1", attempt = 1)
        val second = SynchronizationRecord("conversation-1", attempt = 5, date = Date(0))
        val third = SynchronizationRecord("conversation-2", attempt = 1)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertEquals(setOf(first), setOf(first, second))
        assertEquals(2, setOf(first, second, third).size)
    }

    // MARK: - Auxiliary

    private fun record(
        attempt: Int,
        secondsAgo: Long,
    ): SynchronizationRecord =
        SynchronizationRecord(
            conversationIDKey = "conversation-1",
            attempt = attempt,
            date = Date(System.currentTimeMillis() - secondsAgo * 1_000),
        )
}
