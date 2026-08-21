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
 * Tracks network reachability and notifies registered effects when
 * connectivity is restored.
 *
 * The outbox retry pipeline registers an effect here so failed sends are
 * retried the moment the device comes back online.
 */
object ConnectionStatusService {
    // MARK: - Properties

    private val online = LockIsolated(true)
    private val restoredEffects = LockIsolated(mapOf<String, () -> Unit>())

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
     * Registers an effect to run whenever connectivity is restored.
     *
     * Registering a new effect with the same identifier replaces the
     * existing one.
     */
    fun addEffectUponConnectivityRestored(
        id: String,
        effect: () -> Unit,
    ) {
        restoredEffects.withValue { it.value = it.value + (id to effect) }
    }

    // MARK: - Auxiliary

    private fun setOnline(value: Boolean) {
        val wasOnline = online.wrappedValue
        online.wrappedValue = value
        if (value && !wasOnline) {
            Logger.log("Connectivity restored; running restored effects.")
            restoredEffects.wrappedValue.values.forEach { it() }
        }
    }

    private fun ConnectivityManager.hasInternet(): Boolean {
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
