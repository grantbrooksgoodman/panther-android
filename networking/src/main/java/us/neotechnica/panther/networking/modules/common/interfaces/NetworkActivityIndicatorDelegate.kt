//
//  NetworkActivityIndicatorDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.interfaces

import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isNetworkActivityOccurring
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthTier
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState

/**
 * An interface for controlling the display of a network
 * activity indicator.
 *
 * Adopt [NetworkActivityIndicatorDelegate] to customize the
 * appearance and behavior of the indicator shown during network
 * operations. Register your implementation with
 * `Networking.config.registerActivityIndicatorDelegate`.
 *
 * The framework provides
 * [DefaultNetworkActivityIndicatorDelegate] for standard
 * behavior.
 */
interface NetworkActivityIndicatorDelegate {
    // MARK: - Computed Properties

    /**
     * The background color of the indicator, or `null` to adopt
     * a system blue color.
     */
    val backgroundColor: Color?
        get() = null

    /**
     * The tint color of the progress view inside the indicator,
     * or `null` to adopt a default.
     */
    val progressViewTintColor: Color?
        get() = null

    /**
     * The action to perform when the indicator is tapped, or
     * `null` to present an alert summarizing the current network
     * health.
     */
    val tapAction: (() -> Unit)?
        get() = null

    // MARK: - Methods

    /**
     * Shows the network activity indicator.
     *
     * The framework calls this method once for each in-flight
     * network operation. Balance every call with a corresponding
     * call to [hide].
     */
    fun show()

    /**
     * Hides the network activity indicator.
     *
     * The framework calls this method once for each network
     * operation that finishes. Each call balances a prior call
     * to [show].
     */
    fun hide()
}

/**
 * A network activity indicator delegate that provides default
 * appearance and behavior.
 */
class DefaultNetworkActivityIndicatorDelegate : NetworkActivityIndicatorDelegate {
    // MARK: - Properties

    /** The tint color of the progress view. The default is white. */
    override val progressViewTintColor: Color = Color.White

    // MARK: - Computed Properties

    /**
     * The background color of the indicator. The default reflects
     * the current network health tier: green for good, orange for
     * fair, red for poor, and `null` (transparent) when health is
     * unknown.
     */
    override val backgroundColor: Color?
        get() =
            when (Networking.config.healthDelegate.health.tier) {
                NetworkHealthTier.FAIR -> ORANGE
                NetworkHealthTier.GOOD -> GREEN
                NetworkHealthTier.POOR -> RED
                null -> null
            }

    // MARK: - NetworkActivityIndicatorDelegate Conformance

    /**
     * Increments the count of in-flight network operations,
     * showing the network activity indicator if it is not already
     * visible.
     *
     * Balance every call with a corresponding call to [hide]. The
     * indicator remains visible until every in-flight operation
     * has been balanced.
     */
    override fun show() {
        activityReferenceCount.withValue { it.value += 1 }
        synchronizeIndicatorVisibility()
    }

    /**
     * Decrements the count of in-flight network operations,
     * hiding the network activity indicator when no operations
     * remain.
     *
     * Each call balances a prior call to [show]. Calls that would
     * drive the count below zero have no effect.
     */
    override fun hide() {
        activityReferenceCount.withValue { it.value = maxOf(0, it.value - 1) }
        synchronizeIndicatorVisibility()
    }

    // MARK: - Companion

    private companion object {
        private val activityReferenceCount = LockIsolated(0)

        private val GREEN = Color(0xFF34C759)
        private val ORANGE = Color(0xFFFF9500)
        private val RED = Color(0xFFFF3B30)

        private fun synchronizeIndicatorVisibility() {
            SharedState { it.isNetworkActivityOccurring }.wrappedValue = activityReferenceCount.wrappedValue > 0
        }
    }
}
