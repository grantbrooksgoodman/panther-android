//
//  HUD.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.HUDDelegate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A brief, centered heads-up display shown over the current content.
 *
 * Use [HUD] to give the user lightweight feedback during short
 * operations:
 *
 * ```kotlin
 * HUD.showProgress(text = "Saving…")
 * // ... perform work ...
 * HUD.showSuccess(text = "Saved")
 * ```
 *
 * For longer operations that should prevent interaction, pass
 * `isModal = true` to [showProgress]. Always call [hide] when the
 * operation finishes to restore interaction.
 */
object HUD {
    // MARK: - Init

    init {
        AppSubsystem.delegates.registerHUDDelegate(HUDDelegateAdapter)
    }

    // MARK: - Types

    /** The image displayed inside a HUD flash. */
    enum class HUDImage {
        /** A success checkmark. */
        SUCCESS,

        /** An exclamation mark. */
        EXCLAMATION,
    }

    // MARK: - Computed Properties

    /** Whether a modal display is currently blocking user interaction. */
    val isBlockingUserInteraction: Boolean
        get() = HUDPresenter.isBlockingUserInteraction

    // MARK: - Methods

    /**
     * Briefly displays the HUD with an image and optional text,
     * dismissing itself automatically.
     *
     * @param text An optional message to display below the image.
     * @param image The image to show in the HUD.
     */
    fun flash(
        text: String? = null,
        image: HUDImage,
    ) {
        HUDPresenter.flash(text, image)
    }

    /**
     * Hides the HUD and restores user interaction.
     *
     * @param after The duration to wait before removing the HUD. The
     *   default is 250 milliseconds.
     */
    fun hide(after: Duration = 250.milliseconds) {
        HUDPresenter.hide(after)
    }

    /**
     * Shows a spinning activity indicator.
     *
     * @param text An optional message to display below the spinner.
     * @param after An optional duration to wait before showing the
     *   HUD, or `null` to show immediately.
     * @param isModal Whether the display blocks interaction with the
     *   underlying content while visible. The default is `false`.
     */
    fun showProgress(
        text: String? = null,
        after: Duration? = null,
        isModal: Boolean = false,
    ) {
        HUDPresenter.showProgress(text, after, isModal)
    }

    /**
     * Shows a success indicator with an optional message, dismissing
     * itself automatically.
     *
     * @param text An optional message to display below the success
     *   indicator.
     */
    fun showSuccess(text: String? = null) {
        HUDPresenter.showSuccess(text)
    }
}

private object HUDDelegateAdapter : HUDDelegate {
    override fun hide() {
        HUD.hide()
    }

    override fun showProgress(isModal: Boolean) {
        HUD.showProgress(isModal = isModal)
    }
}
