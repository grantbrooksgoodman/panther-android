//
//  HealthEstimator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import kotlin.math.log2
import kotlin.math.max

/**
 * A point-in-time snapshot of the estimator's inputs, captured
 * when an update executes.
 */
internal data class EstimatorContext(
    val configuration: NetworkHealthConfiguration,
    val isOnline: Boolean,
    val pathState: PathState,
)

/** A snapshot of the estimator's channel statistics for diagnostics. */
internal data class EstimatorStatistics(
    val failureFraction: Double,
    val flapCount: Double,
    val lastTransferBytesPerSecond: Double?,
    val latencyConfidence: Double,
    val latencyDispersion: Double,
    val latencyMean: Double,
    val stallCount: Int,
    val throughputConfidence: Double,
    val throughputDispersion: Double,
    val throughputMean: Double,
)

/**
 * The framework's built-in network health estimator.
 *
 * Blends a time-decayed latency channel and throughput channel
 * into a single score, applying failure, stability, path, and
 * radio-technology adjustments, and classifies the result into a
 * [NetworkHealthTier]. All state is guarded by [LockIsolated], so
 * every method is safe to call from any thread.
 */
internal class HealthEstimator {
    // MARK: - Types

    private data class State(
        val failureChannel: HealthChannel = HealthChannel(),
        val flapChannel: HealthChannel = HealthChannel(),
        val lastTransferBytesPerSecond: Double? = null,
        val latencyChannel: HealthChannel = HealthChannel(),
        val stallCount: Int = 0,
        val throughputChannel: HealthChannel = HealthChannel(),
    )

    // MARK: - Properties

    private val state = LockIsolated(State())

    // MARK: - Methods

    fun computeHealth(context: EstimatorContext): NetworkHealth =
        state.withValue { computeHealth(it.value, context) }

    fun statistics(context: EstimatorContext): EstimatorStatistics {
        val halfLife = context.configuration.halfLife
        val now = System.currentTimeMillis()
        val state = state.wrappedValue

        return EstimatorStatistics(
            failureFraction = state.failureChannel.mean,
            flapCount = state.flapChannel.decayedWeight(now, halfLife),
            lastTransferBytesPerSecond = state.lastTransferBytesPerSecond,
            latencyConfidence = state.latencyChannel.decayedWeight(now, halfLife),
            latencyDispersion = state.latencyChannel.standardDeviation / max(state.latencyChannel.mean, DISPERSION_EPSILON),
            latencyMean = state.latencyChannel.mean,
            stallCount = state.stallCount,
            throughputConfidence = state.throughputChannel.decayedWeight(now, halfLife),
            throughputDispersion = state.throughputChannel.standardDeviation,
            throughputMean = state.throughputChannel.mean,
        )
    }

    fun record(
        event: NetworkHealthEvent,
        context: EstimatorContext,
    ): NetworkHealth =
        state.withValue {
            it.value = applyEvent(it.value, event, context.configuration.halfLife)
            computeHealth(it.value, context)
        }

    fun recordLatency(
        seconds: Double,
        isCensored: Boolean,
        context: EstimatorContext,
    ): NetworkHealth =
        state.withValue {
            val halfLife = context.configuration.halfLife
            val now = System.currentTimeMillis()

            it.value =
                it.value.copy(
                    latencyChannel = it.value.latencyChannel.record(seconds, now, halfLife),
                    // A timeout is the failure signal; a completed round-trip is a success.
                    failureChannel = it.value.failureChannel.record(if (isCensored) 1.0 else 0.0, now, halfLife),
                )

            computeHealth(it.value, context)
        }

    fun recordThroughput(
        bytes: Int,
        seconds: Double,
        context: EstimatorContext,
    ): NetworkHealth =
        state.withValue {
            if (bytes < context.configuration.minimumThroughputSampleBytes) {
                return@withValue computeHealth(it.value, context)
            }

            val bytesPerSecond = bytes.toDouble() / max(seconds, MINIMUM_INTERVAL_SECONDS)
            val now = System.currentTimeMillis()

            it.value =
                it.value.copy(
                    lastTransferBytesPerSecond = bytesPerSecond,
                    throughputChannel =
                        it.value.throughputChannel.record(
                            log2(bytesPerSecond),
                            now,
                            context.configuration.halfLife,
                        ),
                )

            computeHealth(it.value, context)
        }

    fun resetConfidence() {
        state.withValue {
            it.value =
                it.value.copy(
                    failureChannel = HealthChannel(),
                    flapChannel = HealthChannel(),
                    latencyChannel = HealthChannel(),
                    throughputChannel = HealthChannel(),
                )
        }
    }

    // MARK: - Auxiliary

