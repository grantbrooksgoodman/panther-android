//
//  Dependency.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.dependencyinjection.models

import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyScopes
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A property delegate that reads a dependency from the current
 * scope.
 *
 * Use [Dependency] in reducers, services, or other non-view code
 * to access a registered dependency by its accessor on
 * [DependencyValues]:
 *
 * ```kotlin
 * private val formatter: TimestampDateFormatter
 *     by Dependency { it.timestampDateFormatter }
 * ```
 *
 * The delegate resolves its value from [DependencyValues.current]
 * each time the property is read. Overrides applied by
 * [DependencyScopes.withDependencies] are visible to any access
 * within that scope.
 *
 * **Important:** The delegate resolves from the scope that is
 * active at the point of access, not the scope that was active
 * when the delegate was initialized. Accessing the dependency
 * after an override's closure returns resolves from the
 * enclosing scope.
 */
class Dependency<Value>(
    private val accessor: (DependencyValues) -> Value,
) : ReadOnlyProperty<Any?, Value> {
    // MARK: - Computed Properties

    /**
     * The current value of the dependency.
     *
     * Reading this property resolves the value from the
     * [DependencyValues] scope that is active at the point of
     * access.
     */
    val wrappedValue: Value
        get() = accessor(DependencyValues.current)

    // MARK: - ReadOnlyProperty Conformance

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Value = wrappedValue
}
