//
//  NetworkHealthDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.interfaces

import us.neotechnica.panther.networking.modules.health.models.NetworkHealth
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthEvent

/**
 * An interface for passive network quality estimation.
 *
 * The framework's built-in implementation observes byte transfers
 * and operation round-trips to maintain a continuously updated
 * health score. Conform to this interface to substitute a custom
 * estimator, then register it with
 * [Networking.Config.registerHealthDelegate][us.neotechnica.panther.networking.Networking.Config.registerHealthDelegate].
 *
 * **Important:** Only the framework's built-in Firebase-backed
 * implementations are instrumented to produce samples. If you
 * register a custom database or storage delegate, those operations
 * will not feed the health estimator.
 */
interface NetworkHealthDelegate {
    // MARK: - Properties

    /**
     * The most recently computed network health value.
     *
     * This property is synchronous and safe to read from any
     * context.
     */
    val health: NetworkHealth

    // MARK: - Methods

    /**
     * Records a discrete network health event.
     *
     * Events carry one-shot evidence – connection flaps, handshake
     * timings, transfer stalls, and probe failures – that
     * complements the continuous latency and throughput sample
     * channels. See [NetworkHealthEvent] for the available signals.
     *
     * The default implementation does nothing, so custom
     * conformances only implement this method to incorporate event
     * evidence.
     *
     * @param event The event to record.
     */
    fun record(event: NetworkHealthEvent) {}

    /**
     * Records a censored latency sample at the given duration.
     *
     * A censored sample indicates that the true latency is unknown
     * but bounded below by the timeout value. This is the strongest
     * single piece of negative evidence the latency channel
     * receives.
     *
     * @param seconds The operation's configured timeout duration,
     *   in seconds.
     */
    fun recordCensoredLatencySample(seconds: Double)

    /**
     * Records a latency sample for a completed network round-trip.
     *
     * @param seconds The wall-clock duration of the round-trip, in
     *   seconds.
     */
    fun recordLatencySample(seconds: Double)

    /**
     * Records a throughput sample for a completed storage transfer.
     *
     * Samples below
     * [NetworkHealthConfiguration.minimumThroughputSampleBytes] are
     * silently discarded by the built-in implementation.
     *
     * @param bytes The number of bytes transferred.
     * @param seconds The wall-clock duration of the transfer, in
     *   seconds.
     */
    fun recordThroughputSample(
        bytes: Int,
        seconds: Double,
    )

    /**
     * Begins monitoring for network interface transitions and path
     * property changes.
     */
    fun startMonitoring()

    /** Stops monitoring and releases the underlying path monitor. */
    fun stopMonitoring()

    /**
     * A multi-line, human-readable summary of the current health
     * estimate and its evidence, for the developer-mode inspection
     * surface.
     */
    fun debugSummary(): String = ""
}
