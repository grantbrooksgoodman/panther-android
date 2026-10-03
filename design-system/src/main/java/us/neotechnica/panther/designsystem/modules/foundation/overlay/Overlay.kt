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
    private val mutableAlpha = MutableStateFlow(DEFAULT_ALPHA)
    private val mutableShowsActivityIndicator = MutableStateFlow(true)

    // MARK: - Computed Properties

    /** Whether the overlay is currently shown. */
    val isVisible: StateFlow<Boolean> = mutableIsVisible.asStateFlow()

    /** The opacity of the overlay's dimming scrim. */
    val alpha: StateFlow<Float> = mutableAlpha.asStateFlow()

    /** Whether the overlay shows an activity indicator. */
    val showsActivityIndicator: StateFlow<Boolean> = mutableShowsActivityIndicator.asStateFlow()

    // MARK: - Methods

    /**
     * Shows the overlay, mirroring the iOS `CoreKit.UI.addOverlay`.
     *
     * @param alpha The opacity of the dimming scrim.
     * @param showsActivityIndicator Whether to show the spinner (pass
     *   `false` when another control, such as a progress alert, reports
     *   progress instead).
     */
    fun show(
        alpha: Float = DEFAULT_ALPHA,
        showsActivityIndicator: Boolean = true,
    ) {
        mutableAlpha.value = alpha
        mutableShowsActivityIndicator.value = showsActivityIndicator
        mutableIsVisible.value = true
    }

    /** Hides the overlay. */
    fun hide() {
        mutableIsVisible.value = false
    }

    // MARK: - Companion

    private const val DEFAULT_ALPHA = 1f
}
