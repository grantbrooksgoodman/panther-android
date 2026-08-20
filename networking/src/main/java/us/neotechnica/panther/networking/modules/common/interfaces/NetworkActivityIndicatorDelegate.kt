//
//  NetworkActivityIndicatorDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.interfaces

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * positive.
 */
class DefaultNetworkActivityIndicatorDelegate : NetworkActivityIndicatorDelegate {
    // MARK: - Properties

    private val activeCount = AtomicInteger(0)
    private val mutableIsActive = MutableStateFlow(false)

    // MARK: - Computed Properties

    /** A stream that emits whether any network operation is in flight. */
    val isActive: StateFlow<Boolean> = mutableIsActive.asStateFlow()

    // MARK: - NetworkActivityIndicatorDelegate Conformance

    override fun show() {
        activeCount.incrementAndGet()
        mutableIsActive.value = activeCount.get() > 0
    }

    override fun hide() {
        activeCount.updateAndGet { if (it > 0) it - 1 else 0 }
        mutableIsActive.value = activeCount.get() > 0
    }
}
