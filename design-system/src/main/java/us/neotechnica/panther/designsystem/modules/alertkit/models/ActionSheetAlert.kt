//
//  ActionSheetAlert.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.suspendCancellableCoroutine
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import kotlin.coroutines.resume

/**
 * A bottom action sheet that asks the user to confirm or cancel an
 * action, standing in for the iOS `AKActionSheet`.
 *
 * [present] resolves to `true` when the user taps the confirm button
 * and `false` when they cancel.
 *
 * **Note:** translation of sheet content is deferred to the reducers
 * that present it, which resolve their strings beforehand.
 */
class ActionSheetAlert(
    private val title: String? = null,
    private val message: String? = null,
    private val confirmButtonTitle: String,
    private val cancelButtonTitle: String = "Cancel",
    private val isDestructive: Boolean = false,
) {
    // MARK: - Methods

    /**
     * Presents the sheet and suspends until the user makes a choice.
     *
     * @return `true` if the user confirms; otherwise, `false`.
     */
    suspend fun present(): Boolean =
        suspendCancellableCoroutine { continuation ->
            AlertPresenter.present(
                PresentedAlert.ActionSheet(
                    title = title,
                    message = message,
                    confirmButtonTitle = confirmButtonTitle,
                    cancelButtonTitle = cancelButtonTitle,
                    isDestructive = isDestructive,
                ) { result ->
                    AlertPresenter.dismiss()
                    if (continuation.isActive) continuation.resume(result)
                },
            )

            continuation.invokeOnCancellation { AlertPresenter.dismiss() }
        }
}
