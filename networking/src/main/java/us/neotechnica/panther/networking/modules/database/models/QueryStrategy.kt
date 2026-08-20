//
//  QueryStrategy.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.models

/**
 * A value that determines which subset of results to return from
 * a database query.
 *
 * Pass a query strategy to
 * [DatabaseDelegate.queryValues][us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate.queryValues]
 * to limit the number of results returned:
 *
 * ```kotlin
 * val result: Map<String, Any?> = database.queryValues(
 *     path = "messages",
 *     strategy = QueryStrategy.Last(25),
 * )
 * ```
 */
sealed interface QueryStrategy {
    // MARK: - Properties

    /** The raw string identifier of the strategy. */
    val rawValue: String

    // MARK: - Cases

    /** Returns the first *n* results. */
    data class First(
        val limit: Int,
    ) : QueryStrategy {
        override val rawValue: String get() = "first_$limit"
    }

    /** Returns the last *n* results. */
    data class Last(
        val limit: Int,
    ) : QueryStrategy {
        override val rawValue: String get() = "last_$limit"
    }
}
