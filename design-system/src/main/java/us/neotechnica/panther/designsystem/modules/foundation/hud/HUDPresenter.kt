//
//  HUDPresenter.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.foundation.services.KeyboardService
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

    private var blockingJob: Job? = null
    private var pendingJob: Job? = null

    // MARK: - Computed Properties

    /** The heads-up display currently presented, or `null`. */
    val presentation: StateFlow<Presentation?> = mutablePresentation.asStateFlow()

    /** Whether a modal display is currently blocking user interaction. */
    val isBlockingUserInteraction: Boolean
        get() = blockingUserInteraction.wrappedValue

    // MARK: - Methods

    /**
     * Dismisses the current display, restoring interaction and
     * starting the 0.15-second dismiss animation immediately.
     *
     * The host's exit animation fades the display out at once; [after]
     * delays the final teardown of any pending show, never the start of
     * the dismissal.
     */
    fun hide(after: Duration = Duration.ZERO) {
        pendingJob?.cancel()
        blockingJob?.cancel()
        blockingJob = null
        blockingUserInteraction.wrappedValue = false
        // Clear the presentation now so the host begins its fade-out
        // immediately rather than keeping the display fully visible for
        // the delay.
        mutablePresentation.value = null
        pendingJob =
            scope.launch {
                delay(after)
            }
    }

    /** Requests presentation of an image flash. */
    fun flash(
        text: String?,
        image: HUD.HUDImage,
    ) {
        pendingJob?.cancel()
        val resolvedText = if (text?.endsWith(".") == true) text.dropLast(1) else text
        present(Presentation.Flash(resolvedText, image, System.nanoTime()), isModal = false)
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
        if (isModal) startBlockingInteractiveContent()
    }

    // While a modal display blocks interaction, repeatedly dismisses
    // interactive content – the toast, any presented alert, and the
    // keyboard – so nothing competes with the display. Root sheets
    // stay presented.
    private fun startBlockingInteractiveContent() {
        if (blockingJob?.isActive == true) return
        blockingJob =
            scope.launch {
                while (blockingUserInteraction.wrappedValue) {
                    Toast.hide()
                    AlertPresenter.dismiss()
                    KeyboardService.resignFirstResponders()
                    delay(BLOCKING_INTERVAL_MILLISECONDS.milliseconds)
                }
            }
    }
}

private const val BLOCKING_INTERVAL_MILLISECONDS = 100L
