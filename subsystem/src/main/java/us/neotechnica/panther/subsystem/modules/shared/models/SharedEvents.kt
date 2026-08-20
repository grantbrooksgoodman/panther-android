//
//  SharedEvents.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.shared.models

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * The container that holds every [EventStream] instance for the
 * current dependency scope.
 *
 * [SharedEvents] is the registry through which shared events are
 * declared and resolved. Declare each event as an extension
 * property on [SharedEvents], using [event]:
 *
 * ```kotlin
 * val SharedEvents.sessionDidExpire: EventStream<Unit>
 *     get() = event("sessionDidExpire")
 * ```
 *
 * Access declared events exclusively through the
 * [SharedEvent][us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent]
 * wrapper – [SharedEvents] is intentionally not resolvable
 * through [Dependency][us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency].
 *
 * ## Thread Safety
 *
 * All stored instances are protected by [LockIsolated]. Values
 * can be resolved from any thread.
 */
class SharedEvents internal constructor() {
    // MARK: - Properties

    private val storage = LockIsolated(mapOf<String, Any>())

    // MARK: - Methods

    /**
     * Resolves the [EventStream] instance for the calling
     * declaration, creating it on first access.
     *
     * Call this method only from the body of the extension
     * property that declares the event – the key forms the
     * instance's identity and MUST match the property name:
     *
     * ```kotlin
     * val SharedEvents.storeDidChange: EventStream<StoreChange>
     *     get() = event("storeDidChange")
     * ```
     *
     * **Warning:** Keys must be unique across every declaring
     * file. Reusing a key silently resolves another
     * declaration's instance.
     *
     * @param key The identity of the declaration – the name of
     *   the declaring property.
     *
     * @return The [EventStream] instance for the calling
     *   declaration.
     */
    fun <Payload> event(key: String): EventStream<Payload> =
        storage.withValue {
            val existing = it.value[key]
            if (existing != null) {
                @Suppress("UNCHECKED_CAST")
                existing as EventStream<Payload>
            } else {
                val instance = EventStream<Payload>()
                it.value = it.value + (key to instance)
                instance
            }
        }
}
