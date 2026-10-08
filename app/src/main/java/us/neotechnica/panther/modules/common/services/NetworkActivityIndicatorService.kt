//
//  NetworkActivityIndicatorService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.bundle.networkActivityOccurred
import us.neotechnica.panther.networking.modules.common.interfaces.DefaultNetworkActivityIndicatorDelegate
import us.neotechnica.panther.networking.modules.common.interfaces.NetworkActivityIndicatorDelegate
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvent

/**
 * The delegate that styles and controls the network activity
 * indicator.
 *
 * The service forwards presentation to the framework's default
 * indicator behavior and notifies observers whenever network
 * activity occurs.
 */
object NetworkActivityIndicatorService : NetworkActivityIndicatorDelegate {
    // MARK: - Properties

    private val defaultNetworkActivityIndicatorDelegate = DefaultNetworkActivityIndicatorDelegate()
    private val networkActivityOccurred = SharedEvent { it.networkActivityOccurred }

    // MARK: - Computed Properties

    /** The background color of the network activity indicator. */
    override val backgroundColor: Color?
        get() = defaultNetworkActivityIndicatorDelegate.backgroundColor

    /** The tint color of the network activity indicator's progress view. */
    override val progressViewTintColor: Color
        get() = Color.White

    // MARK: - NetworkActivityIndicatorDelegate Conformance

    /** Hides the network activity indicator. */
    override fun hide() {
        defaultNetworkActivityIndicatorDelegate.hide()
    }

    /**
     * Shows the network activity indicator and notifies observers
     * that network activity occurred.
     */
    override fun show() {
        defaultNetworkActivityIndicatorDelegate.show()
        networkActivityOccurred.wrappedValue.send(Unit)
    }
}
