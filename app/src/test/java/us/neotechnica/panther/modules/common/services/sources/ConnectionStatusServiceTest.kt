//
//  ConnectionStatusServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.content.user.models.ConnectionStatusServiceEffectID

class ConnectionStatusServiceTest {
    // MARK: - Teardown

    @After
    fun tearDown() {
        ConnectionStatusService.removeEffect(ConnectionStatusServiceEffectID.RETRY_MESSAGE_OUTBOX)
        ConnectionStatusService.reachabilityDidChange(isOnline = true)
    }

    // MARK: - Tests

    @Test
    fun `effects run on every offline event and once on restoration`() {
        var runs = 0
        ConnectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.RETRY_MESSAGE_OUTBOX) { runs += 1 }

        ConnectionStatusService.reachabilityDidChange(isOnline = false)
        ConnectionStatusService.reachabilityDidChange(isOnline = false)
        assertEquals(2, runs)
        assertFalse(ConnectionStatusService.isOnline)

        ConnectionStatusService.reachabilityDidChange(isOnline = true)
        ConnectionStatusService.reachabilityDidChange(isOnline = true)
        assertEquals(3, runs)
        assertTrue(ConnectionStatusService.isOnline)
    }

    @Test
    fun `an online event while online runs nothing`() {
        var runs = 0
        ConnectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.RETRY_MESSAGE_OUTBOX) { runs += 1 }

        ConnectionStatusService.reachabilityDidChange(isOnline = true)

        assertEquals(0, runs)
    }
}
