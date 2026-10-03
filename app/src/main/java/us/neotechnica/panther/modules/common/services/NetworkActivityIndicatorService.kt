//
//  NetworkActivityIndicatorService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.flow.StateFlow
import us.neotechnica.panther.bundle.networkActivityOccurred
import us.neotechnica.panther.networking.modules.common.interfaces.DefaultNetworkActivityIndicatorDelegate
import us.neotechnica.panther.networking.modules.common.interfaces.NetworkActivityIndicatorDelegate
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent

/**
 * The delegate that controls the network activity indicator.
 *
 * The service forwards presentation to the framework's default
 * indicator behavior and notifies observers whenever network activity
 * occurs.
 */
object NetworkActivityIndicatorService : NetworkActivityIndicatorDelegate {
    // MARK: - Properties

    private val defaultNetworkActivityIndicatorDelegate = DefaultNetworkActivityIndicatorDelegate()
    private val networkActivityOccurred = SharedEvent { it.networkActivityOccurred }

    // MARK: - Computed Properties

    /** A stream that emits whether any network operation is in flight. */
    val isActive: StateFlow<Boolean>
        get() = defaultNetworkActivityIndicatorDelegate.isActive

    /** The ARGB background color reflecting the current network health tier. */
    override val backgroundColor: Int?
        get() = defaultNetworkActivityIndicatorDelegate.backgroundColor

    /** The ARGB tint color of the progress indicator. */
    override val progressViewTintColor: Int?
        get() = defaultNetworkActivityIndicatorDelegate.progressViewTintColor

    /** The action to perform when the indicator is tapped, or `null` for the default summary. */
    override val tapAction: (() -> Unit)?
        get() = defaultNetworkActivityIndicatorDelegate.tapAction

    // MARK: - NetworkActivityIndicatorDelegate Conformance

    /** Hides the network activity indicator. */
    override fun hide() {
        defaultNetworkActivityIndicatorDelegate.hide()
    }

    /**
     * Shows the network activity indicator and notifies observers that
     * network activity occurred.
     */
    override fun show() {
        defaultNetworkActivityIndicatorDelegate.show()
        networkActivityOccurred.wrappedValue.send(Unit)
    }
}
