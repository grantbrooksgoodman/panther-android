//
//  NetworkHealthTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
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
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthEvent
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthResolver
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthTier
import us.neotechnica.panther.networking.modules.health.models.NetworkInterfaceType
import us.neotechnica.panther.networking.modules.health.models.PathState
import us.neotechnica.panther.networking.modules.health.models.RadioTechnology

/**
 * Exercises the network health estimator, tier classification, and
 * adaptive cache-strategy resolution.
 */
@Suppress("MagicNumber")
class NetworkHealthTest {
    private val configuration = NetworkHealthConfiguration.default

    @Test
    fun legacyRadioTechnologyCapsTheCellularScore() {
        val estimator = HealthEstimator()
        val legacyContext =
            EstimatorContext(
                configuration = configuration,
                isOnline = true,
                pathState =
                    PathState(
                        interfaceType = NetworkInterfaceType.CELLULAR,
                        radioTechnology = RadioTechnology.LEGACY,
                    ),
            )

        var health: NetworkHealth = NetworkHealth.Unknown
        repeat(10) { health = estimator.recordLatency(0.05, isCensored = false, context = legacyContext) }

        val legacyScore = (health as NetworkHealth.Measured).score
        assertTrue(legacyScore <= configuration.legacyRadioScoreCap)

        val modernContext =
            legacyContext.copy(
                pathState =
                    PathState(
                        interfaceType = NetworkInterfaceType.CELLULAR,
                        radioTechnology = RadioTechnology.MODERN,
                    ),
            )

        val modernHealth = estimator.computeHealth(modernContext)
        assertTrue((modernHealth as NetworkHealth.Measured).score > legacyScore)
    }

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

    @Test
    fun constrainedPathPenalizesScore() {
        val unconstrained = HealthEstimator().recordLatency(0.1, isCensored = false, context = onlineContext())
        val constrained =
            HealthEstimator().recordLatency(
                0.1,
                isCensored = false,
                context = EstimatorContext(configuration, isOnline = true, pathState = PathState(isConstrained = true)),
            )

        assertTrue(unconstrained is NetworkHealth.Measured)
        assertTrue(constrained is NetworkHealth.Measured)
        assertTrue((constrained as NetworkHealth.Measured).score < (unconstrained as NetworkHealth.Measured).score)
    }

    @Test
    fun connectionFlapsPenalizeScore() {
        val estimator = HealthEstimator()
        val before = estimator.recordLatency(0.1, isCensored = false, context = onlineContext())

        var after = before
        repeat(5) { after = estimator.record(NetworkHealthEvent.ConnectionFlap, onlineContext()) }

        assertTrue(before is NetworkHealth.Measured)
        assertTrue(after is NetworkHealth.Measured)
        assertTrue((after as NetworkHealth.Measured).score < (before as NetworkHealth.Measured).score)
    }

    @Test
    fun transferStallsPenalizeScore() {
        val estimator = HealthEstimator()
        val before = estimator.recordLatency(0.1, isCensored = false, context = onlineContext())

        var after = before
        repeat(3) { after = estimator.record(NetworkHealthEvent.TransferStall, onlineContext()) }

        assertTrue(before is NetworkHealth.Measured)
        assertTrue(after is NetworkHealth.Measured)
        assertTrue((after as NetworkHealth.Measured).score < (before as NetworkHealth.Measured).score)
    }

    private fun onlineContext() = EstimatorContext(configuration, isOnline = true, pathState = PathState())
}
