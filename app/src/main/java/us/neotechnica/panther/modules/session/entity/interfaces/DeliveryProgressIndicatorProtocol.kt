//
//  DeliveryProgressIndicatorProtocol.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.interfaces

/**
 * A type that displays the progress of a message delivery.
 *
 * **Important:** access every member on the main thread.
 */
interface DeliveryProgressIndicator {
    /**
     * Advances the displayed delivery progress by the given amount.
     *
     * @param by The amount to add to the current progress.
     */
    fun incrementDeliveryProgress(by: Float)

    /** Starts animating the delivery progress indicator. */
    fun startAnimatingDeliveryProgress()

    /**
     * Completes the delivery progress indicator, then fades it out
     * and resets it.
     */
    fun stopAnimatingDeliveryProgress()
}
