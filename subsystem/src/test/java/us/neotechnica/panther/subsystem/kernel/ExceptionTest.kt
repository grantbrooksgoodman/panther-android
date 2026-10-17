//
//  ExceptionTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.extensions.compiledException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

class ExceptionTest {
    // MARK: - Tests

    @Test
    fun `id is lowercased`() {
        val exception = Exception("Something failed.", metadata = ExceptionMetadata(this))
        assertEquals(exception.id, exception.id.lowercase())
    }

    @Test
    fun `from derives a static code from the throwable type`() {
        val first = Exception.from(IllegalStateException("first message"), ExceptionMetadata(this))
        val second = Exception.from(IllegalStateException("second message"), ExceptionMetadata(this))
        val other = Exception.from(IllegalArgumentException("first message"), ExceptionMetadata(this))

        assertTrue(first.code, first.code.matches(Regex("[0-9A-F]{4}")))
        assertEquals(first.code, second.code)
        assertNotEquals(first.code, other.code)
        assertEquals(first.code, first.userInfo?.get("StaticErrorCode"))
        assertEquals("java.lang.IllegalStateException", first.userInfo?.get("NSErrorDomain"))
        assertEquals("first message", first.descriptor)
    }

    @Test
    fun `from carries a supplied error code in the identifier and user info`() {
        val withCode =
            Exception.from(
                IllegalStateException("object not found"),
                userInfo = mapOf("NSErrorCode" to -13010),
                metadata = ExceptionMetadata(this),
            )
        val withoutCode = Exception.from(IllegalStateException("object not found"), ExceptionMetadata(this))

        assertTrue(withCode.code, withCode.code.matches(Regex("[0-9A-F]{4}")))
        assertEquals(-13010, withCode.userInfo?.get("NSErrorCode"))
        assertEquals("java.lang.IllegalStateException", withCode.userInfo?.get("NSErrorDomain"))
        // The identifier's digits do not contribute to the hash, so a
        // numeric code shares its type's code.
        assertEquals(withoutCode.code, withCode.code)
    }

    @Test
    fun `the labelled from honours reportability and underlying exceptions`() {
        val underlying = Exception("Underlying.", metadata = ExceptionMetadata(this))
        val exception =
            Exception.from(
                IllegalStateException("outer"),
                isReportable = false,
                underlyingExceptions = listOf(underlying),
                metadata = ExceptionMetadata(this),
            )

        assertFalse(exception.isReportable)
        assertEquals(listOf(underlying), exception.underlyingExceptions)
    }

    @Test
    fun `timedOut is non-reportable with the expected descriptor`() {
        val exception = Exception.timedOut(ExceptionMetadata(this))
        assertFalse(exception.isReportable)
        assertEquals("The operation timed out. Please try again later.", exception.descriptor)
    }

    @Test
    fun `cancelled and internetConnectionOffline are non-reportable`() {
        assertFalse(Exception.cancelled(ExceptionMetadata(this)).isReportable)
        assertFalse(Exception.internetConnectionOffline(ExceptionMetadata(this)).isReportable)
    }

    @Test
    fun `compiledException chains every exception onto the last`() {
        val metadata = ExceptionMetadata(this)
        val first = Exception("First.", metadata = metadata)
        val second = Exception("Second.", metadata = metadata)
        val third = Exception("Third.", metadata = metadata)

        val compiled = listOf(first, second, third).compiledException
        assertEquals("Third.", compiled?.descriptor)
        assertTrue(compiled?.underlyingExceptions?.any { it.descriptor == "First." } == true)
        assertTrue(compiled?.underlyingExceptions?.any { it.descriptor == "Second." } == true)
    }

    @Test
    fun `compiledException of an empty list is null`() {
        assertEquals(null, emptyList<Exception>().compiledException)
    }
}
