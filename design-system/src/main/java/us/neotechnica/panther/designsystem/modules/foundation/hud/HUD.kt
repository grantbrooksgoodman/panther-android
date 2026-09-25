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
 */
object HUD {
    // MARK: - Methods

    /** Presents the success heads-up display. */
    fun showSuccess() {
        HUDPresenter.showSuccess()
    }
}
