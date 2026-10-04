//
//  HealthChannel.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Time-decayed exponentially weighted moving average (EWMA) over
 * irregularly spaced samples.
 *
 * Each channel tracks a single signal (for example, latency or
 * throughput) with no fixed-count window. The decayed [weight]
 * doubles as the channel's confidence – long idle periods degrade
 * confidence without requiring new samples.
 *
 * Alongside the mean, each channel maintains a second moment
 * decayed with the same factor, from which the variance and
 * standard deviation of the sample history are derived.
 *
 * Timestamps are expressed as epoch milliseconds and half-life as
 * a duration in seconds.
 */
internal data class HealthChannel(
    val lastUpdate: Long? = null,
    val mean: Double = 0.0,
    val secondMoment: Double = 0.0,
    val weight: Double = 0.0,
) {
    // MARK: - Computed Properties

    /** The standard deviation of the channel's decayed history. */
    val standardDeviation: Double
        get() = sqrt(variance)

    /**
     * The variance of the channel's decayed sample history.
     *
     * Floating-point error can drive the raw second-moment
     * difference slightly negative; the value is clamped to zero
     * in that case.
     */
    val variance: Double
        get() = max(secondMoment - mean * mean, 0.0)

    // MARK: - Methods

    /**
     * Returns the channel's weight decayed to the given point in
     * time, reflecting current confidence in the estimate.
     */
    fun decayedWeight(
        at: Long,
        halfLife: Double,
    ): Double {
        val lastUpdate = lastUpdate ?: return 0.0
        if (halfLife <= 0.0) return 0.0

        val elapsed = (at - lastUpdate) / MILLIS_PER_SECOND
        return weight * DECAY_BASE.pow(-elapsed / halfLife)
    }

    /**
     * Records a new sample, applying time-based decay to previous
     * state before incorporating it, and returns the updated
     * channel.
     */
    fun record(
        sample: Double,
        at: Long,
        halfLife: Double,
    ): HealthChannel {
        val previous = lastUpdate
        if (previous == null || halfLife <= 0.0) {
            return HealthChannel(
                lastUpdate = at,
                mean = sample,
                secondMoment = sample * sample,
                weight = 1.0,
            )
        }

        val elapsed = (at - previous) / MILLIS_PER_SECOND
        val decayedWeight = weight * DECAY_BASE.pow(-elapsed / halfLife)
        val totalWeight = decayedWeight + 1.0

        return HealthChannel(
            lastUpdate = at,
            mean = (mean * decayedWeight + sample) / totalWeight,
            secondMoment = (secondMoment * decayedWeight + sample * sample) / totalWeight,
            weight = totalWeight,
        )
    }

    /**
     * Returns a channel reset to its initial state, discarding all
     * accumulated history and confidence.
     */
    fun reset(): HealthChannel = HealthChannel()

    // MARK: - Auxiliary

    private companion object {
        private const val DECAY_BASE = 2.0
        private const val MILLIS_PER_SECOND = 1000.0
    }
}
