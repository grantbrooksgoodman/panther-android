//
//  LoggerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.DefaultLoggerDomainSubscriptionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ErrorReportDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

class LoggerTest {
    // MARK: - Tests

    @Test
    fun `an automatically reported exception is filed once and hydrated`() {
        val filed = mutableListOf<Exception>()
        val delegate =
            object : ErrorReportDelegate {
                override fun fileReport(exception: Exception) {
                    filed.add(exception)
                }
            }
        AppSubsystem.delegates.registerErrorReportDelegate(delegate)
        Logger.setReportsErrorsAutomatically(true)

        try {
            Logger.log(Exception("Something reportable failed.", metadata = ExceptionMetadata(this)))
        } finally {
            Logger.setReportsErrorsAutomatically(false)
        }

        assertEquals(1, filed.size)
        assertEquals("Something reportable failed.", filed.single().userInfo?.get("Descriptor"))
        assertEquals(filed.single().code, filed.single().userInfo?.get("ErrorCode"))
    }

    @Test
    fun `a non-reportable exception is never filed automatically`() {
        val filed = mutableListOf<Exception>()
        val delegate =
            object : ErrorReportDelegate {
                override fun fileReport(exception: Exception) {
                    filed.add(exception)
                }
            }
        AppSubsystem.delegates.registerErrorReportDelegate(delegate)
        Logger.setReportsErrorsAutomatically(true)

        try {
            Logger.log(Exception.cancelled(ExceptionMetadata(this)))
        } finally {
            Logger.setReportsErrorsAutomatically(false)
        }

        assertTrue(filed.isEmpty())
    }

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
