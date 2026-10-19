//
//  ConnectionStatusService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.modules.content.user.models.ConnectionStatusServiceEffectID
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Tracks network reachability and runs registered effects when
 * network connectivity changes.
 *
 * Registered effects run both when connectivity is lost and when it
 * is restored; guard on [isOnline] within the effect to react to a
 * single direction. The outbox retry pipeline registers an effect
 * here so failed sends are retried the moment the device comes back
 * online, and the offline-mode toast registers one so it appears the
 * moment connectivity drops.
 */
object ConnectionStatusService {
    // MARK: - Properties

    private val awaitingConnectionRestoration = LockIsolated(false)
    private val online = LockIsolated(true)
    private val uponConnectionChanged = LockIsolated(mapOf<ConnectionStatusServiceEffectID, () -> Unit>())

    private var connectivityManager: ConnectivityManager? = null
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    /**
     * Whether the device currently has a validated internet
     * connection.
     */
    val isOnline: Boolean
        get() = online.wrappedValue

    // MARK: - Initialization

    /** Begins observing network reachability. */
    fun initialize(context: Context) {
        val manager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
        connectivityManager = manager
        online.wrappedValue = manager.hasInternet()
        awaitingConnectionRestoration.wrappedValue = !online.wrappedValue

        // The callbacks arrive on a connectivity thread; the effects
        // run on the main thread.
        manager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = post(manager.getNetworkCapabilities(network).hasInternet())

                override fun onLost(network: Network) = post(manager.hasInternet())

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) = post(networkCapabilities.hasInternet())

                private fun post(isOnline: Boolean) {
                    mainScope.launch { reachabilityDidChange(isOnline) }
                }
            },
        )
    }

    // MARK: - Effects

    /**
     * Registers an effect to run whenever connection status changes.
     *
     * Registering a new effect with the same identifier replaces the
     * existing one.
     *
     * **Warning:** the effect runs perpetually, upon each change in
     * connection status. Call [removeEffect] or [clearAllEffects] if
     * this is not the desired behavior.
     */
    fun addEffectUponConnectionChanged(
        id: ConnectionStatusServiceEffectID,
        effect: () -> Unit,
    ) {
        uponConnectionChanged.withValue { it.value = it.value + (id to effect) }
    }

    /** Removes every registered effect. */
    fun clearAllEffects() { // NIT: Unused on Android.
        uponConnectionChanged.wrappedValue = emptyMap()
    }

    /** Removes the effect registered under the given identifier. */
    fun removeEffect(id: ConnectionStatusServiceEffectID) {
        uponConnectionChanged.withValue { it.value = it.value - id }
    }

    // MARK: - Auxiliary

    /**
     * Records a reachability change and runs the effects: on every
     * offline event, and once when the connection is restored.
     */
    internal fun reachabilityDidChange(isOnline: Boolean) {
        online.wrappedValue = isOnline
        if (!isOnline) {
            runEffects()
            awaitingConnectionRestoration.wrappedValue = true
            return
        }

        if (!awaitingConnectionRestoration.wrappedValue) return
        runEffects()
        awaitingConnectionRestoration.wrappedValue = false
    }

    private fun runEffects() {
        // Snapshot the registry first so an effect that registers or
        // removes another does not disturb the iteration.
        uponConnectionChanged.wrappedValue.values
            .toList()
            .forEach { it() }
    }

    private fun ConnectivityManager.hasInternet(): Boolean = getNetworkCapabilities(activeNetwork).hasInternet()

    private fun NetworkCapabilities?.hasInternet(): Boolean {
        val capabilities = this ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
