//
//  LockIsolatedTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import kotlin.concurrent.thread

class LockIsolatedTest {
    // MARK: - Tests

    @Test
    fun `wrapped value reads and writes`() {
        val isolated = LockIsolated(1)
        assertEquals(1, isolated.wrappedValue)

        isolated.wrappedValue = 2
        assertEquals(2, isolated.wrappedValue)
    }

    @Test
    fun `with value mutates atomically under contention`() {
        val isolated = LockIsolated(0)
        val threads =
            (1..8).map {
                thread {
                    repeat(1_000) {
                        isolated.withValue { it.value += 1 }
                    }
                }
            }

        threads.forEach { it.join() }
        assertEquals(8_000, isolated.wrappedValue)
    }

    @Test
    fun `with value supports reentrant reads`() {
        val isolated = LockIsolated(5)
        val observed = isolated.withValue { isolated.wrappedValue }
        assertEquals(5, observed)
    }

    @Test
    fun `with value returns the operation result`() {
        val isolated = LockIsolated(listOf("a"))
        val result =
            isolated.withValue {
                it.value = it.value + "b"
                it.value.size
            }

        assertEquals(2, result)
        assertEquals(listOf("a", "b"), isolated.wrappedValue)
    }
}
