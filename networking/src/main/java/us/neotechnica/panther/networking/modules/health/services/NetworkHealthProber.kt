//
//  NetworkHealthProber.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.services

import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.health.extensions.isNetworkLevelFailure
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthEvent
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthProbeConfiguration
import us.neotechnica.panther.networking.modules.health.models.PathState
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/**
 * A demand-driven, rate-limited network prober.
 *
 * It fires only when asked, never on a timer, and only when every gate
 * passes: probing configured, device online and foregrounded, path
 * permitted, power-save inactive, rate limits satisfied, and no probe
 * already in flight. A successful probe reports its full round-trip
 * duration as latency evidence; a network-level failure reports
 * [NetworkHealthEvent.ProbeFailure]; any other outcome means the server
 * answered and counts as latency evidence.
 *
 * @param isOnlineProvider Supplies whether the device is online.
 * @param onEvent Receives probe-failure events.
 * @param onLatencySample Receives round-trip latency samples, in seconds.
 * @param pathStateProvider Supplies the current network path state.
 */
internal class NetworkHealthProber(
    private val isOnlineProvider: () -> Boolean,
    private val onEvent: (NetworkHealthEvent) -> Unit,
    private val onLatencySample: (Double) -> Unit,
    private val pathStateProvider: () -> PathState,
) {
    // MARK: - Types

    private data class MutableState(
        val isProbeInFlight: Boolean = false,
        val lastAttemptAt: Long? = null,
        val lastOutcomeDescription: String? = null,
        val probeAttempts: List<Long> = emptyList(),
    )

    // MARK: - Properties

    private val state = LockIsolated(MutableState())

    // MARK: - Computed Properties

    /** A human-readable summary of probe activity for the developer-mode inspection surface. */
    val statsDescription: String
        get() {
            val probeConfiguration = Networking.config.networkHealthConfiguration.probeConfiguration
            val maximumProbesPerHour = probeConfiguration?.maximumProbesPerHour ?: 0
            val now = System.currentTimeMillis()
            return state.withValue { reference ->
                val recent = reference.value.probeAttempts.filter { now - it < BUDGET_WINDOW_MILLIS }
                reference.value = reference.value.copy(probeAttempts = recent)
                val remainingBudget = max(maximumProbesPerHour - recent.size, 0)
                val lastAttemptAt = reference.value.lastAttemptAt
                if (lastAttemptAt == null) {
                    "never · ℛ $remainingBudget/$maximumProbesPerHour"
                } else {
                    val secondsAgo = ((now - lastAttemptAt) / MILLIS_PER_SECOND).toInt()
                    val outcomeDescription = reference.value.lastOutcomeDescription ?: "in flight"
                    "${secondsAgo}s ago ($outcomeDescription) · $remainingBudget/$maximumProbesPerHour rem."
                }
            }
        }

    // MARK: - Methods

    /** Sends a single probe if – and only if – every gate in the guard chain passes. */
    fun maybeProbe() {
        val configuration = Networking.config.networkHealthConfiguration.probeConfiguration ?: return
        if (!isOnlineProvider()) return
        if (isBackgrounded()) return

        val path = pathStateProvider()
        if ((path.isConstrained && !configuration.allowsConstrainedPaths) ||
            (path.isExpensive && !configuration.allowsExpensivePaths) ||
            isPowerSaveMode()
        ) {
            return
        }

        if (!claimProbeSlot(configuration)) return
        performProbe(configuration)
    }

    // MARK: - Auxiliary

    private fun claimProbeSlot(configuration: NetworkHealthProbeConfiguration): Boolean =
        state.withValue { reference ->
            val now = System.currentTimeMillis()
            val recent = reference.value.probeAttempts.filter { timestamp -> now - timestamp < BUDGET_WINDOW_MILLIS }
            reference.value = reference.value.copy(probeAttempts = recent)

            if (reference.value.isProbeInFlight) return@withValue false
            if (recent.size >= configuration.maximumProbesPerHour) return@withValue false
            val lastProbe = recent.maxOrNull()
            if (lastProbe != null &&
                now - lastProbe < configuration.minimumIntervalSeconds * MILLIS_PER_SECOND
            ) {
                return@withValue false
            }

            reference.value = reference.value.copy(isProbeInFlight = true, lastAttemptAt = now, probeAttempts = recent + now)
            true
        }

    private fun performProbe(configuration: NetworkHealthProbeConfiguration) {
        Logger.log("Probing \"${configuration.url}\" for network health.", domain = LoggerDomain.Networking.health)

        val timeoutMillis = (configuration.timeoutSeconds * MILLIS_PER_SECOND).toInt()
        val start = System.currentTimeMillis()
        val outcomeDescription =
            try {
                val connection = URL(configuration.url).openConnection() as HttpURLConnection
                connection.requestMethod = configuration.httpMethod
                connection.connectTimeout = timeoutMillis
                connection.readTimeout = timeoutMillis
                val landingHost =
                    try {
                        connection.responseCode
                        connection.url.host
                    } finally {
                        connection.disconnect()
                    }

                val elapsed = (System.currentTimeMillis() - start) / MILLIS_PER_SECOND
                val probeHost = runCatching { URL(configuration.url).host }.getOrNull()
                if (landingHost != null && probeHost != null && !landingHost.equals(probeHost, ignoreCase = true)) {
                    // A redirect off the operator's host is not evidence about their endpoint.
                    "cross-host redirect, discarded"
                } else {
                    onLatencySample(elapsed)
                    "%.3fs".format(elapsed)
                }
            } catch (throwable: Throwable) {
                if (throwable.isNetworkLevelFailure) {
                    onEvent(NetworkHealthEvent.ProbeFailure(timeoutSeconds = configuration.timeoutSeconds))
                    "network failure"
                } else {
                    // The server answered or the failure was not network-level; the round-trip
                    // duration is still honest latency evidence.
                    val elapsed = (System.currentTimeMillis() - start) / MILLIS_PER_SECOND
                    onLatencySample(elapsed)
                    "%.3fs (non-network error)".format(elapsed)
                }
            }

        state.withValue {
            it.value = it.value.copy(isProbeInFlight = false, lastOutcomeDescription = outcomeDescription)
        }
        Logger.log("Probe completed: $outcomeDescription.", domain = LoggerDomain.Networking.health)
    }

    private fun isBackgrounded(): Boolean =
        !ProcessLifecycleOwner
            .get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.STARTED)

    private fun isPowerSaveMode(): Boolean =
        runCatching {
            Networking.requireContext().getSystemService(PowerManager::class.java).isPowerSaveMode
        }.getOrDefault(false)

    // MARK: - Companion

    private companion object {
        private const val MILLIS_PER_SECOND = 1000.0
        private const val BUDGET_WINDOW_SECONDS = 3600L
        private const val BUDGET_WINDOW_MILLIS = BUDGET_WINDOW_SECONDS * 1000L
    }
}
