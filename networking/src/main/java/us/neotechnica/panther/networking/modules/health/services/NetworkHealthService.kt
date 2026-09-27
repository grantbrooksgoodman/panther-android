//
//  NetworkHealthService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.services

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.health.extensions.networkHealth
import us.neotechnica.panther.networking.modules.health.interfaces.NetworkHealthDelegate
import us.neotechnica.panther.networking.modules.health.models.EstimatorContext
import us.neotechnica.panther.networking.modules.health.models.HealthEstimator
import us.neotechnica.panther.networking.modules.health.models.NetworkHealth
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthEvent
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthProbeConfiguration
import us.neotechnica.panther.networking.modules.health.models.NetworkInterfaceType
import us.neotechnica.panther.networking.modules.health.models.PathState
import us.neotechnica.panther.networking.modules.health.models.RadioTechnology
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import java.net.HttpURLConnection
import java.net.URL

/**
 * The framework's built-in [NetworkHealthDelegate].
 *
 * Observes the active network path through [ConnectivityManager]
 * and folds latency, throughput, and event evidence into a
 * [HealthEstimator], publishing each result to the `networkHealth`
 * shared value.
 */
internal object NetworkHealthService : NetworkHealthDelegate {
    // MARK: - Properties

    private val estimator = HealthEstimator()
    private val isMonitoring = LockIsolated(false)
    private val pathState = LockIsolated(PathState())
    private val probeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val probeTimestamps = LockIsolated(listOf<Long>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val latestHealth = LockIsolated<NetworkHealth>(NetworkHealth.Unknown)

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                handlePathUpdate(networkCapabilities)
            }
        }

    // MARK: - Computed Properties

    override val health: NetworkHealth
        get() {
            val health = latestHealth.wrappedValue

            // Probing exists to fill the idle-confidence gap: an
            // unknown read is the demand signal.
            if (health.isUnknown) maybeProbe()
            return health
        }

    private val connectivityManager: ConnectivityManager
        get() = Networking.requireContext().getSystemService(ConnectivityManager::class.java)

    private val estimatorContext: EstimatorContext
        get() =
            EstimatorContext(
                configuration = Networking.config.networkHealthConfiguration,
                isOnline = isOnline(),
                pathState = pathState.wrappedValue,
            )

    // MARK: - NetworkHealthDelegate Conformance

    override fun record(event: NetworkHealthEvent) {
        if (event is NetworkHealthEvent.ConnectionRestored) {
            Logger.log(
                "Connection restored after ${"%.1f".format(event.afterSeconds)} seconds.",
                domain = LoggerDomain.Networking.health,
            )
        }

        updateHealth { estimator.record(event, estimatorContext) }
    }

    override fun recordCensoredLatencySample(seconds: Double) = submitLatencySample(seconds, isCensored = true)

    override fun recordLatencySample(seconds: Double) = submitLatencySample(seconds, isCensored = false)

    override fun recordThroughputSample(
        bytes: Int,
        seconds: Double,
    ) {
        updateHealth { estimator.recordThroughput(bytes, seconds, estimatorContext) }
    }

    override fun startMonitoring() {
        if (isMonitoring.wrappedValue) return
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            isMonitoring.wrappedValue = true
        }
    }

    override fun stopMonitoring() {
        if (!isMonitoring.wrappedValue) return
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        isMonitoring.wrappedValue = false
    }

    // MARK: - Auxiliary

    private fun claimProbeSlot(configuration: NetworkHealthProbeConfiguration): Boolean =
        probeTimestamps.withValue {
            val now = System.currentTimeMillis()
            val recent = it.value.filter { timestamp -> now - timestamp < ONE_HOUR_MILLIS }
            it.value = recent

            val lastProbe = recent.maxOrNull()
            if (recent.size >= configuration.maximumProbesPerHour) return@withValue false
            if (lastProbe != null &&
                now - lastProbe < configuration.minimumIntervalSeconds * MILLIS_PER_SECOND
            ) {
                return@withValue false
            }

            it.value = recent + now
            true
        }

    private fun handlePathUpdate(capabilities: NetworkCapabilities) {
        val newState =
            PathState(
                interfaceType = interfaceType(capabilities),
                // Android exposes no per-path Low Data Mode analog.
                isConstrained = false,
                isExpensive = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                radioTechnology = RadioTechnology.UNKNOWN,
            )

        val previousInterfaceType = pathState.wrappedValue.interfaceType
        pathState.wrappedValue = newState

        // Reset channel confidence on interface transitions; previous
        // samples are not representative of the new path.
        if (previousInterfaceType == null || previousInterfaceType == newState.interfaceType) return

        scope.launch {
            estimator.resetConfidence()
            publish(estimator.computeHealth(estimatorContext))
        }

        maybeProbe(afterDelayMillis = PROBE_SETTLE_DELAY_MILLIS)
    }

    private fun interfaceType(capabilities: NetworkCapabilities): NetworkInterfaceType =
        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkInterfaceType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkInterfaceType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkInterfaceType.WIRED_ETHERNET
            else -> NetworkInterfaceType.OTHER
        }

    private fun isOnline(): Boolean =
        runCatching {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)

    /**
     * Fire-and-forget probe trigger; bails immediately when probing
     * is unconfigured so unconfigured behavior is identical to
     * baseline.
     */
    private fun maybeProbe(afterDelayMillis: Long = 0L) {
        val configuration = Networking.config.networkHealthConfiguration.probeConfiguration ?: return
        probeScope.launch {
            if (afterDelayMillis > 0L) delay(afterDelayMillis)
            performProbe(configuration)
        }
    }

    private fun performProbe(configuration: NetworkHealthProbeConfiguration) {
        val path = pathState.wrappedValue
        if ((path.isExpensive && !configuration.allowsExpensivePaths) ||
            (path.isConstrained && !configuration.allowsConstrainedPaths)
        ) {
            return
        }

        if (!claimProbeSlot(configuration)) return

        val timeoutMillis = (configuration.timeoutSeconds * MILLIS_PER_SECOND).toInt()
        val start = System.currentTimeMillis()
        val succeeded =
            runCatching {
                val connection = URL(configuration.url).openConnection() as HttpURLConnection
                connection.requestMethod = configuration.httpMethod
                connection.connectTimeout = timeoutMillis
                connection.readTimeout = timeoutMillis
                try {
                    connection.responseCode
                } finally {
                    connection.disconnect()
                }
            }.isSuccess

        if (succeeded) {
            recordLatencySample((System.currentTimeMillis() - start) / MILLIS_PER_SECOND)
        } else {
            record(NetworkHealthEvent.ProbeFailure(timeoutSeconds = configuration.timeoutSeconds))
        }
    }

    private fun publish(health: NetworkHealth) {
        val previousTier = latestHealth.wrappedValue.tier
        latestHealth.wrappedValue = health
        SharedState { it.networkHealth }.wrappedValue = health

        if (previousTier == health.tier) return
        Logger.log(
            "Network health transitioned from ${previousTier?.rawValue ?: "unknown"} " +
                "to ${health.tier?.rawValue ?: "unknown"}.",
            domain = LoggerDomain.Networking.health,
        )
    }

    private fun submitLatencySample(
        seconds: Double,
        isCensored: Boolean = false,
    ) {
        updateHealth { estimator.recordLatency(seconds, isCensored, estimatorContext) }
    }

    /**
     * Runs an estimator update off the caller's thread – recording
     * stays fire-and-forget – and publishes the resulting health.
     */
    private fun updateHealth(update: () -> NetworkHealth) {
        scope.launch { publish(update()) }
    }

    private const val MILLIS_PER_SECOND = 1000.0
    private const val ONE_HOUR_MILLIS = 3_600_000L
    private const val PROBE_SETTLE_DELAY_MILLIS = 2000L
}
