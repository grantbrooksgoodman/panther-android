//
//  TaskTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class TaskTest {
    // MARK: - Teardown

    @After
    fun tearDown() {
        Task.resetScope()
    }

    // MARK: - Tests

    @Test
    fun `debounced runs only the latest call for a key`() =
        runTest {
            Task.setScope(CoroutineScope(StandardTestDispatcher(testScheduler)))
            val counter = AtomicInteger(0)

            Task.debounced("key", 100.milliseconds) { counter.incrementAndGet() }
            Task.debounced("key", 100.milliseconds) { counter.incrementAndGet() }
            advanceTimeBy(150)
            runCurrent()

            assertEquals(1, counter.get())
        }

    @Test
    fun `debounced runs independently for distinct keys`() =
        runTest {
            Task.setScope(CoroutineScope(StandardTestDispatcher(testScheduler)))
            val counter = AtomicInteger(0)

            Task.debounced("a", 100.milliseconds) { counter.incrementAndGet() }
            Task.debounced("b", 100.milliseconds) { counter.incrementAndGet() }
            advanceTimeBy(150)
            runCurrent()

            assertEquals(2, counter.get())
        }

    @Test
    fun `delayed runs after the delay elapses`() =
        runTest {
            Task.setScope(CoroutineScope(StandardTestDispatcher(testScheduler)))
            var ran = false

            Task.delayed(by = 50.milliseconds) { ran = true }
            runCurrent()
            assertFalse(ran)

            advanceTimeBy(60)
            runCurrent()
            assertTrue(ran)
        }
}
