//
//  CacheStrategy.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

/**
 * A value that specifies how cached data is used during a
 * network operation.
 *
 * When performing database or storage operations, pass a cache
 * strategy to control whether cached results are preferred, used
 * as a fallback, or bypassed entirely.
 */
enum class CacheStrategy(
    /** The raw string identifier of the strategy. */
    val rawValue: String,
) {
    // MARK: - Cases

    /**
     * Resolves to a concrete strategy at operation time based on
     * the current network health.
     *
     * **Note:** The network Health module is deferred, so
     * `adaptive` currently resolves to [returnCacheOnFailure]
     * unconditionally. When Health lands, this resolves to
     * [returnCacheFirst] under poor health and
     * [returnCacheOnFailure] otherwise.
     */
    ADAPTIVE("adaptive"),

    /** Ignores any cached data and always fetches from the network. */
    DISREGARD_CACHE("disregardCache"),

    /**
     * Returns cached data immediately when available, without
     * making a network request.
     */
    RETURN_CACHE_FIRST("returnCacheFirst"),

    /**
     * Fetches from the network first, and falls back to cached
     * data only if the request fails.
     */
    RETURN_CACHE_ON_FAILURE("returnCacheOnFailure"),
    ;

    // MARK: - Computed Properties

    /**
     * The concrete strategy this value resolves to at operation
     * time. Every strategy other than [ADAPTIVE] resolves to
     * itself.
     */
    val resolved: CacheStrategy
        get() = if (this == ADAPTIVE) RETURN_CACHE_ON_FAILURE else this
}
