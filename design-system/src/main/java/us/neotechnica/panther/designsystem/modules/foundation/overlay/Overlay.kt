//
//  Overlay.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.overlay

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.designsystem.modules.foundation.services.UserInteractionBlocker
import us.neotechnica.panther.designsystem.modules.foundation.views.root.RootWindowStatus
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import kotlin.time.Duration.Companion.milliseconds

/**
 * A global, dimming activity overlay.
 *
 * Toggle it from anywhere with [addOverlay] and [removeOverlay]; the
 * [OverlayHost][us.neotechnica.panther.designsystem.modules.foundation.overlay.OverlayHost]
 * composable renders it over the current screen. While a modal
 * overlay is visible, input to the underlying UI is blocked, the
 * build-info overlay is hidden, and interactive content – the
 * toast, any presented alert or sheet, and the keyboard – is
 * dismissed; alerts and toasts requested meanwhile wait until the
 * overlay is removed.
 */
object Overlay {
    // MARK: - Types

    /** The appearance of the overlay's activity indicator. */
    class OverlayActivityIndicatorConfiguration(
        /** The indicator's color. */
        val color: Color,
    ) {
        // MARK: - Companion

        companion object {
            /** A large white activity indicator. */
            val largeWhite = OverlayActivityIndicatorConfiguration(Color.White)
        }
    }

    // MARK: - Properties

    private val blockingUserInteraction = LockIsolated(false)
    private val mutableActivityIndicator = MutableStateFlow<OverlayActivityIndicatorConfiguration?>(null)
    private val mutableAlpha = MutableStateFlow(DEFAULT_ALPHA)
    private val mutableAnimatesRemoval = MutableStateFlow(true)
    private val mutableBackgroundColor = MutableStateFlow(Color.Black)
    private val mutableIsModal = MutableStateFlow(true)
    private val mutableIsVisible = MutableStateFlow(false)

    // MARK: - Computed Properties

    /** The activity indicator's configuration, or `null` when hidden. */
    val activityIndicator: StateFlow<OverlayActivityIndicatorConfiguration?> = mutableActivityIndicator.asStateFlow()

    /** The opacity of the overlay's dimming scrim. */
    val alpha: StateFlow<Float> = mutableAlpha.asStateFlow()

    /** Whether the overlay fades out when removed. */
    val animatesRemoval: StateFlow<Boolean> = mutableAnimatesRemoval.asStateFlow()

    /** The overlay's scrim color. */
    val backgroundColor: StateFlow<Color> = mutableBackgroundColor.asStateFlow()

    /** Whether the overlay blocks input to the underlying UI. */
    val isModal: StateFlow<Boolean> = mutableIsModal.asStateFlow()

    /** Whether the overlay is currently shown. */
    val isVisible: StateFlow<Boolean> = mutableIsVisible.asStateFlow()

    internal val isBlockingUserInteraction: Boolean
        get() = blockingUserInteraction.wrappedValue

    // MARK: - Methods

    /**
     * Shows the overlay.
     *
     * @param alpha The opacity of the dimming scrim.
     * @param activityIndicator The activity indicator to show, or
     *   `null` to omit it (pass `null` when another control, such as
     *   a progress alert, reports progress instead).
     * @param backgroundColor The scrim color.
     * @param isModal Whether the overlay blocks input to the
     *   underlying UI while visible.
     */
    fun addOverlay(
        alpha: Float = DEFAULT_ALPHA,
        activityIndicator: OverlayActivityIndicatorConfiguration? = null,
        backgroundColor: Color = Color.Black,
        isModal: Boolean = true,
    ) {
        if (isModal) {
            blockingUserInteraction.wrappedValue = true
            UserInteractionBlocker.blockUserInteraction()
            RootWindowStatus.setRootOverlayWindowAlpha(0f)
        }

        mutableActivityIndicator.value = activityIndicator
        mutableAlpha.value = alpha
        mutableAnimatesRemoval.value = true
        mutableBackgroundColor.value = backgroundColor
        mutableIsModal.value = isModal
        mutableIsVisible.value = true
    }

    /**
     * Hides the overlay.
     *
     * Input to the underlying UI and the build-info overlay return
     * once the overlay has gone – immediately, or after the fade.
     *
     * @param animated Whether the overlay fades out over 0.2
     *   seconds. Pass `false` to remove it immediately.
     */
    fun removeOverlay(animated: Boolean = true) {
        mutableAnimatesRemoval.value = animated
        mutableIsVisible.value = false

        if (!animated) return removeViews()
        Task.delayed(by = FADE_DURATION) { removeViews() }
    }

    // MARK: - Auxiliary

    private fun removeViews() {
        blockingUserInteraction.wrappedValue = false
        UserInteractionBlocker.unblockUserInteraction()
        RootWindowStatus.setRootOverlayWindowAlpha(1f)
    }

    // MARK: - Companion

    private const val DEFAULT_ALPHA = 1f
    private val FADE_DURATION = 200.milliseconds
}
