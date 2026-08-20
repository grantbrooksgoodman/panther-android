//
//  Overlay.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A global, dimming activity overlay, standing in for the iOS
 * `CoreKit.UI.addOverlay`/`removeOverlay`.
 *
 * Toggle it from anywhere; the
 * [OverlayHost][us.neotechnica.panther.designsystem.modules.foundation.overlay.OverlayHost]
 * composable renders it over the current screen and blocks input while
 * visible.
 */
object Overlay {
    // MARK: - Properties

    private val mutableIsVisible = MutableStateFlow(false)

    // MARK: - Computed Properties

    /** Whether the overlay is currently shown. */
    val isVisible: StateFlow<Boolean> = mutableIsVisible.asStateFlow()

    // MARK: - Methods

    /** Shows the overlay. */
    fun show() {
        mutableIsVisible.value = true
    }

    /** Hides the overlay. */
    fun hide() {
        mutableIsVisible.value = false
    }
}
