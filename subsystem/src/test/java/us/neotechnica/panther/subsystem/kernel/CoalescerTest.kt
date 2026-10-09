//
//  CoalescerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CoalescerTest {
    // MARK: - Tests

    @Test
    fun `coalesce policy shares a single in-flight operation`() =
        runBlocking {
            val coalescer = SingleSlotCoalescer<Int>(Coalescer.Policy.COALESCE)
            val calls = AtomicInteger(0)
            val gate = CompletableDeferred<Unit>()
            val operation: suspend () -> Int = {
                calls.incrementAndGet()
                gate.await()
                42
            }

            val first = async(Dispatchers.Default) { coalescer(operation) }
            delay(50)
            val second = async(Dispatchers.Default) { coalescer(operation) }
            delay(50)
            gate.complete(Unit)

            assertEquals(42, first.await())
            assertEquals(42, second.await())
            assertEquals(1, calls.get())
        }

    @Test
    fun `replace policy cancels the previous operation`() =
        runBlocking {
            val coalescer = SingleSlotCoalescer<Int>(Coalescer.Policy.REPLACE)
            val firstStarted = CompletableDeferred<Unit>()
            val firstCancelled = AtomicBoolean(false)

            val first =
                async(Dispatchers.Default) {
                    runCatching {
                        coalescer {
                            firstStarted.complete(Unit)
                            delay(10_000)
                            1
                        }
                    }.onFailure { firstCancelled.set(true) }
                }
            firstStarted.await()

            val second = async(Dispatchers.Default) { coalescer { 2 } }

            assertEquals(2, second.await())
            first.await()
            assertTrue(firstCancelled.get())
        }

    @Test
    fun `rerun policy runs the operation once more for mid-flight callers`() =
        runBlocking {
            val coalescer = SingleSlotCoalescer<Int>(Coalescer.Policy.RERUN)
            val calls = AtomicInteger(0)
            val firstStarted = CompletableDeferred<Unit>()
            val gate = CompletableDeferred<Unit>()

            val first =
                async(Dispatchers.Default) {
                    coalescer {
                        val attempt = calls.incrementAndGet()
                        if (attempt == 1) {
                            firstStarted.complete(Unit)
                            gate.await()
                        }
                        attempt
                    }
                }

            firstStarted.await()
            val second = async(Dispatchers.Default) { coalescer { calls.incrementAndGet() } }
            delay(50)
            gate.complete(Unit)

            assertEquals(1, first.await())
            assertEquals(2, second.await())
            assertEquals(2, calls.get())
        }
}
