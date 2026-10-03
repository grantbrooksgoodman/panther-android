//
//  HUDPresenter.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import kotlin.time.Duration

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
        /** An optional status message shown below the indicator. */
        val text: String?

        /**
         * A progress spinner shown until dismissed with [HUD.hide].
         *
         * @property text An optional status message.
         * @property isModal Whether the display blocks interaction
         *   with the underlying content while visible.
         */
        data class Progress(
            override val text: String?,
            val isModal: Boolean,
        ) : Presentation

        /**
         * A self-dismissing success indicator.
         *
         * @property text An optional status message.
         * @property token A value identifying the request, so that
         *   repeated requests restart the display.
         */
        data class Success(
            override val text: String?,
            val token: Long,
        ) : Presentation

        /**
         * A self-dismissing image flash.
         *
         * @property text An optional status message.
         * @property image The image to show.
         * @property token A value identifying the request, so that
         *   repeated requests restart the display.
         */
        data class Flash(
            override val text: String?,
            val image: HUD.HUDImage,
            val token: Long,
        ) : Presentation
    }

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutablePresentation = MutableStateFlow<Presentation?>(null)
    private val blockingUserInteraction = LockIsolated(false)

    private var pendingJob: Job? = null

    // MARK: - Computed Properties

    /** The heads-up display currently presented, or `null`. */
    val presentation: StateFlow<Presentation?> = mutablePresentation.asStateFlow()

    /** Whether a modal display is currently blocking user interaction. */
    val isBlockingUserInteraction: Boolean
        get() = blockingUserInteraction.wrappedValue

    // MARK: - Methods

    /**
     * Dismisses the current display, restoring interaction
     * immediately and removing the display after [after].
     */
    fun hide(after: Duration = Duration.ZERO) {
        pendingJob?.cancel()
        blockingUserInteraction.wrappedValue = false
        pendingJob =
            scope.launch {
                delay(after)
                mutablePresentation.value = null
            }
    }

    /** Requests presentation of an image flash. */
    fun flash(
        text: String?,
        image: HUD.HUDImage,
    ) {
        pendingJob?.cancel()
        present(Presentation.Flash(text, image, System.nanoTime()), isModal = false)
    }

    /** Requests presentation of the progress display, optionally after a delay. */
    fun showProgress(
        text: String?,
        after: Duration?,
        isModal: Boolean,
    ) {
        pendingJob?.cancel()
        if (after == null) {
            present(Presentation.Progress(text, isModal), isModal)
            return
        }

        pendingJob =
            scope.launch {
                delay(after)
                present(Presentation.Progress(text, isModal), isModal)
            }
    }

    /** Requests presentation of the success display. */
    fun showSuccess(text: String?) {
        pendingJob?.cancel()
        present(Presentation.Success(text, System.nanoTime()), isModal = false)
    }

    // MARK: - Auxiliary

    private fun present(
        presentation: Presentation,
        isModal: Boolean,
    ) {
        blockingUserInteraction.wrappedValue = isModal
        mutablePresentation.value = presentation
    }
}
