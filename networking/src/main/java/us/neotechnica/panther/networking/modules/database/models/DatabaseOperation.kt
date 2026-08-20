//
//  DatabaseOperation.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.models

import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable

/**
 * A coalescable database operation.
 *
 * Reads and writes are keyed by their [hashFactors] so that
 * identical concurrent operations share a single network request.
 */
internal sealed interface DatabaseOperation : EncodedHashable {
    // MARK: - Cases

    data class GetValues(
        val path: String,
        val cacheStrategy: CacheStrategy,
    ) : DatabaseOperation {
        override val hashFactors: List<String>
            get() = listOf(path, cacheStrategy.rawValue)
    }

    data class QueryValues(
        val path: String,
        val strategy: QueryStrategy,
        val cacheStrategy: CacheStrategy,
    ) : DatabaseOperation {
        override val hashFactors: List<String>
            get() = listOf(path, strategy.rawValue, cacheStrategy.rawValue)
    }

    data class SetValue(
        val value: Any?,
        val key: String,
    ) : DatabaseOperation {
        override val hashFactors: List<String>
            get() = listOf(jsonIdentifier(value), key)
    }

    data class UpdateChildValues(
        val key: String,
        val data: Map<String, Any?>,
    ) : DatabaseOperation {
        override val hashFactors: List<String>
            get() = listOf(key, jsonIdentifier(data))
    }

    // MARK: - Methods

    /**
     * Returns a copy with any [CacheStrategy.ADAPTIVE] cache
     * strategy resolved to a concrete value.
     */
    fun resolvingAdaptiveCacheStrategy(): DatabaseOperation =
        when (this) {
            is GetValues ->
                if (cacheStrategy == CacheStrategy.ADAPTIVE) copy(cacheStrategy = cacheStrategy.resolved) else this

            is QueryValues ->
                if (cacheStrategy == CacheStrategy.ADAPTIVE) copy(cacheStrategy = cacheStrategy.resolved) else this

            is SetValue, is UpdateChildValues -> this
        }
}

/**
 * Returns a stable, order-independent string identifier for a
 * value tree, used only to key coalesced operations.
 */
internal fun jsonIdentifier(value: Any?): String =
    when (value) {
        null -> "null"
        is Map<*, *> ->
            value.entries
                .map { "${it.key}=${jsonIdentifier(it.value)}" }
                .sorted()
                .joinToString(prefix = "{", postfix = "}", separator = ",")

        is List<*> ->
            value.joinToString(prefix = "[", postfix = "]", separator = ",") {
                jsonIdentifier(it)
            }

        else -> value.toString()
    }
