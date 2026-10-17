//
//  CoalescerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
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
            val firstCancelled = CompletableDeferred<Unit>()

            val first =
                async(Dispatchers.Default) {
                    coalescer {
                        firstStarted.complete(Unit)
                        try {
                            delay(10_000)
                        } catch (cancellation: CancellationException) {
                            firstCancelled.complete(Unit)
                            throw cancellation
                        }
                        1
                    }
                }
            firstStarted.await()

            val second = async(Dispatchers.Default) { coalescer { 2 } }

            // The displaced operation is cancelled, and its caller
            // receives the replacement's result.
            assertEquals(2, second.await())
            assertEquals(2, first.await())
            withTimeout(5_000) { firstCancelled.await() }
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
