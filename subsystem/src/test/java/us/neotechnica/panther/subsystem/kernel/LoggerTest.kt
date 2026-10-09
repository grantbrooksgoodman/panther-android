//
//  LoggerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.DefaultLoggerDomainSubscriptionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

class LoggerTest {
    // MARK: - Tests

    @Test
    fun `default subscription contains the built-in subsystem domains`() {
        val domains = DefaultLoggerDomainSubscriptionDelegate.subscribedDomains
        assertTrue(domains.contains(LoggerDomain.alertKit))
        assertTrue(domains.contains(LoggerDomain.caches))
        assertTrue(domains.contains(LoggerDomain.concurrency))
        assertTrue(domains.contains(LoggerDomain.general))
        assertTrue(domains.contains(LoggerDomain.localization))
        assertTrue(domains.contains(LoggerDomain.translation))
    }

    @Test
    fun `subscribe and unsubscribe mutate the subscribed domains`() {
        val domain = LoggerDomain("loggerTestDomain")
        assertFalse(Logger.subscribedDomains.contains(domain))

        Logger.subscribe(domain)
        assertTrue(Logger.subscribedDomains.contains(domain))

        Logger.unsubscribe(domain)
        assertFalse(Logger.subscribedDomains.contains(domain))
    }
}
