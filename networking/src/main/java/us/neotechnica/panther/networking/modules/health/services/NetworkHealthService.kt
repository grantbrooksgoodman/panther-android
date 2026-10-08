//
//  NetworkHealthService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
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
import us.neotechnica.panther.networking.modules.health.models.NetworkInterfaceType
import us.neotechnica.panther.networking.modules.health.models.PathState
import us.neotechnica.panther.networking.modules.health.models.RadioTechnology
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import kotlin.math.pow

// This service exceeds the type-body-length limit.

/**
 * The framework's built-in [NetworkHealthDelegate].
 *
 * Observes the active network path through [ConnectivityManager]
 * and folds latency, throughput, and event evidence into a
 * [HealthEstimator], publishing each result to the `networkHealth`
 * shared value.
 */
@Suppress("LargeClass")
internal object NetworkHealthService : NetworkHealthDelegate {
    // MARK: - Properties

    private val connectionStabilityObserver = ConnectionStabilityObserver(isOnlineProvider = ::isOnline, onEvent = ::record)
    private val estimator = HealthEstimator()
    private val hasStartedConnectionObserver = LockIsolated(false)
    private val isMonitoring = LockIsolated(false)
    private val pathState = LockIsolated(PathState())
    private val probeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prober = LockIsolated<NetworkHealthProber?>(null)
    private val radioTechnologyObserver = LockIsolated<TelephonyCallback?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Suppress("ObjectPropertyNaming", "ktlint:standard:property-naming")
    private val _health = LockIsolated<NetworkHealth>(NetworkHealth.Unknown)

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                handlePathUpdate(networkCapabilities)
            }

            override fun onLost(network: Network) {
                applyPathState(PathState())
            }
        }

    // Data Saver changes do not fire the network callback, so a dedicated
    // receiver refreshes the path's constrained flag when it toggles.
    private val dataSaverReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                refreshPath()
            }
        }

    // MARK: - Computed Properties

    override val health: NetworkHealth
        get() {
            val health = _health.wrappedValue

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

    override fun recordCensoredLatencySample(seconds: Double) {
        startConnectionStabilityMonitoringIfNeeded()
        submitLatencySample(seconds, isCensored = true)
    }

    override fun recordLatencySample(seconds: Double) {
        // Evidence of realtime database use: attach the stability observer lazily on the first sample.
        startConnectionStabilityMonitoringIfNeeded()
        submitLatencySample(seconds, isCensored = false)
    }

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
            Networking.requireContext().registerReceiver(
                dataSaverReceiver,
                IntentFilter(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED),
            )
            isMonitoring.wrappedValue = true
        }

        registerRadioTechnologyObserver()
    }

    override fun stopMonitoring() {
        if (!isMonitoring.wrappedValue) return
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        runCatching { Networking.requireContext().unregisterReceiver(dataSaverReceiver) }
        connectionStabilityObserver.stop()
        hasStartedConnectionObserver.wrappedValue = false
        isMonitoring.wrappedValue = false
        removeRadioTechnologyObserver()
    }

    fun debugSummary(): String {
        val configuration = Networking.config.networkHealthConfiguration
        val statistics = estimator.statistics(estimatorContext)
        val currentHealth = health

        val score = currentHealth.score
        val tier = currentHealth.tier
        val scoreDescription =
            if (score != null && tier != null) {
                "%.2f".format(score) + " (${tier.rawValue.replaceFirstChar { it.uppercase() }})"
            } else {
                "unknown"
            }

        val latencyDescription =
            if (statistics.latencyConfidence > 0) {
                "${formattedSeconds(statistics.latencyMean)} ±${(statistics.latencyDispersion * PERCENT).toInt()}%"
            } else {
                "none"
            }

        val throughputDescription =
            if (statistics.throughputConfidence > 0) {
                "${formattedBytesPerSecond(2.0.pow(statistics.throughputMean))} ±${"%.2f".format(statistics.throughputDispersion)}"
            } else {
                "none"
            }

        val confidenceDescription = "%.1f · %.1f".format(statistics.latencyConfidence, statistics.throughputConfidence)

        val path = pathState.wrappedValue
        val pathComponents = mutableListOf(interfaceDescription(path.interfaceType))
        if (path.isConstrained) pathComponents.add("constrained")
        if (path.isExpensive) pathComponents.add("expensive")
        if (path.interfaceType == NetworkInterfaceType.CELLULAR) pathComponents.add(path.radioTechnology.rawValue)

        val socketDescription =
            if (hasStartedConnectionObserver.wrappedValue) {
                val reconnectSuffix =
                    connectionStabilityObserver.lastReconnectDuration?.let { " · reconnect ${"%.1f".format(it)}s" } ?: ""
                connectionStabilityObserver.connectedStateDescription + reconnectSuffix
            } else {
                "unattached"
            }

        val probeDescription =
            if (configuration.probeConfiguration == null) {
                "disabled"
            } else {
                prober.wrappedValue?.statsDescription ?: "enabled, no attempts"
            }
        val transferDescription = statistics.lastTransferBytesPerSecond?.let { formattedBytesPerSecond(it) } ?: "none"

        val failuresPercent = (statistics.failureFraction * PERCENT).toInt()
        val flapCount = "%.1f".format(statistics.flapCount)
        return listOf(
            "Score: $scoreDescription",
            "",
            "Latency: $latencyDescription",
            "Throughput: $throughputDescription",
            "Confidence: $confidenceDescription",
            "",
            "Failures: $failuresPercent% · Flaps: $flapCount · Stalls: ${statistics.stallCount}",
            "",
            "Path: ${pathComponents.joinToString(" · ")}",
            "Socket: $socketDescription",
            "Transfer: $transferDescription",
            "",
            "Probing: $probeDescription",
        ).joinToString("\n")
    }

    private fun formattedSeconds(seconds: Double): String =
        if (seconds < 1) "${(seconds * MILLIS_PER_SECOND).toInt()} ms" else "%.2f s".format(seconds)

    private fun formattedBytesPerSecond(bytesPerSecond: Double): String =
        when {
            bytesPerSecond >= BYTES_PER_MEGABYTE -> "%.1f MB/s".format(bytesPerSecond / BYTES_PER_MEGABYTE)
            bytesPerSecond >= BYTES_PER_KILOBYTE -> "%.1f KB/s".format(bytesPerSecond / BYTES_PER_KILOBYTE)
            else -> "%.0f B/s".format(bytesPerSecond)
        }

    private fun interfaceDescription(type: NetworkInterfaceType?): String =
        when (type) {
            NetworkInterfaceType.CELLULAR -> "Cellular"
            NetworkInterfaceType.LOOPBACK -> "Loopback"
            NetworkInterfaceType.OTHER -> "Other"
            NetworkInterfaceType.WIFI -> "Wi-Fi"
            NetworkInterfaceType.WIRED_ETHERNET -> "Ethernet"
            NetworkInterfaceType.UNKNOWN, null -> "unknown"
        }

    // Lazily attaches the connection stability observer on the
    // first database latency sample – evidence the app actually
    // uses the realtime database. An observer attached eagerly
    // would itself keep the realtime connection alive.
    private fun startConnectionStabilityMonitoringIfNeeded() {
        if (!Networking.config.networkHealthConfiguration.isConnectionStabilityMonitoringEnabled) return
        val shouldStart =
            hasStartedConnectionObserver.withValue {
                if (it.value) {
                    false
                } else {
                    it.value = true
                    true
                }
            }
        if (shouldStart) connectionStabilityObserver.start()
    }

    private fun refreshPath() {
        runCatching {
            val network = connectivityManager.activeNetwork ?: return
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return
            handlePathUpdate(capabilities)
        }
    }

    // MARK: - Auxiliary

    private fun applyPathState(newState: PathState) {
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

    private fun createProberIfNeeded(): NetworkHealthProber =
        prober.withValue { reference ->
            reference.value ?: NetworkHealthProber(
                isOnlineProvider = ::isOnline,
                onEvent = ::record,
                onLatencySample = { submitLatencySample(it, isCensored = false) },
                pathStateProvider = { pathState.wrappedValue },
            ).also { reference.value = it }
        }

    private fun handlePathUpdate(capabilities: NetworkCapabilities) {
        applyPathState(
            PathState(
                interfaceType = interfaceType(capabilities),
                isConstrained = isConstrained(capabilities),
                isExpensive = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                radioTechnology = RadioTechnology.current,
            ),
        )
    }

    private fun handleRadioTechnologyChange() {
        val radioTechnology = RadioTechnology.current
        val didChange =
            pathState.withValue {
                if (it.value.radioTechnology == radioTechnology) {
                    false
                } else {
                    it.value = it.value.copy(radioTechnology = radioTechnology)
                    true
                }
            }

        if (!didChange) return
        updateHealth { estimator.computeHealth(estimatorContext) }
    }

    /**
     * Whether the active path is bandwidth-constrained: Data Saver is
     * restricting background data, or (on API 35+) the path reports
     * itself bandwidth-constrained.
     */
    private fun isConstrained(capabilities: NetworkCapabilities): Boolean {
        val isDataSaverEnabled =
            runCatching {
                connectivityManager.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
            }.getOrDefault(false)

        val isBandwidthConstrained =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED)

        return isDataSaverEnabled || isBandwidthConstrained
    }

    private fun interfaceType(capabilities: NetworkCapabilities): NetworkInterfaceType =
        when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkInterfaceType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkInterfaceType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkInterfaceType.WIRED_ETHERNET
            else -> NetworkInterfaceType.OTHER
        }

    /**
     * A Boolean value that indicates whether the device currently
     * has network connectivity.
     */
    fun isOnline(): Boolean =
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
        Networking.config.networkHealthConfiguration.probeConfiguration ?: return
        val prober = createProberIfNeeded()

        probeScope.launch {
            if (afterDelayMillis > 0L) delay(afterDelayMillis)
            prober.maybeProbe()
        }
    }

    private fun publish(health: NetworkHealth) {
        val previousTier = _health.wrappedValue.tier
        _health.wrappedValue = health
        SharedState { it.networkHealth }.wrappedValue = health

        if (previousTier == health.tier) return
        Logger.log(
            "Network health transitioned from ${previousTier?.rawValue ?: "unknown"} " +
                "to ${health.tier?.rawValue ?: "unknown"}.",
            domain = LoggerDomain.Networking.health,
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun makeRadioTechnologyCallback(): TelephonyCallback {
        class RadioTechnologyCallback :
            TelephonyCallback(),
            TelephonyCallback.DataConnectionStateListener {
            override fun onDataConnectionStateChanged(
                state: Int,
                networkType: Int,
            ) {
                handleRadioTechnologyChange()
            }
        }

        return RadioTechnologyCallback()
    }

    private fun registerRadioTechnologyObserver() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        runCatching {
            val telephonyManager =
                Networking
                    .requireContext()
                    .getSystemService(TelephonyManager::class.java)

            val callback = makeRadioTechnologyCallback()
            telephonyManager.registerTelephonyCallback(Networking.requireContext().mainExecutor, callback)
            radioTechnologyObserver.wrappedValue = callback
        }
    }

    private fun removeRadioTechnologyObserver() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val observer =
            radioTechnologyObserver.withValue {
                val current = it.value
                it.value = null
                current
            } ?: return

        runCatching {
            Networking
                .requireContext()
                .getSystemService(TelephonyManager::class.java)
                .unregisterTelephonyCallback(observer)
        }
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

    private const val BYTES_PER_KILOBYTE = 1024.0
    private const val BYTES_PER_MEGABYTE = 1_048_576.0
    private const val MILLIS_PER_SECOND = 1000.0
    private const val PERCENT = 100.0
    private const val PROBE_SETTLE_DELAY_MILLIS = 2000L
}
