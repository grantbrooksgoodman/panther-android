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

        assertEquals("[java.lang.IllegalStateException]", first.code)
        assertEquals(first.code, second.code)
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
