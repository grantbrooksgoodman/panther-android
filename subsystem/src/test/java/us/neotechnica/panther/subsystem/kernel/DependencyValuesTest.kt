//
//  DependencyValuesTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.kernel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyScopes
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

private object CounterDependency : DependencyKey<Counter> {
    override fun resolve(dependencies: DependencyValues): Counter = Counter("default")
}

private data class Counter(
    val label: String,
)

class DependencyValuesTest {
    // MARK: - Tests

    @Test
    fun `resolution is cached within the root scope`() {
        val first = DependencyValues.current[CounterDependency]
        val second = DependencyValues.current[CounterDependency]
        assertSame(first, second)
    }

    @Test
    fun `overrides are visible inside the scope and restored after`() {
        val fallback = DependencyValues.current[CounterDependency]

        val observed =
            DependencyScopes.withDependencies({
                it[CounterDependency] = Counter("override")
            }) {
                DependencyValues.current[CounterDependency]
            }

        assertEquals(Counter("override"), observed)
        assertEquals(fallback, DependencyValues.current[CounterDependency])
    }

    @Test
    fun `async overrides propagate across suspension and dispatch`() =
        runTest {
            val observed =
                DependencyScopes.withDependenciesAsync({
                    it[CounterDependency] = Counter("async")
                }) {
                    withContext(Dispatchers.Default) {
                        DependencyValues.current[CounterDependency]
                    }
                }

            assertEquals(Counter("async"), observed)
        }

    @Test
    fun `captured dependencies restore the scope in escaping coroutines`() =
        runTest {
            var observed: Counter? = null

            val captured =
                DependencyScopes.withDependencies({
                    it[CounterDependency] = Counter("captured")
                }) {
                    DependencyScopes.withEscapedDependencies { it }
                }

            launch {
                captured.withValue {
                    observed = DependencyValues.current[CounterDependency]
                }
            }.join()

            assertEquals(Counter("captured"), observed)
        }
}
