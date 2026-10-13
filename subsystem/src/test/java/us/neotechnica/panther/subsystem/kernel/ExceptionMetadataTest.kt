//
//  ExceptionMetadataTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain

class ExceptionMetadataTest {
    // MARK: - Tests

    @Test
    fun `metadata is attributed to the call site`() {
        val expectedLine = Throwable().stackTrace.first().lineNumber + 1
        val metadata = ExceptionMetadata(this)

        assertEquals("ExceptionMetadataTest", metadata.fileName)
        assertEquals("metadata is attributed to the call site", metadata.function)
        assertEquals(expectedLine, metadata.line)
        assertEquals("ExceptionMetadataTest", metadata.sender)
    }

    @Test
    fun `metadata without a sender derives it from the call site`() {
        assertEquals("ExceptionMetadataTest", ExceptionMetadata().sender)
    }

    @Test
    fun `metadata created in a lambda is attributed to its enclosing function`() {
        val metadata = metadataFromLambda()

        assertEquals("metadataFromLambda", metadata.function)
        assertEquals("ExceptionMetadataTest", metadata.sender)
    }

    @Test
    fun `metadata created in a coroutine is attributed to its enclosing function`() =
        runTest {
            val metadata = ExceptionMetadata()

            assertEquals("metadata created in a coroutine is attributed to its enclosing function", metadata.function)
            assertEquals("ExceptionMetadataTest", metadata.sender)
        }

    @Test
    fun `companion object senders resolve to their enclosing type`() {
        assertEquals("LoggerDomain", ExceptionMetadata(LoggerDomain).sender)
    }

    // MARK: - Auxiliary

    private fun metadataFromLambda(): ExceptionMetadata {
        val makeMetadata = { ExceptionMetadata() }
        return makeMetadata()
    }
}
