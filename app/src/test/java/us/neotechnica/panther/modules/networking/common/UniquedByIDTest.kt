//
//  UniquedByIDTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import us.neotechnica.panther.modules.networking.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import java.util.Date

/**
 * Exercises identifier de-duplication: order is preserved and a copy
 * carrying read receipts wins over one without.
 */
class UniquedByIDTest {
    // MARK: - Tests

    @Test
    fun `duplicates collapse in order and read receipts take priority`() {
        val plain = SessionTestEnvironment.message("-1", fromAccountID = "them", sentDate = Date(1_000))
        val read = plain.copy(readReceipts = listOf(ReadReceipt(userID = "me", readDate = Date(2_000))))
        val other = SessionTestEnvironment.message("-2", fromAccountID = "them", sentDate = Date(3_000))

        val readLast = listOf(plain, other, read).uniquedByID
        assertEquals(listOf("-1", "-2"), readLast.map { it.id })
        assertNotNull(readLast.first().readReceipts)

        val readFirst = listOf(read, other, plain).uniquedByID
        assertEquals(listOf("-1", "-2"), readFirst.map { it.id })
        assertNotNull(readFirst.first().readReceipts)

        val neverRead = listOf(plain, plain, other).uniquedByID
        assertEquals(2, neverRead.size)
        assertNull(neverRead.first().readReceipts)
    }
}
