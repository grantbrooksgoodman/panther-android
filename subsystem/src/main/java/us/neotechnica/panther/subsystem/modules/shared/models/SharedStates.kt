//
//  SharedStates.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.shared.models

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * The container that holds every [StateStream] instance for the
 * current dependency scope.
 *
 * [SharedStates] is the registry through which shared state is
 * declared and resolved. Declare each value as an extension
 * property on [SharedStates], using [state]:
 *
 * ```kotlin
 * val SharedStates.isLoggedIn: StateStream<Boolean>
 *     get() = state("isLoggedIn") { false }
 * ```
 *
 * Access declared state exclusively through the
 * [SharedState][us.neotechnica.panther.subsystem.modules.shared.models.SharedState]
 * wrapper – [SharedStates] is intentionally not resolvable
 * through [Dependency][us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency].
 *
 * Each dependency scope resolves its own container, so tests can
 * call `resetSharedValues()` on the builder to isolate shared
 * state.
 *
 * ## Thread Safety
 *
 * All stored instances are protected by [LockIsolated]. Values
 * can be resolved from any thread.
 */
class SharedStates internal constructor() {
    // MARK: - Properties

    private val storage = LockIsolated(mapOf<String, Any>())

    // MARK: - Methods

    /**
     * Resolves the [StateStream] instance for the calling
     * declaration, creating it on first access.
     *
     * Call this method only from the body of the extension
     * property that declares the value – the key forms the
     * instance's identity and MUST match the property name:
     *
     * ```kotlin
     * val SharedStates.isLoggedIn: StateStream<Boolean>
     *     get() = state("isLoggedIn") { false }
     * ```
     *
     * The initial value is evaluated only when the instance is
     * first created; subsequent resolutions return the existing
     * instance.
     *
     * **Warning:** Keys must be unique across every declaring
     * file. Reusing a key silently resolves another
     * declaration's instance.
     *
     * @param key The identity of the declaration – the name of
     *   the declaring property.
     * @param initialValue The value the state holds before the
     *   first write.
     *
     * @return The [StateStream] instance for the calling
     *   declaration.
     */
    fun <Value> state(
        key: String,
        initialValue: () -> Value,
    ): StateStream<Value> =
        storage.withValue {
            val existing = it.value[key]
            if (existing != null) {
                @Suppress("UNCHECKED_CAST")
                existing as StateStream<Value>
            } else {
                val instance = StateStream(initialValue())
                it.value = it.value + (key to instance)
                instance
            }
        }
}
