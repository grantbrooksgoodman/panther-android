//
//  DependencyValues+SharedExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.shared.extensions

import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents
import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates

/**
 * The shared-event container for the current dependency scope.
 *
 * Access declared events through the
 * [SharedEvent][us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent]
 * wrapper rather than reading this property directly.
 */
val DependencyValues.sharedEvents: SharedEvents
    get() = this[SharedEventsDependency]

/**
 * The shared-state container for the current dependency scope.
 *
 * Access declared state through the
 * [SharedState][us.neotechnica.panther.subsystem.modules.shared.models.SharedState]
 * wrapper rather than reading this property directly.
 */
val DependencyValues.sharedStates: SharedStates
    get() = this[SharedStatesDependency]

/**
 * Replaces the shared-state and shared-event containers with
 * fresh instances for the receiving scope.
 *
 * Use this method in tests to isolate shared values:
 *
 * ```kotlin
 * DependencyScopes.withDependencies({ it.resetSharedValues() }) {
 *     // Shared values resolved here are isolated to this scope.
 * }
 * ```
 */
fun DependencyValues.Builder.resetSharedValues() {
    this[SharedEventsDependency] = SharedEvents()
    this[SharedStatesDependency] = SharedStates()
}

private object SharedEventsDependency : DependencyKey<SharedEvents> {
    override fun resolve(dependencies: DependencyValues): SharedEvents = SharedEvents()
}

private object SharedStatesDependency : DependencyKey<SharedStates> {
    override fun resolve(dependencies: DependencyValues): SharedStates = SharedStates()
}
