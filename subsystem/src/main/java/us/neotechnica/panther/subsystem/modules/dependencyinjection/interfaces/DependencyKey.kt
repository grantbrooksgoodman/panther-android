//
//  DependencyKey.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces

import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * A type that provides a default value for a dependency.
 *
 * Implement [DependencyKey] as an `object` to register a new
 * dependency with the dependency injection system. Each key
 * declares the type of value it provides and a method that
 * produces the default instance:
 *
 * ```kotlin
 * object TimestampDateFormatterDependency :
 *     DependencyKey<TimestampDateFormatter> {
 *     override fun resolve(
 *         dependencies: DependencyValues,
 *     ): TimestampDateFormatter = TimestampDateFormatter()
 * }
 * ```
 *
 * After defining a key, expose it on [DependencyValues] through
 * an extension property so that callers can access it by
 * accessor:
 *
 * ```kotlin
 * val DependencyValues.timestampDateFormatter: TimestampDateFormatter
 *     get() = this[TimestampDateFormatterDependency]
 * ```
 *
 * The [resolve] method receives the current [DependencyValues]
 * container, which allows a dependency to compose other
 * dependencies during resolution.
 */
interface DependencyKey<Value> {
    // MARK: - Methods

    /**
     * Returns the default value for this dependency.
     *
     * The system calls this method the first time the dependency
     * is accessed and caches the result for subsequent lookups
     * within the same scope.
     *
     * @param dependencies The current dependency container,
     *   available for composing other dependencies during
     *   resolution.
     *
     * @return The resolved dependency value.
     */
    fun resolve(dependencies: DependencyValues): Value
}
