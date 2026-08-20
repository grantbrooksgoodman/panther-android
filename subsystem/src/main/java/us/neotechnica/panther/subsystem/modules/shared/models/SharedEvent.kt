//
//  SharedEvent.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.shared.models

import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * A wrapper that reads a shared event stream from the current
 * dependency scope.
 *
 * Use [SharedEvent] in reducers, services, and effects to access
 * an [EventStream] declared on [SharedEvents] by its accessor:
 *
 * ```kotlin
 * private val sessionDidExpire = SharedEvent { it.sessionDidExpire }
 *
 * sessionDidExpire.wrappedValue.send()
 * viewModel.observing(sessionDidExpire.wrappedValue.events) { … }
 * ```
 *
 * State has its own wrapper – access a [StateStream] through
 * [SharedState] instead.
 *
 * The wrapper resolves its value from [DependencyValues.current]
 * each time you access [wrappedValue], so overrides applied by
 * [DependencyScopes.withDependencies][us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyScopes.withDependencies]
 * are visible to any access within that scope.
 */
class SharedEvent<Payload>(
    private val accessor: (SharedEvents) -> EventStream<Payload>,
) {
    // MARK: - Computed Properties

    /**
     * The underlying [EventStream] instance.
     *
     * Reading this property resolves the instance from the
     * [DependencyValues] scope that is active at the point of
     * access.
     */
    val wrappedValue: EventStream<Payload>
        get() = accessor(DependencyValues.current.sharedEvents)
}
