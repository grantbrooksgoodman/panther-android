//
//  ErrorReportingServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.util.Date

class ErrorReportingServiceTest {
    // MARK: - Setup

    @Before
    fun setUp() {
        Persistent.initializeForTesting()
    }

    // MARK: - Parent Directory Name

    @Test
    fun `a hydrated exception without a static code gains the shorthand descriptor suffix`() {
        val exception = exception(mapOf("Descriptor" to "The operation timed out."))

        assertEquals("AB12_OPERATION_TIMED_OUT", ErrorReportingService.parentDirectoryName(exception, "AB12"))
    }

    @Test
    fun `an exception without a descriptor entry keeps the bare error code`() {
        assertEquals("AB12", ErrorReportingService.parentDirectoryName(exception(null), "AB12"))
    }

    @Test
    fun `a static error code suppresses the shorthand suffix`() {
        val exception =
            exception(
                mapOf(
                    "Descriptor" to "Failed to typecast values to URL.",
                    "StaticErrorCode" to "3530",
                ),
            )

        assertEquals("AB12", ErrorReportingService.parentDirectoryName(exception, "AB12"))
    }

    @Test
    fun `a hosted override code replaces the error code`() {
        val exception =
            exception(
                mapOf(
                    "Descriptor" to "Is the value missing?",
                    "HostedOverrideErrorCode" to "FF00",
                ),
            )

        assertEquals("FF00_VALUE_MISSING", ErrorReportingService.parentDirectoryName(exception, "AB12"))
    }

    // MARK: - Timestamp

    @Test
    fun `the timestamp metadata uses the shared timestamp format`() {
        val userInfo = ErrorReportingService.standardUserInfo(Date(0))

        assertEquals("1970-01-01 00:00:00 GMT", userInfo["Timestamp"])
    }

    @Test
    fun `the file name suffix ends with the short hash of the formatted timestamp`() {
        val date = Date(0)
        val shortDateHash =
            encodedHashOf(listOf(DependencyValues.current.timestampDateFormatter.format(date)))
                .take(SHORT_DATE_HASH_LENGTH)

        assertTrue(ErrorReportingService.fileNameSuffix(date).endsWith("_$shortDateHash"))
    }

    // MARK: - Auxiliary

    private fun exception(userInfo: Map<String, Any>?): Exception =
        Exception(
            "Underlying descriptor.",
            userInfo = userInfo,
            metadata = ExceptionMetadata(this),
        )

    // MARK: - Companion

    private companion object {
        const val SHORT_DATE_HASH_LENGTH = 5
    }
}
