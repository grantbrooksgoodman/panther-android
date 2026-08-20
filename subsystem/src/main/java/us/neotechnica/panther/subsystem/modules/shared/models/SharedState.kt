//
//  SharedState.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.shared.models

import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates

/**
 * A wrapper that reads and writes a shared value from the
 * current dependency scope.
 *
 * Use [SharedState] in reducers, services, and effects to access
 * a [StateStream] declared on [SharedStates] by its accessor:
 *
 * ```kotlin
 * private val isLoggedIn = SharedState { it.isLoggedIn }
 *
 * isLoggedIn.wrappedValue = true                   // Writes the shared value.
 * if (!isLoggedIn.wrappedValue) return             // Reads the shared value.
 * viewModel.observing(isLoggedIn.projectedValue.changes) { … }
 * ```
 *
 * Events have their own wrapper – access an [EventStream]
 * through [SharedEvent] instead.
 *
 * This wrapper is the only way to access a [StateStream] – the
 * [SharedStates] container is not resolvable through
 * [Dependency][us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency].
 *
 * The wrapper resolves its value from [DependencyValues.current]
 * each time you access [wrappedValue] or [projectedValue], so
 * overrides applied by
 * [DependencyScopes.withDependencies][us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyScopes.withDependencies]
 * are visible to any access within that scope.
 *
 * **Important:** Reading a shared value in a composable does not
 * invalidate the composition when the value changes. Reactivity
 * flows exclusively through
 * [ViewModel.observing][us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel.observing]
 * – map each change to a reducer action and drive view updates
 * from reducer state.
 */
class SharedState<Value>(
    private val accessor: (SharedStates) -> StateStream<Value>,
) {
    // MARK: - Computed Properties

    /**
     * The underlying [StateStream] instance.
     *
     * Use the projected value to subscribe to
     * [StateStream.changes] or to perform an atomic
     * read-modify-write with [StateStream.withValue].
     */
    val projectedValue: StateStream<Value>
        get() = accessor(DependencyValues.current.sharedStates)

    /**
     * The current value of the shared state.
     *
     * Reading this property resolves the [StateStream] instance
     * from the [DependencyValues] scope that is active at the
     * point of access and returns its current value. Writing
     * stores the new value and yields it to every active
     * [StateStream.changes] subscriber.
     */
    var wrappedValue: Value
        get() = projectedValue.value
        set(newValue) {
            projectedValue.value = newValue
        }
}
