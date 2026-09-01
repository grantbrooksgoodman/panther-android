//
//  ToastPresenter.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single source of truth for the toast currently being
 * presented.
 *
 * The [Toast.show] and [Toast.hide] façade methods set the current
 * value; the
 * [ToastHost][us.neotechnica.panther.designsystem.modules.foundation.toast.ToastHost]
 * composable renders it. Only one toast is presented at a time;
 * presenting a new toast replaces any current one.
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

    // MARK: - Computed Properties

    /** The toast currently requested for presentation, or `null`. */
    val current: StateFlow<PresentedToast?> = mutableCurrent.asStateFlow()

    // MARK: - Methods

    /** Dismisses the current toast, if any. */
    fun hide() {
        mutableCurrent.value = null
    }

    /**
     * Requests presentation of the given toast. Ignores the
     * request when an identical toast is already on screen.
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

        mutableCurrent.value = PresentedToast(toast, onTap)
    }
}
