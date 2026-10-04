//
//  NetworkHealthConfiguration.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

/**
 * Configuration parameters for network health estimation.
 *
 * All scoring constants – half-life, ramp anchors, channel
 * weights, penalties, priors, trust gates, and tier boundaries –
 * are collected in this single value type, along with the passive
 * signal opt-outs and the opt-in [probeConfiguration]. Start from
 * [default] and customize with `copy`, then apply the result
 * through
 * [Networking.Config.setNetworkHealthConfiguration][us.neotechnica.panther.networking.Networking.Config.setNetworkHealthConfiguration].
 */
@Suppress("LongParameterList") // All fields are supplied at the one memberwise call site.
data class NetworkHealthConfiguration(
    /**
     * The health score below which [CacheStrategy.ADAPTIVE]
     * resolves to [CacheStrategy.RETURN_CACHE_FIRST].
     */
    val adaptiveScoreThreshold: Double,
    /** The relative weight of the latency channel when blending. */
    val channelWeightLatency: Double,
    /** The relative weight of the throughput channel when blending. */
    val channelWeightThroughput: Double,
    /**
     * Multiplicative penalty applied when the current network path
     * is constrained.
     */
    val constrainedPenalty: Double,
    /**
     * Multiplicative penalty applied when the current network path
     * is expensive.
     */
    val expensivePenalty: Double,
    /**
     * The weight of the multiplicative penalty applied to the score
     * as operations fail. A value of `0` disables the penalty.
     */
    val failureRatePenaltyWeight: Double,
    /**
     * The score at or above which health is classified as
     * [NetworkHealthTier.FAIR].
     */
    val fairTierThreshold: Double,
    /**
     * The duration, in seconds, after the app returns to the
     * foreground during which realtime connection drops are not
     * counted as flaps.
     */
    val flapForegroundGraceSeconds: Double,
    /**
     * The score at or above which health is classified as
     * [NetworkHealthTier.GOOD].
     */
    val goodTierThreshold: Double,
    /**
     * The half-life, in seconds, of the exponentially weighted
     * moving average used by both channels.
     */
    val halfLife: Double,
    /** The maximum score reportable on 3G-class cellular technologies. */
    val intermediateRadioScoreCap: Double,
    /**
     * A Boolean value that determines whether the realtime client's
     * connection stability is monitored as health evidence.
     */
    val isConnectionStabilityMonitoringEnabled: Boolean,
    /**
     * A Boolean value that determines whether the device's cellular
     * radio access technology caps the health score.
     */
    val isRadioTechnologyPriorEnabled: Boolean,
    /**
     * A Boolean value that determines whether transaction metrics
     * from the framework's own HTTPS requests contribute evidence.
     */
    val isURLSessionMetricsEnabled: Boolean,
    /**
     * The weight of the per-channel score reduction applied as
     * sample dispersion (jitter) grows. A value of `0` disables it.
     */
    val jitterPenaltyWeight: Double,
    /**
     * The latency, in seconds, at or above which the latency
     * channel maps to a score of approximately zero.
     */
    val latencyCeiling: Double,
    /**
     * The latency, in seconds, at or below which the latency
     * channel maps to a score of approximately one.
     */
    val latencyFloor: Double,
    /**
     * The latency channel's coefficient of variation at or above
     * which the jitter reduction saturates.
     */
    val latencyJitterCeiling: Double,
    /** The maximum score reportable on 2G-class cellular technologies. */
    val legacyRadioScoreCap: Double,
    /**
     * The minimum aggregate channel confidence required to produce
     * a [NetworkHealth.Measured] value.
     */
    val minimumConfidence: Double,
    /**
     * The minimum byte count for a storage transfer to be recorded
     * as a throughput sample.
     */
    val minimumThroughputSampleBytes: Int,
    /**
     * The active probing configuration, or `null` to disable
     * probing entirely.
     */
    val probeConfiguration: NetworkHealthProbeConfiguration?,
    /**
     * The decayed flap count at or above which the stability
     * penalty saturates.
     */
    val stabilityFlapCeiling: Double,
    /**
     * The weight of the multiplicative penalty applied to the score
     * as the realtime connection flaps. A value of `0` disables it.
     */
    val stabilityPenaltyWeight: Double,
    /**
     * The log₂(bytes per second) value at or above which the
     * throughput channel maps to a score of approximately one.
     */
    val throughputCeiling: Double,
    /**
     * The log₂(bytes per second) value at or below which the
     * throughput channel maps to a score of approximately zero.
     */
    val throughputFloor: Double,
    /**
     * The throughput channel's standard deviation, in log₂(bytes
     * per second) units, at or above which jitter reduction
     * saturates.
     */
    val throughputJitterCeiling: Double,
    /**
     * The interval, in seconds, at which an active storage transfer
     * is checked for stalled progress.
     */
    val transferStallCheckInterval: Double,
    /**
     * The duration, in seconds, without progress after which an
     * active storage transfer is considered stalled.
     */
    val transferStallSeconds: Double,
) {
    // MARK: - Methods

    /** Returns the tier classification for the given score. */
    fun tier(score: Double): NetworkHealthTier =
        when {
            score >= goodTierThreshold -> NetworkHealthTier.GOOD
            score >= fairTierThreshold -> NetworkHealthTier.FAIR
            else -> NetworkHealthTier.POOR
        }

    // MARK: - Constants

    companion object {
        /** The default configuration. */
        val default =
            NetworkHealthConfiguration(
                adaptiveScoreThreshold = 0.3,
                channelWeightLatency = 0.6,
                channelWeightThroughput = 0.4,
                constrainedPenalty = 0.9,
                expensivePenalty = 0.95,
                failureRatePenaltyWeight = 0.5,
                fairTierThreshold = 0.3,
                flapForegroundGraceSeconds = 10.0,
                goodTierThreshold = 0.7,
                halfLife = 30.0,
                intermediateRadioScoreCap = 0.75,
                isConnectionStabilityMonitoringEnabled = true,
                isRadioTechnologyPriorEnabled = true,
                isURLSessionMetricsEnabled = true,
                jitterPenaltyWeight = 0.3,
                latencyCeiling = 3.0,
                latencyFloor = 0.1,
                latencyJitterCeiling = 1.0,
                legacyRadioScoreCap = 0.4,
                minimumConfidence = 0.5,
                minimumThroughputSampleBytes = 51_200,
                probeConfiguration = null,
                stabilityFlapCeiling = 3.0,
                stabilityPenaltyWeight = 0.4,
                throughputCeiling = 22.0,
                throughputFloor = 13.0,
                throughputJitterCeiling = 2.0,
                transferStallCheckInterval = 2.0,
                transferStallSeconds = 8.0,
            )
    }
}
