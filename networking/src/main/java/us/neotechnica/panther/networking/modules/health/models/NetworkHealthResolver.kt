//
//  NetworkHealthResolver.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

import us.neotechnica.panther.networking.modules.common.models.CacheStrategy

/**
 * Resolves [CacheStrategy.ADAPTIVE] to a concrete cache strategy
 * based on the current network health.
 *
 * This is the single internal function that maps a health snapshot
 * and configuration to a concrete strategy, ensuring the
 * resolution logic cannot drift between the database and storage
 * funnels.
 */
internal object NetworkHealthResolver {
    // MARK: - Methods

    /**
     * Returns a concrete cache strategy for the current health.
     *
     * @param health The current network health snapshot.
     * @param configuration The active health configuration.
     *
     * @return [CacheStrategy.RETURN_CACHE_FIRST] when the score
     *   falls below the adaptive threshold;
     *   [CacheStrategy.RETURN_CACHE_ON_FAILURE] otherwise
     *   (including when health is [NetworkHealth.Unknown]).
     */
    fun resolve(
        health: NetworkHealth,
        configuration: NetworkHealthConfiguration,
    ): CacheStrategy =
        when (health) {
            is NetworkHealth.Measured ->
                if (health.score < configuration.adaptiveScoreThreshold) {
                    CacheStrategy.RETURN_CACHE_FIRST
                } else {
                    CacheStrategy.RETURN_CACHE_ON_FAILURE
                }

            NetworkHealth.Unknown -> CacheStrategy.RETURN_CACHE_ON_FAILURE
        }
}
