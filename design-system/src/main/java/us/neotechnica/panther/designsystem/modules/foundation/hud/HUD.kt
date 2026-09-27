//
//  HUD.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

/**
 * A brief, centered heads-up display shown over the current content.
 *
 * Use [HUD] to confirm that an action succeeded. The display appears
 * with a success mark and dismisses itself after a short interval:
 *
 * ```kotlin
 * HUD.showSuccess()
 * ```
 *
 * For longer operations, present a progress spinner and dismiss it
 * yourself when the work completes. Pass `isModal = true` to block
 * interaction with the underlying content while it is visible:
 *
 * ```kotlin
 * HUD.showProgress(isModal = true)
 * // ... perform work ...
 * HUD.hide()
 * ```
 */
object HUD {
    // MARK: - Methods

    /** Dismisses the currently presented heads-up display. */
    fun hide() {
        HUDPresenter.hide()
    }

    /**
     * Presents the progress heads-up display.
     *
     * @param isModal Whether the display blocks interaction with the
     *   underlying content while visible. The default is `false`.
     */
    fun showProgress(isModal: Boolean = false) {
        HUDPresenter.showProgress(isModal)
    }

    /** Presents the success heads-up display. */
    fun showSuccess() {
        HUDPresenter.showSuccess()
    }
}
