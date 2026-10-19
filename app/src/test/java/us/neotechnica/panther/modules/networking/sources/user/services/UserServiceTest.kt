//
//  UserServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.user.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.networking.sources.user.models.UserDataSnapshot
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

class UserServiceTest {
    // MARK: - Tests

    @Test
    fun `a user data snapshot expires on whole seconds only`() {
        val now = System.currentTimeMillis()

        assertFalse(snapshot(capturedAt = now - 900L).isExpired)
        assertTrue(snapshot(capturedAt = now - 1_000L).isExpired)
    }

    @Test
    fun `the empty snapshot is already expired`() {
        assertTrue(UserDataSnapshot.empty.isExpired)
    }

    // MARK: - Auxiliary

    private fun snapshot(capturedAt: Long): UserDataSnapshot =
        UserDataSnapshot(
            date = Date(capturedAt),
            data = emptyMap(),
            expiryThreshold = SNAPSHOT_EXPIRY_MILLIS.milliseconds,
        )

    private companion object {
        const val SNAPSHOT_EXPIRY_MILLIS = 500L
    }
}
