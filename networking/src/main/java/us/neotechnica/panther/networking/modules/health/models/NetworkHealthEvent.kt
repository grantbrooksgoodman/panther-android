//
//  NetworkHealthEvent.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

/**
 * A discrete network health signal.
 *
 * Events complement the continuous latency and throughput sample
 * channels with one-shot evidence – connection flaps, handshake
 * timings, transfer stalls, and probe failures – that no
 * per-operation sample can capture. Report an event through
 * [NetworkHealthDelegate.record]; the built-in delegate folds it
 * into the health estimate.
 */
sealed class NetworkHealthEvent {
    /**
     * The realtime connection dropped unexpectedly while the
     * device was otherwise online and active.
     */
    data object ConnectionFlap : NetworkHealthEvent()

    /**
     * The realtime connection was reestablished after an outage of
     * the given duration.
     *
     * This event is informational and does not affect the health
     * score – reconnect timing reflects backoff scheduling, not
     * network quality.
     *
     * @property afterSeconds The duration of the outage, in
     *   seconds.
     */
    data class ConnectionRestored(
        val afterSeconds: Double,
    ) : NetworkHealthEvent()

    /**
     * A fresh connection handshake completed in the given
     * duration.
     *
     * Handshake timing – DNS resolution plus connection
     * establishment – is on the order of a light network
     * round-trip and contributes to the latency channel.
     *
     * @property seconds The handshake duration, in seconds.
     */
    data class Handshake(
        val seconds: Double,
    ) : NetworkHealthEvent()

    /**
     * A network probe failed to complete within its timeout.
     *
     * Contributes a failure sample and a censored latency sample
     * bounded by the probe's timeout.
     *
     * @property timeoutSeconds The probe's configured timeout, in
     *   seconds.
     */
    data class ProbeFailure(
        val timeoutSeconds: Double,
    ) : NetworkHealthEvent()

    /**
     * An active transfer stopped making progress.
     *
     * Contributes a failure sample.
     */
    data object TransferStall : NetworkHealthEvent()
}
