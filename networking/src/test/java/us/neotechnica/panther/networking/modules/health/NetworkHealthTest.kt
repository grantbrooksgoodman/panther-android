//
//  NetworkHealthTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.health.models.EstimatorContext
import us.neotechnica.panther.networking.modules.health.models.HealthEstimator
import us.neotechnica.panther.networking.modules.health.models.NetworkHealth
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthConfiguration
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthResolver
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthTier
import us.neotechnica.panther.networking.modules.health.models.PathState

/**
 * Exercises the network health estimator, tier classification, and
 * adaptive cache-strategy resolution.
 */
@Suppress("MagicNumber")
class NetworkHealthTest {
    private val configuration = NetworkHealthConfiguration.default

    @Test
    fun adaptiveResolvesToCacheFirstUnderPoorHealth() {
        assertEquals(
            CacheStrategy.RETURN_CACHE_FIRST,
            NetworkHealthResolver.resolve(NetworkHealth.Measured(0.2, NetworkHealthTier.POOR), configuration),
        )
    }

    @Test
    fun adaptiveResolvesToCacheOnFailureUnderHealthyNetwork() {
        assertEquals(
            CacheStrategy.RETURN_CACHE_ON_FAILURE,
            NetworkHealthResolver.resolve(NetworkHealth.Measured(0.8, NetworkHealthTier.GOOD), configuration),
        )
    }

    @Test
    fun adaptiveResolvesToCacheOnFailureWhenHealthIsUnknown() {
        assertEquals(
            CacheStrategy.RETURN_CACHE_ON_FAILURE,
            NetworkHealthResolver.resolve(NetworkHealth.Unknown, configuration),
        )
    }

    @Test
    fun tierBoundariesClassifyScores() {
        assertEquals(NetworkHealthTier.GOOD, configuration.tier(0.8))
        assertEquals(NetworkHealthTier.FAIR, configuration.tier(0.4))
        assertEquals(NetworkHealthTier.POOR, configuration.tier(0.1))
    }

    @Test
    fun highLatencyYieldsAPoorScore() {
        val health = HealthEstimator().recordLatency(3.0, isCensored = false, context = onlineContext())
        assertTrue(health is NetworkHealth.Measured)
        assertEquals(NetworkHealthTier.POOR, (health as NetworkHealth.Measured).tier)
    }

    @Test
    fun lowLatencyYieldsAGoodScore() {
        val health = HealthEstimator().recordLatency(0.05, isCensored = false, context = onlineContext())
        assertTrue(health is NetworkHealth.Measured)
        assertEquals(NetworkHealthTier.GOOD, (health as NetworkHealth.Measured).tier)
    }

    @Test
    fun offlineYieldsAMeasuredPoorScore() {
        val health =
            HealthEstimator().computeHealth(
                EstimatorContext(configuration, isOnline = false, pathState = PathState()),
            )
        assertEquals(NetworkHealth.Measured(0.0, NetworkHealthTier.POOR), health)
    }

    private fun onlineContext() = EstimatorContext(configuration, isOnline = true, pathState = PathState())
}
