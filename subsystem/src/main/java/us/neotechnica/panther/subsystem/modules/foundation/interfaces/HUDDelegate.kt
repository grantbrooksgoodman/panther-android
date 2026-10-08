//
//  HUDDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

/**
 * A type that presents and dismisses a heads-up display on
 * behalf of the subsystem.
 *
 * Conform to [HUDDelegate] to let subsystem services show
 * progress over the app's content – for example, while a
 * long-running translation is in flight. Register the
 * conforming instance through
 * [AppSubsystem.Delegates.registerHUDDelegate][us.neotechnica.panther.subsystem.AppSubsystem.Delegates.registerHUDDelegate].
 */
interface HUDDelegate {
    // MARK: - Methods

    /** Hides the heads-up display. */
    fun hide()

    /**
     * Shows a progress heads-up display.
     *
     * @param isModal Whether the display blocks interaction with
     *   the underlying content while visible.
     */
    fun showProgress(isModal: Boolean)
}
