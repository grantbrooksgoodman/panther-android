//
//  ToastPresenter.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.foundation.services.UserInteractionBlocker
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The single source of truth for the toast currently being
 * presented.
 *
 * The [Toast.show] and [Toast.hide] façade methods set the
 * current value; the
 * [ToastHost][us.neotechnica.panther.designsystem.modules.foundation.toast.ToastHost]
 * composable renders it. Only one toast is presented at a time;
 * presenting another toast defers it until the current one
 * dismisses and no modal display blocks interaction.
 */
object ToastPresenter {
    // MARK: - Types

    /** A toast paired with its optional tap handler. */
    data class PresentedToast(
        val toast: Toast,
        val onTap: (() -> Unit)?,
    )

    // MARK: - Properties

    private val mutableCurrent = MutableStateFlow<PresentedToast?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    /** The toast currently requested for presentation, or `null`. */
    val current: StateFlow<PresentedToast?> = mutableCurrent.asStateFlow()

    // MARK: - Methods

    /** Dismisses the currently visible toast, if any. */
    fun hide() {
        mutableCurrent.value = null
    }

    /**
     * Requests presentation of the given toast, deferring it while
     * another toast is showing or a modal display blocks
     * interaction. Ignores the request when an identical toast is
     * already on screen.
     */
    fun show(
        toast: Toast,
        onTap: (() -> Unit)?,
    ) {
        val existing = mutableCurrent.value
        if (existing?.toast == toast &&
            (existing.onTap == null) == (onTap == null)
        ) {
            return
        }

        val isBlocked = UserInteractionBlocker.isBlockingUserInteraction
        if (isBlocked || mutableCurrent.value != null) {
            scope.launch {
                delay(if (isBlocked) BLOCKED_RETRY_INTERVAL else SHOWING_RETRY_INTERVAL)
                show(toast, onTap)
            }

            return
        }

        mutableCurrent.value = PresentedToast(toast, onTap)
    }

    // MARK: - Companion

    private val BLOCKED_RETRY_INTERVAL = 100.milliseconds
    private val SHOWING_RETRY_INTERVAL = 1.seconds
}