    private fun applyEvent(
        state: State,
        event: NetworkHealthEvent,
        halfLife: Double,
    ): State {
        val now = System.currentTimeMillis()
        return when (event) {
            NetworkHealthEvent.ConnectionFlap ->
                state.copy(flapChannel = state.flapChannel.record(1.0, now, halfLife))

            // Reconnect timing reflects backoff scheduling, not network quality.
            is NetworkHealthEvent.ConnectionRestored -> state

            is NetworkHealthEvent.Handshake ->
                state.copy(latencyChannel = state.latencyChannel.record(event.seconds, now, halfLife))

            is NetworkHealthEvent.ProbeFailure ->
                state.copy(
                    failureChannel = state.failureChannel.record(1.0, now, halfLife),
                    latencyChannel = state.latencyChannel.record(event.timeoutSeconds, now, halfLife),
                )

            NetworkHealthEvent.TransferStall ->
                state.copy(
                    stallCount = state.stallCount + 1,
                    failureChannel = state.failureChannel.record(1.0, now, halfLife),
                )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun computeHealth(
        state: State,
        context: EstimatorContext,
    ): NetworkHealth {
        val configuration = context.configuration
        val pathState = context.pathState

        if (!context.isOnline) return NetworkHealth.Measured(0.0, NetworkHealthTier.POOR)

        val now = System.currentTimeMillis()
        val weightedLatencyConfidence =
            state.latencyChannel.decayedWeight(now, configuration.halfLife) *
                configuration.channelWeightLatency
        val weightedThroughputConfidence =
            state.throughputChannel.decayedWeight(now, configuration.halfLife) *
                configuration.channelWeightThroughput
        val totalConfidence = weightedLatencyConfidence + weightedThroughputConfidence

        if (totalConfidence < configuration.minimumConfidence) return NetworkHealth.Unknown

        val latencyScore =
            channelScore(
                mean = log2(max(state.latencyChannel.mean, MINIMUM_LATENCY_SECONDS)),
                floor = log2(configuration.latencyFloor),
                ceiling = log2(configuration.latencyCeiling),
                inverted = true,
                dispersion = state.latencyChannel.standardDeviation / max(state.latencyChannel.mean, MINIMUM_LATENCY_SECONDS),
                jitterCeiling = configuration.latencyJitterCeiling,
                jitterPenaltyWeight = configuration.jitterPenaltyWeight,
            )

        val throughputScore =
            channelScore(
                mean = state.throughputChannel.mean,
                floor = configuration.throughputFloor,
                ceiling = configuration.throughputCeiling,
                inverted = false,
                dispersion = state.throughputChannel.standardDeviation,
                jitterCeiling = configuration.throughputJitterCeiling,
                jitterPenaltyWeight = configuration.jitterPenaltyWeight,
            )

        var score =
            (latencyScore * weightedLatencyConfidence + throughputScore * weightedThroughputConfidence) /
                totalConfidence
        score *= 1.0 - failureRatePenalty(state, now, configuration)
        score *= 1.0 - stabilityPenalty(state, now, configuration)
        if (pathState.isConstrained) score *= configuration.constrainedPenalty
        if (pathState.isExpensive) score *= configuration.expensivePenalty
        score = score.coerceIn(0.0, 1.0)
        score = applyRadioTechnologyPrior(score, pathState, configuration)

        return NetworkHealth.Measured(score, configuration.tier(score))
    }

    // A prior, never a boost: the cap only stops a starved estimator
    // from over-reporting on legacy cellular technology.
    private fun applyRadioTechnologyPrior(
        score: Double,
        pathState: PathState,
        configuration: NetworkHealthConfiguration,
    ): Double {
        if (!configuration.isRadioTechnologyPriorEnabled ||
            pathState.interfaceType != NetworkInterfaceType.CELLULAR
        ) {
            return score
        }

        return when (pathState.radioTechnology) {
            RadioTechnology.INTERMEDIATE -> score.coerceAtMost(configuration.intermediateRadioScoreCap)
            RadioTechnology.LEGACY -> score.coerceAtMost(configuration.legacyRadioScoreCap)
            RadioTechnology.MODERN,
            RadioTechnology.UNKNOWN,
            -> score
        }
    }

    /**
     * Maps a channel mean to `[0, 1]` via a piecewise-linear ramp,
     * then reduces the result in proportion to the channel's
     * normalized sample dispersion (jitter).
     */
    @Suppress("LongParameterList")
    private fun channelScore(
        mean: Double,
        floor: Double,
        ceiling: Double,
        inverted: Boolean,
        dispersion: Double,
        jitterCeiling: Double,
        jitterPenaltyWeight: Double,
    ): Double {
        if (ceiling <= floor) return HALF

        val clamped = ((mean - floor) / (ceiling - floor)).coerceIn(0.0, 1.0)
        val rampScore = if (inverted) 1.0 - clamped else clamped

        val normalizedDispersion = (dispersion / max(jitterCeiling, MINIMUM_INTERVAL_SECONDS)).coerceAtMost(1.0)
        val jitterPenalty = (jitterPenaltyWeight * normalizedDispersion).coerceIn(0.0, 1.0)

        return rampScore * (1.0 - jitterPenalty)
    }

    private fun failureRatePenalty(
        state: State,
        time: Long,
        configuration: NetworkHealthConfiguration,
    ): Double {
        val decayedWeight = state.failureChannel.decayedWeight(time, configuration.halfLife)
        val penalty =
            configuration.failureRatePenaltyWeight *
                state.failureChannel.mean *
                decayedWeight.coerceAtMost(1.0)
        return penalty.coerceIn(0.0, 1.0)
    }

    private fun stabilityPenalty(
        state: State,
        time: Long,
        configuration: NetworkHealthConfiguration,
    ): Double {
        val decayedFlapCount = state.flapChannel.decayedWeight(time, configuration.halfLife)
        val penalty =
            configuration.stabilityPenaltyWeight *
                (decayedFlapCount / max(configuration.stabilityFlapCeiling, MINIMUM_INTERVAL_SECONDS)).coerceAtMost(1.0)
        return penalty.coerceIn(0.0, 1.0)
    }

    private companion object {
        private const val DISPERSION_EPSILON = 0.001
        private const val HALF = 0.5
        private const val MINIMUM_INTERVAL_SECONDS = 0.001
        private const val MINIMUM_LATENCY_SECONDS = 0.001
    }
}
