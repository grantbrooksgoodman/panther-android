//
//  NetworkHealthProbeConfiguration.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

/**
 * Configuration for opt-in active network health probing.
 *
 * Probing is the health system's only source of self-generated
 * traffic. It exists to fill the idle-confidence gap – the
 * situation where a consumer wants a decision and the estimate has
 * decayed to [NetworkHealth.Unknown] – and fires on demand, never
 * on a timer.
 *
 * Enabling probing requires supplying an endpoint you control.
 * Choose one that returns a small response, such as an empty `200`
 * or `204`.
 */
data class NetworkHealthProbeConfiguration(
    /** The endpoint probes are sent to. */
    val url: String,
    /**
     * A Boolean value that determines whether probes may be sent
     * while the network path is constrained.
     *
     * Default value is `false`.
     */
    val allowsConstrainedPaths: Boolean = false,
    /**
     * A Boolean value that determines whether probes may be sent
     * while the network path is expensive (for example, a cellular
     * or personal hotspot connection).
     *
     * Default value is `false`.
     */
    val allowsExpensivePaths: Boolean = false,
    /**
     * The HTTP method used for probe requests.
     *
     * Default value is `"HEAD"`.
     */
    val httpMethod: String = DEFAULT_HTTP_METHOD,
    /**
     * The hard budget of probe attempts per hour.
     *
     * Default value is `10`.
     */
    val maximumProbesPerHour: Int = DEFAULT_MAXIMUM_PROBES_PER_HOUR,
    /**
     * The minimum interval, in seconds, between any two probe
     * attempts – successful or not.
     *
     * Default value is `60` seconds.
     */
    val minimumIntervalSeconds: Double = DEFAULT_MINIMUM_INTERVAL_SECONDS,
    /**
     * The probe request's timeout, in seconds.
     *
     * A probe that fails at the network level contributes a
     * censored latency sample bounded by this value.
     *
     * Default value is `5` seconds.
     */
    val timeoutSeconds: Double = DEFAULT_TIMEOUT_SECONDS,
) {
    // MARK: - Auxiliary

    private companion object {
        private const val DEFAULT_HTTP_METHOD = "HEAD"
        private const val DEFAULT_MAXIMUM_PROBES_PER_HOUR = 10
        private const val DEFAULT_MINIMUM_INTERVAL_SECONDS = 60.0
        private const val DEFAULT_TIMEOUT_SECONDS = 5.0
    }
}
