//
//  NetworkActivityIndicatorDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.interfaces

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.networking.modules.health.extensions.networkHealth
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthTier
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import java.util.concurrent.atomic.AtomicInteger

/**
 * An interface for reflecting in-flight network activity in the
 * UI.
 *
 * The database and auth layers call [show] when an operation
 * begins and [hide] when it ends. Calls are balanced with a
 * reference count, so the indicator is visible whenever at least
 * one operation is in flight.
 */
interface NetworkActivityIndicatorDelegate {
    // MARK: - Computed Properties

    /**
     * The ARGB background color of the indicator, or `null` to adopt a
     * default. Colors are ARGB integers because this module has no
     * Compose dependency; the renderer converts them.
     */
    val backgroundColor: Int?
        get() = null

    /** The ARGB tint color of the progress indicator, or `null`. */
    val progressViewTintColor: Int?
        get() = null

    /**
     * The action to perform when the indicator is tapped, or `null` to
     * present a summary of the current network health.
     */
    val tapAction: (() -> Unit)?
        get() = null

    // MARK: - Methods

    /** Signals that a network operation has begun. */
    fun show()

    /** Signals that a network operation has ended. */
    fun hide()
}

/**
 * The default reference-counting activity indicator delegate.
 *
 * Observe [isActive] to drive a UI indicator; it is `true`
 * whenever the balanced count of [show] over [hide] calls is
 * positive. The reference count is static, so every instance
 * shares the same balance.
 */
class DefaultNetworkActivityIndicatorDelegate : NetworkActivityIndicatorDelegate {
    // MARK: - Computed Properties

    /** A stream that emits whether any network operation is in flight. */
    val isActive: StateFlow<Boolean> = mutableIsActive.asStateFlow()

    /** The default tint color of the progress view, white. */
    override val progressViewTintColor: Int = WHITE_ARGB

    /**
     * The background color reflecting the current network health tier:
     * green for good, orange for fair, red for poor, and `null`
     * (transparent) when health is unknown.
     */
    override val backgroundColor: Int?
        get() =
            when (SharedState { it.networkHealth }.wrappedValue.tier) {
                NetworkHealthTier.GOOD -> GREEN_ARGB
                NetworkHealthTier.FAIR -> ORANGE_ARGB
                NetworkHealthTier.POOR -> RED_ARGB
                null -> null
            }

    // MARK: - NetworkActivityIndicatorDelegate Conformance

    override fun show() {
        activityReferenceCount.incrementAndGet()
        synchronizeIndicatorVisibility()
    }

    override fun hide() {
        activityReferenceCount.updateAndGet { if (it > 0) it - 1 else 0 }
        synchronizeIndicatorVisibility()
    }

    // MARK: - Companion

    private companion object {
        private val activityReferenceCount = AtomicInteger(0)
        private val mutableIsActive = MutableStateFlow(false)

        private const val WHITE_ARGB = 0xFFFFFFFF.toInt()
        private const val GREEN_ARGB = 0xFF34C759.toInt()
        private const val ORANGE_ARGB = 0xFFFF9500.toInt()
        private const val RED_ARGB = 0xFFFF3B30.toInt()

        private fun synchronizeIndicatorVisibility() {
            mutableIsActive.value = activityReferenceCount.get() > 0
        }
    }
}
