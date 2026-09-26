//
//  SingleSlotCoalescerTest.kt
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
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SingleSlotCoalescerTest {
    // MARK: - Tests

    @Test
    fun `coalesce shares a single in-flight operation`() =
        runBlocking {
            val coalescer = SingleSlotCoalescer<Int>()
            val calls = AtomicInteger(0)
            val gate = CompletableDeferred<Unit>()
            val operation: suspend () -> Int = {
                calls.incrementAndGet()
                gate.await()
                42
            }

            val first = async(Dispatchers.Default) { coalescer(SingleSlotCoalescer.Mode.COALESCE, operation) }
            delay(50)
            val second = async(Dispatchers.Default) { coalescer(SingleSlotCoalescer.Mode.COALESCE, operation) }
            delay(50)
            gate.complete(Unit)

            assertEquals(42, first.await())
            assertEquals(42, second.await())
            assertEquals(1, calls.get())
        }

    @Test
    fun `last caller wins cancels the previous operation`() =
        runBlocking {
            val coalescer = SingleSlotCoalescer<Int>()
            val firstStarted = CompletableDeferred<Unit>()
            val firstCancelled = AtomicBoolean(false)

            val first =
                async(Dispatchers.Default) {
                    runCatching {
                        coalescer(SingleSlotCoalescer.Mode.LAST_CALLER_WINS) {
                            firstStarted.complete(Unit)
                            delay(10_000)
                            1
                        }
                    }.onFailure { firstCancelled.set(true) }
                }
            firstStarted.await()

            val second = async(Dispatchers.Default) { coalescer(SingleSlotCoalescer.Mode.LAST_CALLER_WINS) { 2 } }

            assertEquals(2, second.await())
            first.await()
            assertTrue(firstCancelled.get())
        }
}
