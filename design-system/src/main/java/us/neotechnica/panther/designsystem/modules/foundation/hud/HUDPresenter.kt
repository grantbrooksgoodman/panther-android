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
 * The [HUD] façade sets the current value; the [HUDHost] composable
 * renders it.
 */
object HUDPresenter {
    // MARK: - Types

    /** A heads-up display awaiting or undergoing presentation. */
    sealed interface Presentation {
        /**
         * A progress spinner shown until dismissed with [HUD.hide].
         *
         * @property isModal Whether the display blocks interaction
         *   with the underlying content while visible.
         */
        data class Progress(
            val isModal: Boolean,
        ) : Presentation

        /**
         * A self-dismissing success indicator.
         *
         * @property token A value identifying the request, so that
         *   repeated requests restart the display.
         */
        data class Success(
            val token: Long,
        ) : Presentation
    }

    // MARK: - Properties

    private val mutablePresentation = MutableStateFlow<Presentation?>(null)

    // MARK: - Computed Properties

    /** The heads-up display currently presented, or `null`. */
    val presentation: StateFlow<Presentation?> = mutablePresentation.asStateFlow()

    // MARK: - Methods

    /** Dismisses the current display, if any. */
    fun hide() {
        mutablePresentation.value = null
    }

    /** Requests presentation of the progress display. */
    fun showProgress(isModal: Boolean) {
        mutablePresentation.value = Presentation.Progress(isModal)
    }

    /** Requests presentation of the success display. */
    fun showSuccess() {
        mutablePresentation.value = Presentation.Success(System.nanoTime())
    }
}
