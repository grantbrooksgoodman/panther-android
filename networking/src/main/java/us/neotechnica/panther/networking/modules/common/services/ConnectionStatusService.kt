//
//  ConnectionStatusService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

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

    private val online = LockIsolated(true)
    private val uponConnectionChanged = LockIsolated(mapOf<String, () -> Unit>())

    private var connectivityManager: ConnectivityManager? = null

    // MARK: - Computed Properties

    /** Whether the device currently has a validated internet connection. */
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

        manager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = setOnline(true)

                override fun onLost(network: Network) = setOnline(manager.hasInternet())

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) = setOnline(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                )
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
        id: String,
        effect: () -> Unit,
    ) {
        uponConnectionChanged.withValue { it.value = it.value + (id to effect) }
    }

    /** Removes every registered effect. */
    fun clearAllEffects() {
        uponConnectionChanged.wrappedValue = emptyMap()
    }

    /** Removes the effect registered under the given identifier. */
    fun removeEffect(id: String) {
        uponConnectionChanged.withValue { it.value = it.value - id }
    }

    // MARK: - Auxiliary

    private fun setOnline(value: Boolean) {
        val wasOnline = online.wrappedValue
        online.wrappedValue = value
        if (value != wasOnline) {
            Logger.log("Connection status changed (online: $value); running effects.")
            uponConnectionChanged.wrappedValue.values.forEach { it() }
        }
    }

    private fun ConnectivityManager.hasInternet(): Boolean {
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
