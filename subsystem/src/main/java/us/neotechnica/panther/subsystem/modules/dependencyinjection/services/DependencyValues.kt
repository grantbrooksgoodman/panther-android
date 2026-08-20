//
//  DependencyValues.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.dependencyinjection.services

import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The container that holds every registered dependency in the
 * current scope.
 *
 * [DependencyValues] is the central registry for the dependency
 * injection system. Each dependency is accessed through an
 * indexed accessor keyed by its [DependencyKey] object:
 *
 * ```kotlin
 * val formatter = DependencyValues.current[TimestampDateFormatterDependency]
 * ```
 *
 * In practice, you rarely use the accessor directly. Instead,
 * define an extension property on [DependencyValues] and access
 * it through the [Dependency][us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency]
 * delegate.
 *
 * ## Scoping
 *
 * The [current] value is stored per thread and propagated into
 * coroutines by [DependencyScopes] and by effects, which capture
 * the active scope at creation. Use
 * [DependencyScopes.withDependencies] to override individual
 * dependencies for the duration of a closure.
 *
 * ## Resolution and Caching
 *
 * When a dependency is accessed for the first time in a given
 * scope, the system calls the key's [DependencyKey.resolve]
 * method and caches the result. Subsequent accesses return the
 * cached value without calling `resolve` again.
 */
class DependencyValues internal constructor(
    private val storage: Map<DependencyKey<*>, Any?>,
) {
    // MARK: - Types

    /**
     * A mutable view of a [DependencyValues] container, used to
     * apply scope-local overrides inside
     * [DependencyScopes.withDependencies].
     */
    class Builder internal constructor(
        current: DependencyValues,
    ) {
        // MARK: - Properties

        internal val storage = current.storage.toMutableMap()

        // MARK: - Methods

        /**
         * Reads the dependency value associated with the given
         * key, resolving through the values being built.
         */
        operator fun <Value> get(key: DependencyKey<Value>): Value = build()[key]

        /**
         * Stores a scope-local override for the given key.
         */
        operator fun <Value> set(
            key: DependencyKey<Value>,
            value: Value,
        ) {
            storage[key] = value
        }

        internal fun build(): DependencyValues = DependencyValues(storage.toMap())
    }

    // MARK: - Companion

    companion object {
        /**
         * The dependency values for the current scope.
         *
         * This value is propagated through [DependencyScopes]
         * and captured by effects at creation. Override it for a
         * specific scope using
         * [DependencyScopes.withDependencies].
         */
        val current: DependencyValues
            get() = threadLocalValues.get() ?: root

        internal val root = DependencyValues(emptyMap())

        internal val threadLocalValues = ThreadLocal<DependencyValues?>()
    }

    // MARK: - Subscript

    /**
     * Accesses the dependency value associated with the given
     * key.
     *
     * The accessor first checks for an explicit override in the
     * current scope. If none is found, it falls back to the
     * cached result of the key's [DependencyKey.resolve] method.
     */
    operator fun <Value> get(key: DependencyKey<Value>): Value {
        if (storage.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return storage[key] as Value
        }

        return ResolverCache.value(
            key,
            dependencies = this,
        )
    }
}

private object ResolverCache {
    // MARK: - Properties

    // A reentrant lock rather than LockIsolated: resolution may
    // recursively resolve other dependencies on the same thread.
    private val cache = mutableMapOf<DependencyKey<*>, Any?>()
    private val lock = ReentrantLock()

    // MARK: - Methods

    fun <Value> value(
        key: DependencyKey<Value>,
        dependencies: DependencyValues,
    ): Value =
        lock.withLock {
            if (cache.containsKey(key)) {
                @Suppress("UNCHECKED_CAST")
                return cache[key] as Value
            }

            val value = key.resolve(dependencies)
            cache[key] = value

            value
        }
}
