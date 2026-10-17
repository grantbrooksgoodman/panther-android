//
//  FailurePageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.failurepageview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.effect.Send
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

@OptIn(ExperimentalCoroutinesApi::class)
class FailurePageReducerTest {
    // MARK: - Properties

    private val exception = Exception("Something failed.", metadata = ExceptionMetadata(this))

    // MARK: - Tests

    @Test
    fun `tapping report bug files the exception once and records the report`() =
        runTest {
            val filed = mutableListOf<Exception>()
            val reducer = FailurePageReducer(FailurePageReducer.Dependencies(fileReport = { filed.add(it) }, isOnline = { true }))

            val result = reducer.reduce(FailurePageReducer.State(exception), FailurePageReducer.Action.ReportBugButtonTapped)
            result.effect.operation(Send {})

            assertTrue(result.state.didReportBug)
            assertEquals(listOf(exception), filed)
        }

    @Test
    fun `tapping report bug offline presents the connection alert and files nothing`() =
        runTest {
            val filed = mutableListOf<Exception>()
            var presentedConnectionAlert = false
            val reducer =
                FailurePageReducer(
                    FailurePageReducer.Dependencies(
                        fileReport = { filed.add(it) },
                        isOnline = { false },
                        presentConnectionAlert = { presentedConnectionAlert = true },
                    ),
                )

            val result = reducer.reduce(FailurePageReducer.State(exception), FailurePageReducer.Action.ReportBugButtonTapped)
            result.effect.operation(Send {})

            assertFalse(result.state.didReportBug)
            assertTrue(presentedConnectionAlert)
            assertTrue(filed.isEmpty())
        }

    @Test
    fun `the retry handler runs on request`() {
        var retried = 0
        val reducer = FailurePageReducer()

        val state = FailurePageReducer.State(exception, retryHandler = { retried += 1 })
        reducer.reduce(state, FailurePageReducer.Action.ExecuteRetryHandler)
        reducer.reduce(state.copy(retryHandler = null), FailurePageReducer.Action.ExecuteRetryHandler)

        assertEquals(1, retried)
    }
}
