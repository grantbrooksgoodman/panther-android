//
//  HUDPresenter.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single source of truth for the heads-up display currently
 * being presented.
 *
 * The [HUD.showSuccess][us.neotechnica.panther.designsystem.modules.foundation.hud.HUD.showSuccess]
 * façade sets the current value; the [HUDHost] composable renders it.
 */
object HUDPresenter {
    // MARK: - Properties

    private val mutableToken = MutableStateFlow<Long?>(null)

    // MARK: - Computed Properties

    /** A token identifying the current success display, or `null`. */
    val token: StateFlow<Long?> = mutableToken.asStateFlow()

    // MARK: - Methods

    /** Dismisses the current display, if any. */
    fun hide() {
        mutableToken.value = null
    }

    /** Requests presentation of the success display. */
    fun showSuccess() {
        mutableToken.value = System.nanoTime()
    }
}
