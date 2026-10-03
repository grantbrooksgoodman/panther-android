//
//  ConnectionStabilityObserver.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.services

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthEvent
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * A passive observer of the Firebase realtime client's own connection
 * state, reported at the special `.info/connected` location, mirroring
 * the iOS `ConnectionStabilityObserver`.
 *
 * Unexpected socket drops in the foreground (flaps) are reported as
 * [NetworkHealthEvent.ConnectionFlap]; reconnections as
 * [NetworkHealthEvent.ConnectionRestored]. Drops that coincide with
 * going offline, backgrounding, or the grace period after foregrounding
 * are app lifecycle, not network evidence, and are filtered out.
 *
 * An attached observer keeps the realtime connection alive; attach only
 * once the app has shown evidence of realtime database use.
 *
 * @param isOnlineProvider Supplies whether the device is online.
 * @param onEvent Receives the derived network health events.
 */
internal class ConnectionStabilityObserver(
    private val isOnlineProvider: () -> Boolean,
    private val onEvent: (NetworkHealthEvent) -> Unit,
) {
    // MARK: - Types

    private class MutableState {
        var disconnectedAt: Long? = null
        var foregroundReturnedAt: Long? = null
        var isInBackground: Boolean = false
        var lastReconnectDuration: Double? = null
        var observerListener: ValueEventListener? = null
        var previousConnectedState: Boolean? = null
    }

    // MARK: - Properties

    private val state = LockIsolated(MutableState())

    private val lifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                state.withValue {
                    it.value.foregroundReturnedAt = System.currentTimeMillis()
                    it.value.isInBackground = false
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                state.withValue { it.value.isInBackground = true }
            }
        }

    // MARK: - Computed Properties

    /**
     * A textual description of the realtime client's last reported
     * connection state, or `"unknown"` before the first value arrives.
     */
    val connectedStateDescription: String
        get() =
            state.wrappedValue.previousConnectedState?.let { if (it) "connected" else "disconnected" } ?: "unknown"

    /** The duration, in seconds, of the most recently observed outage, or `null`. */
    val lastReconnectDuration: Double?
        get() = state.wrappedValue.lastReconnectDuration

    // MARK: - Methods

    /** Attaches the `.info/connected` observer and begins tracking transitions. */
    fun start() {
        if (state.wrappedValue.observerListener != null) return

        Logger.log("Attaching realtime connection stability observer.", domain = LoggerDomain.Networking.health)

        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        }

        val listener =
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    handleConnectedStateChange(snapshot.getValue(Boolean::class.java) ?: false)
                }

                override fun onCancelled(error: DatabaseError) = Unit
            }

        connectedReference.addValueEventListener(listener)
        state.withValue { it.value.observerListener = listener }
    }

    /** Removes the `.info/connected` observer and discards transition state. */
    fun stop() {
        val listener =
            state.withValue {
                val listener = it.value.observerListener
                it.value.disconnectedAt = null
                it.value.observerListener = null
                it.value.previousConnectedState = null
                listener
            } ?: return

        Logger.log("Detaching realtime connection stability observer.", domain = LoggerDomain.Networking.health)

        connectedReference.removeEventListener(listener)
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        }
    }

    // MARK: - Auxiliary

    private val connectedReference: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference.child(INFO_CONNECTED_PATH)

    private fun handleConnectedStateChange(isConnected: Boolean) {
        val flapForegroundGraceSeconds = Networking.config.networkHealthConfiguration.flapForegroundGraceSeconds
        val isOnline = isOnlineProvider()
        val now = System.currentTimeMillis()

        val event: NetworkHealthEvent? =
            state.withValue { reference ->
                val mutableState = reference.value

                // The first observed value is initial state, not a transition.
                val previousConnectedState =
                    mutableState.previousConnectedState ?: run {
                        mutableState.previousConnectedState = isConnected
                        return@withValue null
                    }

                if (previousConnectedState == isConnected) return@withValue null
                mutableState.previousConnectedState = isConnected

                if (isConnected) {
                    val disconnectedAt = mutableState.disconnectedAt ?: return@withValue null
                    val duration = (now - disconnectedAt) / MILLIS_PER_SECOND
                    mutableState.disconnectedAt = null
                    mutableState.lastReconnectDuration = duration
                    return@withValue NetworkHealthEvent.ConnectionRestored(duration)
                }

                mutableState.disconnectedAt = now

                // Going offline is the hard-zero path, not a flap; backgrounding deliberately drops
                // the socket; and a drop within the grace period after foregrounding is lifecycle noise.
                if (!isOnline || mutableState.isInBackground) return@withValue null
                val foregroundReturnedAt = mutableState.foregroundReturnedAt
                if (foregroundReturnedAt != null &&
                    (now - foregroundReturnedAt) / MILLIS_PER_SECOND < flapForegroundGraceSeconds
                ) {
                    return@withValue null
                }

                NetworkHealthEvent.ConnectionFlap
            }

        event?.let { onEvent(it) }
    }

    // MARK: - Companion

    private companion object {
        private const val INFO_CONNECTED_PATH = ".info/connected"
        private const val MILLIS_PER_SECOND = 1000.0
    }
}
